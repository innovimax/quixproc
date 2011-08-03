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

import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.S9apiUtils;
import com.xmlcalabash.util.TreeWriter;
import com.xmlcalabash.model.RuntimeValue;

import org.apache.fop.apps.*;
//import com.renderx.xep.FormatterImpl;
//import com.renderx.xep.FOTarget;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import java.util.Properties;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;

import com.xmlcalabash.util.URIUtils;
import org.xml.sax.InputSource;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;

public class XSLFormatter extends DefaultStep {
    private static final QName _href = new QName("","href");
    private static final QName _content_type = new QName("","content-type");

    private FopFactory fopFactory = FopFactory.newInstance();

    private ReadablePipe source = null;
    private WritablePipe result = null;
    private Properties options = new Properties();

    /** Creates a new instance of Unzip */
    public XSLFormatter(XProcRuntime runtime, XAtomicStep step) {
        super(runtime,step);
    }

    public void setInput(String port, ReadablePipe pipe) {
        source = pipe;
    }

    public void setOutput(String port, WritablePipe pipe) {
        result = pipe;
    }

    public void setParameter(QName name, RuntimeValue value) {
        if (!"".equals(name.getNamespaceURI())) {
            throw new XProcException(step.getNode(), "The p:xsl-formatter parameters are in no namespace: " + name + " (" + name.getNamespaceURI() + ")");
        }
        options.setProperty(name.getLocalName(), value.getString());
    }

    public void reset() {
        result.resetWriter(stepContext);
    }

    public void gorun() throws SaxonApiException {
        super.gorun();

        String contentType = getOption(_content_type).getString();
        String href = getOption(_href).getBaseURI().resolve(getOption(_href).getString()).toASCIIString();
        String output = null;

        if (href.startsWith("file:/")) {
            output = URIUtils.getFile(href).getPath();
        } else {
            throw new XProcException(step.getNode(), "Don't know how to write p:xsl-formatter output to " + href);
        }

        String outputFormat = null;
        
        if (contentType == null || "application/pdf".equalsIgnoreCase(contentType)) {
            outputFormat = MimeConstants.MIME_PDF; // "PDF";
        } else if ("application/PostScript".equalsIgnoreCase(contentType)) {
            outputFormat = MimeConstants.MIME_POSTSCRIPT; //"PostScript";
        } else if ("application/afp".equalsIgnoreCase(contentType)) {
            outputFormat =  MimeConstants.MIME_AFP;  //"AFP";
        } else if ("application/rtf".equalsIgnoreCase(contentType)) {
            outputFormat = MimeConstants.MIME_RTF;
        } else if ("text/plain".equalsIgnoreCase(contentType)) {
           outputFormat = MimeConstants.MIME_PLAIN_TEXT;            
        } else {
            throw new XProcException(step.getNode(), "Unsupported content-type on p:xsl-formatter: " + contentType);
        }

 //       FormatterImpl xep = null;
 //       try {
 //           xep = new FormatterImpl(options);
 //       }
 //        catch (Exception e) {
 //            throw new XProcException(e);
 //       }

        try {
            InputSource doc = S9apiUtils.xdmToInputSource(runtime, source.read(stepContext));
            SAXSource saxdoc = new SAXSource(doc);

            OutputStream out = null;
            out = new BufferedOutputStream(new FileOutputStream(output));

            try {
                try {
//                xep.render(saxdoc, new FOTarget(out, outputFormat));
           
                
                      // No URI Resolver : fopFactory.setURIResolver(uriResolver);
                      Fop fop = fopFactory.newFop(outputFormat, out);
                      FOUserAgent userAgent = fop.getUserAgent();
                      userAgent.setBaseURL(step.getNode().getBaseURI().toString());
                      TransformerFactory transformerFactory = TransformerFactory.newInstance();
                      Transformer transformer = transformerFactory.newTransformer();
                      transformer.transform(saxdoc, new SAXResult(fop.getDefaultHandler()));
                } catch (Exception e) {
                    throw new XProcException(e);
                }
                finally {
                    out.close();
                }
            }
            catch (Exception e) {
                throw new XProcException(e);
            }
        } catch (Exception e) {
            throw new XProcException(e);
        } finally {
           // xep.cleanup();
        }

        TreeWriter tree = new TreeWriter(runtime);
        tree.startDocument(step.getNode().getBaseURI());
        tree.addStartElement(XProcConstants.c_result);
        tree.startContent();
        tree.addText(href);
        tree.addEndElement();
        tree.endDocument();
        result.write(stepContext, tree.getResult());
    }
}
