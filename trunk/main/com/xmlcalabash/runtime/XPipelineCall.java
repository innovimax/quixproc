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

import innovimax.quixproc.codex.util.OptionsCalculator;

import java.util.HashSet;

import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;

import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.model.DeclareStep;
import com.xmlcalabash.model.Parameter;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.model.Step;
// Innovimax: new import

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Oct 8, 2008
 * Time: 5:25:42 AM
 * To change this template use File | Settings | File Templates.
 */
public class XPipelineCall extends XAtomicStep {
    private DeclareStep decl = null;

    public XPipelineCall(XProcRuntime runtime, Step step, XCompoundStep parent) {
        super(runtime, step, parent);
        this.parent = parent;
    }

    public void setDeclaration(DeclareStep decl) {
        this.decl = decl;
    }

    public XCompoundStep getParent() {
        return parent;
    }

    // Innovimax: modified function
    public void gorun() throws SaxonApiException {
        fine(null, "Running " + step.getType());

        decl.setup();

        if (runtime.getErrorCode() != null) {
            throw new XProcException(runtime.getErrorCode(), runtime.getErrorMessage());
        }

        XRootStep root = new XRootStep(runtime);
        XPipeline newstep = new XPipeline(runtime, decl, root);
        // Innovimax: propagate step context
        newstep.stepContext = stepContext;         

        newstep.instantiate(decl);

        // Calculate all the options
        inScopeOptions = parent.getInScopeOptions();

        HashSet<QName> pipeOpts = new HashSet<QName> ();
        for (QName name : newstep.step.getOptions()) {
            pipeOpts.add(name);
        }

        // Innovimax: calculate all the options  
        /*for (QName name : step.getOptions()) {
            Option option = step.getOption(name);
            RuntimeValue value = computeValue(option);
            setOption(name, value);

            if (pipeOpts.contains(name)) {
                newstep.passOption(name, value);
            }

            inScopeOptions.put(name, value);
        }*/
        OptionsCalculator ocalculator = new OptionsCalculator(runtime,this,step,inScopeOptions,newstep,pipeOpts);
        ocalculator.exec();          

        for (QName name : step.getParameters()) {
            Parameter param = step.getParameter(name);
            RuntimeValue value = computeValue(param);
            newstep.setParameter(name, value);
        }

        for (String port : inputs.keySet()) {
            if (!port.startsWith("|")) {
                newstep.inputs.put(port, inputs.get(port));
            }
        }

        for (String port : outputs.keySet()) {
            if (!port.endsWith("|")) {
                newstep.outputs.put(port, outputs.get(port));
            }
        }

        // Innovimax: run in a thread
        //newstep.run();
        Thread t = new Thread(newstep);
        runtime.getTracer().debug(this,null,-1,null,null,"  PIPELINE CALL > RUN PIPELINE THREAD");        
        t.start();          

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
        XPipelineCall clone = new XPipelineCall(runtime, step, parent);
        super.cloneStep(clone); 
        clone.cloneInstantiation(decl);    
        return clone;
    }        
    
    // Innovimax: new function
    private void cloneInstantiation(DeclareStep decl) {
        this.decl = decl;        
    }     
    
}