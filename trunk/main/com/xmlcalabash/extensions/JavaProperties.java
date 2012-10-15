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

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;

import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;

import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.library.DefaultStep;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.TreeWriter;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Oct 8, 2008
 * Time: 7:44:07 AM
 * To change this template use File | Settings | File Templates.
 */

public class JavaProperties extends DefaultStep {
    private static final QName c_param_set = new QName("c", XProcConstants.NS_XPROC_STEP, "param-set");
    private static final QName c_param = new QName("c", XProcConstants.NS_XPROC_STEP, "param");
    private static final QName _href = new QName("","href");
    private static final QName _name = new QName("name");
    private static final QName _namespace = new QName("namespace");
    private static final QName _value = new QName("value");
    private WritablePipe result = null;

    /**
     * Creates a new instance of Identity
     */
    public JavaProperties(XProcRuntime runtime, XAtomicStep step) {
        super(runtime,step);
    }

    public void setOutput(String port, WritablePipe pipe) {
        result = pipe;
    }

    public void reset() {
        result.resetWriter(stepContext);
    }

    public void gorun() throws SaxonApiException {
        super.gorun();

        Properties properties = new Properties();

        String pFn = null;
        URI pURI = null;

        if (getOption(_href) != null) {
            pFn = getOption(_href).getString();
            pURI = getOption(_href).getBaseURI();
        }

        if (pURI == null) {
            properties = System.getProperties();
        } else {
            try {
                URL url = pURI.resolve(pFn).toURL();
                URLConnection connection = url.openConnection();
                InputStream stream = connection.getInputStream();
                properties.load(stream);
            } catch (MalformedURLException mue) {
                throw new XProcException(XProcException.err_E0001, mue);
            } catch (IOException ioe) {
                throw new XProcException(XProcException.err_E0001, ioe);
            }
        }

        TreeWriter tree = new TreeWriter(runtime);
        tree.startDocument(step.getNode().getBaseURI());
        tree.addStartElement(c_param_set);
        tree.startContent();

        for (String name : properties.stringPropertyNames()) {
            String value = properties.getProperty(name);
            tree.addStartElement(c_param);
            tree.addAttribute(_name, name);
            tree.addAttribute(_namespace, "");
            tree.addAttribute(_value, value);
            tree.startContent();
            tree.addEndElement();
        }

        tree.addEndElement();
        tree.endDocument();
        result.write(stepContext,tree.getResult());
    }
}