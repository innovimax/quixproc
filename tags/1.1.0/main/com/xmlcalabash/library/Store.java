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

package com.xmlcalabash.library;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URLConnection;

import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XdmNode;

import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.Base64;
import com.xmlcalabash.util.JSONtoXML;
import com.xmlcalabash.util.S9apiUtils;
import com.xmlcalabash.util.TreeWriter;
import com.xmlcalabash.util.XMLtoJSON;

/**
 *
 * @author ndw
 */
public class Store extends DefaultStep {
    private static final QName _href = new QName("href");
    private static final QName _encoding = new QName("encoding");
    private static final QName _content_type = new QName("content-type");
    private static final QName c_encoding = new QName("c", XProcConstants.NS_XPROC_STEP, "encoding");
    private static final QName c_body = new QName("c", XProcConstants.NS_XPROC_STEP, "body");
    private static final QName c_json = new QName("c", XProcConstants.NS_XPROC_STEP, "json");
    private static final QName cx_decode = new QName("cx", XProcConstants.NS_CALABASH_EX, "decode");

    private ReadablePipe source = null;
    private WritablePipe result = null;

    /**
     * Creates a new instance of Store
     */
    public Store(XProcRuntime runtime, XAtomicStep step) {
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

    // Innovimax: modified function
    public void gorun() throws SaxonApiException {
        super.gorun();

        if (runtime.getSafeMode()) {
            throw XProcException.dynamicError(21);
        }

        URI href = null;
        RuntimeValue hrefOpt = getOption(_href);

        XdmNode doc = source.read(stepContext);

        if (doc == null || source.moreDocuments(stepContext)) {
            throw XProcException.dynamicError(6);
        }

        // Innovimax: external base uri
        /*if (hrefOpt != null) {
            href = hrefOpt.getBaseURI().resolve(hrefOpt.getString());
        } else {
            href = doc.getBaseURI();
        }*/
        if (hrefOpt != null) {
            href = runtime.getQConfig().resolveURI(hrefOpt.getBaseURI(), hrefOpt.getString()); 
        } else {
            href = runtime.getQConfig().getBaseURI(doc.getBaseURI());  
        }          

        finer(hrefOpt.getNode(), "Storing to \"" + href + "\".");

        String decode = step.getExtensionAttribute(cx_decode);
        XdmNode root = S9apiUtils.getDocumentElement(doc);
        if (("true".equals(decode) || "1".equals(decode))
             && ((XProcConstants.NS_XPROC_STEP.equals(root.getNodeName().getNamespaceURI())
                  && "base64".equals(root.getAttributeValue(_encoding)))
                 || ("".equals(root.getNodeName().getNamespaceURI())
                     && "base64".equals(root.getAttributeValue(c_encoding))))) {
            storeBinary(doc, href);
        } else if (runtime.transparentJSON()
                   && (((c_body.equals(root.getNodeName())
                        && ("application/json".equals(root.getAttributeValue(_content_type))
                            || "text/json".equals(root.getAttributeValue(_content_type))))
                       || c_json.equals(root.getNodeName()))
                       || JSONtoXML.JSONX_NS.equals(root.getNodeName().getNamespaceURI())
                       || JSONtoXML.JXML_NS.equals(root.getNodeName().getNamespaceURI())
                       || JSONtoXML.MLJS_NS.equals(root.getNodeName().getNamespaceURI()))) {
            storeJSON(doc, href);
        } else {
            storeXML(doc, href);
        }

        TreeWriter tree = new TreeWriter(runtime);
        tree.startDocument(step.getNode().getBaseURI());
        tree.addStartElement(XProcConstants.c_result);
        tree.startContent();
        tree.addText(href.toString());
        tree.addEndElement();
        tree.endDocument();
        result.write(stepContext,tree.getResult());
    }

    // Innovimax: modified function
    private void storeXML(XdmNode doc, URI href) throws SaxonApiException {
        Serializer serializer = makeSerializer();

        try {
            OutputStream outstr;
            if(href.getScheme().equals("file")) {
                File output = new File(href);

                File path = new File(output.getParent());
                if (!path.isDirectory()) {
                    if (!path.mkdirs()) {
                        throw XProcException.stepError(50);
                    }
                }
                outstr = new FileOutputStream(output);
            } else {
                final URLConnection conn = href.toURL().openConnection();
                conn.setDoOutput(true);
                outstr = conn.getOutputStream();
            }

            serializer.setOutputStream(outstr);
            S9apiUtils.serialize(runtime, doc, serializer);
            outstr.close();
            // Innovimax: statistics    
            File output = new File(href);                  
            totalFileSize += output.length();               
        } catch (IOException ioe) {
            throw XProcException.stepError(50, ioe);
        }
    }

    // Innovimax: modified function
    private void storeBinary(XdmNode doc, URI href) {
        try {
            byte[] decoded = Base64.decode(doc.getStringValue());
            File output = new File(href);

            File path = new File(output.getParent());
            if (!path.isDirectory()) {
                if (!path.mkdirs()) {
                    throw XProcException.stepError(50);
                }
            }

            FileOutputStream outstr = new FileOutputStream(output);
            outstr.write(decoded);
            outstr.close();
            // Innovimax: statistics                      
            totalFileSize += output.length();                 
        } catch (IOException ioe) {
            throw new XProcException(ioe);
        }
    }

    // Innovimax: modified function
    private void storeJSON(XdmNode doc, URI href) throws SaxonApiException {
        try {
            OutputStream outstr;
            if(href.getScheme().equals("file")) {
                File output = new File(href);

                File path = new File(output.getParent());
                if (!path.isDirectory()) {
                    if (!path.mkdirs()) {
                        throw XProcException.stepError(50);
                    }
                }
                outstr = new FileOutputStream(output);
            } else {
                final URLConnection conn = href.toURL().openConnection();
                conn.setDoOutput(true);
                outstr = conn.getOutputStream();
            }

            PrintWriter writer = new PrintWriter(outstr);
            String json = XMLtoJSON.convert(doc);
            writer.print(json);
            writer.close();
            outstr.close();
            // Innovimax: statistics      
            File output = new File(href);                             
            totalFileSize += output.length();                 
        } catch (IOException ioe) {
            throw XProcException.stepError(50, ioe);
        }
    }
    
    //*************************************************************************
    //*************************************************************************        
    //*************************************************************************
    // INNOVIMAX IMPLEMENTATION
    //*************************************************************************
    //*************************************************************************
    //************************************************************************* 
    
    // Innovimax: statistics  
    private static long totalFileSize = 0; 
    public static long getTotalFileSize() { return totalFileSize; }         
    public static void resetTotalFileSize() { totalFileSize = 0; }     
}

