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
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.util.TreeWriter;
import com.xmlcalabash.io.ReadablePipe;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import com.xmlcalabash.runtime.XAtomicStep;

public class Error extends DefaultStep {
    private static final QName c_error = new QName("c", XProcConstants.NS_XPROC_STEP, "error");
    private static final QName _name = new QName("name");
    private static final QName _code = new QName("code");
    private static final QName _code_prefix = new QName("code-prefix");
    private static final QName _code_namespace = new QName("code-namespace");
    private static final QName _type = new QName("type");
    private ReadablePipe source = null;

    /** Creates a new instance of Delete */
    public Error(XProcRuntime runtime, XAtomicStep step) {
        super(runtime,step);
    }

    public void setInput(String port, ReadablePipe pipe) {
        source = pipe;
    }

    public void setOutput(String port, WritablePipe pipe) {
        // p:error always throws an exception, so who cares.
    }

    public void reset() {
        source.resetReader(stepContext);
    }

    public void gorun() throws SaxonApiException {
        super.gorun();

        XdmNode doc = source.read(stepContext);
        finest(null, "Error step " + "???" + " read " + doc.getDocumentURI());

        RuntimeValue codeNameValue = getOption(_code);
        String codeNameStr = codeNameValue.getString();
        String cpfx = getOption(_code_prefix, (String) null);
        String cns = getOption(_code_namespace, (String) null);

        if (cpfx == null && cns != null) {
            cpfx = "ERR";
        }

        if (cpfx != null && cns == null) {
            throw XProcException.dynamicError(34, "You can't specify a prefix without a namespace");
        }

        if (cns != null && codeNameStr.contains(":")) {
            throw XProcException.dynamicError(34, "You can't specify a namespace if the code name contains a colon");
        }

        QName errorCode = null;
        if (codeNameStr.contains(":")) {
            errorCode = new QName(codeNameStr, codeNameValue.getNode());
        } else {
            errorCode = new QName(cpfx == null ? "" : cpfx, cns, codeNameStr);
        }

        cpfx = errorCode.getPrefix();
        cns = errorCode.getNamespaceURI();

        TreeWriter treeWriter = new TreeWriter(runtime);
        treeWriter.startDocument(step.getNode().getBaseURI());
        treeWriter.addStartElement(c_error);
        treeWriter.addNamespace(cpfx, cns);

        treeWriter.addAttribute(_name, step.getName());
        treeWriter.addAttribute(_type, "p:error");
        treeWriter.addAttribute(_code, errorCode.toString());
        treeWriter.startContent();
        treeWriter.addSubtree(doc);
        treeWriter.addEndElement();
        treeWriter.endDocument();

        step.reportError(treeWriter.getResult());

        if (errorCode != null) {
            throw new XProcException(errorCode, doc.getStringValue());
        } else {
            throw new XProcException();
        }
    }
}

