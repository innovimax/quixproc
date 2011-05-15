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

package com.xmlcalabash.core;

import com.xmlcalabash.util.URIUtils;
import net.sf.saxon.s9api.QName;
import com.xmlcalabash.model.Step;
import net.sf.saxon.s9api.XdmNode;

import javax.xml.transform.SourceLocator;
import java.net.URI;

/**
 *
 * @author ndw
 */
public class XProcException extends RuntimeException {
    public static final QName err_E0001 = new QName(XProcConstants.NS_XPROC_ERROR_EX, "XE0001"); // invalid pipeline
    public static final QName err_E0002 = new QName(XProcConstants.NS_XPROC_ERROR_EX, "XE0002"); // invalid configuration

    private QName error = null;
    private Step step = null;
    private XdmNode node = null;

    /** Creates a new instance of XProcException */
    public XProcException() {
        super();
    }

    /** Creates a new instance of XProcException */
    public XProcException(String message) {
        super(message);
    }
    
    /** Creates a new instance of XProcException */
    public XProcException(Step step, String message) {
        super(message);
        this.step = step;
    }

    /** Creates a new instance of XProcException */
    public XProcException(XdmNode node, String message) {
        super(message);
        this.node = node;
    }

    /** Creates a new instance of XProcException */
    public XProcException(String message, Throwable cause) {
        super(message, cause);
    }

    /** Creates a new instance of XProcException */
    public XProcException(Step step, String message, Throwable cause) {
        super(message, cause);
        this.step = step;
    }

    /** Creates a new instance of XProcException */
    public XProcException(Throwable cause) {
        super(cause);
    }

    /** Creates a new instance of XProcException */
    public XProcException(XdmNode node, Throwable cause) {
        super(cause);
        this.node = node;
    }

    /** Creates a new instance of XProcException */
    public XProcException(XdmNode node, String message, Throwable cause) {
        super(message, cause);
        this.node = node;
    }

    public XProcException(QName errorCode) {
    	super(errorCode.getLocalName());
        error = errorCode;
    }

    public XProcException(Step step, QName errorCode) {
    	super(errorCode.getLocalName());
        error = errorCode;
        this.step = step;
    }

    public XProcException(QName errorCode, String message) {
        super(message);
        error = errorCode;
    }

    public XProcException(Step step, QName errorCode, String message) {
        super(message);
        error = errorCode;
        this.step = step;
    }

    public XProcException(QName errorCode, XdmNode node, Throwable cause, String message) {
        super(message,cause);
        error = errorCode;
        this.node = node;
    }

    public XProcException(QName errorCode, XdmNode node, String message) {
        super(message);
        error = errorCode;
        this.node = node;
    }

    public XProcException(QName errorCode, Throwable cause) {
        super("XProc error err:" + errorCode.getLocalName(), cause);
        error = errorCode;
    }

    public static XProcException staticError(int errno) {
        return new XProcException(XProcConstants.staticError(errno));
    }

    public static XProcException staticError(int errno, String message) {
        return new XProcException(XProcConstants.staticError(errno), message);
    }

    public static XProcException staticError(int errno, XdmNode node, String message) {
        return new XProcException(XProcConstants.staticError(errno), node, message);
    }

    public static XProcException staticError(int errno, XdmNode node, Throwable cause, String message) {
        return new XProcException(XProcConstants.staticError(errno), node, cause, message);
    }

    public static XProcException staticError(int errno, Exception except) {
        return new XProcException(XProcConstants.staticError(errno), except);
    }

    public static XProcException dynamicError(int errno) {
        return new XProcException(XProcConstants.dynamicError(errno));
    }

    public static XProcException dynamicError(Step step, int errno) {
        return new XProcException(step, XProcConstants.dynamicError(errno));
    }

    public static XProcException dynamicError(int errno, String message) {
        return new XProcException(XProcConstants.dynamicError(errno), message);
    }

    public static XProcException dynamicError(int errno, XdmNode node, String message) {
        return new XProcException(XProcConstants.dynamicError(errno), node, message);
    }

    public static XProcException dynamicError(int errno, XdmNode node, Exception except, String message) {
        return new XProcException(XProcConstants.dynamicError(errno), node, except, message);
    }

    public static XProcException dynamicError(Step step, int errno, String message) {
        return new XProcException(step, XProcConstants.dynamicError(errno), message);
    }

    public static XProcException dynamicError(int errno, Exception except) {
        return new XProcException(XProcConstants.dynamicError(errno), except);
    }

    public static XProcException stepError(int errno) {
        return new XProcException(XProcConstants.stepError(errno));
    }

    public static XProcException stepError(int errno, String message) {
        return new XProcException(XProcConstants.stepError(errno), message);
    }

    public static XProcException stepError(int errno, Exception except) {
        return new XProcException(XProcConstants.stepError(errno), except);
    }

    public QName getErrorCode() {
        return error;
    }

    public Step getStep() {
        return step;
    }

    public SourceLocator getLocator() {
        XdmNode locNode = null;
        if (step != null) locNode = step.getNode();
        if (node != null) locNode = node;
        return new ExceptionLocation(locNode);
    }

    private class ExceptionLocation implements SourceLocator {
        private int line = -1;
        private int col = -1;
        private String systemId = null;

        public ExceptionLocation(XdmNode node) {
            if (node != null) {
                URI cwd = URIUtils.cwdAsURI();
                systemId = cwd.relativize(node.getBaseURI()).toASCIIString();
                line = node.getLineNumber();
                col = node.getColumnNumber();
            }
        }

        public String getPublicId() {
            return null;
        }

        public String getSystemId() {
            return systemId;
        }

        public int getLineNumber() {
            return line;
        }

        public int getColumnNumber() {
            return col;
        }
    }
}