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

import java.util.Vector;

import net.sf.saxon.s9api.XdmNode;

import com.xmlcalabash.core.XProcRuntime;

/**
 *
 * @author ndw
 */
public class EndPoint extends SourceArtifact {
    protected Step step = null;
    protected Vector<Binding> bindings = new Vector<Binding> ();
    
    /** Creates a new instance of EndPoint */
    public EndPoint(XProcRuntime xproc, XdmNode node) {
        super(xproc, node);
    }

    public void setStep(Step step) {
        this.step = step;
    }
    
    public Step getStep() {
        return step;
    }

    public void addBinding(Binding binding) {
        bindings.add(binding);
    }

    public void clearBindings() {
        bindings = new Vector<Binding> ();
    }

    public Vector<Binding> getBinding() {
        return bindings;
    }

    public PipeNameBinding findPipeBinding(String stepName, String portName) {
        for (Binding binding : getBinding()) {
            if (binding.getBindingType() == Binding.PIPE_NAME_BINDING) {
                PipeNameBinding pipe = (PipeNameBinding) binding;
                if (pipe.getStep().equals(stepName) && pipe.getPort().equals(portName)) {
                    return pipe;
                }
            }
        }
        
        return null;
    }
}
