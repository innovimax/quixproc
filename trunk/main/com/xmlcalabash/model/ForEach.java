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

public class ForEach extends DeclareStep {
    private String select = null;
    
    /** Creates a new instance of ForEach */
    public ForEach(XProcRuntime xproc, XdmNode node, String name) {
        super(xproc, node, name);
        declaration = this;
        stepType = XProcConstants.p_for_each;

        Output current = new Output(xproc, node);
        current.setPort("#current");
        current.setSequence(true);
        addOutput(current);
    }

    public boolean isPipeline() {
        return false;
    }

    public DeclareStep getDeclaration() {
        return declaration;
    }

    @Override
    public boolean loops() {
        return true;
    }
    
    @Override
    protected void augmentIO() {
        if (getInput("#iteration-source") == null) {
            Input isource = new Input(runtime, node);
            isource.setPort("#iteration-source");
            isource.setPrimary(true);
            isource.setSequence(true);
            addInput(isource);
        }
        
        super.augmentIO();
    }

    public Output getOutput(String portName) {
        if ("current".equals(portName)) {
            return getOutput("#current");
        } else {
            return super.getOutput(portName);
        }
    }

    public void patchEnvironment(Environment env) {
        env.setDefaultReadablePort(getOutput("#current"));
    }
}
