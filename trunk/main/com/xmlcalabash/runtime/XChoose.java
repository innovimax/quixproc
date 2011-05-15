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
import com.xmlcalabash.io.ReadableEmpty;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.Pipe;
import com.xmlcalabash.model.*;

import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcData;

import java.util.Vector;

import innovimax.quixproc.codex.util.OptionsCalculator; 
 

public class XChoose extends XCompoundStep {
    public XChoose(XProcRuntime runtime, Step step, XCompoundStep parent) {
          super(runtime, step, parent);
    }

    public void instantiate(Step step) {
        instantiateReaders(step);
        parent.addStep(this);

        DeclareStep decl = step.getDeclaration();

        for (Step substep : decl.subpipeline()) {
            if (XProcConstants.p_when.equals(substep.getType())) {
                XWhen newstep = new XWhen(runtime, substep, this);
                newstep.instantiate(substep);
            } else if (XProcConstants.p_otherwise.equals(substep.getType())) {
                XOtherwise newstep = new XOtherwise(runtime, substep, this);
                newstep.instantiate(substep);
            } else {
                throw new XProcException(step.getNode(), "This can't happen, can it? choose contains something that isn't a when or an otherwise?");
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

    // Innovimax: modified
    public ReadablePipe getBinding(String stepName, String portName) {
        if (name.equals(stepName) && "#xpath-context".equals(portName)) {
            // FIXME: Check that .get(0) works, and that there's no sequence
            Vector<ReadablePipe> xpc = inputs.get("#xpath-context");
            if (xpc.size() == 0) {
                // If there's no binding for a p:choose, the default is an empty binding...
                return new ReadableEmpty();
            }
            ReadablePipe pipe = xpc.get(0);  
            // Innovimax: initialize pipe                        
            pipe.initialize(stepContext);              
            return new Pipe(runtime, pipe.documents());
        } else {
            return super.getBinding(stepName, portName);
        }
    }


    // Innovimax: replaced/modified by gorun()
    //public void run() throws SaxonApiException {
    public void gorun() throws SaxonApiException {  
        // N.B. At this time, there are no compound steps that accept parameters or options,
        // so the order in which we calculate them doesn't matter. That will change if/when
        // there are such compound steps.
        
        // Don't reset iteration-position and iteration-size
        // Innovimax: XProcData desactivated
        //XProcData data = runtime.getXProcData();      
        //int ipos = data.getIterationPosition();
        //int isize = data.getIterationSize();
        //data.openFrame(this);
        // Innovimax: iteration position & size
        //data.setIterationPosition(ipos);
        //data.setIterationSize(isize);

        inScopeOptions = parent.getInScopeOptions();
        
        // Innovimax: calculate all the options
        /*for (Variable var : step.getVariables()) {
            RuntimeValue value = computeValue(var);
            inScopeOptions.put(var.getName(), value);
        }*/
        OptionsCalculator ocalculator = new OptionsCalculator(runtime, this, step, inScopeOptions);
        ocalculator.exec();                  

        XCompoundStep xstep = null;
        for (XStep step : subpipeline) {
            // Innovimax: set step context            
            step.stepContext = stepContext;            
        
            if (step instanceof XWhen) {
                XWhen when = (XWhen) step;
                if (when.shouldRun()) {
                    xstep = when;
                    break;
                }
            } else {
                // Must be an otherwise
                xstep = (XOtherwise) step;
                break;
            }
        }

        if (xstep == null) {
            throw XProcException.dynamicError(4);
        }

        for (String port : inputs.keySet()) {
            if (!port.startsWith("|") && !"#xpath-context".equals(port)) {
                xstep.inputs.put(port, inputs.get(port));
            }
        }

        for (String port : outputs.keySet()) {
            if (!port.endsWith("|")) {
                xstep.outputs.put(port, outputs.get(port));
            }
        }

        // Innovimax: run replaced by gorun
        //xstep.run();                
        xstep.gorun();
        
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
        XChoose clone = new XChoose(runtime, step, parent);                        
        super.cloneStep(clone);
        return clone;
    }     
}
