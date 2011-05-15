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

import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcData;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.model.DeclareStep;
import com.xmlcalabash.model.Option;
import com.xmlcalabash.model.Output;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.model.Serialization;
import com.xmlcalabash.model.Step;
import com.xmlcalabash.model.Variable;
import com.xmlcalabash.util.TreeWriter;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;

import java.util.*;
import java.util.List; // Innovimax: new import
import java.util.ArrayList; // Innovimax: new import

import innovimax.quixproc.codex.util.Waiting;

import innovimax.quixproc.codex.util.DocumentCollector;
import innovimax.quixproc.codex.util.ParameterCollector;
import innovimax.quixproc.codex.util.OptionsCalculator;
import innovimax.quixproc.codex.util.VariablesCalculator;
import innovimax.quixproc.codex.util.ErrorHandler;
import innovimax.quixproc.util.SuicideException;

public class XPipeline extends XCompoundStep {
    private static final QName c_param_set = new QName("c", XProcConstants.NS_XPROC_STEP, "param-set");
    private static final QName c_param = new QName("c", XProcConstants.NS_XPROC_STEP, "param");
    private static final QName _name = new QName("name");
    private static final QName _namespace = new QName("namespace");
    private static final QName _value = new QName("value");

    private Hashtable<QName, RuntimeValue> optionsPassedIn = null;
    private boolean doPhoneHome = false;

    public XPipeline(XProcRuntime runtime, Step step, XCompoundStep parent) {
        super(runtime, step, parent);
    }

    public DeclareStep getDeclareStep() {
        return step.getDeclaration();
    }

    public void passOption(QName name, RuntimeValue value) {
        if (optionsPassedIn == null) {
            optionsPassedIn = new Hashtable<QName,RuntimeValue> ();
        }
        optionsPassedIn.put(name,value);
    }

    public Hashtable<QName,RuntimeValue> getInScopeOptions() {
        // We make a copy so that what our children do can't effect us
        Hashtable<QName,RuntimeValue> globals = new Hashtable<QName,RuntimeValue> ();
        if (inScopeOptions != null) {
            for (QName name : inScopeOptions.keySet()) {
                globals.put(name,inScopeOptions.get(name));
            }
        }

        // We also need to pass through any options passed in...
        if (optionsPassedIn != null) {
            for (QName name : optionsPassedIn.keySet()) {
                globals.put(name,optionsPassedIn.get(name));
            }
        }

        return globals;
    }

    public Set<String> getInputs() {
        HashSet<String> ports = new HashSet<String> ();
        for (String port : inputs.keySet()) {
            if (!port.startsWith("|")) {
                ports.add(port);
            }
        }
        return ports;
    }

    public void clearInputs(String port) {
        Vector<ReadablePipe> v = inputs.get(port);
        v.clear();
    }

    public void writeTo(String port, XdmNode node) {
        WritablePipe pipe = outputs.get(port+"|");
        finest(step.getNode(), "writesTo " + pipe + " for " + port);
        pipe.write(stepContext, node);
    }

    public Set<String> getOutputs() {
        HashSet<String> ports = new HashSet<String> ();
        for (String port : outputs.keySet()) {
            if (!port.endsWith("|")) {
                ports.add(port);
            }
        }
        return ports;
    }

    public ReadablePipe readFrom(String port) {
        ReadablePipe rpipe = null;
        XOutput output = getOutput(port);
        rpipe = output.getReader();
        rpipe.canReadSequence(true); // FIXME: I should be able to set this correctly!
        return rpipe;
    }

    public Serialization getSerialization(String port) {
        Output output = step.getOutput(port);
        return output.getSerialization();
    }

    // Innovimax: replaced/modified by gorun()
    //public void run() throws SaxonApiException {
    public void gorun() throws SaxonApiException {        
        QName infoName = XProcConstants.p_pipeline;
        /*
        if (!step.isAnonymous()) {
            infoName = step.getDeclaredType();
        }
        */

        info(null, "Running " + infoName + " " + step.getName());
        if (runtime.getAllowGeneralExpressions()) {
            info(step.getNode(), "Running with the 'general-values' extension enabled.");
        }
        // Innovimax: XProcData desactivated
        //XProcData data = runtime.getXProcData();
        //data.openFrame(this);
        
        runtime.start(this);
        try {            
            doRun();
        } catch (XProcException ex) {
            runtime.phoneHome(ex);
            runtime.error(ex);
            throw ex;
        } catch (SaxonApiException ex) {
            runtime.phoneHome(ex);
            runtime.error(ex);
            throw ex;
        }
        runtime.finish(this);

        // Innovimax: XProcData desactivated
        //data.closeFrame();
    }

    private void setupParameters() {
        Vector<String> ports = new Vector<String> ();
        Iterator<String> portIter = getParameterPorts().iterator();
        while (portIter.hasNext()) {
            ports.add(portIter.next());
        }

        for (String port : ports) {
            TreeWriter tree = new TreeWriter(runtime);

            tree.startDocument(step.getNode().getBaseURI());
            tree.addStartElement(c_param_set);
            tree.startContent();

            Iterator<QName> paramIter = getParameters(port).iterator();
            while (paramIter.hasNext()) {
                QName name = paramIter.next();

                String value = getParameter(port, name).getString();
                tree.addStartElement(c_param);
                tree.addAttribute(_name, name.getLocalName());
                if (name.getNamespaceURI() != null) {
                    tree.addAttribute(_namespace, name.getNamespaceURI());
                }
                tree.addAttribute(_value, value);
                tree.startContent();
                tree.addEndElement();
            }

            tree.addEndElement();
            tree.endDocument();
            
            writeTo(port,tree.getResult());            
        }
    }

    // Innovimax: modified function
    private void doRun() throws SaxonApiException {        
        // innovimax: statistics
        if (startTotalMem==0) { 
          startTotalMem = Runtime.getRuntime().totalMemory();  
          startedMemory = Runtime.getRuntime().freeMemory();
        }   
              
        // Innovimax: collect inputs                
        /*for (String port : inputs.keySet()) {
            if (!port.startsWith("|")) {
                String wport = port + "|";
                WritablePipe pipe = outputs.get(wport);
                for (ReadablePipe reader : inputs.get(port)) {
                    while (reader.moreDocuments(stepContext)) {
                        XdmNode doc = reader.read(stepContext);
                        pipe.write(stepContext, doc);
                        finest(step.getNode(), "Pipeline input copy from " + reader + " to " + pipe);
                    }
                }
            }
        }*/
        List<DocumentCollector> inCollectors = new ArrayList<DocumentCollector>();    
        for (String port : inputs.keySet()) {                 
            if (!port.startsWith("|") && inputs.get(port).size() > 0) {            
                String wport = port + "|";
                WritablePipe pipe = outputs.get(wport);                    
                DocumentCollector inCollector = new DocumentCollector(DocumentCollector.TYPE_INPUT, runtime, this, inputs.get(port), pipe, false); 
                inCollectors.add(inCollector);
                Thread tic = new Thread(errorHandler, inCollector); 
                runtime.getTracer().debug(this,null,-1,null,null,"PIPELINE > RUN COLLECT-INPUT THREAD '"+wport+"'");
                tic.start();                                     
            }
            // Innovimax: close missing pipe
            if (!port.startsWith("|") && inputs.get(port).size() == 0) {            
                closeMissingPipe(port);
            }
        }
        checkErrors();
        
        // collect parameters        
        //setupParameters();
        ParameterCollector parCollector = new ParameterCollector(runtime, this, inCollectors); 
        Thread tpc = new Thread(errorHandler, parCollector); 
        runtime.getTracer().debug(this,null,-1,null,null,"PIPELINE > RUN COLLECT-PARAM THREAD");
        tpc.start();                                     
        checkErrors();

        // N.B. At this time, there are no compound steps that accept parameters or options,
        // so the order in which we calculate them doesn't matter. That will change if/when
        // there are such compound steps.

        inScopeOptions = parent.getInScopeOptions();
        
        // Innovimax: calculate all the options        
        /*for (QName name : step.getOptions()) {
            Option option = step.getOption(name);
            RuntimeValue value = null;
            if (optionsPassedIn != null && optionsPassedIn.containsKey(name)) {
                value = optionsPassedIn.get(name);
            } else {
                if (option.getRequired() && option.getSelect() == null) {
                    throw XProcException.staticError(18, option.getNode(), "No value provided for required option \"" + option.getName() + "\"");
                }
                if (option.getSelect() == null) {
                    value = new RuntimeValue();
                } else {
                    value = computeValue(option);
                }
            }
            setOption(name, value);
            inScopeOptions.put(name, value);
        }*/        
        OptionsCalculator ocalculator = new OptionsCalculator(runtime,this,step,inScopeOptions,optionsPassedIn);
        ocalculator.exec(errorHandler);        
        checkErrors();

        // Innovimax: calculate all the variables   
        /*for (Variable var : step.getVariables()) {
            RuntimeValue value = computeValue(var);
            inScopeOptions.put(var.getName(), value);
        }*/
        VariablesCalculator vcalculator = new VariablesCalculator(runtime, this, step, inScopeOptions);
        vcalculator.exec(errorHandler);        
        checkErrors();
        
        for (XStep step : subpipeline) {            
            // Innovimax: run each step in thread
            //step.run();            
            step.stepContext = stepContext;            
            Thread ts = new Thread(errorHandler, step);
            runtime.getTracer().debug(this,null,-1,null,null,"PIPELINE > RUN STEP THREAD ["+step.getName()+"]"); 
            ts.start();                
        }        
        checkErrors();

        // Innovimax: collect outputs     
        /*for (String port : inputs.keySet()) {
            if (port.startsWith("|")) {
                String wport = port.substring(1);
                WritablePipe pipe = outputs.get(wport);
                for (ReadablePipe reader : inputs.get(port)) {
                    while (reader.moreDocuments(stepContext)) {
                        XdmNode doc = reader.read(stepContext);
                        pipe.write(stepContext, doc);
                        finest(step.getNode(), "Pipeline output copy from " + reader + " to " + pipe);
                    }
                }
                pipe.close(stepContext); // Indicate that we're done writing to it
            }
        }*/
        List<DocumentCollector> outCollectors = new ArrayList<DocumentCollector>();            
        for (String port : inputs.keySet()) {            
            if (port.startsWith("|") && inputs.get(port).size() > 0) {
                String wport = port.substring(1);
                WritablePipe pipe = outputs.get(wport);
                DocumentCollector outCollector = new DocumentCollector(DocumentCollector.TYPE_OUTPUT, runtime, this, inputs.get(port), pipe, true);                
                outCollectors.add(outCollector);
                Thread toc = new Thread(errorHandler, outCollector); 
                runtime.getTracer().debug(this,null,-1,null,null,"PIPELINE > RUN COLLECT-OUTPUT THREAD '"+wport+"'");
                toc.start();                   
            }
        }          
        checkErrors();
        
        // Innovimax: waiting before close frame        
        runtime.getWaiter().initialize(this,stepContext.curChannel,null,null,"PIPELINE > WAITING END OF STEPS..."); 
        while (subpipelineRunning() || collectorRunning(outCollectors)) {            
            checkErrors();
            runtime.getWaiter().check(true);
            // innovimax: statistics        
            long mem = Runtime.getRuntime().totalMemory() - startTotalMem + startedMemory - Runtime.getRuntime().freeMemory();
            if (mem>maximumMemory) { maximumMemory = mem; }              
            Thread.yield();
        }                                               
        runtime.getTracer().debug(this,null,-1,null,null,"PIPELINE > ALL STEPS TERMINATED");         
    }
    
    //*************************************************************************
    //*************************************************************************        
    //*************************************************************************
    // INNOVIMAX IMPLEMENTATION
    //*************************************************************************
    //*************************************************************************
    //*************************************************************************
    
    private ErrorHandler errorHandler = null; // Innovimax: new property
    
    // innovimax: statistics
    private static long startTotalMem = 0;  
    private static long startedMemory = 0;
    private static long maximumMemory = 0;     
    
    // Innovimax: new function
    public void exec() {      
        try {
            // set error handler            
            errorHandler = new ErrorHandler("pipeline");            
            Thread.setDefaultUncaughtExceptionHandler(errorHandler);
            // activate xpath functions
            XProcData data = runtime.getXProcData();
            data.openFrame(this);            
            // Innovimax: run pipeline            
            runtime.getTracer().debug(this,null,-1,null,null,"  PIPELINE > STARTED");
            gorun();                            
            runtime.getTracer().debug(this,null,-1,null,null,"  PIPELINE > TERMINATED");
        } catch (RuntimeException e) {
            runtime.getTracer().debug(this,null,-1,null,null,"  PIPELINE > TERMINATED ON ERROR");        
            throw e;
        } catch (Exception e) {          
            runtime.getTracer().debug(this,null,-1,null,null,"  PIPELINE > TERMINATED ON ERROR");        
            throw new RuntimeException(e);
        }               
    }       
    
    // Innovimax: new function
    private void checkErrors() {
      if (errorHandler != null) errorHandler.checkError();
    }    

    // Innovimax: new function
    private Thread instantiateThread(Runnable r) {    
      if (errorHandler != null) {
          return new Thread(errorHandler, r); 
      } else {
          return new Thread(r); 
      }
    }
    
    // Innovimax: new function
    public void collectParameters() throws SaxonApiException {    
        // collect parameters
        setupParameters();
        // close pipes
        closeCollectedPipes();        
    }

    // Innovimax: new function    
    private void closeMissingPipe(String port) {      
        WritablePipe pipe = outputs.get(port+"|");        
        pipe.close(stepContext);   
    }
    
    // Innovimax: new function
    private void closeCollectedPipes() throws SaxonApiException {    
        for (String port : inputs.keySet()) {            
            if (!port.startsWith("|") && inputs.get(port).size() > 0) {
                String wport = port + "|";
                WritablePipe pipe = outputs.get(wport);   
                pipe.close(stepContext);                
            }
        }        
    }    

    // Innovimax: new function    
    public void closeWrittenPipe(String port) {
        WritablePipe pipe = outputs.get(port+"|");            
        pipe.close(stepContext);        
    }    
    
    // Innovimax: new function
    public Object clone() {
        XPipeline clone = new XPipeline(runtime, step, parent);
        super.cloneStep(clone);              
        clone.cloneInstantiation(optionsPassedIn);
        return clone;
    }           
    
    // Innovimax: new function
    private void cloneInstantiation(Hashtable<QName, RuntimeValue> optionsPassedIn) {
        this.optionsPassedIn.putAll(optionsPassedIn);        
    }     
    
    // innovimax: statistics
    public static long getMaximumMemory() { return maximumMemory; }      
    public static void resetMaximumMemory() { maximumMemory = 0; }         
}
