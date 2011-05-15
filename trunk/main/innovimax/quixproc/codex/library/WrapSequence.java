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
import innovimax.quixproc.datamodel.QuixEvent;
import innovimax.quixproc.util.ReadingException;
import net.sf.saxon.s9api.QName;

import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.library.DefaultStep;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.runtime.XAtomicStep;

public class WrapSequence extends DefaultStep {
    private static QName _wrapper = new QName("", "wrapper");
    private static QName _wrapper_prefix = new QName("", "wrapper-prefix");
    private static QName _wrapper_namespace = new QName("", "wrapper-namespace");
    private static QName _group_adjacent = new QName("", "group-adjacent");
    private ReadablePipe source = null;
    private WritablePipe result = null;
    private QName wrapper = null;
    private RuntimeValue groupAdjacent = null;      
    private PipedDocument out = null;    
    
    public WrapSequence(XProcRuntime runtime, XAtomicStep step) {    
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
        RuntimeValue wrapperNameValue = getOption(_wrapper);
        String wname = wrapperNameValue.getString();
        String wpfx = getOption(_wrapper_prefix, (String) null);
        String wns = getOption(_wrapper_namespace, (String) null);

        if (wpfx != null && wns == null) {
            throw XProcException.dynamicError(34, step.getNode(), "You can't specify a prefix without a namespace");
        }

        if (wns != null && wname.contains(":")) {
            throw XProcException.dynamicError(34, step.getNode(), "You can't specify a namespace if the wrapper name contains a colon");
        }
        
        if (wname.contains(":")) {
            QName qname = new QName(wname, wrapperNameValue.getNode());            
            wname = qname.getLocalName();
            wpfx = qname.getPrefix();
            wns = qname.getNamespaceURI();
        } 

        out = result.newPipedDocument(stepContext.curChannel);            
        out.append(QuixEvent.getStartDocument(step.getNode().getBaseURI().toString()));         
        out.append(QuixEvent.getStartElement(wname, wns, wpfx));         

        groupAdjacent = getOption(_group_adjacent);

        if (groupAdjacent != null) {
            runAdjacent();
        } else {
            runSimple();
        }   
        
        out.append(QuixEvent.getEndElement(wname, wns, wpfx));         
        out.append(QuixEvent.getEndDocument(step.getNode().getBaseURI().toString())); 
        out.close();
        result.close(stepContext); 
    }
        
    private void runSimple() {              
        try {                                                                 
            EventReader reader = new EventReader(stepContext, source, null);
            while (reader.hasEvent()) {              
               processEvent(reader.nextEvent(), reader.pipe());
               Thread.yield();
           }              
        } 
        catch (Exception e) {            
            throw new XProcException(e);      
        }
    }
    
    private void runAdjacent() {
        // Innovimax: need an implementation
        throw new ReadingException("WrapSequence.runAdjacent() is not implemented !");
    }    
    
    /** 	  
     * reading handler interface
     */ 
     
    private void processEvent(QuixEvent event, ReadablePipe in) throws ReadingException { 
        try {            
            switch (event.getType()) {
                case START_SEQUENCE :
                    runtime.getTracer().debug(step,null,-1,in,null,"  WRAP > START SEQUENCE");        
                    break;
                case END_SEQUENCE :
                    runtime.getTracer().debug(step,null,-1,in,null,"  WRAP > END SEQUENCE");
                    break;              
                case START_DOCUMENT :
                    runtime.getTracer().debug(step,null,-1,in,null,"  WRAP > START DOCUMENT"); 
                    break;
                case END_DOCUMENT :
                    runtime.getTracer().debug(step,null,-1,in,null,"  WRAP > END DOCUMENT");                                    
                    break;
                default :
                    out.append(event);    
                    break;
            }                      
        } catch (Exception e) {            
            throw new ReadingException(e); 
        }         
    }     

}

