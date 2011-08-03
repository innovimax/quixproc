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
import com.xmlcalabash.io.Pipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.model.Step;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.model.Variable;
import com.xmlcalabash.model.Option;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.QName;

import java.util.Vector;
import java.util.List; // Innovimax: new import
import java.util.ArrayList; // Innovimax: new import
import java.util.Hashtable; // Innovimax: new import

import innovimax.quixproc.codex.util.PipedDocument;  
  
import innovimax.quixproc.codex.util.Waiting;  
import innovimax.quixproc.codex.util.DocumentCollector;  
import innovimax.quixproc.codex.util.ForEachCollector;  
import innovimax.quixproc.codex.util.VariablesCalculator;  
import innovimax.quixproc.codex.util.StepContext;  
import innovimax.quixproc.util.shared.ChannelList;  

public class XForEach extends XCompoundStep {    
    protected Pipe current = null;  // Innovimax: private changed to protected
    protected int sequencePosition = 0;  // Innovimax: private changed to protected
    protected int sequenceLength = 0; // Innovimax: private changed to protected

    public XForEach(XProcRuntime runtime, Step step, XCompoundStep parent) {
        super(runtime, step, parent);
    }

    public ReadablePipe getBinding(String stepName, String portName) {
        if (name.equals(stepName) && ("#current".equals(portName) || "current".equals(portName))) {
            if (current == null) {
                current = new Pipe(runtime);
            }
            return new Pipe(runtime,current.documents());
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
    
    // Innovimax: replaced/mofified by gorun()
    //public void run() throws SaxonApiException {
    public void gorun() throws SaxonApiException {  
        info(null, "Running p:for-each " + step.getName());

        // Innovimax: XProcData desactivated
        //XProcData data = runtime.getXProcData();
        //data.openFrame(this);

        if (current == null) {
            current = new Pipe(runtime);
        }

        String iport = "#iteration-source";

        sequencePosition = 0;
        sequenceLength = 0;

        inScopeOptions = parent.getInScopeOptions();
        
        // Innovimax: execute subpipeline in thread
        doRun(iport);     
               
        /* Innovimax: desactivated code
        // FIXME: Do I really have to do this? At the very least, only do it if we have to!
        Vector<XdmNode> nodes = new Vector<XdmNode> ();
        for (ReadablePipe is_reader : inputs.get(iport)) {
            while (is_reader.moreDocuments(stepContext)) {
                XdmNode is_doc = is_reader.read(stepContext);
                finest(step.getNode(), "Input copy from " + is_reader);
                finest(step.getNode(), is_doc.toString());
                nodes.add(is_doc);
                sequenceLength++;
            }
        }                

        runtime.getXProcData().setIterationSize(sequenceLength);
        
        for (XdmNode is_doc : nodes) {
            // Setup the current port before we compute variables!
            current.resetWriter(stepContext);
            current.write(stepContext, is_doc);            
            finest(step.getNode(), "Copy to current");

            sequencePosition++;
            runtime.getXProcData().setIterationPosition(sequencePosition);

            for (Variable var : step.getVariables()) {
                RuntimeValue value = computeValue(var);
                inScopeOptions.put(var.getName(), value);
            }            

            for (XStep step : subpipeline) {             
                step.run();                    
            }

            for (String port : inputs.keySet()) {
                if (port.startsWith("|")) {
                    String wport = port.substring(1);

                    boolean seqOk = step.getOutput(wport).getSequence();
                    int docsCopied = 0;

                    WritablePipe pipe = outputs.get(wport);
                    // The output of a for-each is a sequence, irrespective of what the output says
                    pipe.canWriteSequence(true);

                    for (ReadablePipe reader : inputs.get(port)) {
                        reader.canReadSequence(true); // Hack again!
                        while (reader.moreDocuments(stepContext)) {
                            XdmNode doc = reader.read(stepContext);
                            pipe.write(stepContext, doc);
                            docsCopied++;
                            finest(step.getNode(), "Output copy from " + reader + " to " + pipe);
                        }
                        reader.resetReader(stepContext);
                    }

                    if (docsCopied != 1 && !seqOk) {
                        throw XProcException.dynamicError(6);
                    }
                }
            }

            for (XStep step : subpipeline) {
                step.reset();
            }
        }

        for (String port : inputs.keySet()) {
            if (port.startsWith("|")) {
                String wport = port.substring(1);
                WritablePipe pipe = outputs.get(wport);
                pipe.close(stepContext); // Indicate that we're done
            }
        }

        data.closeFrame();*/
    }
    
    //*************************************************************************
    //*************************************************************************        
    //*************************************************************************
    // INNOVIMAX IMPLEMENTATION
    //*************************************************************************
    //*************************************************************************
    //*************************************************************************    
        
    protected XForEachSub xfeSub = null;  // Innovimax: new property       
    private int inCount = 0;  // Innovimax: new property       
    private int outCount = 0;  // Innovimax: new property       
    private int loopMax = 10;  // Innovimax: new property           
    private int loopCount = 0;  // Innovimax: new property          
    private int packet = 1000;  // Innovimax: new property     
    private int curCount = 0;  // Innovimax: new property                  
    private boolean trace = false;  // Innovimax: new property    
            
    // Innovimax: new function
    private void doRun(String iport) {
        // get extended attributes
        String xatt = getExtensionAttribute(XProcConstants.ix_loop);
        if (xatt != null) {            
            try { loopMax = new Integer(xatt.trim()).intValue(); } catch (Exception ignored) {}
        }     
        xatt = getExtensionAttribute(XProcConstants.ix_packet);
        if (xatt != null) {            
            try { packet = new Integer(xatt.trim()).intValue(); } catch (Exception ignored) {}
        }             
        xatt = getExtensionAttribute(XProcConstants.ix_trace);
        if (xatt != null) {            
            trace = xatt.trim().toLowerCase().equals("true");
        }          
      
        // collect current documents
        current.canWriteSequence(true);
        current.setWriter(stepContext, step);
        ChannelList channels = new ChannelList();
        ForEachCollector selCollector = new ForEachCollector(runtime, this, inputs.get(iport), current, channels);        
        Thread t = new Thread(selCollector);                         
        runtime.getTracer().debug(this,null,-1,null,null,"  FOREACH > RUN COLLECT-SELECT THREAD");            
        t.start();                    

        // run each subpipeline in thread
        xfeSub = new XForEachSub(this, parent, selCollector, current, channels);
        Thread t2 = new Thread(xfeSub);                         
        runtime.getTracer().debug(this,null,-1,null,null,"  FOREACH > RUN SUBPIPELINES THREAD");            
        t2.start();     
        
        // prepare outputs      
        for (String port : inputs.keySet()) {
            if (port.startsWith("|")) {
                String wport = port.substring(1);                        
                WritablePipe pipe = outputs.get(wport);                        
                // The output of a for-each is a sequence, irrespective of what the output says
                pipe.canWriteSequence(true);                        
                pipe.setWriter(stepContext, step);               
                for (ReadablePipe reader : inputs.get(port)) {
                    reader.canReadSequence(true); // Hack again!
                    reader.setReader(stepContext, step);
                }          
                pipe.addChannel(stepContext.curChannel);
            }
        }        

        // collect outputs        
        Waiting waiter = runtime.newWaiterInstance(this,stepContext.curChannel,null,null,"  FOREACH > WAITING NEXT OUTPUT COLLECTING...");         
        int index = 0;      
        while (selCollector.isRunning() || index < inCount) {                
            if (index < inCount) {                        
                int selChannel = channels.get(index++);                                                                                      
                for (String port : inputs.keySet()) {
                    if (port.startsWith("|")) {
                        String wport = port.substring(1);                        
                        WritablePipe pipe = outputs.get(wport);                                                
                        DocumentCollector outCollector = new DocumentCollector(DocumentCollector.TYPE_OUTPUT, runtime, this, inputs.get(port), pipe, selChannel, false);                        
                        outCollector.setEndAdvising();                        
                        Thread t3 = new Thread(outCollector); 
                        runtime.getTracer().debug(this,null,selChannel,null,null,"  FOREACH > RUN COLLECT-OUTPUT THREAD '"+wport+"'");
                        t3.start();                        
                    }
                }
            } else {  
                waiter.check();                  
            }
            Thread.yield();                     
        }        
        
        // waiting before close writed pipes
        waiter = runtime.newWaiterInstance(this,stepContext.curChannel,null,null,"  FOREACH > WAITING END OF COLLECTING..."); 
        while (inCount > outCount) {
            waiter.check();                     
            Thread.yield();
        }                   
        for (String port : inputs.keySet()) {
            if (port.startsWith("|")) {
                String wport = port.substring(1);
                WritablePipe pipe = outputs.get(wport);
                pipe.close(stepContext); // Indicate that we're done
            }
        }                   
        runtime.getTracer().debug(this,null,-1,null,null,"  FOREACH > OUPUT COLLECTING TERMINATED");  
        System.err.println(">>> MAXIMUM LOOP IS : "+loopMax);
        System.err.println(">>> MANAGED LOOP IS : "+loopCount);
        System.err.println(">>> PACKET SIZE IS  : "+packet);
    }
    
    // Innovimax: new function
    public boolean selectionPaused() {     
        if (packet > 0 && curCount >= packet) {
            if (inCount > outCount) {
                return true;          
            } else {
                curCount = 0;
                if (trace) System.err.println(">>> START NEW PACKET : "+inCount+"/"+outCount+":"+curCount); 
            }
        }           
        if (loopMax == 0) {
            return false;
        } else {       
            return (inCount - outCount) >= loopMax;            
        }
    }
        
    // Innovimax: new function     
    public void startSelectedDocument() {        
        inCount++;             
        int d = inCount - outCount;
        if (d > loopCount) {
            loopCount = d;            
        }
        if (trace) System.err.println(">>> START SELECTED DOCUMENT : "+inCount+"/"+outCount+":"+curCount);                   
    }                                  
    
    // Innovimax: new function     
    public void endSelectedDocument() {                
        curCount++;                       
        if (trace) System.err.println(">>> END SELECTED DOCUMENT   : "+inCount+"/"+outCount+":"+curCount);   
    }       
    
    // Innovimax: new function     
    public void endOutputCollecting() {        
        synchronized(this) {      
            outCount++;            
        }        
        if (trace) System.err.println(">>> END OUTPUT COLLECTOR    : "+inCount+"/"+outCount+":"+curCount); 
    }                                     
    
    // Innovimax: new internal class
    private class XForEachSub implements Runnable
    {        
        private XForEach foreach = null;
        private XStep parent = null;
        private ForEachCollector selCollector = null;
        private Pipe current;
        private ChannelList channels = null;
        private StepContext stepContext = null;      
        
        private boolean running = true;        
        
        public XForEachSub(XForEach foreach, XStep parent, ForEachCollector selCollector, Pipe current, ChannelList channels) {
            this.foreach = foreach;
            this.parent = parent;
            this.selCollector = selCollector;
            this.current = current;
            this.channels = channels;
            stepContext = foreach.getContext();
        }             
      
        public void run() {
            try {
                runtime.getTracer().debug(foreach,null,-1,null,null,"    SUBPIPELINE > START THREAD");       
                doRun();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {          
                throw new RuntimeException(e);
            }                                    
        }
        
        private void doRun() {                                           
            int index = 0;
            Waiting waiter = runtime.newWaiterInstance(foreach,stepContext.curChannel,null,null,"    SUBPIPELINE > BEGIN INPUT COLLECTING..."); 
            int size = channels.size();                 
            while (selCollector.isRunning() || index < size) {                
                if (index < size) {                    
                    int selChannel = channels.get(index++);                    
                    runtime.getTracer().debug(foreach,null,selChannel,null,null,"    SUBPIPELINE > NEW DOCUMENT ITERATION");
                                        
                    XForEach xfeClone = (XForEach)foreach.clone();                                            
                    xfeClone.setCurrentChannel(selChannel);     
                    xfeClone.setAlternateChannel(stepContext.curChannel);     
                    xfeClone.setIterationPosition(index);                                                            
                    //xfeClone.setIterationSize(size);  // Innovimax: don't know definitive size at this time                    
                    
                    // Calculate all the variables              
                    Hashtable<QName,RuntimeValue> inScopeOptions = parent.getInScopeOptions();                    
                    VariablesCalculator calculator = new VariablesCalculator(runtime, xfeClone, step, inScopeOptions);
                    calculator.exec();                                                             
                    xfeClone.setInScopeOptions(inScopeOptions);                    
                                                
                    for (XStep step : xfeClone.subpipeline) {
                        step.setCurrentChannel(selChannel);
                        step.setAlternateChannel(stepContext.curChannel);
                        step.setIterationPosition(xfeClone.getIterationPosition());
                        step.setIterationSize(xfeClone.getIterationSize());
                        Thread t2 = new Thread(step);                        
                        runtime.getTracer().debug(xfeClone,stepContext,selChannel,null,null,"    SUBPIPELINE > RUN STEP THREAD ["+step.getName()+"]");                                    
                        t2.start();                                    
                    } 
                } else {
                    waiter.check();
                }                                
                Thread.yield();                  
                size = channels.size(); 
            }            
            runtime.getTracer().debug(foreach,null,-1,null,null,"    SUBPIPELINE > INPUT COLLECTING TERMINATED");             
            running = false;
        }                                            
        
        public boolean isRunning() {
            return running;
        }                     
    }   
     
    // Innovimax: new function
    public Object clone() {
        XForEach clone = new XForEach(runtime, step, parent);
        super.cloneStep(clone);
        clone.cloneInstantiation(current, xfeSub);        
        return clone;
    }      
        
    // Innovimax: new function
    private void cloneInstantiation(Pipe current, XForEachSub xfeSub) {
        this.current = current;
        this.xfeSub = xfeSub;
    }          
    
}
