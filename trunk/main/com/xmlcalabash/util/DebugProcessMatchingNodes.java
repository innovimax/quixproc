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
package com.xmlcalabash.util;

import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Apr 18, 2008
 * Time: 10:22:16 AM
 * To change this template use File | Settings | File Templates.
 */
public class DebugProcessMatchingNodes implements ProcessMatchingNodes {
    public boolean processStartDocument(XdmNode node) throws SaxonApiException {
        System.err.println("processStartDocument(" + node + ")");
        return true;
    }

    public void processEndDocument(XdmNode node) throws SaxonApiException {
        System.err.println("processEndDocument(" + node + ")");
    }

    public boolean processStartElement(XdmNode node) throws SaxonApiException {
        System.err.println("processStartElement(" + node + ")");
        return true;
    }

    public void processEndElement(XdmNode node) throws SaxonApiException {
        System.err.println("processEndElement(" + node + ")");
    }

    public void processText(XdmNode node) throws SaxonApiException {
        System.err.println("processText(" + node + ")");
    }

    public void processComment(XdmNode node) throws SaxonApiException {
        System.err.println("processComment(" + node + ")");
    }

    public void processPI(XdmNode node) throws SaxonApiException {
        System.err.println("processPI(" + node + ")");
    }

    public void processAttribute(XdmNode node) throws SaxonApiException {
        System.err.println("processAttribute(" + node + ")");
    }
}
