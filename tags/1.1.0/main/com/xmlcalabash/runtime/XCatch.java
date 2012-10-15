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

import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;

import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.Pipe;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.model.Binding;
import com.xmlcalabash.model.Step;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Oct 13, 2008
 * Time: 7:44:46 PM
 * To change this template use File | Settings | File Templates.
 */
public class XCatch extends XCompoundStep {
    Pipe errorPipe = null;

    public XCatch(XProcRuntime runtime, Step step, XCompoundStep parent) {
          super(runtime, step, parent);
    }

    public void writeError(XdmNode doc) {
        errorPipe.write(stepContext,doc);
    }

    protected ReadablePipe getPipeFromBinding(Binding binding) {
        if (binding.getBindingType() == Binding.ERROR_BINDING) {
            errorPipe = new Pipe(runtime);
            return errorPipe;
        } else {
            return super.getPipeFromBinding(binding);
        }
    }

    public ReadablePipe getBinding(String stepName, String portName) {
        if (name.equals(stepName) && "error".equals(portName)) {
            return new Pipe(runtime,errorPipe.documents(stepContext));
        } else {
            return super.getBinding(stepName, portName);
        }
    }

    protected void copyInputs() throws SaxonApiException {
        for (String port : inputs.keySet()) {
            if (!port.startsWith("|") && !"error".equals(port)) {
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
        }
    }

    public void reset() {
        super.reset();
        errorPipe.resetReader(stepContext);
        errorPipe.resetWriter(stepContext);
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
        XCatch clone = new XCatch(runtime, step, parent);
        super.cloneStep(clone);
        return clone;
    }      
       
}
