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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XdmNode;

import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;

/**
 *
 * @author ndw
 */
public class NodeToBytes {
    private static final QName _encoding = new QName("encoding");
    private static final QName _content_type = new QName("content-type");
    private static final QName c_encoding = new QName("c", XProcConstants.NS_XPROC_STEP, "encoding");
    private static final QName c_body = new QName("c", XProcConstants.NS_XPROC_STEP, "body");
    private static final QName c_json = new QName("c", XProcConstants.NS_XPROC_STEP, "json");
    private static final QName cx_decode = new QName("cx", XProcConstants.NS_CALABASH_EX, "decode");

    private NodeToBytes() {
        // you aren't allowed to do this
    }

    public static byte[] convert(XProcRuntime runtime, XdmNode doc, boolean decode) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        XdmNode root = S9apiUtils.getDocumentElement(doc);
        if (decode
             && ((XProcConstants.NS_XPROC_STEP.equals(root.getNodeName().getNamespaceURI())
                  && "base64".equals(root.getAttributeValue(_encoding)))
                 || ("".equals(root.getNodeName().getNamespaceURI())
                     && "base64".equals(root.getAttributeValue(c_encoding))))) {
            storeBinary(doc, stream);
        } else if (runtime.transparentJSON()
                   && (((c_body.equals(root.getNodeName())
                        && ("application/json".equals(root.getAttributeValue(_content_type))
                            || "text/json".equals(root.getAttributeValue(_content_type))))
                       || c_json.equals(root.getNodeName()))
                       || JSONtoXML.JSONX_NS.equals(root.getNodeName().getNamespaceURI())
                       || JSONtoXML.JXML_NS.equals(root.getNodeName().getNamespaceURI())
                       || JSONtoXML.MLJS_NS.equals(root.getNodeName().getNamespaceURI()))) {
            storeJSON(doc, stream);
        } else {
            storeXML(runtime, doc, stream);
        }

        return stream.toByteArray();
    }

    private static void storeXML(XProcRuntime runtime, XdmNode doc, OutputStream stream) {
        Serializer serializer = new Serializer();
        serializer.setOutputProperty(Serializer.Property.ENCODING, "utf-8");
        serializer.setOutputProperty(Serializer.Property.INDENT, "no");
        serializer.setOutputProperty(Serializer.Property.OMIT_XML_DECLARATION, "yes");
        serializer.setOutputProperty(Serializer.Property.METHOD, "xml");

        try {
            serializer.setOutputStream(stream);
            S9apiUtils.serialize(runtime, doc, serializer);
            stream.close();
        } catch (IOException ioe) {
            throw new XProcException("Failed to serialize as XML: " + doc, ioe);
        } catch (SaxonApiException sae) {
            throw new XProcException("Failed to serialize as XML: " + doc, sae);
        }
    }

    private static void storeBinary(XdmNode doc, OutputStream stream) {
        try {
            byte[] decoded = Base64.decode(doc.getStringValue());
            stream.write(decoded);
            stream.close();
        } catch (IOException ioe) {
            throw new XProcException(ioe);
        }
    }

    private static void storeJSON(XdmNode doc, OutputStream stream) {
        try {
            PrintWriter writer = new PrintWriter(stream);
            String json = XMLtoJSON.convert(doc);
            writer.print(json);
            writer.close();
            stream.close();
        } catch (IOException ioe) {
            throw XProcException.stepError(50, ioe);
        }
    }
}

