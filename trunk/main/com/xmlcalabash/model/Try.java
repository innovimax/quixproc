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

package com.xmlcalabash.model;

import java.util.Hashtable;

import net.sf.saxon.s9api.XdmNode;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcConstants;

public class Try  extends DeclareStep {
    
    /** Creates a new instance of Try */
    public Try(XProcRuntime xproc, XdmNode node, String name) {
        super(xproc, node, name);
        declaration = this;
        stepType = XProcConstants.p_try;
    }

    public boolean isPipeline() {
        return false;
    }

    public DeclareStep getDeclaration() {
        return declaration;
    }

    @Override
    protected void augmentIO() {
        // Assume that everything will be OK when we validate...
        if (subpipeline.size() > 0) {
            Step step = subpipeline.get(0);
            for (Input input : step.inputs()) {
                Input cinput = new Input(runtime, step.getNode());
                cinput.setPort(input.getPort());
                cinput.setPrimary(input.getPrimary());
                addInput(cinput);
            }
            for (Output output : step.outputs()) {
                Output coutput = new Output(runtime, step.getNode());
                coutput.setPort(output.getPort());
                coutput.setPrimary(output.getPrimary());
                addOutput(coutput);
            }
        }
        super.augmentIO();
    }

    @Override
    protected void setEnvironment(Environment newEnvironment) {
        Environment env = new Environment(newEnvironment);

        // Now what about my subpipeline?
        for (Step step : subpipeline) {
            env.addStep(step);
        }
        
        patchEnvironment(env);
        super.setEnvironment(env);
        
        // Now what about my subpipeline
        for (Step step : subpipeline()) {
            Environment senv = new Environment(env);
            step.setEnvironment(senv);
        }
    }
    
    protected boolean validBindings() {
        boolean valid = true;
       
        // First, make sure all the substeps have the same bindings
        Hashtable<String,Input> inputs = new Hashtable<String,Input>();
        Hashtable<String,Output> outputs = new Hashtable<String,Output>();

         if (subpipeline.size() == 2) {
            Group p_group = (Group) subpipeline.get(0);
            for (Input input : p_group.inputs()) {
                inputs.put(input.getPort(), input);
            }
            for (Output output : p_group.outputs()) {
                outputs.put(output.getPort(), output);
            }

            Catch p_catch = (Catch) subpipeline.get(1);
            // there aren't any inputs here FIXME: Really?

            if (p_catch.outputs().size() != outputs.size()) {
                valid = false;
                runtime.error(null, p_group.getNode(), "The p:group and p:catch in a p:try must declare the same outputs", XProcConstants.staticError(9));
            }

            for (Output output : p_catch.outputs()) {
                if (outputs.containsKey(output.getPort())) {
                    Output s1output = outputs.get(output.getPort());

                    if (output.getPort().endsWith("|") || output.getPort().startsWith("!")) {
                        // assume it's ok
                    } else {
                        if (s1output.getPrimary() != output.getPrimary()) {
                            valid = false;
                            runtime.error(null, p_group.getNode(), "Output port " + output.getPort() + " has different primary status.", XProcConstants.staticError(9));
                        }
                    }
                } else {
                    valid = false;
                    runtime.error(null, p_group.getNode(), "Output port " + output.getPort() + " is extra.", XProcConstants.staticError(9));
                }
            }
            for (String port : outputs.keySet()) {
                if (!port.endsWith("|") && p_group.getOutput(port) == null) {
                    valid = false;
                    runtime.error(null, p_group.getNode(), "Output port " + port + " missing.", XProcConstants.staticError(9));
                }
            }
        } else {
           error("Try must contain a group and a catch", XProcConstants.staticError(27));
        }

        return valid;
    }

    protected boolean validOutputBinding() {
        // The output of try is special.
        return true;
    }
}
