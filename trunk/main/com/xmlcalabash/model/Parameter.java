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

import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmNode;

import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcRuntime;

/**
 *
 * @author ndw
 */
public class Parameter extends EndPoint implements ComputableValue {
    private String port = null;
    private QName name = null;
    private boolean required = false;
    private String select = null;
    protected Vector<Binding> bindings = new Vector<Binding> ();
    private int position = 0;
    private Vector<NamespaceBinding> nsBindings = new Vector<NamespaceBinding> ();

    /** Creates a new instance of Parameter */
    public Parameter(XProcRuntime xproc, XdmNode node) {
        super(xproc, node);
    }
    
    public void setPort(String port) {
        this.port = port;
    }

    public String getPort() {
        return port;
    }

    public void setName(QName name) {
        this.name = name;
    }

    public QName getName() {
        return name;
    }

    public String getType() {
        return null;
    }

    public QName getTypeAsQName() {
        return null;
    }

    public void setPosition(int pos) {
        position = pos;
    }
    
    public int getPosition() {
        return position;
    }
    
    public void setSelect(String select) {
        this.select = select;
    }
    
    public String getSelect() {
        return select;
    }

    public void addBinding(Binding binding) {
        bindings.add(binding);
    }

    public Vector<Binding> getBinding() {
        return bindings;
    }

    public void addNamespaceBinding(NamespaceBinding binding) {
        nsBindings.add(binding);
    }

    public Vector<NamespaceBinding> getNamespaceBindings() {
        return nsBindings;
    }

    public boolean valid(Environment env) {
        boolean valid = true;
        
        if (bindings.size() > 1) {
            error("Parameter can have at most one binding.", XProcConstants.dynamicError(8));
            valid = false;
        }

        return valid;
    }

    public String toString() {
        return "with-param " + name;
    }

    protected void dump(int depth) {
        String indent = "";
        for (int count = 0; count < depth; count++) {
            indent += " ";
        }

        System.err.println(indent + "parameter " + getName());
        if (bindings.size() == 0) {
            System.err.println(indent + "  no binding");
        }
        for (Binding binding : getBinding()) {
            binding.dump(depth+2);
        }
    }
    
}
