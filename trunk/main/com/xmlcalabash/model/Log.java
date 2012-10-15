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

import net.sf.saxon.s9api.XdmNode;

import com.xmlcalabash.core.XProcRuntime;

/**
 *
 * @author ndw
 */
public class Log extends SourceArtifact {
    private String port = null;
    private URI href = null;
    
    /** Creates a new instance of Journal */
    public Log(XProcRuntime xproc, XdmNode node) {
        super(xproc, node);
    }

    public void setPort(String port) {
        this.port = port;
    }
    
    public String getPort() {
        return port;
    }
    
    public void setHref(URI href) {
        this.href = href;
    }
    
    public URI getHref() {
        return href;
    }

    public String toString() {
        String result = "log for \"" + port + "\"";
        if (href != null) {
            result += " to \"" + href + "\"";
        }
        if (node.getLineNumber() > 0) {
            result += " at " + node.getDocumentURI() + ":" + node.getLineNumber();
        } else {
            result += " in " + node.getDocumentURI();
        }
        return result;
    }
}
