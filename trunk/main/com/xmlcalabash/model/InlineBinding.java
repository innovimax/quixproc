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
import java.util.HashSet;

import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmValue;
import com.xmlcalabash.core.XProcRuntime;

public class InlineBinding extends Binding {
    XdmNode root = null;
    Vector<XdmValue> nodes = null;
    HashSet<String> excludeNS = null;
    
    /** Creates a new instance of InlineBinding */
    public InlineBinding(XProcRuntime xproc, XdmNode node) {
        super(xproc, node);
        bindingType = INLINE_BINDING;
        nodes = new Vector<XdmValue> ();
    }

    public void addNode(XdmNode node) {
        nodes.add(node);
    }

    public void excludeNamespaces(HashSet<String> exclude) {
        excludeNS = exclude;
    }

    public HashSet<String> getExcludedNamespaces() {
        return excludeNS;
    }

    public Vector<XdmValue> nodes() {
        return nodes;
    }

    protected void dump(int depth) {
        String indent = "";
        for (int count = 0; count < depth; count++) {
            indent += " ";
        }

        System.err.println(indent + "inline binding");
    }
}
