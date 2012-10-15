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

import innovimax.quixproc.codex.util.VariablesCalculator;

import java.util.Iterator;

import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XPathCompiler;
import net.sf.saxon.s9api.XPathExecutable;
import net.sf.saxon.s9api.XPathSelector;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;

import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.Pipe;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.model.Step;
import com.xmlcalabash.model.Variable;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Oct 14, 2008
 * Time: 5:44:42 AM
 * To change this template use File | Settings | File Templates.
 */
public class XUntilUnchanged extends XCompoundStep {
    private static final QName doca = new QName("","doca");
    private static final QName docb = new QName("","docb");

    private Pipe current = null;
    private int sequencePosition = 0;
    private int sequenceLength = 0;

    public XUntilUnchanged(XProcRuntime runtime, Step step, XCompoundStep parent) {
        super(runtime, step, parent);
    }

    public ReadablePipe getBinding(String stepName, String portName) {
        if (name.equals(stepName) && ("#current".equals(portName) || "current".equals(portName))) {
            if (current == null) {
                current = new Pipe(runtime);
            }
            return new Pipe(runtime,current.documents(stepContext));
        } else {
            return super.getBinding(stepName, portName);
        }
    }

    protected void copyInputs() throws SaxonApiException {
        // nop;
    }

    public void reset() {
        super.reset();
        sequenceLength = 0;
        sequencePosition = 0;
    }

    // Innovimax: modified function
    public void gorun() throws SaxonApiException {
        fine(null, "Running cx:until-unchanged " + step.getName());

        // Innovimax: XProcData desactivated
        //XProcData data = runtime.getXProcData();
        //data.openFrame(this);

        if (current == null) {
            current = new Pipe(runtime);
        }

        String iport = "#iteration-source";

        sequencePosition = 0;
        sequenceLength = 1;

        inScopeOptions = parent.getInScopeOptions();

        // Innovimax: XProcData desactivated
        //runtime.getXProcData().setIterationSize(sequenceLength);

        String iPortName = null;
        String oPortName = null;
        for (String port : inputs.keySet()) {
            if (port.startsWith("|")) {
                iPortName = port;
                oPortName = port.substring(1);
            }
        }

        for (ReadablePipe is_reader : inputs.get(iport)) {
            XdmNode os_doc = null;

            while (is_reader.moreDocuments(stepContext)) {
                XdmNode is_doc = is_reader.read(stepContext);
                boolean changed = true;

                while (changed) {
                    // Setup the current port before we compute variables!
                    current.resetWriter(stepContext);
                    current.write(stepContext,is_doc);
                    finest(step.getNode(), "Copy to current");

                    sequencePosition++;
                    // Innovimax: XProcData desactivated
                    //runtime.getXProcData().setIterationPosition(sequencePosition);

                    for (Variable var : step.getVariables()) {
                        RuntimeValue value = computeValue(var);
                        inScopeOptions.put(var.getName(), value);
                    }

                    // N.B. At this time, there are no compound steps that accept parameters or options,
                    // so the order in which we calculate them doesn't matter. That will change if/when
                    // there are such compound steps.

                    // Calculate all the variables
                    inScopeOptions = parent.getInScopeOptions();
                    
                    // Innovimax: XProcData desactivated
                    /*for (Variable var : step.getVariables()) {
                        RuntimeValue value = computeValue(var);
                        inScopeOptions.put(var.getName(), value);
                    }*/
                    VariablesCalculator vcalculator = new VariablesCalculator(runtime, this, step, inScopeOptions);
                    vcalculator.exec();                      

                    for (XStep step : subpipeline) {
                        step.gorun();
                    }

                    int docsCopied = 0;

                    for (ReadablePipe reader : inputs.get(iPortName)) {
                        while (reader.moreDocuments(stepContext)) {
                            os_doc = reader.read(stepContext);
                            docsCopied++;
                        }
                        reader.resetReader(stepContext);
                    }

                    if (docsCopied != 1) {
                        throw XProcException.dynamicError(6);
                    }

                    for (XStep step : subpipeline) {
                        step.reset();
                    }

                    XPathCompiler xcomp = runtime.getProcessor().newXPathCompiler();
                    xcomp.declareVariable(doca);
                    xcomp.declareVariable(docb);

                    XPathExecutable xexec = xcomp.compile("deep-equal($doca,$docb)");
                    XPathSelector selector = xexec.load();

                    selector.setVariable(doca, is_doc);
                    selector.setVariable(docb, os_doc);

                    Iterator<XdmItem> values = selector.iterator();
                    XdmAtomicValue item = (XdmAtomicValue) values.next();
                    changed = !item.getBooleanValue();

                    is_doc = os_doc;
                }

                WritablePipe pipe = outputs.get(oPortName);
                pipe.write(stepContext,os_doc);
            }
        }

        for (String port : inputs.keySet()) {
            if (port.startsWith("|")) {
                String wport = port.substring(1);
                WritablePipe pipe = outputs.get(wport);
                pipe.close(stepContext); // Indicate that we're done
            }
        }

        // Innovimax: XProcData desactivated
        //data.closeFrame();
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
        XUntilUnchanged clone = new XUntilUnchanged(runtime, step, parent);
        super.cloneStep(clone);
        clone.cloneInstantiation(current);        
        return clone;
    }          
    
    // Innovimax: new function
    private void cloneInstantiation(Pipe current) {
        this.current = current;                
    }       
      
}