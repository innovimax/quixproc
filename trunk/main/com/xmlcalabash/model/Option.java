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

import java.util.Vector;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmNode;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcConstants;

public class Option extends EndPoint implements ComputableValue {
    private QName name = null;
    private boolean required = false;
    private String select = null;
    private String type = null;
    private XdmNode typeNode = null;
    private Vector<NamespaceBinding> nsBindings = new Vector<NamespaceBinding>();

    /** Creates a new instance of Option */
    public Option(XProcRuntime xproc, XdmNode node) {
        super(xproc,node);
    }

    public void setName(QName name) {
        this.name = name;
    }
    
    public QName getName() {
        return name;
    }

    public void setType(String type, XdmNode node) {
        this.type = type;
        typeNode = node;
    }

    public String getType() {
        return type;
    }

    public QName getTypeAsQName() {
        return new QName(type,typeNode);
    }

    public void setRequired(String required) {
        this.required = "true".equals(required);
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public boolean getRequired() {
        return required;
    }

    public void setSelect(String select) {
        this.select = select;
    }
    
    public String getSelect() {
        return select;
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
            error("Option can have at most one binding.", XProcConstants.dynamicError(8));
            valid = false;
        }

        if (required && (select != null)) {
            error("You can't specify a default value on a required option", XProcConstants.staticError(17));
        }
        
        return valid;
    }

    public String toString() {
        if (XProcConstants.p_option.equals(node.getNodeName())) {
            return "with-option " + name;
        } else {
            return "option " + name;
        }
    }

    protected void dump(int depth) {
        String indent = "";
        for (int count = 0; count < depth; count++) {
            indent += " ";
        }

        if (select != null) {
            System.err.println(indent + "option " + getName() + " select=" + select);
        } else {
            System.err.println(indent + "option " + getName());
            if (getBinding().size() == 0) {
                if (XProcConstants.p_option.equals(node.getNodeName())) {
                    // System.err.println(indent + "  no binding allowed");
                } else {
                    System.err.println(indent + "  no binding");
                }
            }
        }
        for (Binding binding : getBinding()) {
            binding.dump(depth+2);
        }
    }
}
