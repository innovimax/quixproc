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
import innovimax.quixproc.util.shared.ChannelList;

import java.util.List;

import net.sf.saxon.s9api.XdmNode;

import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.Pipe;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.model.Step;
import com.xmlcalabash.runtime.XForEach;

public class ForEachCollector implements Runnable {        
    private XProcRuntime runtime = null;  
    private XForEach forStep = null;      
    private Step step = null;          
    private List<ReadablePipe> in = null;        
    private Pipe out = null;
    private ChannelList channels = null;    
    private StepContext rContext = null;
    private StepContext wContext = null;    
    private EventReader reader = null;
    private PipedDocument document = null;    
    private boolean running = true;    
    private boolean docStarted = false;
    
    public ForEachCollector(XProcRuntime runtime, XForEach forStep, List<ReadablePipe> in, Pipe out, ChannelList channels) {        
        this.runtime = runtime;
        this.forStep = forStep;                            
        this.in = in;            
        this.out = out;
        this.channels = channels;
        rContext = new StepContext(forStep.getContext());  
        rContext.altChannel = rContext.curChannel;
        wContext = new StepContext(forStep.getContext());  
    }    
    
    public void run() {                    
        try {                   
           startProcess();           
           if (forStep.isStreamed()) {               
               for (ReadablePipe pipe : in) { 
                   pipe.setReader(rContext, forStep.getStep());    
               }                 
               EventReader reader = new EventReader(rContext, in, null);                                           
               while (reader.hasEvent()) {                              
                   if (docStarted || !forStep.selectionPaused()) {
                       processEvent(reader.nextEvent(), reader.pipe());
                   } else {
                       //System.err.println(">>> SELECTION PAUSED");
                   }
                   Thread.yield();
               }   
               if (document != null && !document.isClosed()) {                  
                   throw new XProcException("Thread concurrent error : unclosed selected document");                   
               } 
           } else {
               for (ReadablePipe pipe : in) {
                   while (pipe.moreDocuments(rContext)) {
                       if (!forStep.selectionPaused()) {
                           runtime.getTracer().debug(forStep,null,-1,pipe,null,"    COLLECT-SELECT > WRITE DOCUMENT");
                           XdmNode doc = pipe.read(rContext);                       
                           int wChannel = out.nextChannel();
                           channels.add(wChannel);                           
                           forStep.startSelectedDocument();
                           wContext.curChannel = wChannel;
                           wContext.altChannel = wChannel;
                           out.write(wContext, doc);                       
                           out.close(wContext); 
                           forStep.endSelectedDocument();
                        }
                   }                   
               }                   
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
    
    /** 	  
     * collect handler interface
     */ 
     
    private void startProcess() {
        runtime.getTracer().debug(forStep,null,-1,null,null,"    COLLECT-SELECT > START THREAD");    
    }
    
    private void endProcess() {
        runtime.getTracer().debug(forStep,null,-1,null,null,"    COLLECT-SELECT > END THREAD");
    }           
  
    private void processEvent(QuixEvent event, ReadablePipe in) throws CollectException { 
        try {            
            switch (event.getType()) {
                case START_SEQUENCE :
                    runtime.getTracer().debug(forStep,null,-1,in,null,"    COLLECT-SELECT > START SEQUENCE");                            
                    break;
                case END_SEQUENCE :
                    runtime.getTracer().debug(forStep,null,-1,in,null,"    COLLECT-SELECT > END SEQUENCE");
                    break;              
                case START_DOCUMENT :                      
                    runtime.getTracer().debug(forStep,null,-1,in,null,"    COLLECT-SELECT > START DOCUMENT");                                     
                    int wChannel = out.nextChannel();
                    channels.add(wChannel);   
                    docStarted = true;
                    forStep.startSelectedDocument();                    
                    wContext.curChannel = wChannel;
                    wContext.altChannel = wChannel;
                    document = out.newPipedDocument(wChannel);
                    document.append(event);                     
                    break;
                case END_DOCUMENT :
                    runtime.getTracer().debug(forStep,null,-1,in,null,"    COLLECT-SELECT > END DOCUMENT");   
                    document.append(event);    
                    document.close();                     
                    out.close(wContext);       
                    docStarted = false;
                    forStep.endSelectedDocument();             
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

