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

import net.sf.saxon.s9api.XdmNode;

import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcRuntime;

/**
 *
 * @author ndw
 */
public class When extends DeclareStep {
    private String testExpr = null;
    
    /** Creates a new instance of When */
    public When(XProcRuntime xproc, XdmNode node, String name) {
        super(xproc, node, name);
        declaration = this;
        stepType = XProcConstants.p_when;
    }

    public boolean isPipeline() {
        return false;
    }

    public DeclareStep getDeclaration() {
        return declaration;
    }

    public void setTest(String test) {
        testExpr = test;
    }

    public String getTest() {
        return testExpr;
    }

    @Override
    public HashSet<String> getExcludeInlineNamespaces() {
        return ((DeclareStep) parent).getExcludeInlineNamespaces();
    }

    @Override
    protected void augmentIO() {
        if (getInput("#xpath-context") == null) {
            Input isource = new Input(runtime, node);
            isource.setPort("#xpath-context");
            addInput(isource);
        }
        super.augmentIO();
    }
    
    @Override
    public boolean valid() {
        boolean valid = true;
        
        if (testExpr == null || "".equals(testExpr)) {
            runtime.error(null, node, "Test expression on p:when must be specified.", XProcConstants.staticError(38));
            valid = false;
        }

        if (!super.valid()) {
            valid = false;
        }
        return valid;
    }
}
