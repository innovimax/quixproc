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
package com.xmlcalabash.runtime;

import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcData;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.io.ReadableEmpty;
import com.xmlcalabash.model.*;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;

import java.util.Hashtable;
import java.util.Vector;
import java.util.List; // Innovimax: new import
import java.util.ArrayList; // Innovimax: new import

 
import innovimax.quixproc.codex.util.DocumentCollector; 
import innovimax.quixproc.codex.util.OptionsCalculator; 
import innovimax.quixproc.codex.util.VariablesCalculator;
import innovimax.quixproc.codex.util.Waiting;

public class XCompoundStep extends XAtomicStep {
    protected Hashtable<QName, RuntimeValue> variables = new Hashtable<QName,RuntimeValue> ();
    protected Vector<XStep> subpipeline = new Vector<XStep> ();

    public XCompoundStep(XProcRuntime runtime, Step step, XCompoundStep parent) {
        super(runtime, step, parent);
    }

    /*
    public void addVariable(QName name, RuntimeValue value) {
        variables.put(name, value);
    }
    */

    public boolean hasInScopeVariableBinding(QName name) {
        if (variables.containsKey(name) || inScopeOptions.containsKey(name)) {
            return true;
        }

        return getParent() == null ? false : getParent().hasInScopeVariableBinding(name);
    }

    public boolean hasInScopeVariableValue(QName name) {
        if (variables.containsKey(name) || inScopeOptions.containsKey(name)) {
            RuntimeValue v = getVariable(name);
            return v != null &&  v.initialized();
        }

        return getParent() == null ? false : getParent().hasInScopeVariableValue(name);
    }

    public RuntimeValue getVariable(QName name) {
        if (variables.containsKey(name)) {
            return variables.get(name);
        } else {
            if (inScopeOptions.containsKey(name)) {
                return inScopeOptions.get(name);
            } else {
                return null;
            }
        }
    }

    public ReadablePipe getBinding(String stepName, String portName) {
        if (name.equals(stepName)) {
            XInput input = getInput(portName);
            return input.getReader();
        }

        for (XStep step : subpipeline) {
            if (stepName.equals(step.getName())) {
                XOutput output = step.getOutput(portName);
                if (output == null) {
                    return new ReadableEmpty();
                } else {
                    ReadablePipe rpipe = output.getReader();
                    return rpipe;
                }
            }
        }
        return parent.getBinding(stepName, portName);
    }

    protected void addStep(XStep step) {
        subpipeline.add(step);
    }

    // Innovimax: modified function
    public void instantiate(Step step) {
        finest(step.getNode(), "--> instantiate " + step);
        
        instantiateReaders(step);
        parent.addStep(this);

        DeclareStep decl = step.getDeclaration();

        for (Step substep : decl.subpipeline()) {
            // Innovimax: check streamed step
            boolean streamed = true;
            String mode = substep.getExtensionAttribute(XProcConstants.ix_mode);                    
            if (mode != null) {                
                streamed = !mode.equals("dom");                 
            }         
            if (streamed && runtime.isDOMAll()) {                
                streamed = false;
            } 
                      
            if (XProcConstants.p_choose.equals(substep.getType())) {
                // Innovimax: instantiate appropriate class
                if (runtime.isStreamAll()) { 
                   throw new RuntimeException("Choose step not implemented in stream mode !");
                }                  
                XChoose newstep = new XChoose(runtime, substep, this);
                newstep.instantiate(substep);
            } else if (XProcConstants.p_group.equals(substep.getType())) {
                // Innovimax: instantiate appropriate class
                if (runtime.isStreamAll()) { 
                   throw new RuntimeException("Group step not implemented in stream mode !");
                }                  
                XGroup newstep = new XGroup(runtime, substep, this);
                newstep.instantiate(substep);
            } else if (XProcConstants.p_try.equals(substep.getType())) {
                // Innovimax: instantiate appropriate class
                if (runtime.isStreamAll()) { 
                   throw new RuntimeException("Try step not implemented in stream mode !");
                }                  
                XTry newstep = new XTry(runtime, substep, this);
                newstep.instantiate(substep);
            } else if (XProcConstants.p_catch.equals(substep.getType())) {
                // Innovimax: instantiate appropriate class
                if (runtime.isStreamAll()) { 
                   throw new RuntimeException("Catch step not implemented in stream mode !");
                }                  
                XCatch newstep = new XCatch(runtime, substep, this);
                newstep.instantiate(substep);
            } else if (XProcConstants.p_for_each.equals(substep.getType())) {
                XForEach newstep = new XForEach(runtime, substep, this);
                newstep.instantiate(substep);                         
            } else if (XProcConstants.p_viewport.equals(substep.getType())) {
                // Innovimax: instantiate appropriate class
                if (runtime.isStreamAll()) { 
                   throw new RuntimeException("Viewport step not implemented in stream mode !");
                }                
                XViewport newstep = new XViewport(runtime, substep, this);
                newstep.instantiate(substep);
            } else if (XProcConstants.cx_until_unchanged.equals(substep.getType())) {
                // Innovimax: instantiate appropriate class
                if (runtime.isStreamAll()) { 
                   throw new RuntimeException("UntilChanged step not implemented in stream mode !");
                }                
                XUntilUnchanged newstep = new XUntilUnchanged(runtime,substep,this);
                newstep.instantiate(substep);
            } else if (substep.isPipelineCall()) {
                DeclareStep subdecl = substep.getDeclaration();
                XPipelineCall newstep = new XPipelineCall(runtime, substep, this);
                newstep.setDeclaration(subdecl);
                newstep.instantiate(substep);

                /*
                // Make sure the caller's inputs and outputs have the right sequence values
                for (Input input : subdecl.inputs()) {
                    String port = input.getPort();
                    for (ReadablePipe rpipe : inputs.get(port)) {
                        rpipe.canReadSequence(input.getSequence());
                    }
                }

                for (Output output : subdecl.outputs()) {
                    String port = output.getPort();
                    WritablePipe wpipe = outputs.get(port);
                    wpipe.canWriteSequence(output.getSequence());
                }
                */
            } else {
                XAtomicStep newstep = new XAtomicStep(runtime, substep, this);
                newstep.instantiate(substep);
            }
        }

        for (Input input : step.inputs()) {
            String port = input.getPort();
            if (port.startsWith("|")) {
                Vector<ReadablePipe> readers = null;
                if (inputs.containsKey(port)) {
                    readers = inputs.get(port);
                } else {
                    readers = new Vector<ReadablePipe> ();
                    inputs.put(port, readers);
                }
                for (Binding binding : input.getBinding()) {
                    ReadablePipe pipe = getPipeFromBinding(binding);
                    pipe.canReadSequence(input.getSequence());
                    pipe.setReader(stepContext, step);
                    readers.add(pipe);
                    finest(step.getNode(), step.getName() + " reads from " + pipe + " for " + port);
                    
                    /* Attempted fix by ndw on 7 Dec...seems to work
                    if (binding.getBindingType() == Binding.PIPE_NAME_BINDING) {
                        PipeNameBinding pnbinding = (PipeNameBinding) binding;
                        ReadablePipe pipe = getBinding(pnbinding.getStep(), pnbinding.getPort());
                        pipe.canReadSequence(input.getSequence());
                        pipe.setReader(stepContext, step);
                        readers.add(pipe);
                        finest(step.getNode(), step.getName() + " reads from " + pipe + " for " + port);
                    } else {
                        throw new XProcException("Don't know how to handle binding " + binding.getBindingType());
                    }
                    */
                }

                XInput xinput = new XInput(runtime, input);
                addInput(xinput);
            }
        }

        for (Output output : step.outputs()) {
            String port = output.getPort();
            if (port.endsWith("|")) {
                String rport = port.substring(0,port.length()-1);
                XInput xinput = getInput(rport);
                WritablePipe wpipe = xinput.getWriter();
                wpipe.setWriter(stepContext, step);
                wpipe.canWriteSequence(true); // Let the other half work it out
                outputs.put(port, wpipe);
                finest(step.getNode(), step.getName() + " writes to " + wpipe + " for " + port);
            } else {
                XOutput xoutput = new XOutput(runtime, output);
                xoutput.setLogger(step.getLog(port));
                addOutput(xoutput);
                WritablePipe wpipe = xoutput.getWriter();
                wpipe.setWriter(stepContext, step);
                wpipe.canWriteSequence(output.getSequence());
                outputs.put(port, wpipe);
                finest(step.getNode(), step.getName() + " writes to " + wpipe + " for " + port);
            }
        }
    }

    // Innovimax: modified function
    protected void copyInputs() throws SaxonApiException {
        // Innovimax: collect inputs                                   
        /*for (String port : inputs.keySet()) {
            if (!port.startsWith("|")) {
                String wport = port + "|";
                WritablePipe pipe = outputs.get(wport);
                for (ReadablePipe reader : inputs.get(port)) {
                    while (reader.moreDocuments(stepContext)) {
                        XdmNode doc = reader.read(stepContext);
                        pipe.write(stepContext, doc);
                        finest(step.getNode(), "Compound input copy from " + reader + " to " + pipe);
                    }
                }
            }
        }*/
        for (String port : inputs.keySet()) {
            if (!port.startsWith("|") && inputs.get(port).size() > 0) {
                String wport = port + "|";
                WritablePipe pipe = outputs.get(wport);                              
                DocumentCollector inCollector = new DocumentCollector(DocumentCollector.TYPE_INPUT, runtime, this, inputs.get(port), pipe, true); 
                Thread t = new Thread(inCollector); 
                runtime.getTracer().debug(this,null,-1,null,null,"  COMPOUND > RUN COLLECT-INPUT THREAD '"+wport+"'");
                t.start();                       
            }
        }              
    }

    public void reset() {
        super.reset();
        for (XStep step : subpipeline) {
            step.reset();
        }
    }

    // Innovimax: replaced/modified by gorun()
    //public void run() throws SaxonApiException {
    public void gorun() throws SaxonApiException {
        // Innovimax: XProcData desactivated
        //XProcData data = runtime.getXProcData();
        //data.openFrame(this);
        
        copyInputs();        
        
        // N.B. At this time, there are no compound steps that accept parameters or options,
        // so the order in which we calculate them doesn't matter. That will change if/when
        // there are such compound steps.

        inScopeOptions = parent.getInScopeOptions();  

        // Innovimax: calculate all the options                        
        /*for (QName name : step.getOptions()) {
            Option option = step.getOption(name);
            RuntimeValue value = computeValue(option);
            setOption(name, value);
            inScopeOptions.put(name, value);
        }*/
        OptionsCalculator ocalculator = new OptionsCalculator(runtime, this, step, inScopeOptions);
        ocalculator.exec();         

        // Innovimax: calculate all the variables                    
        /*for (Variable var : step.getVariables()) {
            RuntimeValue value = computeValue(var);
            inScopeOptions.put(var.getName(), value);
        }*/
        VariablesCalculator vcalculator = new VariablesCalculator(runtime, this, step, inScopeOptions);
        vcalculator.exec();         

        for (XStep step : subpipeline) {            
            // Innovimax: run each step in thread
            //step.run();                        
            step.stepContext = stepContext;           
            Thread t = new Thread(step); 
            runtime.getTracer().debug(this,null,-1,null,null,"  COMPOUND > RUN STEP THREAD ["+step.getName()+"]");            
            t.start();             
        }

        // Innovimax: collect outputs                 
        /*for (String port : inputs.keySet()) {
            if (port.startsWith("|")) {
                String wport = port.substring(1);
                WritablePipe pipe = outputs.get(wport);
                for (ReadablePipe reader : inputs.get(port)) {
                    while (reader.moreDocuments(stepContext)) {
                        XdmNode doc = reader.read(stepContext);
                        pipe.write(stepContext, doc);
                        finest(step.getNode(), "Compound output copy from " + reader + " to " + pipe);
                    }
                }
            }
        }*/
        List<DocumentCollector> outCollectors = new ArrayList<DocumentCollector>();                                
        for (String port : inputs.keySet()) {            
            if (port.startsWith("|") && inputs.get(port).size() > 0) {
                String wport = port.substring(1);
                WritablePipe pipe = outputs.get(wport);
                DocumentCollector outCollector = new DocumentCollector(DocumentCollector.TYPE_OUTPUT, runtime, this, inputs.get(port), pipe, true);        
                outCollectors.add(outCollector);
                Thread t = new Thread(outCollector); 
                runtime.getTracer().debug(this,null,-1,null,null,"  COMPOUND > RUN COLLECT-OUTPUT THREAD '"+wport+"'");
                t.start();            
            }
        } 
        
        // Innovimax: waiting before close frame     
        runtime.getWaiter().initialize(this,stepContext.curChannel,null,null,"    COMPOUND > WAITING END OF STEPS...");    
        while (subpipelineRunning() || collectorRunning(outCollectors)) {
            runtime.getWaiter().check();
            Thread.yield();
        }   
        runtime.getTracer().debug(this,null,-1,null,null,"  COMPOUND > EXIT");                  

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
    protected boolean subpipelineRunning() {
        for (XStep step : subpipeline) {
            if (step.isRunning()) {
                return true;
            }
        }       
        return false; 
    }  

    // Innovimax: new function     
    protected boolean collectorRunning(List<DocumentCollector> collectors) {
        for (DocumentCollector collector : collectors) {
            if (collector.isRunning()) {
                return true;
            }
        }       
        return false; 
    }     
    
    // Innovimax: new function     
    public void endInputCollecting() {}    
    
    // Innovimax: new function     
    public void endOutputCollecting() {}        
    
    // Innovimax: new function
    public void cloneStep(XStep clone) { 
        super.cloneStep(clone);              
        ((XCompoundStep)clone).cloneInstantiation(variables, subpipeline);
    }        
    
    // Innovimax: new function
    private void cloneInstantiation(Hashtable<QName, RuntimeValue> variables, Vector<XStep> subpipeline) {
        for (QName qname : variables.keySet()) {
            RuntimeValue rv = variables.get(qname);
            runtime.getTracer().debug(this,null,-1,null,null,"  COMPOUND > CLONE VAR "+qname);            
            RuntimeValue clone = (RuntimeValue)rv.clone();            
            this.variables.put(qname, clone);            
        }        
        for (XStep step : subpipeline) {            
            XStep clone = (XStep)step.clone();
            runtime.getTracer().debug(this,null,-1,null,null,"  COMPOUND > CLONE STEP "+step.getName());            
            clone.parent = this;
            this.subpipeline.add(clone);            
        }        
    }                
}
