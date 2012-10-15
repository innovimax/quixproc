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

import innovimax.quixproc.codex.util.StepContext;
import innovimax.quixproc.codex.util.VariablesCalculator;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;

import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.Pipe;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.model.Step;
import com.xmlcalabash.model.Viewport;
import com.xmlcalabash.util.ProcessMatch;
import com.xmlcalabash.util.ProcessMatchingNodes;
import com.xmlcalabash.util.TreeWriter;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Oct 15, 2008
 * Time: 7:03:35 AM
 * To change this template use File | Settings | File Templates.
 */
public class XViewport extends XCompoundStep implements ProcessMatchingNodes {
    private Pipe current = null;
    private ProcessMatch matcher = null;
    private int sequencePosition = 0;
    private int sequenceLength = 0;

    public XViewport(XProcRuntime runtime, Step step, XCompoundStep parent) {
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
        fine(null, "Running p:viewport " + step.getName());

        // Innovimax: XProcData desactivated
        //XProcData data = runtime.getXProcData();
        //data.openFrame(this);

        if (current == null) {
            current = new Pipe(runtime);
        }

        RuntimeValue match = ((Viewport) step).getMatch();

        String iport = "#viewport-source";
        ReadablePipe vsource = null;

        if (inputs.get(iport).size() != 1) {
            throw XProcException.dynamicError(3);
        } else {
            vsource = inputs.get(iport).get(0);
        }

        XdmNode doc = vsource.read(stepContext);
        if (doc == null || vsource.moreDocuments(stepContext)) {
            throw XProcException.dynamicError(3);
        }
        
        matcher = new ProcessMatch(runtime, this);

        // FIXME: Only do this if we really need to!
        sequenceLength = matcher.count(doc, match, false);

        // Innovimax: XProcData desactivated
        //runtime.getXProcData().setIterationSize(sequenceLength);
        setIterationSize(sequenceLength);              
        
        matcher.match(doc, match);        

        for (String port : inputs.keySet()) {
            if (port.startsWith("|")) {
                String wport = port.substring(1);
                WritablePipe pipe = outputs.get(wport);
                XdmNode result = matcher.getResult();
                pipe.write(stepContext,result);
                finest(step.getNode(), "Viewport output copy from matcher to " + pipe);
                // Innovimax: close pipe
                pipe.close(stepContext);                 
            }
        }        
    }

    public boolean processStartDocument(XdmNode node) {
        return true;
    }

    public void processEndDocument(XdmNode node) {
        // nop
    }

    // Innovimax: modified function
    public boolean processStartElement(XdmNode node) {
        final int LOOP_NUMBER = 500000;        
        // Use a TreeWriter to make the matching node into a proper document
        TreeWriter treeWriter = new TreeWriter(runtime);
        treeWriter.startDocument(node.getBaseURI());
        treeWriter.addSubtree(node);
        treeWriter.endDocument();

        current.resetWriter(stepContext);        
        current.write(stepContext,treeWriter.getResult());        
        // Innovimax: close pipe
        current.close(stepContext);                

        finest(step.getNode(), "Viewport copy matching node to " + current);

        sequencePosition++;
        // Innovimax: XProcData desactivated
        //runtime.getXProcData().setIterationPosition(sequencePosition);        
        setIterationPosition(sequencePosition); 

        // Calculate all the variables
        inScopeOptions = parent.getInScopeOptions();
        // Innovimax: calculate all variables        
        /*for (Variable var : step.getVariables()) {
            RuntimeValue value = computeValue(var);

            if ("p3".equals(var.getName().getLocalName())) {
                System.err.println("DEBUG ME1: " + value.getString());
            }
=
            inScopeOptions.put(var.getName(), value);
        }*/
        VariablesCalculator vcalculator = new VariablesCalculator(runtime, this, step, inScopeOptions);
        vcalculator.exec();         

        try {
            for (XStep step : subpipeline) {
                step.reset();
                // Innovimax: set step context
                step.stepContext = new StepContext(stepContext);                 
                step.gorun();                 
            }
        } catch (SaxonApiException sae) {
            throw new XProcException(sae);
        }
        
        try {
            int count = 0;
            for (String port : inputs.keySet()) {
                if (port.startsWith("|")) {
                    for (ReadablePipe reader : inputs.get(port)) {
                        while (reader.moreDocuments(stepContext)) {
                            count++;

                            if (count > 1) {
                                XOutput output = getOutput(port.substring(1));
                                if (!output.getSequence()) {
                                    throw XProcException.dynamicError(7);
                                }
                            }                            
                            XdmNode doc = reader.read(stepContext);                            
                            matcher.addSubtree(doc);
                        }
                        reader.resetReader(stepContext);
                    }
                }
            }
        } catch (SaxonApiException sae) {
            throw new XProcException(sae);
        }        
        return false;
    }

    public void processEndElement(XdmNode node) {
        // nop
    }

    public void processText(XdmNode node) {
        throw new UnsupportedOperationException("Can't run a viewport over text, PI, or comments");
    }

    public void processComment(XdmNode node) {
        throw new UnsupportedOperationException("Can't run a viewport over text, PI, or comments");
    }

    public void processPI(XdmNode node) {
        throw new UnsupportedOperationException("Can't run a viewport over text, PI, or comments");
    }

    public void processAttribute(XdmNode node) {
        throw new UnsupportedOperationException("Can't run a viewport over attributes");
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
        XViewport clone = new XViewport(runtime, step, parent);
        super.cloneStep(clone);
        clone.cloneInstantiation(current, matcher);
        return clone;
    }       

    // Innovimax: new function
    private void cloneInstantiation(Pipe current, ProcessMatch matcher) {
        this.current = current;        
        this.matcher = matcher;        
    }      
    
}
