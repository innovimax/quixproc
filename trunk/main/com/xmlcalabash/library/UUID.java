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

package com.xmlcalabash.library;

import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.util.ProcessMatchingNodes;
import com.xmlcalabash.util.ProcessMatch;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.QName;
import com.xmlcalabash.runtime.XAtomicStep;

public class UUID extends DefaultStep implements ProcessMatchingNodes {
    private static final QName _version = new QName("", "version");
    private static final QName _match = new QName("", "match");
    protected static final String logger = "org.xproc.library.hash";
    private ReadablePipe source = null;
    private WritablePipe result = null;
    private ProcessMatch matcher = null;
    private String uuid = null;

    /**
     * Creates a new instance of UUID
     */
    public UUID(XProcRuntime runtime, XAtomicStep step) {
        super(runtime,step);
    }

    public void setInput(String port, ReadablePipe pipe) {
        source = pipe;
    }

    public void setOutput(String port, WritablePipe pipe) {
        result = pipe;
    }

    public void reset() {
        source.resetReader(stepContext);
        result.resetWriter(stepContext);
    }

    public void gorun() throws SaxonApiException {
        super.gorun();

        String version = null;
        if (getOption(_version) != null) {
            version = getOption(_version).getString();
        }

        if (version != null && !"4".equals(version)) {
            throw XProcException.stepError(60);
        }

        java.util.UUID id = java.util.UUID.randomUUID();
        uuid = id.toString();

        matcher = new ProcessMatch(runtime, this);
        matcher.match(source.read(stepContext), getOption(_match));

        if (source.moreDocuments(stepContext)) {
            throw XProcException.dynamicError(6);
        }

        result.write(stepContext, matcher.getResult());

    }

    public boolean processStartDocument(XdmNode node) throws SaxonApiException {
        return true;
    }

    public void processEndDocument(XdmNode node) throws SaxonApiException {
        // nop?
    }

    public boolean processStartElement(XdmNode node) throws SaxonApiException {
        matcher.addText(uuid);
        return false;
    }

    public void processEndElement(XdmNode node) throws SaxonApiException {
        // nop?
    }

    public void processText(XdmNode node) throws SaxonApiException {
        matcher.addText(uuid);
    }

    public void processComment(XdmNode node) throws SaxonApiException {
        matcher.addComment(uuid);
    }

    public void processPI(XdmNode node) throws SaxonApiException {
        matcher.addPI(node.getNodeName().getLocalName(),uuid);
    }

    public void processAttribute(XdmNode node) throws SaxonApiException {
        matcher.addAttribute(node.getNodeName(), uuid);
    }
}

