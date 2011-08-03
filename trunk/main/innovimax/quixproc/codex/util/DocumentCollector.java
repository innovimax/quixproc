/*
QuiXProc: efficient evaluation of XProc Pipelines.
Copyright (C) 2011 Innovimax
2008-2011 Mark Logic Corporation.
Portions Copyright 2007 Sun Microsystems, Inc.
All rights reserved.

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 3
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/
package innovimax.quixproc.codex.util;

import innovimax.quixproc.datamodel.QuixEvent;
import innovimax.quixproc.util.CollectException;

import java.util.List;

import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.model.Step;
import com.xmlcalabash.runtime.XCompoundStep;
import com.xmlcalabash.runtime.XGroup;
import com.xmlcalabash.runtime.XStep;

public class DocumentCollector implements Runnable {    
    public static int TYPE_INPUT = 1; 
    public static int TYPE_OUTPUT = 2;      
    
    private int type = 0;
    private XProcRuntime runtime = null;  
    private XStep xstep = null;      
    private XStep xstop = null;  
    private Step step = null;          
    private List<ReadablePipe> in = null;    
    private WritablePipe out = null;
    private StepContext rContext = null;
    private StepContext wContext = null;   
    private boolean closeOut = false;    
    private EventReader reader = null;
    private PipedDocument document = null;    
    private boolean running = true;    
    private boolean locked = false;    
    private boolean cancel = false;    
    private boolean endAdvising = false;    
    
    public DocumentCollector(int type, XProcRuntime runtime, XStep xstep, List<ReadablePipe> in, WritablePipe out, int rChannel, boolean closeOut) {
        StepContext rContext = new StepContext(xstep.getContext());
        rContext.curChannel = rChannel;
        rContext.altChannel = rChannel;
        initialize(type, runtime, xstep, in, out, rContext, closeOut);
    }
    
    public DocumentCollector(int type, XProcRuntime runtime, XStep xstep, List<ReadablePipe> in, WritablePipe out, boolean closeOut) {    
        initialize(type, runtime, xstep, in, out, xstep.getContext(), closeOut);
    }
    
    private void initialize(int type, XProcRuntime runtime, XStep xstep, List<ReadablePipe> in, WritablePipe out, StepContext rContext, boolean closeOut) {    
        this. type = type;
        this.runtime = runtime;
        this.xstep = xstep;                            
        this.in = in;    
        this.out = out;
        this.rContext = rContext;           
        this.wContext = xstep.getContext(); 
        this.closeOut = closeOut; 
        if (xstep instanceof XGroup) locked = ((XGroup)xstep).outputLocked();
        if (locked) {
            runtime.getTracer().debug(xstep,null,-1,null,null,"    COLLECT-"+debugKey()+" > LOCK COLLECTING");        
        }    
    }
    
    public void setEndAdvising() {        
        endAdvising = true;
    }
    
    public void run() {                    
        try {             
           startProcess(); 
           out.addChannel(wContext.curChannel);
           for (ReadablePipe pipe : in) {                
               pipe.setReader(rContext, xstep.getStep());    
           }  
           if (xstep.isStreamed()) {
               EventReader reader = new EventReader(rContext, in, null);               
               while (reader.hasEvent()) {                             
                   checkStatus();
                   if (cancel) {
                       reader.nextEvent();
                   } else if (!locked) {
                       processEvent(reader.nextEvent(), reader.pipe());
                   }
                   Thread.yield();
               }                
               if (document != null && !document.isClosed()) {                                    
                  throw new XProcException("Thread concurrent error : unclosed collected document");                   
               }
           } else {
               for (ReadablePipe pipe : in) {  
                   while (pipe.moreDocuments(rContext)) {                                     
                       checkStatus();
                       if (cancel) {
                           pipe.read(rContext);
                       } else if (!locked) {
                           out.write(wContext, pipe.read(rContext));
                       }
                       Thread.yield();
                   }
               }                   
           }           
           if (closeOut && !cancel) {              
              out.close(wContext);                    
           }
           endProcess();            
           running = false;               
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {          
            throw new RuntimeException(e);
        }           
    }
    
    public boolean isRunning() {
        return running;
    }
    
    public WritablePipe getOutput() {
        return out;
    }    
    
    private String debugKey() {
        if (type == TYPE_INPUT) {       
            return "INPUT";
        } else if (type == TYPE_OUTPUT) {       
            return "OUTPUT";            
        }
        return null;        
    }
    
//    private String memoryKey() {
//        if (type == TYPE_INPUT) {       
//            return "input";
//        } else if (type == TYPE_OUTPUT) {       
//            return "output";            
//        }
//        return null;         
//    }    
    
    private void checkStatus() {
        if (locked) {            
            if (((XGroup)xstep).outputReleased()) {
                runtime.getTracer().debug(xstep,null,-1,null,null,"    COLLECT-"+debugKey()+" > UNLOCK COLLECTING");        
                locked = false;
            } else if (((XGroup)xstep).outputCanceled()) {
                runtime.getTracer().debug(xstep,null,-1,null,null,"    COLLECT-"+debugKey()+" > UNDO COLLECTING");        
                cancel = true;
            }
        }
    }
    
    /** 	  
     * collect handler interface
     */ 
     
    private void startProcess() {
        runtime.getTracer().debug(xstep,null,-1,null,null,"    COLLECT-"+debugKey()+" > START THREAD");    
    }
    
    private void endProcess() {
        runtime.getTracer().debug(xstep,null,-1,null,null,"    COLLECT-"+debugKey()+" > END THREAD");
        if (endAdvising) {
            if (type == TYPE_INPUT) {       
                ((XCompoundStep)xstep).endInputCollecting();
            } else {
                ((XCompoundStep)xstep).endOutputCollecting();
            }
        }
    }           
  
    private void processEvent(QuixEvent event, ReadablePipe in) { 
        try {            
            switch (event.getType()) {
                case START_SEQUENCE :
                    runtime.getTracer().debug(xstep,null,-1,in,null,"    COLLECT-"+debugKey()+" > START SEQUENCE");        
                    break;
                case END_SEQUENCE :
                    runtime.getTracer().debug(xstep,null,-1,in,null,"    COLLECT-"+debugKey()+" > END SEQUENCE");
                    break;              
                case START_DOCUMENT :
                    runtime.getTracer().debug(xstep,null,-1,in,null,"    COLLECT-"+debugKey()+" > START DOCUMENT");                     
                    document = out.newPipedDocument(wContext.curChannel);
                    document.append(event); 
                    break;
                case END_DOCUMENT :
                    runtime.getTracer().debug(xstep,null,-1,in,null,"    COLLECT-"+debugKey()+" > END DOCUMENT");   
                    document.append(event);    
                    document.close();                                     
                    break;
                default :
                    document.append(event);    
                    break;
            }                      
        } catch (Exception e) {            
            throw new CollectException(e); 
        }         
    } 
}

