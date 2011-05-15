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

public class Catch extends DeclareStep {
    
    /** Creates a new instance of Catch */
    public Catch(XProcRuntime xproc, XdmNode node, String name) {
        super(xproc, node, name);
        declaration = this;
        stepType = XProcConstants.p_catch;
    }

    public boolean isPipeline() {
        return false;
    }

    public DeclareStep getDeclaration() {
        return declaration;
    }

    @Override
    protected void augmentIO() {
        // Output bindings on a compound step are really the input half of an input/output binding;
        // create the other half
        for (Output output : outputs) {
            Input input = getInput("|" + output.getPort());
            if (input == null) {
                input = new Input(runtime, output.getNode());
                input.setPort("|" + output.getPort());
                input.setSequence(true); // the other half will check
                input.setPrimary(output.getPrimary());
                addInput(input);
            }
        }
        
        // The only input on catch is errors and it's special
    }
}
