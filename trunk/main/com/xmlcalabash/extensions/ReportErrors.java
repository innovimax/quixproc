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
package com.xmlcalabash.extensions;

import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmSequenceIterator;

import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.library.DefaultStep;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.runtime.XAtomicStep;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Oct 8, 2008
 * Time: 7:44:07 AM
 * To change this template use File | Settings | File Templates.
 */

public class ReportErrors extends DefaultStep {
    private static final QName c_error = new QName(XProcConstants.NS_XPROC_STEP, "error");
    private static final QName _code = new QName("code");
    private static final QName _code_prefix = new QName("code-prefix");
    private static final QName _code_namespace = new QName("code-namespace");
    private ReadablePipe source = null;
    private ReadablePipe report = null;
    private WritablePipe result = null;

    /**
     * Creates a new instance of Identity
     */
    public ReportErrors(XProcRuntime runtime, XAtomicStep step) {
        super(runtime,step);
    }

    public void setInput(String port, ReadablePipe pipe) {
        if ("source".equals(port)) {
            source = pipe;
        } else {
            report = pipe;
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

        String codeNameStr = null;
        String cpfx = null;
        String cns = null;
        
        RuntimeValue codeNameValue = getOption(_code);
        if (codeNameValue != null) {
            codeNameStr = codeNameValue.getString();
            cpfx = getOption(_code_prefix, (String) null);
            cns = getOption(_code_namespace, (String) null);
        }

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
        if (codeNameStr != null) {
            if (codeNameStr.contains(":")) {
                errorCode = new QName(codeNameStr, codeNameValue.getNode());
            } else {
                errorCode = new QName(cpfx == null ? "" : cpfx, cns, codeNameStr);
            }
        }

        while (report.moreDocuments(stepContext)) {
            XdmNode doc = report.read(stepContext);
            XdmSequenceIterator iter = doc.axisIterator(Axis.DESCENDANT, c_error);
            if (iter.hasNext()) {
                while (iter.hasNext()) {
                    runtime.warning(this, doc, iter.next().getStringValue());
                }
            } else {
                runtime.warning(this, doc, doc.getStringValue());
            }
        }

        if (errorCode != null) {
            throw new XProcException(errorCode, "error code?");
        }

        while (source.moreDocuments(stepContext)) {
            XdmNode doc = source.read(stepContext);
            runtime.finest(this, step.getNode(), "ReportErrors step " + step.getName() + " read " + doc.getDocumentURI());
            result.write(stepContext,doc);
        }
    }
}