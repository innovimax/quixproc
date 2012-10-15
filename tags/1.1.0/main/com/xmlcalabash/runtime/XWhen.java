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
package com.xmlcalabash.runtime;

import innovimax.quixproc.codex.util.DocumentCollector;

import java.util.Hashtable;
import java.util.Vector;

import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;

import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.model.NamespaceBinding;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.model.Step;
import com.xmlcalabash.model.When;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Oct 13, 2008
 * Time: 4:57:48 PM
 * To change this template use File | Settings | File Templates.
 */
public class XWhen extends XCompoundStep {
    public XWhen(XProcRuntime runtime, Step step, XCompoundStep parent) {
          super(runtime, step, parent);
    }

    public boolean shouldRun() throws SaxonApiException {
        String testExpr = ((When) step).getTest();
        XdmNode doc = null;
        NamespaceBinding nsbinding = new NamespaceBinding(runtime, step.getNode());
        Hashtable<QName,RuntimeValue> globals = parent.getInScopeOptions();

        ReadablePipe reader = inputs.get("#xpath-context").firstElement();
        doc = reader.read(stepContext);

        if (reader.moreDocuments(stepContext) || inputs.get("#xpath-context").size() > 1) {
            throw XProcException.dynamicError(5);
        }

        // Surround testExpr with "boolean()" to force the EBV.
        Vector<XdmItem> results = evaluateXPath(doc, nsbinding.getNamespaceBindings(), "boolean(" + testExpr + ")", globals);

        if (results.size() != 1) {
            throw new XProcException("Attempt to compute EBV in p:when did not return a singleton!?");
        }

        XdmAtomicValue value = (XdmAtomicValue) results.get(0);
        return value.getBooleanValue();
    }

    // Innovimax: modified function
    protected void copyInputs() throws SaxonApiException {
        // Innovimax: collect inputs      
        /*for (String port : inputs.keySet()) {
            if (!port.startsWith("|") && !"#xpath-context".equals(port)) {
            String wport = port + "|";
                WritablePipe pipe = outputs.get(wport);
                for (ReadablePipe reader : inputs.get(port)) {
                    while (reader.moreDocuments(stepContext)) {
                        XdmNode doc = reader.read(stepContext);
                        pipe.write(stepContext,doc);
                        finest(step.getNode(), "Compound input copy from " + reader + " to " + pipe);
                    }
                }
            }
        }*/
        for (String port : inputs.keySet()) {            
            if (!port.startsWith("|") && !"#xpath-context".equals(port) && inputs.get(port).size() > 0) {
                String wport = port + "|";
                WritablePipe pipe = outputs.get(wport);                              
                DocumentCollector inCollector = new DocumentCollector(DocumentCollector.TYPE_INPUT, runtime, this, inputs.get(port), pipe, true); 
                Thread t = new Thread(inCollector); 
                runtime.getTracer().debug(this,null,-1,null,null,"  COMPOUND > RUN COLLECT-INPUT THREAD '"+wport+"'");
                t.start();                       
            }
        }         
    }   
    
    //*************************************************************************
    //*************************************************************************        
    //*************************************************************************
    // INNOVIMAX IMPLEMENTATION
    //*************************************************************************
    //*************************************************************************
    //*************************************************************************    
    
    // Innovimax: new function
    public Object clone() {
        XWhen clone = new XWhen(runtime, step, parent);
        super.cloneStep(clone);       
        return clone;
    }      
     
}
