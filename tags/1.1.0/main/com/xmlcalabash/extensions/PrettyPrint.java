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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.s9api.XsltTransformer;

import org.xml.sax.InputSource;

import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.library.DefaultStep;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.S9apiUtils;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Oct 8, 2008
 * Time: 7:44:07 AM
 * To change this template use File | Settings | File Templates.
 */

public class PrettyPrint extends DefaultStep {
    private ReadablePipe source = null;
    private WritablePipe result = null;
    private static XdmNode prettyPrint = null;

    public PrettyPrint(XProcRuntime runtime, XAtomicStep step) {
        super(runtime,step);

        if (prettyPrint == null) {
            try {
                InputStream instream = getClass().getResourceAsStream("/etc/prettyprint.xsl");
                if (instream == null) {
                    throw new UnsupportedOperationException("Failed to load prettyprint.xsl stylesheet from resources.");
                }
                XdmNode ppd = runtime.parse(new InputSource(instream));
                prettyPrint = S9apiUtils.getDocumentElement(ppd);
            } catch (Exception e) {
                throw new XProcException(e);
            }
        }
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

        XsltCompiler compiler = runtime.getProcessor().newXsltCompiler();
        XsltExecutable exec = compiler.compile(prettyPrint.asSource());
        XsltTransformer transformer = exec.load();
        transformer.setInitialContextNode(source.read(stepContext));

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Serializer serializer = new Serializer();
        serializer.setOutputProperty(Serializer.Property.ENCODING, "utf-8");
        serializer.setOutputProperty(Serializer.Property.INDENT, "yes");
        serializer.setOutputProperty(Serializer.Property.OMIT_XML_DECLARATION, "yes");
        serializer.setOutputProperty(Serializer.Property.METHOD, "xml");

        serializer.setOutputStream(stream);
        transformer.setDestination(serializer);
        transformer.transform();

        XdmNode output = runtime.parse(new InputSource(new ByteArrayInputStream(stream.toByteArray())));
        result.write(stepContext,output);
    }
}