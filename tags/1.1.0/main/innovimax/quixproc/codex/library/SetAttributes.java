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
import innovimax.quixproc.codex.util.PipedDocument;
import innovimax.quixproc.codex.util.XPathMatcher;
import innovimax.quixproc.datamodel.MatchEvent;
import innovimax.quixproc.datamodel.QuixEvent;
import innovimax.quixproc.datamodel.QuixException;
import innovimax.quixproc.util.MatchHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.saxon.s9api.QName;

import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.library.DefaultStep;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.runtime.XAtomicStep;

public class SetAttributes extends DefaultStep implements MatchHandler {
   
    private static final QName _match = new QName("", "match");
    
    private ReadablePipe source = null;
    private ReadablePipe attributes = null;
    private WritablePipe result = null; 
    private RuntimeValue match = null;    
    private String attrName = null;       
    private String attrValue = null;       
    private String attrPrefix = null;       
    private String attrNS = null;                  
    private PipedDocument out = null;    
    private boolean matched = false;    
        
    private List<QuixEvent>newAttrs = new ArrayList<QuixEvent>();
    private Map<javax.xml.namespace.QName, QuixEvent> oldAttrs = new HashMap<javax.xml.namespace.QName, QuixEvent>();    
    
    public SetAttributes(XProcRuntime runtime, XAtomicStep step) {    
      super(runtime,step);
    }
    
    public void setInput(String port, ReadablePipe pipe) {
        if ("source".equals(port)) {
            source = pipe;
        } else {
            attributes = pipe;
        }  
    }

    public void setOutput(String port, WritablePipe pipe) {
        result = pipe;
    } 
    
    public void reset() {        
        // nop
    }       
   
    public void gorun() {             
       match = getOption(_match);        
               
       try {       
           loadAttributes();
           out = result.newPipedDocument(stepContext.curChannel);             
           EventReader evr = new EventReader(source.readAsStream(stepContext), null);                 
           XPathMatcher xmatch = new XPathMatcher(runtime.getProcessor(), runtime.getQConfig().getQuiXPath(), evr, this, match.getString(), false, !streamAll);
           Thread t = new Thread(xmatch);                       
           runtime.getTracer().debug(step,null,-1,source,null,"  SETATT > RUN MATCH THREAD");        
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
        if (!out.isClosed()) {                  
            throw new XProcException("Thread concurrent error : unclosed renamed document");                   
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
            boolean close = false;
            QuixEvent event = match.getEvent();
            switch (event.getType()) {
                case START_DOCUMENT :
                    runtime.getTracer().debug(step,null,-1,source,null,"    MATCH > START DOCUMENT"); 
                    if (match.isMatched()) {
                        throw XProcException.stepError(23);
                    }                    
                    break;
                case END_DOCUMENT :
                    runtime.getTracer().debug(step,null,-1,source,null,"    MATCH > END DOCUMENT");        
                    if (match.isMatched()) {
                        throw XProcException.stepError(23);
                    }                        
                    close = true;                 
                    break;
                case START_ELEMENT :
                    setAttributes();                    
                    if (match.isMatched()) {
                        matched = true;
                        oldAttrs.clear();
                    }
                    break;
                case END_ELEMENT :  
                    setAttributes();                    
                    break;
                case ATTRIBUTE :  
                    if (match.isMatched()) {
                        throw XProcException.stepError(23);
                    } else if (matched) {                         
                        oldAttrs.put(event.asAttribute().getQName(), event);                                       
                    }
                    break;
                case PI :  
                    setAttributes();
                    if (match.isMatched()) {
                        throw XProcException.stepError(23);
                    }
                    break;                   
                case COMMENT :
                    setAttributes();
                    if (match.isMatched()) {
                        throw XProcException.stepError(23);
                    }
                    break;
                case TEXT :
                    setAttributes();
                    if (match.isMatched()) {
                        throw XProcException.stepError(23);
                    }
                    break;
            }
            //match.clear();
            if (event != null) { 
                out.append(event); 
            }
            if (close) { 
                out.close(); 
            }
        } catch (XProcException e) {            
            throw e;                         
        } catch (Exception e) {    
           e.printStackTrace();
            throw new XProcException(e); 
        }         
    }
    
    private void loadAttributes() throws QuixException {
        boolean loading = true;
        EventReader evr = new EventReader(stepContext, attributes, null);                
        while (evr.hasEvent()) {           
            QuixEvent event = evr.nextEvent();            
            switch (event.getType()) {
                case START_DOCUMENT :                    
                    break;                
                case START_ELEMENT :               
                    break;                                    
                case ATTRIBUTE :     
                    if (loading) {
                        newAttrs.add(event);          
                    }
                    break;                        
                default :
                    loading = false;
                    break; 
            }            
            Thread.yield();    
        }        
    }    
    
    private void setAttributes() {        
        for (QuixEvent attrEvent : newAttrs) {
            oldAttrs.put(attrEvent.asAttribute().getQName(), attrEvent);
        }            
        for (QuixEvent attrEvent : oldAttrs.values()) {        
            out.append(attrEvent);            
        }
    }

}

