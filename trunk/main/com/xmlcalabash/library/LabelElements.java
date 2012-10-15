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

package com.xmlcalabash.library;

import java.util.Iterator;

import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XPathCompiler;
import net.sf.saxon.s9api.XPathExecutable;
import net.sf.saxon.s9api.XPathSelector;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmSequenceIterator;

import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.ProcessMatch;
import com.xmlcalabash.util.ProcessMatchingNodes;

/**
 *
 * @author ndw
 */
public class LabelElements extends DefaultStep implements ProcessMatchingNodes {
    private static final QName _attribute = new QName("attribute");
    private static final QName _attribute_prefix = new QName("attribute-prefix");
    private static final QName _attribute_namespace = new QName("attribute-namespace");
    private static final QName _match = new QName("match");
    private static final QName _label = new QName("label");
    private static final QName _replace = new QName("replace");
    private static final QName p_index = new QName("p", XProcConstants.NS_XPROC, "index");
    private ReadablePipe source = null;
    private WritablePipe result = null;
    private ProcessMatch matcher = null;
    private QName attribute = null;
    private RuntimeValue label = null;
    private boolean replace = true;
    private int count = 1;

    /** Creates a new instance of LabelElements */
    public LabelElements(XProcRuntime runtime, XAtomicStep step) {
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

        RuntimeValue attrNameValue = getOption(_attribute);
        String attrNameStr = attrNameValue.getString();
        String apfx = getOption(_attribute_prefix, (String) null);
        String ans = getOption(_attribute_namespace, (String) null);

        if (apfx != null && ans == null) {
            throw XProcException.dynamicError(34, "You can't specify a prefix without a namespace");
        }

        if (ans != null && attrNameStr.contains(":")) {
            throw XProcException.dynamicError(34, "You can't specify a namespace if the attribute name contains a colon");
        }

        if (attrNameStr.contains(":")) {
            attribute = new QName(attrNameStr, attrNameValue.getNode());
        } else {
          // For Saxon 9.4, make sure there's some sort of prefix if there's a namespace;
          // Saxon will take care of resolving collisions, if necessary
            if (apfx == null && ans != null) {
                apfx = "_1";
            }
            attribute = new QName(apfx == null ? "" : apfx, ans, attrNameStr);
        }

        label = getOption(_label);
        replace = getOption(_replace).getBoolean();

        matcher = new ProcessMatch(runtime, this);
        matcher.match(source.read(stepContext), getOption(_match));

        result.write(stepContext,matcher.getResult());
    }

    public boolean processStartDocument(XdmNode node) throws SaxonApiException {
        throw XProcException.stepError(24);
    }

    public void processEndDocument(XdmNode node) throws SaxonApiException {
        throw XProcException.stepError(24);
    }

    public boolean processStartElement(XdmNode node) throws SaxonApiException {
        matcher.addStartElement(node);

        boolean found = false;
        XdmSequenceIterator iter = node.axisIterator(Axis.ATTRIBUTE);
        while (iter.hasNext()) {
            XdmNode attr = (XdmNode) iter.next();
            if (attribute.equals(attr.getNodeName())) {
                found = true;
                if (replace) {
                    matcher.addAttribute(attr, computedLabel(node));
                } else {
                    matcher.addAttribute(attr);
                }
            } else {
                matcher.addAttribute(attr);
            }
        }

        if (!found) {
            matcher.addAttribute(attribute, computedLabel(node));
        }

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
        throw XProcException.stepError(23);
    }

    public void processAttribute(XdmNode node) throws SaxonApiException {
        throw XProcException.stepError(23);
    }

    private String computedLabel(XdmNode node) throws SaxonApiException {
        XPathCompiler xcomp = runtime.getProcessor().newXPathCompiler();
        xcomp.setBaseURI(step.getNode().getBaseURI());

        // Make sure any namespace bindings in-scope for the label are available for the expression
        for (String prefix : label.getNamespaceBindings().keySet()) {
            xcomp.declareNamespace(prefix, label.getNamespaceBindings().get(prefix));
        }
        xcomp.declareVariable(p_index);

        XPathExecutable xexec = xcomp.compile(label.getString());
        XPathSelector selector = xexec.load();

        selector.setVariable(p_index,new XdmAtomicValue(count++));

        selector.setContextItem(node);

        Iterator<XdmItem> values = selector.iterator();

        XdmItem item = values.next();

        return item.getStringValue();
    }
}

