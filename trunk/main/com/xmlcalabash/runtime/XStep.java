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

import com.xmlcalabash.core.XProcRunnable;
import com.xmlcalabash.core.XProcStep;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;

import java.util.logging.Logger;
import java.util.Hashtable;
import java.util.Set;
import java.util.HashSet;

import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.model.Step;
import com.xmlcalabash.model.Input;
import com.xmlcalabash.model.DeclareStep;


import innovimax.quixproc.codex.util.StepContext;

public abstract class XStep implements XProcRunnable 
{
    protected XProcRuntime runtime = null;
    protected Step step = null;
    protected String name = null;
    private Hashtable<String,XInput> inputs = new Hashtable<String,XInput> ();
    private Hashtable<String,XOutput> outputs = new Hashtable<String,XOutput> ();
    private Hashtable<QName, RuntimeValue> options = new Hashtable<QName, RuntimeValue> ();
    private Hashtable<String, Hashtable<QName, RuntimeValue>> parameters = new Hashtable<String, Hashtable<QName, RuntimeValue>> ();
    protected XCompoundStep parent = null;
    protected Logger logger = Logger.getLogger(this.getClass().getName());
    protected Hashtable<QName,RuntimeValue> inScopeOptions = new Hashtable<QName,RuntimeValue> ();

    // Innovimax: modified constructor
    public XStep(XProcRuntime runtime, Step step) {
        this.runtime = runtime;
        this.step = step;
        if (step != null) {
            name = step.getName();
        }
        // Innovimax: set step mode
        setStepMode();         
    }

    public XdmNode getNode() {
        return step.getNode();
    }

    public QName getType() {
        return step.getNode().getNodeName();
    }

    public String getName() {
        return name;
    }

    public DeclareStep getDeclareStep() {
        return step.getDeclaration();
    }

    public XCompoundStep getParent() {
        return parent;
    }

    public void addInput(XInput input) {
        String port = input.getPort();
        if (inputs.containsKey(port)) {
            throw new XProcException(input.getNode(), "Attempt to add output '" + port + "' port to the same step twice.");
        }
        inputs.put(port, input);
    }

    public void addOutput(XOutput output) {
        String port = output.getPort();
        if (outputs.containsKey(port)) {
            throw new XProcException(output.getNode(), "Attempt to add output '" + port + "' port to the same step twice.");
        }
        outputs.put(port, output);
    }

    public XInput getInput(String port) {
        if (inputs.containsKey(port)) {
            return inputs.get(port);
        } else {
            throw new XProcException(step.getNode(), "Attempt to get non-existant input '" + port + "' port from step.");
        }
    }

    public XOutput getOutput(String port) {
        if (outputs.containsKey(port)) {
            return outputs.get(port);
        } else {
            if (XProcConstants.NS_XPROC.equals(step.getType().getNamespaceURI())
                    && step.getStep().getVersion() > 1.0) {
                return null;
            } else {
                throw new XProcException(step.getNode(), "Attempt to get non-existant output '" + port + "' port from step.");
            }
        }
    }

    public void setParameter(QName name, RuntimeValue value) {
        Set<String> ports = getParameterPorts();
        int pportCount = 0;
        String pport = null;
        for (String port : ports) {
            pport = port;
            pportCount++;
        }

        if (pportCount == 0) {
            throw new XProcException(step.getNode(), "Attempt to set parameter but there's no parameter port.");
        }

        if (pportCount > 1) {
            throw new XProcException(step.getNode(), "Attempt to set parameter w/o specifying a port (and there's more than one)");
        }

        setParameter(pport, name, value);
    }

    public void setParameter(String port, QName name, RuntimeValue value) {
        Hashtable<QName,RuntimeValue> pparams;
        if (parameters.containsKey(port)) {
            pparams = parameters.get(port);
        } else {
            pparams = new Hashtable<QName,RuntimeValue> ();
            parameters.put(port, pparams);
        }

        if (pparams.containsKey(name)) {
            throw new XProcException(step.getNode(), "Duplicate parameter: " + name);
        }

        if (XProcConstants.NS_XPROC.equals(name.getNamespaceURI())) {
            throw XProcException.dynamicError(31);
        }

        pparams.put(name, value);

    }

    public Set<QName> getOptions() {
        return options.keySet();
    }

    public RuntimeValue getOption(QName name) {
        if (options.containsKey(name)) {
            return options.get(name);
        } else {
            return null;
        }
    }

    public void setOption(QName name, RuntimeValue value) {
        if (options.containsKey(name)) {
            throw new XProcException(step.getNode(), "Duplicate option: " + name);
        }
        options.put(name, value);
    }

    public void clearOptions() {
        options.clear();
    }

    public Set<QName> getParameters() {
        return getParameters("*");
    }

    public RuntimeValue getParameter(QName name) {
        Set<String> ports = getParameterPorts();
        int pportCount = 0;
        String pport = null;
        for (String port : ports) {
            pport = port;
            pportCount++;
        }

        if (pportCount != 1) {
            return null;
        }

        return getParameter(pport, name);
    }

    public Set<String> getParameterPorts() {
        HashSet<String> ports = new HashSet<String> ();
        for (Input input : step.inputs()) {
            if (input.getParameterInput()) {
                ports.add(input.getPort());
            }

        }
        return ports;
    }

    public Set<QName> getParameters(String port) {
        if (parameters.containsKey(port)) {
            return parameters.get(port).keySet();
        } else {
            return new HashSet<QName> ();
        }
    }

    public RuntimeValue getParameter(String port, QName name) {
        if (parameters.containsKey(port)) {
            Hashtable<QName,RuntimeValue> pparams = parameters.get(port);
            if (pparams.containsKey(name)) {
                return pparams.get(name);
            }
        }
        return null;
    }

    public String getExtensionAttribute(QName name) {
        if (step != null) {
            return step.getExtensionAttribute(name);
        } else {
            return null;
        }
    }

    public String getInheritedExtensionAttribute(QName name) {
        if (getExtensionAttribute(name) != null) {
            return getExtensionAttribute(name);
        }
        if (parent != null) {
            return parent.getInheritedExtensionAttribute(name);
        }
        return null;
    }

    public boolean hasInScopeVariableBinding(QName name) {
        if (inScopeOptions.containsKey(name)) {
            return true;
        }

        return getParent() == null ? false : getParent().hasInScopeVariableBinding(name);
    }

    public boolean hasInScopeVariableValue(QName name) {
        if (inScopeOptions.containsKey(name)) {
            RuntimeValue v = getOption(name);
            return v.initialized();
        }

        return getParent() == null ? false : getParent().hasInScopeVariableBinding(name);
    }

    public Hashtable<QName,RuntimeValue> getInScopeOptions() {
        // We make a copy so that what our children do can't effect us
        Hashtable<QName,RuntimeValue> globals = new Hashtable<QName,RuntimeValue> ();
        if (inScopeOptions != null) {
            for (QName name : inScopeOptions.keySet()) {
                globals.put(name,inScopeOptions.get(name));
            }
        }
        return globals;
    }

    public abstract RuntimeValue optionAvailable(QName optName);
    public abstract void instantiate(Step step);
    public abstract void reset();    
    // Innovimax: desactivated function
    //public abstract void run() throws SaxonApiException;   

    public void error(XdmNode node, String message, QName code) {
        runtime.error(this, node, message, code);
    }

    public void warning(XdmNode node, String message) {
        runtime.warning(this, node, message);
    }

    public void info(XdmNode node, String message) {
        runtime.info(this, node, message);
    }

    public void fine(XdmNode node, String message) {
        runtime.fine(this, node, message);
    }

    public void finer(XdmNode node, String message) {
        runtime.finer(this, node, message);
    }

    public void finest(XdmNode node, String message) {
        runtime.finest(this, node, message);
    }
    
    //*************************************************************************
    //*************************************************************************        
    //*************************************************************************
    // INNOVIMAX IMPLEMENTATION
    //*************************************************************************
    //*************************************************************************
    //*************************************************************************                
    
    private boolean streamed = true; // Innovimax: new property    
    private boolean running = true; // Innovimax: new property    
    protected long threadId = 0; // Innovimax: new property        
    protected StepContext stepContext = new StepContext(); // Innovimax: new property    
    
    // Innovimax: new constructor
    public XStep() {
      // nop
    }        
    
    // Innovimax: new function
    public void run() {      
        try {            
            threadId = Thread.currentThread().getId();                   
            step.setRunStep(stepContext.curChannel, this);
            runtime.getTracer().debug(this,null,-1,null,null,"  STEP > START THREAD");          
            gorun();           
            running = false;            
            runtime.getTracer().debug(this,null,-1,null,null,"  STEP > END THREAD");
        } catch (RuntimeException e) {            
            throw e;
        } catch (Exception e) {                      
            throw new RuntimeException(e);
        }           
    }    
    
    // Innovimax: new function
    public void gorun() throws SaxonApiException {}      

    // Innovimax: new function
    private void setStepMode() {    
        String mode = getExtensionAttribute(XProcConstants.ix_mode);                
        if (mode != null) {            
            streamed = !mode.equals("dom");
            if (!streamed) {
                runtime.getTracer().debug(this,null,-1,null,null,"  STEP > FORCE DOM MODE");
            }
        }     
        if (streamed && runtime.isDOMAll()) {
            runtime.getTracer().debug(this,null,-1,null,null,"  STEP > FORCE DOM MODE");
            streamed = false;
        }        
    }
    
    // Innovimax: new function
    public boolean isStreamed() {
        return streamed;
    }    
    
    // Innovimax: new function
    public void setStreamed(boolean streamed) {
        this.streamed = streamed;
    }     
    
    // Innovimax: new function
    public boolean isRunning() {      
        return running;
    }        
    
    // Innovimax: new function
    public void setRunning(boolean running) {      
        this.running = running;
    }        
    
    // Innovimax: new function
    public long threadId() {
        return threadId;
    }     
    
    // Innovimax: new function
    public Step getStep() {
        return step;
    }      
    
    // Innovimax: new function
    public void setParent(XCompoundStep parent) {
        this.parent = parent;
    }       
    
    // Innovimax: new function
    public void setInScopeOptions(Hashtable<QName,RuntimeValue> inScopeOptions) {
        this.inScopeOptions = inScopeOptions;
    }               
    
    // Innovimax: new function
    public StepContext getContext() {
        return stepContext;
    }    
    
    // Innovimax: new function
    public void setCurrentChannel(int channel) {
        stepContext.curChannel = channel;
        step.setRunStep(channel, this);
    }              
    
    // Innovimax: new function
    public void setAlternateChannel(int channel) {
        stepContext.altChannel = channel;
    }
    
    // Innovimax: new function
    public void setIterationPosition(int pos) {
        stepContext.iterationPos = pos;
    }
    
    // Innovimax: new function
    public int getIterationPosition() {
        return stepContext.iterationPos;
    }   
        
    // Innovimax: new function
    public void setIterationSize(int size) {
        stepContext.iterationSize = size;
    }        
    
    // Innovimax: new function
    public int getIterationSize() {
        return stepContext.iterationSize;
    }   
    
    // Innovimax: new function
    public Object clone() {
        return null;
    }     
   
    // Innovimax: new function
    public void cloneStep(XStep clone) {    
        clone.cloneInstantiation(inputs, outputs, options, parameters, inScopeOptions, threadId);        
    }        
    
    // Innovimax: new function
    private void cloneInstantiation(Hashtable<String,XInput> inputs, Hashtable<String,XOutput> outputs, Hashtable<QName, RuntimeValue> options, Hashtable<String, Hashtable<QName, RuntimeValue>> parameters, Hashtable<QName,RuntimeValue> inScopeOptions, long threadId) {
        this.inputs.putAll(inputs);
        this.outputs.putAll(outputs);
        this.options.putAll(options);
        this.parameters.putAll(parameters);    
        this.inScopeOptions.putAll(inScopeOptions);
        this.threadId = threadId;
    }                   
}
