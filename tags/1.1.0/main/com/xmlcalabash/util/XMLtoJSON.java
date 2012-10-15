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

import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmNodeKind;
import net.sf.saxon.s9api.XdmSequenceIterator;

import org.json.JSONException;
import org.json.JSONStringer;

import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: 12/18/10
 * Time: 4:24 PM
 * To change this template use File | Settings | File Templates.
 */
public class XMLtoJSON {
    private static final int ROOT = 0;
    private static final int OBJECT = 1;
    private static final int ARRAY = 2;

    private static QName _type = new QName("", "type");
    private static QName _name = new QName("", "name");

    private static final QName c_body = new QName("c", XProcConstants.NS_XPROC_STEP, "body");

    public static String convert(XdmNode json) {
        JSONStringer js = new JSONStringer();

        json = S9apiUtils.getDocumentElement(json);

        if (c_body.equals(json.getNodeName())) {
            XdmNode jchild = null;
            XdmSequenceIterator iter = json.axisIterator(Axis.CHILD);
            while (iter.hasNext()) {
                XdmItem item = iter.next();
                if (item instanceof XdmNode) {
                    XdmNode child = (XdmNode) item;
                    if (child.getNodeKind() == XdmNodeKind.ELEMENT) {
                        if (jchild != null) {
                            throw new XProcException("Found c:body containing more than one JSON element?");
                        } else {
                            jchild = child;
                        }
                    }
                }
            }
            json = jchild;
        }

        try {
            build(json, js, ROOT);
        } catch (JSONException jse) {
            throw new XProcException(jse);
        }

        return js.toString();
    }

    private static void build(XdmNode json, JSONStringer js, int context) throws JSONException {
        String type = null;

        if (JSONtoXML.JSONX_NS.equals(json.getNodeName().getNamespaceURI())
            || JSONtoXML.JXML_NS.equals(json.getNodeName().getNamespaceURI())) {
            type = json.getNodeName().getLocalName();
        } else {
            type = json.getAttributeValue(_type);
        }

        String name = null;
        if (JSONtoXML.MLJS_NS.equals(json.getNodeName().getNamespaceURI())) {
            name = json.getNodeName().getLocalName();
            if (name.contains("_")) {
                if ("_".equals(name)) {
                    name = "";
                } else {
                    String decoded = "";
                    int upos = name.indexOf("_");
                    while (upos >= 0) {
                        decoded += name.substring(0, upos);
                        String hex = name.substring(upos+1,upos+5);
                        int ch = Integer.parseInt(hex, 16);
                        decoded += Character.toString((char) ch);
                        name = name.substring(upos+5);
                        upos = name.indexOf("_");
                    }
                    name = decoded + name;
                }
            }
        } else {
            name = json.getAttributeValue(_name);
        }

        if (context == OBJECT && name != null) {
            js.key(name);
        }

        if ("object".equals(type)) {
            js.object();
            processChildren(json, js, OBJECT);
            js.endObject();
        } else if ("array".equals(type)) {
            js.array();
            processChildren(json, js, ARRAY);
            js.endArray();
        } else if ("member".equals(type)) { // only happens for JXML
            processChildren(json, js, OBJECT);
        } else {
            if ("null".equals(type)) {
                js.value(null);
            } else if ("number".equals(type)) {
                String value = json.getStringValue();
                if (value.contains(".")) {
                    Double d = Double.parseDouble(value);
                    js.value(d);
                } else {
                    long i = Long.parseLong(value);
                    js.value(i);
                }
            } else if ("boolean".equals(type)) {
                js.value("true".equals(json.getStringValue()));
            } else {
                js.value(json.getStringValue());
            }
        }
    }

    private static void processChildren(XdmNode json, JSONStringer js, int context) throws JSONException {
        XdmSequenceIterator iter = json.axisIterator(Axis.CHILD);
        while (iter.hasNext()) {
            XdmItem item = iter.next();
            if (item instanceof XdmNode) {
                XdmNode child = (XdmNode) item;
                if (child.getNodeKind() == XdmNodeKind.ELEMENT) {
                    build(child, js, context);
                }
            }
        }
    }
}
