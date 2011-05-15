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

import java.util.Map;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.util.ProcessMatchingNodes;
import com.xmlcalabash.util.ProcessMatch;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.model.RuntimeValue;
import net.sf.saxon.s9api.*;
import com.xmlcalabash.runtime.XAtomicStep;

public class Insert extends DefaultStep implements ProcessMatchingNodes {
    private static final QName _match = new QName("match");
    private static final QName _position = new QName("position");
    private ReadablePipe insertion = null;
    private ReadablePipe source = null;
    private WritablePipe result = null;
    private Map<QName, RuntimeValue> inScopeOptions = null;
    private ProcessMatch matcher = null;
    private String position = null;
    private String matchPattern = null;

    /** Creates a new instance of Insert */
    public Insert(XProcRuntime runtime, XAtomicStep step) {
        super(runtime,step);
    }

    public void setInput(String port, ReadablePipe pipe) {
        if ("source".equals(port)) {
            source = pipe;
        } else {
            insertion = pipe;
        }
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

        position = getOption(_position).getString();

        XdmNode doc = source.read(stepContext);

        matcher = new ProcessMatch(runtime, this);
        matcher.match(doc, getOption(_match));

        result.write(stepContext, matcher.getResult());
    }

    public boolean processStartDocument(XdmNode node) throws SaxonApiException {
        throw XProcException.stepError(25);
    }

    public void processEndDocument(XdmNode node) throws SaxonApiException {
        throw XProcException.stepError(25);
    }

    public boolean processStartElement(XdmNode node) throws SaxonApiException {
        if ("before".equals(position)) {
            doInsert();
        }

        matcher.addStartElement(node);
        matcher.addAttributes(node);
        matcher.startContent();

        if ("first-child".equals(position)) {
            doInsert();
        }

        return true;
    }

    public void processEndElement(XdmNode node) throws SaxonApiException {
        if ("last-child".equals(position)) {
            doInsert();
        }

        matcher.addEndElement();

        if ("after".equals(position)) {
            doInsert();
        }
    }

    public void processText(XdmNode node) throws SaxonApiException {
        process(node);
    }

    public void processComment(XdmNode node) throws SaxonApiException {
        process(node);
    }

    public void processPI(XdmNode node) throws SaxonApiException {
        process(node);
    }

    private void process(XdmNode node) throws SaxonApiException {
        if ("before".equals(position)) {
            doInsert();
        }

        if (node.getNodeKind() == XdmNodeKind.COMMENT) {
            matcher.addComment(node.getStringValue());
        } else if (node.getNodeKind() == XdmNodeKind.PROCESSING_INSTRUCTION) {
            matcher.addPI(node.getNodeName().getLocalName(), node.getStringValue());
        } else if (node.getNodeKind() == XdmNodeKind.TEXT) {
            matcher.addText(node.getStringValue());
        } else {
            throw new IllegalArgumentException("What kind of node was that!?");
        }

        if ("after".equals(position)) {
            doInsert();
        }

        if ("first-child".equals(position) || "last-child".equals(position)) {
            throw XProcException.stepError(25);
        }
    }

    public void processAttribute(XdmNode node) throws SaxonApiException {
        throw XProcException.stepError(23);
    }

    private void doInsert() throws SaxonApiException {
        while (insertion.moreDocuments(stepContext)) {
            XdmNode doc = insertion.read(stepContext);
            XdmSequenceIterator iter = doc.axisIterator(Axis.CHILD);
            while (iter.hasNext()) {
                XdmNode child = (XdmNode) iter.next();
                matcher.addSubtree(child);
            }
        }
        insertion.resetReader(stepContext);
    }
}
