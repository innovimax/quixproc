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
import java.util.List;

import net.sf.saxon.s9api.QName;

import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.library.DefaultStep;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.runtime.XAtomicStep;

public class Insert extends DefaultStep implements MatchHandler {
    private static final QName _match = new QName("", "match");
    private static final QName _position = new QName("position");
    private ReadablePipe source = null;
    private ReadablePipe insertion = null;
    private WritablePipe result = null;      
    private RuntimeValue match = null; 
    private String position = null;      
    private PipedDocument out = null;  
    
    private QuixEvent startElement = null;
    private List<QuixEvent> attrs = new ArrayList<QuixEvent>();
          
    public Insert(XProcRuntime runtime, XAtomicStep step) {    
        super(runtime,step);
    }
    
    public void setInput(String port, ReadablePipe pipe) {
        if ("source".equals(port)) {
            source = pipe;
        } else {
            insertion = pipe;
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
        position = getOption(_position).getString();        
                
        try {             
            out = result.newPipedDocument(stepContext.curChannel);             
            EventReader evr = new EventReader(source.readAsStream(stepContext), null);                 
            XPathMatcher xmatch = new XPathMatcher(runtime.getProcessor(), runtime.getQConfig().getQuiXPath(), evr, this, match.getString(), false, !streamAll);
            Thread t = new Thread(xmatch);            
            runtime.getTracer().debug(step,null,-1,source,null,"  INSERT > RUN MATCH THREAD");                    
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
                        throw XProcException.stepError(25);
                    } else {
                        out.append(event); 
                    }
                    break;
                case END_DOCUMENT :
                    runtime.getTracer().debug(step,null,-1,source,null,"    MATCH > END DOCUMENT");        
                    if (match.isMatched()) {                
                        throw XProcException.stepError(25);
                    } else {
                        out.append(event); 
                    }                    
                    close = true;                 
                    break;
                case START_ELEMENT :
                    processStartElement();
                    if (match.isMatched()) {                                              
                        startElement = event;                        
                        attrs.clear();
                    } else {
                        out.append(event); 
                    }  
                    break;
                case END_ELEMENT :  
                    processStartElement();                
                    if (match.isMatched()) {
                        processEndElement(event);
                    } else {
                        out.append(event); 
                    }  
                    break;
                case ATTRIBUTE :  
                    if (match.isMatched()) {                
                        throw XProcException.stepError(23);
                    } else if (startElement!=null) {
                        attrs.add(event);
                    } else {
                        out.append(event);                       
                    }
                    break;
                case PI :  
                    processStartElement();
                    if (match.isMatched()) {
                        processPICommentText(event);
                    } else {
                        out.append(event); 
                    }    
                    break;
                case COMMENT :
                    processStartElement();
                    if (match.isMatched()) {
                        processPICommentText(event);
                    } else {
                        out.append(event); 
                    }     
                    break;
                case TEXT :
                    processStartElement();
                    if (match.isMatched()) {
                        processPICommentText(event);
                    } else {
                        out.append(event); 
                    }        
                    break;
            }
            //match.clear();            
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

    private void processStartElement() throws QuixException {    
        if (startElement!=null) {                       
            if ("before".equals(position)) {
                doInsert();                
            }            
            out.append(startElement);   
            for (QuixEvent attr : attrs) {
                out.append(attr);   
            }            
            if ("first-child".equals(position)) {
                doInsert();
            }      
            startElement = null;
            attrs.clear();
        }
    }
    
    private void processEndElement(QuixEvent event) throws QuixException {            
        if ("last-child".equals(position)) {
            doInsert();
        }
        out.append(event);    
        if ("after".equals(position)) {
            doInsert();
        }
    }
    
    private void processPICommentText(QuixEvent event) throws QuixException {        
        if ("first-child".equals(position) || "last-child".equals(position)) {
            throw XProcException.stepError(25);      
        }      
        if ("before".equals(position)) {
            doInsert();
        }
        out.append(event);    
        if ("after".equals(position)) {
            doInsert();
        }
    }    
    
    private void doInsert() throws QuixException {        
        EventReader evr = new EventReader(stepContext, insertion, null);                
        while (evr.hasEvent()) {           
            QuixEvent event = evr.nextEvent();            
            switch (event.getType()) {
                case START_DOCUMENT :
                    break;
                case END_DOCUMENT :               
                    break;
                default :
                    out.append(event);
                    break; 
            }            
            Thread.yield();    
        }  
        insertion.resetReader(stepContext);
    }    

}

