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
package com.xmlcalabash.util;

import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.QName;

import javax.xml.transform.TransformerException;
import javax.xml.transform.SourceLocator;
import javax.xml.transform.ErrorListener;

import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcConstants;
import net.sf.saxon.trans.XPathException;

import java.net.URI;

public class StepErrorListener implements ErrorListener {
    private static QName c_error = new QName(XProcConstants.NS_XPROC_STEP, "error");
    private static QName _name = new QName("", "name");
    private static QName _type = new QName("", "type");
    private static QName _href = new QName("", "href");
    private static QName _line = new QName("", "line");
    private static QName _column = new QName("", "column");
    private static QName _code = new QName("", "code");

    private XProcRuntime runtime = null;
    private URI baseURI = null;

    public StepErrorListener(XProcRuntime runtime) {
        super();
        this.runtime = runtime;
        baseURI = runtime.getStaticBaseURI();
    }

    public void error(TransformerException exception) throws TransformerException {
        if (!report("error", exception)) {
            runtime.error(exception);
        }
    }

    public void fatalError(TransformerException exception) throws TransformerException {
        if (!report("fatal-error", exception)) {
            runtime.error(exception);
        }
    }

    public void warning(TransformerException exception) throws TransformerException {
        if (!report("warning", exception)) {
            // XProc doesn't have recoverable exceptions...
            runtime.error(exception);
        }
    }

    private boolean report(String type, TransformerException exception) {
        TreeWriter writer = new TreeWriter(runtime);

        writer.startDocument(baseURI);
        writer.addStartElement(c_error);
        writer.addAttribute(_type, type);

        StructuredQName qCode = null;
        if (exception instanceof XPathException) {
            XPathException xxx = (XPathException) exception;
            qCode = xxx.getErrorCodeQName();
            //qCode = ((XPathException) exception).getErrorCodeQName();
        }
        if (qCode == null && exception.getException() instanceof XPathException) {
            qCode = ((XPathException) exception.getException()).getErrorCodeQName();
        }
        if (qCode != null) {
            writer.addAttribute(_code, qCode.getDisplayName());
        }

        if (exception.getLocator() != null) {
            SourceLocator loc = exception.getLocator();
            boolean done = false;
            while (!done && loc == null) {
                if (exception.getException() instanceof TransformerException) {
                    exception = (TransformerException) exception.getException();
                    loc = exception.getLocator();
                } else if (exception.getCause() instanceof TransformerException) {
                    exception = (TransformerException) exception.getCause();
                    loc = exception.getLocator();
                } else {
                    done = true;
                }
            }

            if (loc != null) {
                if (loc.getSystemId() != null && !"".equals(loc.getSystemId())) {
                    writer.addAttribute(_href, loc.getSystemId());
                }

                if (loc.getLineNumber() != -1) {
                    writer.addAttribute(_line, ""+loc.getLineNumber());
                }

                if (loc.getColumnNumber() != -1) {
                    writer.addAttribute(_column, ""+loc.getColumnNumber());
                }
            }
        }


        writer.startContent();
        writer.addText(exception.toString());
        writer.addEndElement();
        writer.endDocument();

        XdmNode node = writer.getResult();

        return runtime.getXProcData().catchError(node);
    }
}