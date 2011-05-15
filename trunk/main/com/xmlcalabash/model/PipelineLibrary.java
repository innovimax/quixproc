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
import com.xmlcalabash.core.XProcException;

import java.util.Hashtable;
import java.util.Set;
import java.util.Vector;

public class PipelineLibrary extends Step {
    Hashtable<QName,DeclareStep> declaredSteps = new Hashtable<QName,DeclareStep> ();
    Vector<DeclareStep> steps = new Vector<DeclareStep> ();

    /** Creates a new instance of PipelineLibrary */
    public PipelineLibrary(XProcRuntime xproc, XdmNode node) {
        super(xproc, node, XProcConstants.p_library);
    }

    public void addStep(DeclareStep step) {
        QName type = step.getDeclaredType();
        if (type == null) {
            // It can't be called so it doesn't really matter...
            return;
        }

        if (declaredSteps.contains(type)) {
            throw new XProcException(step.getNode(), "You aren't allowed to do this");
        }

        steps.add(step);
        declaredSteps.put(type, step);
    }

    public QName firstStep() {
        if (steps.size() > 0) {
            return steps.get(0).getDeclaredType();
        } else {
            return null;
        }
    }

    public Set<QName> declaredTypes() {
        return declaredSteps.keySet();
    }

    public DeclareStep getDeclaration(QName type) {
        if (declaredSteps.containsKey(type)) {
            return declaredSteps.get(type);
        } else {
            return null;
        }
    }
}
