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
package innovimax.quixproc.codex.library;

import innovimax.quixproc.codex.util.EventReader;
import innovimax.quixproc.codex.util.PipedDocument;
import innovimax.quixproc.codex.util.XPathMatcher;
import innovimax.quixproc.datamodel.MatchEvent;
import innovimax.quixproc.datamodel.QuixEvent;
import innovimax.quixproc.util.MatchHandler;
import net.sf.saxon.s9api.QName;

import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.library.DefaultStep;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.runtime.XAtomicStep;


public class Rename extends DefaultStep implements MatchHandler {
    private static final QName _match = new QName("", "match");
    private static final QName _new_name = new QName("", "new-name");
    private static final QName _new_prefix = new QName("", "new-prefix");
    private static final QName _new_namespace = new QName("", "new-namespace");
    private ReadablePipe source = null;
    private WritablePipe result = null;      
    private RuntimeValue match = null;
    private String newName = null;       
    private String newPrefix = null;       
    private String newNS = null;       
    private PipedDocument out = null;    
    
    public Rename(XProcRuntime runtime, XAtomicStep step) {    
        super(runtime,step);
    }
    
    public void setInput(String port, ReadablePipe pipe) {
        source = pipe;
    }

    public void setOutput(String port, WritablePipe pipe) {
        result = pipe;
    } 
    
    public void reset() {        
        // nop
    }      
   
    public void gorun() {             
        match = getOption(_match);  
        RuntimeValue name = getOption(_new_name);
        newName = name.getString();                
        newPrefix = getOption(_new_prefix, (String) null);
        newNS = getOption(_new_namespace, (String) null);
        
        if (newPrefix != null && newNS == null) {
            throw XProcException.dynamicError(34, "You can't specify a prefix without a namespace");
        }
        
        if (newNS != null && newName.contains(":")) {
            throw XProcException.dynamicError(34, "You can't specify a namespace if the new-name contains a colon");
        }
        
        if (newName.contains(":")) {
            QName qname = new QName(newName, name.getNode());            
            newName = qname.getLocalName();
            newPrefix = qname.getPrefix();
            newNS = qname.getNamespaceURI();          
        }
                
        try {                        
            out = result.newPipedDocument(stepContext.curChannel);             
            EventReader evr = new EventReader(source.readAsStream(stepContext), null);               
            XPathMatcher xmatch = new XPathMatcher(runtime.getQConfig().getQuiXPath(), evr, this, match.getString(), false);
            Thread t = new Thread(xmatch);            
            runtime.getTracer().debug(step,null,-1,source,null,"  RENAME > RUN MATCH THREAD");        
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
                    break;
                case END_DOCUMENT :
                    runtime.getTracer().debug(step,null,-1,source,null,"    MATCH > END DOCUMENT");        
                    close = true;                 
                    break;
                case START_ELEMENT :
                    if (match.isMatched()) {
                        event = QuixEvent.getStartElement(newName, newNS == null ? event.asStartElement().getURI() : newNS, newPrefix == null ? event.asStartElement().getPrefix() : newPrefix);     
                    }
                    break;
                case END_ELEMENT :  
                    if (match.isMatched()) {
                        event = QuixEvent.getEndElement(newName, newNS == null ? event.asEndElement().getURI() : newNS, newPrefix == null ? event.asEndElement().getPrefix() : newPrefix);     
                    }
                    break;
                case ATTRIBUTE :  
                    if (match.isMatched()) {
                        event = QuixEvent.getAttribute(newName, event.asAttribute().getURI(), event.asAttribute().getValue());     
                    }
                    break;
                case PI :  
                    if (!newNS.equals("")) {
                        throw XProcException.stepError(13);                        
                    }               
                    if (match.isMatched()) {
                        event = QuixEvent.getPI(newName, event.asPI().getData());     
                    }
                    break;
                case COMMENT :
                case TEXT :
            }
            //match.clear();
            out.append(event);            
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

}

