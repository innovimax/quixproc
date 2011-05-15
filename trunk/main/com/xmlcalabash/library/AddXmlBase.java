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

import java.net.URI;
import java.util.Stack;

import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.util.ProcessMatch;
import com.xmlcalabash.util.ProcessMatchingNodes;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.model.RuntimeValue;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmSequenceIterator;
import net.sf.saxon.s9api.Axis;

import javax.xml.XMLConstants;

import com.xmlcalabash.runtime.XAtomicStep;

public class AddXmlBase extends DefaultStep implements ProcessMatchingNodes {
    private static final QName xml_base = new QName(XMLConstants.XML_NS_URI, "base");
    private static final QName _all = new QName("", "all");
    private static final QName _relative = new QName("", "relative");
    private ProcessMatch matcher = null;
    private ReadablePipe source = null;
    private WritablePipe result = null;
    private boolean all = false;
    private boolean relative = false;
    private Stack<URI> baseURIStack = new Stack<URI> ();
        
    /** Creates a new instance of AddXmlBase */
    public AddXmlBase(XProcRuntime runtime, XAtomicStep step) {
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

        all = getOption(_all, false);
        relative = getOption(_relative, true);

        if (all && relative) {
            throw XProcException.stepError(58);
        }

        matcher = new ProcessMatch(runtime, this);
        matcher.match(source.read(stepContext), new RuntimeValue("*", step.getNode()));

        result.write(stepContext, matcher.getResult());
    }

    public boolean processStartDocument(XdmNode node) {
        return true;
    }

    public void processEndDocument(XdmNode node) {
        // nop
    }

    public boolean processStartElement(XdmNode node) throws SaxonApiException {
        String xmlBase = node.getBaseURI().toASCIIString();
        boolean addXmlBase = all || baseURIStack.size() == 0;
        if (!addXmlBase) {
            addXmlBase = !baseURIStack.peek().equals(node.getBaseURI());
        }

        if (addXmlBase && relative && baseURIStack.size() > 0) {
            // FIXME: What about non-hierarchical URIs?
            // Java.net.URI.relativize doesn't do what you think
            URI relURI = node.getBaseURI();
            String p1 = baseURIStack.peek().toASCIIString();
            String p2 = relURI.toASCIIString();

            boolean commonancestor = false;
            int i1 = p1.indexOf("/");
            int i2 = p2.indexOf("/");
            while (i1 >= 0 && i2 >= 0 && p1.substring(0, i1).equals(p2.substring(0,i2))) {
                commonancestor = true;
                p1 = p1.substring(i1+1);
                p2 = p2.substring(i2+1);
                i1 = p1.indexOf("/");
                i2 = p2.indexOf("/");
            }

            if (commonancestor) {
                String walkUp = "";
                i1 = p1.indexOf("/");
                while (i1 >= 0) {
                    walkUp += "../";
                    p1 = p1.substring(i1+1);
                    i1 = p1.indexOf("/");
                }
                xmlBase = walkUp + p2;
                p1 = "5";
            } else {
                xmlBase = relURI.toASCIIString();
            }
        }

        baseURIStack.push(node.getBaseURI());

        matcher.addStartElement(node);

        boolean found = false;
        XdmSequenceIterator iter = node.axisIterator(Axis.ATTRIBUTE);
        while (iter.hasNext()) {
            XdmNode child = (XdmNode) iter.next();
            if (child.getNodeName().equals(xml_base)) {
                found = true;
                if ((all || addXmlBase || !node.getAttributeValue(xml_base).equals(xmlBase))
                    && !"".equals(xmlBase)) {
                    matcher.addAttribute(child, xmlBase);
                }
            } else {
                matcher.addAttribute(child, child.getStringValue());
            }
        }

        if (!found && addXmlBase) {
            matcher.addAttribute(xml_base, xmlBase);
        }
        
        return true;
    }

    public void processEndElement(XdmNode node) throws SaxonApiException {
        matcher.addEndElement();
        baseURIStack.pop();
    }

    public void processText(XdmNode node) throws SaxonApiException {
        throw new UnsupportedOperationException("This can't happen");
    }

    public void processComment(XdmNode node) throws SaxonApiException {
        throw new UnsupportedOperationException("This can't happen");
    }

    public void processPI(XdmNode node) throws SaxonApiException {
        throw new UnsupportedOperationException("This can't happen");
    }

    public void processAttribute(XdmNode node) throws SaxonApiException {
        throw new UnsupportedOperationException("This can't happen");
    }
}
