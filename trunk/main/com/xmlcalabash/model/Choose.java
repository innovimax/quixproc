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

package com.xmlcalabash.model;

import java.util.HashSet;
import java.util.Hashtable;

import net.sf.saxon.s9api.XdmNode;

import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;

/**
 *
 * @author ndw
 */
public class Choose extends DeclareStep {
    /** Creates a new instance of Choose */
    public Choose(XProcRuntime xproc, XdmNode node, String name) {
        super(xproc, node, name);
        declaration = this;
        stepType = XProcConstants.p_choose;
    }

    public boolean isPipeline() {
        return false;
    }

    public DeclareStep getDeclaration() {
        return declaration;
    }

    public void addInput(Input input) {
        input.setStep(this);

        for (Input current : inputs) {
            if (current.getPort().equals(input.getPort())) {
                if ("#xpath-context".equals(input.getPort())) {
                    return; // It's ok for the xpath-context port to be specified in both places.
                } else {
                    throw XProcException.staticError(11, "Input port name '" + input.getPort() + "' appears more than once.");
                }
            }
        }

        inputs.add(input);
    }

    protected void augmentIO() {
        if (getInput("#xpath-context") == null) {
            Input isource = new Input(runtime, node);
            isource.setPort("#xpath-context");
            addInput(isource);
        }

        // Assume that everything will be OK when we validate...
        if (subpipeline.size() > 0) {
            Step step = subpipeline.get(0);
            for (Input input : step.inputs()) {
                Input cinput = new Input(runtime, step.getNode());
                cinput.setPort(input.getPort());
                cinput.setPrimary(input.getPrimary());
                cinput.setSequence(input.getSequence());
                addInput(cinput);
            }
            for (Output output : step.outputs()) {
                Output coutput = new Output(runtime, step.getNode());
                coutput.setPort(output.getPort());
                coutput.setPrimary(output.getPrimary());
                coutput.setSequence(output.getSequence());
                addOutput(coutput);
            }
        }

        super.augmentIO();
    }

    @Override
    public HashSet<String> getExcludeInlineNamespaces() {
        return ((DeclareStep) parent).getExcludeInlineNamespaces();
    }

    @Override
    protected void setEnvironment(Environment newEnvironment) {
        Environment env = new Environment(newEnvironment);
        patchEnvironment(env);
        super.setEnvironment(env);
        
        // Now what about my subpipeline
        for (Step step : subpipeline()) {
            Environment senv = new Environment(env);
            step.setEnvironment(senv);
        }
    }
    
    @Override
    protected boolean validBindings() {
        boolean valid = true;
       
        // First, make sure all the substeps have the same bindings
        Hashtable<String,Input> inputs = new Hashtable<String,Input>();
        Hashtable<String,Output> outputs = new Hashtable<String,Output>();
        Step step = null;
        
        if (subpipeline.size() > 0) {
            step = subpipeline.get(0);
            for (Input input : step.inputs()) {
                inputs.put(input.getPort(), input);
            }
            for (Output output : step.outputs()) {
                outputs.put(output.getPort(), output);
            }
            
            for (int pos = 1; pos < subpipeline.size(); pos++) {
                step = subpipeline.get(pos);
                for (Input input : step.inputs()) {
                    if (inputs.containsKey(input.getPort())) {
                        if ("#xpath-context".equals(input.getPort()) || input.getPort().startsWith("|")) {
                            // assume it's ok
                        } else {
                            Input s1input = inputs.get(input.getPort());
                            if (s1input.getPrimary() != input.getPrimary()) {
                                valid = false;
                                error("Input port " + input.getPort() + " has different primary status.", XProcConstants.staticError(7));
                            }
                        }
                    } else {
                        valid = false;
                        error("Input port " + input.getPort() + " is extra.", XProcConstants.staticError(7));
                    }
                }
                for (String port : inputs.keySet()) {
                    if (!port.startsWith("|") && !port.startsWith("#") && step.getInput(port) == null) {
                        valid = false;
                        error("Input port " + port + " missing.", XProcConstants.staticError(7));
                    }
                }
                for (Output output : step.outputs()) {
                    if (outputs.containsKey(output.getPort())) {
                        Output s1output = outputs.get(output.getPort());
                        Output chooseOut = getOutput(output.getPort());

                        // If any of the outputs can be a sequence, then the choose out can be a sequence
                        chooseOut.setSequence(chooseOut.getSequence() || output.getSequence());

                        if (output.getPort().endsWith("|") || output.getPort().startsWith("!")) {
                            // assume it's ok
                        } else {
                            if (s1output.getPrimary() != output.getPrimary()) {
                                valid = false;
                                error("Output port " + output.getPort() + " has different primary status.", XProcConstants.staticError(7));
                            }
                        }
                    } else {
                        valid = false;
                        error("Output port " + output.getPort() + " is extra.", XProcConstants.staticError(7));
                    }
                }
                for (String port : outputs.keySet()) {
                    if (!port.endsWith("|") && step.getOutput(port) == null) {
                        valid = false;
                        error("Output port " + port + " missing.", XProcConstants.staticError(7));
                    }
                }
            }
        } else {
            error("Choose must contain when or otherwise", XProcConstants.staticError(27));
        }

        return valid && super.validBindings();
    }

    protected boolean validOutputBinding() {
        // The output of choose is special.
        return true;
    }
}
