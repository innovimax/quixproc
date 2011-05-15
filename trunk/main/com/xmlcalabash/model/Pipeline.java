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
import net.sf.saxon.s9api.QName;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcConstants;

import java.util.Vector;

public class Pipeline extends CompoundStep {
    private Vector<Import> imports = new Vector<Import> ();
    protected boolean psviRequired = false;
    protected String xpathVersion = "2.0";
    private QName declaredType = null;
    private DeclareStep declaration = null;

    /** Creates a new instance of DeclareStep */
    public Pipeline(XProcRuntime xproc, XdmNode node, String name) {
        super(xproc, node, XProcConstants.p_pipeline, name);
    }

    public void setPsviRequired(boolean psvi) {
        psviRequired = psvi;
    }

    public void setXPathVersion(String version) {
        xpathVersion = version;
    }

    public void setDeclaredType(QName type) {
        declaredType = type;
    }

    public QName getDeclaredType() {
        return declaredType;
    }

    public void setDeclaration(DeclareStep step) {
        declaration = step;
    }

    public DeclareStep getDeclaration() {
        return declaration;
    }

    public void addStep(Step step) {
        super.addStep(step);
    }

    public void addImport(Import importelem) {
        imports.add(importelem);
    }

    public void setupEnvironment() {
        setEnvironment(new Environment(this));
    }

    protected void patchEnvironment(Environment env) {
        // See if there's exactly one "ordinary" input
        int count = 0;
        Input defin = null;
        boolean foundPrimary = false;
        for (Input input : inputs) {
            if (!input.getPort().startsWith("|") && !input.getParameterInput()) {
                count++;
                foundPrimary |= input.getPrimary();
                if (defin == null || input.getPrimary()) {
                    defin = input;
                }
            }
        }

        if (count == 1 || foundPrimary) {
            env.setDefaultReadablePort(defin);
        }
    }
}

