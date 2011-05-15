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
import java.util.HashSet;

import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.util.ProcessMatchingNodes;
import com.xmlcalabash.util.ProcessMatch;
import com.xmlcalabash.util.S9apiUtils;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.model.RuntimeValue;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmSequenceIterator;
import net.sf.saxon.s9api.Axis;
import com.xmlcalabash.runtime.XAtomicStep;

public class SetAttributes extends DefaultStep implements ProcessMatchingNodes {
    private static final QName _match = new QName("match");
    private ReadablePipe attributes = null;
    private ReadablePipe source = null;
    private WritablePipe result = null;
    private Map<QName, RuntimeValue> inScopeOptions = null;
    private ProcessMatch matcher = null;
    private XdmNode root = null;
    private HashSet<QName> attrs = null;

    /** Creates a new instance of SetAttributes */
    public SetAttributes(XProcRuntime runtime, XAtomicStep step) {
        super(runtime,step);
    }

    public void setInput(String port, ReadablePipe pipe) {
        if ("source".equals(port)) {
            source = pipe;
        } else {
            attributes = pipe;
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

        root = S9apiUtils.getDocumentElement(attributes.read(stepContext));
        attrs = new HashSet<QName> ();
        XdmSequenceIterator iter = root.axisIterator(Axis.ATTRIBUTE);
        while (iter.hasNext()) {
            XdmNode attr = (XdmNode) iter.next();
            attrs.add(attr.getNodeName());
        }

        matcher = new ProcessMatch(runtime, this);
        matcher.match(source.read(stepContext), getOption(_match));

        if (source.moreDocuments(stepContext)) {
            throw XProcException.dynamicError(6);
        }

        result.write(stepContext, matcher.getResult());
    }

    public boolean processStartDocument(XdmNode node) throws SaxonApiException {
        throw XProcException.stepError(23);
    }

    public void processEndDocument(XdmNode node) throws SaxonApiException {
        // nop
    }

    public boolean processStartElement(XdmNode node) throws SaxonApiException {
        matcher.addStartElement(node);

        XdmSequenceIterator iter = node.axisIterator(Axis.ATTRIBUTE);
        while (iter.hasNext()) {
            XdmNode attr = (XdmNode) iter.next();
            if (!attrs.contains(attr.getNodeName())) {
                matcher.addAttribute(attr);
            }
        }

        matcher.addAttributes(root);
        matcher.startContent();
        return true;
    }

    public void processAttribute(XdmNode node) throws SaxonApiException {
        throw XProcException.stepError(23);
    }
    
    public void processEndElement(XdmNode node) throws SaxonApiException {
        matcher.addEndElement();
    }

    public void processText(XdmNode node) throws SaxonApiException {
        throw XProcException.stepError(23);
    }

    public void processComment(XdmNode node) throws SaxonApiException {
        throw XProcException.stepError(23);
    }

    public void processPI(XdmNode node) throws SaxonApiException {
        throw XProcException.stepError(23);
    }
}

