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

import net.sf.saxon.s9api.XdmNode;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcConstants;

public class Viewport extends DeclareStep {
    RuntimeValue match = null;
    
    /** Creates a new instance of Viewport */
    public Viewport(XProcRuntime xproc, XdmNode node, String name) {
        super(xproc, node, name);
        declaration = this;
        stepType = XProcConstants.p_viewport;

        Output current = new Output(xproc, node);
        current.setPort("#current");
        addOutput(current);
    }

    public boolean isPipeline() {
        return false;
    }

    public DeclareStep getDeclaration() {
        return declaration;
    }

    public boolean loops() {
        return true;
    }
    
    public void setMatch(RuntimeValue match) {
        this.match = match;
    }
    
    public RuntimeValue getMatch() {
        return match;
    }

    @Override
    protected void augmentIO() {
        if (getInput("#viewport-source") == null) {
            Input isource = new Input(runtime, node);
            isource.setPort("#viewport-source");
            addInput(isource);
        }
        super.augmentIO();
    }

    public Output getOutput(String portName) {
        if ("current".equals(portName)) {
            return getOutput("#current");
        } else if ("result".equals(portName)) {
            for (Output output: outputs) {
                if (!"#current".equals(output.getPort())) {
                    return output;
                }
            }
            return null;
        } else {
            return super.getOutput(portName);
        }
    }

    @Override
    public void patchEnvironment(Environment env) {
        env.setDefaultReadablePort(getOutput("#current"));
    }
    
    @Override
    public boolean valid() {
        boolean valid = true;
        
        if (match == null || "".equals(match)) {
            error(node, "Match expression on p:viewport must be specified.", XProcConstants.staticError(38));
            valid = false;
        }

        if (outputs.size() == 1) {
            error(node, "A viewport step must have a primary output", XProcConstants.staticError(6));
        }

        if (!super.valid()) {
            valid = false;
        }
        return valid;
    }
}
