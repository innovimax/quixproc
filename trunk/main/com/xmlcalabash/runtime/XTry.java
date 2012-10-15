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

import innovimax.quixproc.codex.util.ErrorHandler;
import innovimax.quixproc.codex.util.VariablesCalculator;

import java.util.Vector;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerException;

import net.sf.saxon.Configuration;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.trans.XPathException;

import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.model.DeclareStep;
import com.xmlcalabash.model.Output;
import com.xmlcalabash.model.Step;
import com.xmlcalabash.util.TreeWriter;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Oct 13, 2008
 * Time: 7:40:35 PM
 * To change this template use File | Settings | File Templates.
 */
// Innovimax: modified implementation
//public class XTry extends XCompoundStep {
public class XTry extends XCompoundStep implements ErrorListener {
    private static final QName c_errors = new QName("c", XProcConstants.NS_XPROC_STEP, "errors");
    private Vector<XdmNode> errors = new Vector<XdmNode> ();

    public XTry(XProcRuntime runtime, Step step, XCompoundStep parent) {
          super(runtime, step, parent);
    }

    public void instantiate(Step step) {
        parent.addStep(this);

        DeclareStep decl = step.getDeclaration();

        for (Step substep : decl.subpipeline()) {
            if (XProcConstants.p_group.equals(substep.getType())) {
                XGroup newstep = new XGroup(runtime, substep, this);
                newstep.instantiate(substep);
            } else if (XProcConstants.p_catch.equals(substep.getType())) {
                XCatch newstep = new XCatch(runtime, substep, this);
                newstep.instantiate(substep);
            } else {
                throw new XProcException(step.getNode(), "This can't happen, can it? try contains something that isn't a group or a catch?");
            }
        }

        for (Output output : step.outputs()) {
            String port = output.getPort();
            if (port.endsWith("|")) {
                String rport = port.substring(0,port.length()-1);
                XInput xinput = getInput(rport);
                WritablePipe wpipe = xinput.getWriter();
                outputs.put(port, wpipe);
                finest(step.getNode(), " writes to " + wpipe + " for " + port);
            } else {
                XOutput xoutput = new XOutput(runtime, output);
                addOutput(xoutput);
                WritablePipe wpipe = xoutput.getWriter();
                outputs.put(port, wpipe);
                finest(step.getNode(), " writes to " + wpipe + " for " + port);
            }
        }
    }

    // Innovimax: modified function
    public void gorun() throws SaxonApiException {

        inScopeOptions = parent.getInScopeOptions();
        
        // Innovimax: calculate all variables       
        /*for (Variable var : step.getVariables()) {
            RuntimeValue value = computeValue(var);
            inScopeOptions.put(var.getName(), value);
        }*/
        VariablesCalculator vcalculator = new VariablesCalculator(runtime, this, step, inScopeOptions);
        vcalculator.exec();        

        XGroup xgroup = (XGroup) subpipeline.get(0);

        for (String port : inputs.keySet()) {
            if (!port.startsWith("|")) {
                xgroup.inputs.put(port, inputs.get(port));
            }
        }

        for (String port : outputs.keySet()) {
            if (!port.endsWith("|")) {
                xgroup.outputs.put(port, outputs.get(port));
            }
        }

        try {
            // Innovimax set saxon error handler
            Processor processor = runtime.getConfiguration().getProcessor();
            Configuration saxonConfig = processor.getUnderlyingConfiguration();
            saxonErrorListener = saxonConfig.getErrorListener();
            saxonConfig.setErrorListener(this);          
            // Innovimax : set error handler            
            errorHandler = new ErrorHandler("try"+Thread.currentThread().getId());            
            // Innovimax: set group status            
            xgroup.lockOutput();
            // Innovimax: run step in a thread and wait
            //xgroup.gorun();
            Thread t = new Thread(errorHandler, xgroup);             
            runtime.getTracer().debug(this,null,-1,null,null,"  TRY > RUN STEP THREAD ["+xgroup.getName()+"]");            
            t.start();
            while (xgroup.isRunning()) {              
                errorHandler.checkError(); 
                if (!xgroup.subpipelineRunning()) {
                    // Innovimax: set group status                    
                    xgroup.releaseOutput();
                }
                Thread.yield();
            }             
            runtime.getTracer().debug(this,null,-1,null,null,"  TRY > GROUP PASSED CORRECTLY");   
        } catch (Exception xe) {
            // Innovimax set saxon error handler
            Processor processor = runtime.getConfiguration().getProcessor();
            Configuration saxonConfig = processor.getUnderlyingConfiguration();      
            saxonConfig.setErrorListener(saxonErrorListener);                      
            // Innovimax: set group status            
            xgroup.cancelOutput();   
                      
            TreeWriter treeWriter = new TreeWriter(runtime);
            treeWriter.startDocument(step.getNode().getBaseURI());
            treeWriter.addStartElement(c_errors);
            treeWriter.startContent();

            // Innovimax : use try saxon error listener
            //for (XdmNode doc : runtime.getXProcData().errors()) {
            for (XdmNode doc : saxonErrors) {
                treeWriter.addSubtree(doc);
            }

            for (XdmNode doc : errors) {
                treeWriter.addSubtree(doc);
            }

            treeWriter.addEndElement();
            treeWriter.endDocument();

            XCatch xcatch = (XCatch) subpipeline.get(1);

            xcatch.writeError(treeWriter.getResult());

            for (String port : inputs.keySet()) {
                if (!port.startsWith("|")) {
                    xcatch.inputs.put(port, inputs.get(port));
                }
            }

            for (String port : outputs.keySet()) {
                if (!port.endsWith("|")) {
                    xcatch.outputs.put(port, outputs.get(port));
                }
            }

            xcatch.gorun();
        }
    }

    public void reportError(XdmNode doc) {
        errors.add(doc);
    }
    
    //*************************************************************************
    //*************************************************************************        
    //*************************************************************************
    // INNOVIMAX IMPLEMENTATION
    //*************************************************************************
    //*************************************************************************
    //*************************************************************************     
       
    private ErrorHandler errorHandler = null; // Innovimax: new property
    private ErrorListener saxonErrorListener = null; // Innovimax: new property    
    private Vector<XdmNode> saxonErrors = new Vector<XdmNode> (); // Innovimax: new property 
    private static QName c_error = new QName(XProcConstants.NS_XPROC_STEP, "error"); // Innovimax: new property
    private static QName _name = new QName("", "name"); // Innovimax: new property
    private static QName _type = new QName("", "type"); // Innovimax: new property
    private static QName _href = new QName("", "href"); // Innovimax: new property
    private static QName _line = new QName("", "line"); // Innovimax: new property
    private static QName _column = new QName("", "column"); // Innovimax: new property
    private static QName _code = new QName("", "code"); // Innovimax: new property
    
    // Innovimax: new function
    public void error(TransformerException exception) throws TransformerException {
        report("error", exception);        
    }

    // Innovimax: new function
    public void fatalError(TransformerException exception) throws TransformerException {
        report("fatal-error", exception);        
    }

    // Innovimax: new function
    public void warning(TransformerException exception) throws TransformerException {
        report("warning", exception);        
    }

    // Innovimax: new function
    private void report(String type, TransformerException exception) {
        TreeWriter writer = new TreeWriter(runtime);

        writer.startDocument(runtime.getStaticBaseURI());
        writer.addStartElement(c_error);
        writer.addAttribute(_type, type);

        StructuredQName qCode = null;
        if (exception instanceof XPathException) {
            XPathException xxx = (XPathException) exception;
            qCode = xxx.getErrorCodeQName();
            //qCode = ((XPathException) exception).getErrorCodeQName();
        }
        if (qCode == null && exception.getException() instanceof XPathException) {
            qCode = ((XPathException) exception.getException()).getErrorCodeQName();
        }
        if (qCode != null) {
            writer.addAttribute(_code, qCode.getDisplayName());
        }

        if (exception.getLocator() != null) {
            SourceLocator loc = exception.getLocator();
            boolean done = false;
            while (!done && loc == null) {
                if (exception.getException() instanceof TransformerException) {
                    exception = (TransformerException) exception.getException();
                    loc = exception.getLocator();
                } else if (exception.getCause() instanceof TransformerException) {
                    exception = (TransformerException) exception.getCause();
                    loc = exception.getLocator();
                } else {
                    done = true;
                }
            }

            if (loc != null) {
                if (loc.getSystemId() != null && !"".equals(loc.getSystemId())) {
                    writer.addAttribute(_href, loc.getSystemId());
                }

                if (loc.getLineNumber() != -1) {
                    writer.addAttribute(_line, ""+loc.getLineNumber());
                }

                if (loc.getColumnNumber() != -1) {
                    writer.addAttribute(_column, ""+loc.getColumnNumber());
                }
            }
        }


        writer.startContent();
        writer.addText(exception.toString());
        writer.addEndElement();
        writer.endDocument();

        XdmNode node = writer.getResult();

        saxonErrors.add(node);
    }    
            
    // Innovimax: new function
    public Object clone() {
        XTry clone = new XTry(runtime, step, parent);
        super.cloneStep(clone);       
        clone.cloneInstantiation(errors);        
        return clone;
    }       
    
    // Innovimax: new function
    private void cloneInstantiation(Vector<XdmNode> errors) {
        this.errors.addAll(errors);                
    }     
 
}
