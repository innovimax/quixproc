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

import java.net.URI;

import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmNode;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcConstants;

public class Import extends Step {
    URI href = null;
    PipelineLibrary library = null;
    XdmNode root = null;

    /** Creates a new instance of Import */
    public Import(XProcRuntime xproc, XdmNode node) {
        super(xproc, node, XProcConstants.p_import);
        //String x = node.getAttributeValue(new QName("", "href"));
        //System.err.println(x);
    }

    public void setHref(URI href) {
        this.href = href;
    }

    public URI getHref() {
        return href;
    }

    public void setRoot(XdmNode root) {
        this.root = root;
    }

    public XdmNode getRoot() {
        return root;
    }

    public void setLibrary(PipelineLibrary library) {
        this.library = library;
    }
}
