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
import java.util.Iterator;
import java.util.Stack;

import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.util.ProcessMatchingNodes;
import com.xmlcalabash.util.ProcessMatch;
import com.xmlcalabash.util.S9apiUtils;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.model.RuntimeValue;
import net.sf.saxon.s9api.*;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.StandardNames;
import com.xmlcalabash.runtime.XAtomicStep;

public class Wrap extends DefaultStep implements ProcessMatchingNodes {
    private static final QName _match = new QName("match");
    private static final QName _wrapper = new QName("wrapper");
    private static final QName _wrapper_prefix = new QName("wrapper-prefix");
    private static final QName _wrapper_namespace = new QName("wrapper-namespace");
    private static final QName _group_adjacent = new QName("group-adjacent");
    private ReadablePipe source = null;
    private WritablePipe result = null;
    private ProcessMatch matcher = null;
    private Map<QName, RuntimeValue> inScopeOptions = null;
    private QName wrapper = null;
    private int wrapperCode = 0;
    private RuntimeValue groupAdjacent = null;
    private Stack<Boolean> inGroup = new Stack<Boolean> ();

    /** Creates a new instance of Wrap */
    public Wrap(XProcRuntime runtime, XAtomicStep step) {
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

        RuntimeValue wrapperNameValue = getOption(_wrapper);
        String wrapperNameStr = wrapperNameValue.getString();
        String wpfx = getOption(_wrapper_prefix, (String) null);
        String wns = getOption(_wrapper_namespace, (String) null);

        if (wpfx != null && wns == null) {
            throw XProcException.dynamicError(34, step.getNode(), "You can't specify a prefix without a namespace");
        }

        if (wns != null && wrapperNameStr.contains(":")) {
            throw XProcException.dynamicError(34, step.getNode(), "You can't specify a namespace if the wrapper name contains a colon");
        }

        if (wrapperNameStr.contains(":")) {
            wrapper = new QName(wrapperNameStr, wrapperNameValue.getNode());
        } else {
            wrapper = new QName(wpfx == null ? "" : wpfx, wns, wrapperNameStr);
        }

        groupAdjacent = getOption(_group_adjacent);

        inGroup.push(false);

        XdmNode doc = source.read(stepContext);
        NodeInfo inode = doc.getUnderlyingNode();
        NamePool pool = inode.getNamePool();
        wrapperCode = pool.allocate(wrapper.getPrefix(),wrapper.getNamespaceURI(),wrapper.getLocalName());

        matcher = new ProcessMatch(runtime, this);
        matcher.match(doc,getOption(_match));

        if (source.moreDocuments(stepContext)) {
            throw XProcException.dynamicError(6);
        }

        result.write(stepContext, matcher.getResult());
    }

    public boolean processStartDocument(XdmNode node) throws SaxonApiException {
        matcher.startDocument(node.getBaseURI());
        matcher.addStartElement(wrapperCode, StandardNames.XS_UNTYPED, null);
        matcher.startContent();
        matcher.addSubtree(node);
        matcher.addEndElement();
        matcher.endDocument();
        return false;

    }

    public void processEndDocument(XdmNode node) throws SaxonApiException {
        // nop
    }

    public boolean processStartElement(XdmNode node) throws SaxonApiException {
        if (!inGroup.peek()) {
            matcher.addStartElement(wrapperCode, StandardNames.XS_UNTYPED, null);
        }

        if (groupAdjacent != null && nextMatches(node)) {
            inGroup.pop();
            inGroup.push(true);
        } else {
            inGroup.pop();
            inGroup.push(false);
        }

        matcher.addStartElement(node);
        matcher.addAttributes(node);

        inGroup.push(false); // processEndElement will pop it! Value doesn't matter!
        return true;
    }

    public void processAttribute(XdmNode node) throws SaxonApiException {
        throw XProcException.stepError(23);
    }

    public void processEndElement(XdmNode node) throws SaxonApiException {
        matcher.addEndElement();
        inGroup.pop();
        if (!inGroup.peek()) {
            matcher.addEndElement();
        }
    }

    public void processText(XdmNode node) throws SaxonApiException {
        if (!inGroup.peek()) {
            matcher.addStartElement(wrapperCode, StandardNames.XS_UNTYPED, null);
        }

        matcher.addText(node.getStringValue());

        if (groupAdjacent != null && nextMatches(node)) {
            inGroup.pop();
            inGroup.push(true);
        } else {
            matcher.addEndElement();
            inGroup.pop();
            inGroup.push(false);
        }
    }

    public void processComment(XdmNode node) throws SaxonApiException {
        if (!inGroup.peek()) {
            matcher.addStartElement(wrapperCode, StandardNames.XS_UNTYPED, null);
        }

        matcher.addComment(node.getStringValue());

        if (groupAdjacent != null && nextMatches(node)) {
            inGroup.pop();
            inGroup.push(true);
        } else {
            matcher.addEndElement();
            inGroup.pop();
            inGroup.push(false);
        }
    }

    public void processPI(XdmNode node) throws SaxonApiException {
        if (!inGroup.peek()) {
            matcher.addStartElement(wrapperCode, StandardNames.XS_UNTYPED, null);
        }

        matcher.addPI(node.getNodeName().getLocalName(),node.getStringValue());

        if (groupAdjacent != null && nextMatches(node)) {
            inGroup.pop();
            inGroup.push(true);
        } else {
            matcher.addEndElement();
            inGroup.pop();
            inGroup.push(false);
        }
    }

    private boolean nextMatches(XdmNode node) {
        XdmItem nodeValue = computeGroup(node);

        if (nodeValue == null) {
            return false;
        }

        XdmSequenceIterator iter = node.axisIterator(Axis.FOLLOWING_SIBLING);

        while (iter.hasNext()) {
            XdmNode chk = (XdmNode) iter.next();

            boolean skippable
                    = (chk.getNodeKind() == XdmNodeKind.COMMENT
                       || chk.getNodeKind() == XdmNodeKind.PROCESSING_INSTRUCTION);

            if (chk.getNodeKind() == XdmNodeKind.TEXT) {
                if ("".equals(chk.toString().trim())) {
                    skippable = true;
                }
            }

            if (matcher.matches(chk)) {
                XdmItem nextValue = computeGroup(chk);
                boolean same = S9apiUtils.xpathEqual(runtime.getProcessor(), nodeValue, nextValue);
                return same;
            }

            if (!skippable) {
                return false;
            }
        }

        return false;
    }

    private XdmItem computeGroup(XdmNode node) {
        try {
            XPathCompiler xcomp = runtime.getProcessor().newXPathCompiler();
            for (String prefix : groupAdjacent.getNamespaceBindings().keySet()) {
                xcomp.declareNamespace(prefix, groupAdjacent.getNamespaceBindings().get(prefix));
            }

            XPathExecutable xexec = xcomp.compile(groupAdjacent.getString());
            XPathSelector selector = xexec.load();
            selector.setContextItem(node);

            Iterator<XdmItem> values = selector.iterator();
            if (values.hasNext()) {
                return values.next();
            } else {
                return null;
            }
        } catch (SaxonApiException sae) {
            throw new XProcException(sae);
        }
    }
}

