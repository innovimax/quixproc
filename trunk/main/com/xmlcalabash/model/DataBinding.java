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

import java.net.URI;

import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmNode;

import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcRuntime;

/**
 *
 * @author ndw
 */
public class DataBinding extends Binding {
    private String href = null;
    private QName wrapper = XProcConstants.c_data;
    private String contentType = null;

    /** Creates a new instance of DocumentBinding */
    public DataBinding() {
        super(null, null);
        bindingType = DATA_BINDING;
    }

    public DataBinding(XProcRuntime xproc, XdmNode node) {
        super(xproc, node);
        bindingType = DATA_BINDING;
    }

    public void setHref(String href) {
        URI base = node.getBaseURI();
        base = base.resolve(href);
        this.href = base.toASCIIString();
    }

    public String getHref() {
        return href;
    }

    public void setWrapper(QName wrapper) {
        this.wrapper = wrapper;
    }

    public QName getWrapper() {
        return wrapper;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContentType() {
        return contentType;
    }

    protected void dump(int depth) {
        String indent = "";
        for (int count = 0; count < depth; count++) {
            indent += " ";
        }

        System.err.println(indent + "Data binding to " + getHref());
    }
}

