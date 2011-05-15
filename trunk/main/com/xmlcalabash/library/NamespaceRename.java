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

import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.util.ProcessMatchingNodes;
import com.xmlcalabash.util.ProcessMatch;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.model.RuntimeValue;
import net.sf.saxon.s9api.*;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.NamePool;

import javax.xml.XMLConstants;

import com.xmlcalabash.runtime.XAtomicStep;

public class NamespaceRename extends DefaultStep implements ProcessMatchingNodes {
    private static final QName _from = new QName("from");
    private static final QName _to = new QName("to");
    private static final QName _apply_to = new QName("apply-to");
    private ReadablePipe source = null;
    private WritablePipe result = null;
    private ProcessMatch matcher = null;
    private String from = null;
    private String to = null;
    private String applyTo = null;

    /** Creates a new instance of NamespaceRename */
    public NamespaceRename(XProcRuntime runtime, XAtomicStep step) {
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

        if (getOption(_from) != null) {
            from = getOption(_from).getString();
        } else {
            from = "";
        }

        if (getOption(_to) != null) {
            to = getOption(_to).getString();
        } else {
            to = "";
        }

        applyTo = getOption(_apply_to, "all");

        if (XMLConstants.XML_NS_URI.equals(from) || XMLConstants.XML_NS_URI.equals(to)
                || XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(from) || XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(to)) {
            throw XProcException.stepError(14);
        }

        if (from.equals(to)) {
            result.write(stepContext, source.read(stepContext));
        } else {
            matcher = new ProcessMatch(runtime, this);
            matcher.match(source.read(stepContext), new RuntimeValue("*", step.getNode()));
            result.write(stepContext, matcher.getResult());
        }

        if (source.moreDocuments(stepContext)) {
            throw XProcException.dynamicError(6);
        }
    }

    public boolean processStartDocument(XdmNode node) throws SaxonApiException {
        return true;
    }

    public void processEndDocument(XdmNode node) throws SaxonApiException {
        matcher.addEndElement();
    }

    public boolean processStartElement(XdmNode node) throws SaxonApiException {
        NodeInfo inode = node.getUnderlyingNode();
        NamePool pool = inode.getNamePool();
        int inscopeNS[] = inode.getDeclaredNamespaces(null);
        int newNS[] = null;
        int nameCode = inode.getNameCode();
        int typeCode = inode.getTypeAnnotation() & NamePool.FP_MASK;

        if ("attributes".equals(applyTo)) {
            matcher.addStartElement(nameCode, typeCode, inscopeNS);
        } else {
            if (inscopeNS.length > 0) {
                int countNS = 0;

                for (int pos = 0; pos < inscopeNS.length; pos++) {
                    int ns = inscopeNS[pos];
                    String uri = pool.getURIFromNamespaceCode(ns);
                    if (!from.equals(uri) || !"".equals(to)) {
                        countNS++;
                    }
                }

                newNS = new int[countNS];
                int newPos = 0;
                for (int pos = 0; pos < inscopeNS.length; pos++) {
                    int ns = inscopeNS[pos];
                    String pfx = pool.getPrefixFromNamespaceCode(ns);
                    String uri = pool.getURIFromNamespaceCode(ns);
                    if (from.equals(uri)) {
                        if ("".equals(to)) {
                            // Nevermind, we're throwing the namespace away
                        } else {
                            int newns = pool.getNamespaceCode(pfx,to);
                            if (newns < 0) {
                                newns = pool.allocateNamespaceCode(pfx,to);
                            }
                            newNS[newPos++] = newns;
                        }
                    } else {
                        newNS[newPos++] = ns;
                    }
                }
            }

            // Careful, we're messing with the namespace bindings
            // Make sure the nameCode is right...
            String pfx = pool.getPrefix(nameCode);
            String uri = pool.getURI(nameCode);

            if (from.equals(uri)) {
                if ("".equals(to)) {
                    pfx = "";
                }

                nameCode = pool.allocate(pfx,to,node.getNodeName().getLocalName());
            }

            matcher.addStartElement(nameCode, typeCode, newNS);
        }

        if (!"elements".equals(applyTo)) {
            XdmSequenceIterator iter = node.axisIterator(Axis.ATTRIBUTE);
            while (iter.hasNext()) {
                XdmNode attr = (XdmNode) iter.next();
                inode = attr.getUnderlyingNode();
                pool = inode.getNamePool();
                nameCode = inode.getNameCode();
                typeCode = inode.getTypeAnnotation() & NamePool.FP_MASK;
                String pfx = pool.getPrefix(nameCode);
                String uri = pool.getURI(nameCode);

                if (from.equals(uri)) {
                    if ("".equals(pfx)) {
                        pfx = "_1";
                    }
                    nameCode = pool.allocate(pfx,to,attr.getNodeName().getLocalName());
                }
                matcher.addAttribute(nameCode, typeCode, attr.getStringValue());
            }
        } else {
            matcher.addAttributes(node);
        }

        return true;
    }

    public void processEndElement(XdmNode node) throws SaxonApiException {
        matcher.addEndElement();
    }

    public void processText(XdmNode node) throws SaxonApiException {
        matcher.addText(node.getStringValue());
    }

    public void processComment(XdmNode node) throws SaxonApiException {
        matcher.addComment(node.getStringValue());
    }

    public void processPI(XdmNode node) throws SaxonApiException {
        matcher.addPI(node.getNodeName().getLocalName(), node.getStringValue());
    }

    public void processAttribute(XdmNode node) throws SaxonApiException {
        throw new UnsupportedOperationException("processAttribute can't be called in NamespaceRename--but it was!?");
    }
}

