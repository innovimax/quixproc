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
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import com.xmlcalabash.runtime.XAtomicStep;

public class Rename extends DefaultStep implements ProcessMatchingNodes {
    private static final QName _match = new QName("", "match");
    private static final QName _new_name = new QName("", "new-name");
    private static final QName _new_prefix = new QName("", "new-prefix");
    private static final QName _new_namespace = new QName("", "new-namespace");
    private ReadablePipe source = null;
    private WritablePipe result = null;
    private Map<QName, RuntimeValue> inScopeOptions = null;
    private ProcessMatch matcher = null;
    private QName newName = null;

    /** Creates a new instance of Rename */
    public Rename(XProcRuntime runtime, XAtomicStep step) {
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
        
        RuntimeValue nameValue = getOption(_new_name);
        String nameStr = nameValue.getString();
        String npfx = getOption(_new_prefix, (String) null);
        String nns = getOption(_new_namespace, (String) null);

        if (npfx != null && nns == null) {
            throw XProcException.dynamicError(34, "You can't specify a prefix without a namespace");
        }

        if (nns != null && nameStr.contains(":")) {
            throw XProcException.dynamicError(34, "You can't specify a namespace if the new-name contains a colon");
        }

        if (nameStr.contains(":")) {
            newName = new QName(nameStr, nameValue.getNode());
        } else {
            newName = new QName(npfx == null ? "" : npfx, nns, nameStr);
        }

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
        // nop
    }

    public boolean processStartElement(XdmNode node) throws SaxonApiException {
        matcher.addStartElement(node, newName);
        matcher.addAttributes(node);
        return true;
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
        if (!"".equals(newName.getNamespaceURI())) {
            throw XProcException.stepError(13);
        }
        matcher.addPI(newName.getLocalName(), node.getStringValue());
    }

    public void processAttribute(XdmNode node) throws SaxonApiException {
        matcher.addAttribute(newName, node.getStringValue());
    }
}

