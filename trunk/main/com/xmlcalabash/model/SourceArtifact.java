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

import java.util.Hashtable;
import java.util.Set;
import java.util.logging.Logger;

public abstract class SourceArtifact {
    protected XdmNode node = null;
    protected XProcRuntime runtime = null;
    protected Hashtable<QName,String> extnAttrs = null;
    protected Logger logger = null;

    /** Creates a new instance of SourceArtifact */
    public SourceArtifact(XProcRuntime runtime, XdmNode node) {
        this.runtime = runtime;
        this.node = node;
        logger = Logger.getLogger(this.getClass().getName());
    }

    public XProcRuntime getXProc() {
        return runtime;
    }
    
    public XdmNode getNode() {
        return node;
    }

    public String xplFile() {
        if (node == null) {
            return "";
        } else {
            return node.getDocumentURI().toASCIIString();
        }
    }

    public int xplLine() {
        if (node == null) {
            return -1;
        } else {
            return node.getLineNumber();
        }
    }

/*
    public String getLocation() {
        if (node == null) {
            return "";
        } else {
            if (node.getLineNumber() > 0) {
                return node.getDocumentURI() + ":" + node.getLineNumber();
            } else {
                return node.getDocumentURI().toASCIIString();
            }
        }
    }
*/

    public void addExtensionAttribute(XdmNode attr) {
        if (extnAttrs == null) {
            extnAttrs = new Hashtable<QName,String> ();
        }
        extnAttrs.put(attr.getNodeName(),attr.getStringValue());
    }

    public String getExtensionAttribute(QName name) {
        if (extnAttrs == null || !extnAttrs.containsKey(name)) {
            return null;
        }

        return extnAttrs.get(name);
    }

    public Set<QName> getExtensionAttributes() {
        if (extnAttrs == null) {
            extnAttrs = new Hashtable<QName,String> ();
        }
        return extnAttrs.keySet();
    }

    public void error(String message, QName code) {
        runtime.error(null, node, message, code);
    }

    public void error(XdmNode node, String message, QName code) {
        runtime.error(null, node, message, code);
    }
}
