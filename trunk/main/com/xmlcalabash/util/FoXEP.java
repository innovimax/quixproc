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
package com.xmlcalabash.util;

import java.io.OutputStream;
import java.util.Properties;

import javax.xml.transform.sax.SAXSource;

import net.sf.saxon.s9api.XdmNode;

import org.xml.sax.InputSource;

import com.renderx.xep.FOTarget;
import com.renderx.xep.FormatterImpl;
import com.renderx.xep.lib.ConfigurationException;
import com.renderx.xep.lib.Logger;
import com.xmlcalabash.config.FoProcessor;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.runtime.XStep;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: 9/1/11
 * Time: 6:42 AM
 * To change this template use File | Settings | File Templates.
 */
public class FoXEP implements FoProcessor {
    XProcRuntime runtime = null;
    FormatterImpl xep = null;
    XStep step = null;

    public void initialize(XProcRuntime runtime, XStep step, Properties options) {
        this.runtime = runtime;
        this.step = step;
        try {
            xep = new FormatterImpl(options, new FoLogger());
        } catch (ConfigurationException ce) {
            throw new XProcException("Failed to initialize XEP", ce);
        }
    }

    public void format(XdmNode doc, OutputStream out, String contentType) {
        String outputFormat = null;
        if (contentType == null || "application/pdf".equals(contentType)) {
            outputFormat = "PDF";
        } else if ("application/PostScript".equals(contentType)) {
            outputFormat = "PostScript";
        } else if ("application/afp".equals(contentType)) {
            outputFormat = "AFP";
        } else {
            throw new XProcException(step.getNode(), "Unsupported content-type on p:xsl-formatter: " + contentType);
        }

        try {
            InputSource fodoc = S9apiUtils.xdmToInputSource(runtime, doc);
            SAXSource source = new SAXSource(fodoc);
            xep.render(source, new FOTarget(out, outputFormat));
        } catch (Exception e) {
            throw new XProcException(step.getNode(), "Failed to process FO document with XEP", e);
        } finally {
            xep.cleanup();
        }
    }

    private class FoLogger implements Logger {

        public void openDocument() {
            step.fine(step.getNode(), "p:xsl-formatter document processing starts");
        }

        public void closeDocument() {
            step.fine(step.getNode(), "p:xsl-formatter document processing ends");
        }

        public void event(String name, String message) {
            step.finer(step.getNode(), "p:xsl-formatter processing " + name + ": " + message);
        }

        public void openState(String state) {
            step.finest(step.getNode(), "p:xsl-formatter process start: " + state);
        }

        public void closeState(String state) {
            step.finest(step.getNode(), "p:xsl-formatter process end: " + state);
        }

        public void info(String message) {
            step.info(step.getNode(), message);
        }

        public void warning(String message) {
            step.warning(step.getNode(), message);
        }

        public void error(String message) {
            step.error(step.getNode(), message, XProcConstants.stepError(1)); // FIXME: 1?
        }

        public void exception(String message, Exception exception) {
            throw new XProcException(message, exception);
        }
    }

}
