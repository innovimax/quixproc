/*
QuiXProc: efficient evaluation of XProc Pipelines.
Copyright (C) 2011-2012 Innovimax
2008-2012 Mark Logic Corporation.
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
package innovimax.quixproc.codex.library;

import innovimax.quixproc.codex.util.EventReader;
import innovimax.quixproc.codex.util.MultiplexProcessor;
import innovimax.quixproc.codex.util.PipedDocument;
import innovimax.quixproc.codex.util.XPathMatcher;
import innovimax.quixproc.datamodel.MatchEvent;
import innovimax.quixproc.util.MatchHandler;
import net.sf.saxon.s9api.QName;

import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.Pipe;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.library.DefaultStep;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.runtime.XAtomicStep;


public class Filter extends DefaultStep implements MatchHandler {
    private static final QName _select = new QName("", "select");
    private ReadablePipe source = null;
    private Pipe result = null;      
    private RuntimeValue select = null;     
    private PipedDocument out = null;   
    private MultiplexProcessor mxProcess = null;       
    
    public Filter(XProcRuntime runtime, XAtomicStep step) {    
        super(runtime,step);               
    }
    
    public void setInput(String port, ReadablePipe pipe) {
        source = pipe;
    }

    public void setOutput(String port, WritablePipe pipe) {
        result = (Pipe)pipe;
    } 
    
    public void reset() {        
        // nop
    }      
   
    public void gorun() {                     
        select = getOption(_select);               
        
        try {                      
            mxProcess = new MultiplexProcessor(runtime, stepContext, source, result.documents(stepContext), "FILTER");                        
            EventReader evr = new EventReader(source.readAsStream(stepContext), null);                             
            XPathMatcher xmatch = new XPathMatcher(runtime.getProcessor(), runtime.getQConfig().getQuiXPath(), evr, this, select.getString(), true, !streamAll);                        
            Thread t = new Thread(xmatch);            
            runtime.getTracer().debug(step,null,-1,source,null,"  FILTER > RUN MATCH THREAD");                    
            running = true;  
            t.start();                                                     
        }         
        catch (Exception e) {            
            throw new XProcException(e);      
        }      
    }
    
    /** 	  
     * match handler interface
     */  
     
    public void startProcess() {        
        runtime.getTracer().debug(step,null,-1,source,null,"    MATCH > START THREAD");
    }
    
    public void endProcess() {
        runtime.getTracer().debug(step,null,-1,source,null,"    MATCH > END THREAD");
        if (!result.closed(stepContext)) {                  
            throw new XProcException("Thread concurrent error : unclosed filtered sequence");                   
        }          
        running = false;
    }    

    public void errorProcess(Exception e) {    
        if (e instanceof RuntimeException) {
            throw (RuntimeException)e;
        }
        throw new RuntimeException(e);        
    }  
  
    public void processEvent(MatchEvent match) { 
        try {            
            mxProcess.processEvent(match);     
        } catch (XProcException e) {            
            throw e;                         
        } catch (Exception e) {    
           e.printStackTrace();
            throw new XProcException(e); 
        }         
    }

}

