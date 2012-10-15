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

import java.util.Hashtable;

import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmSequenceIterator;
import net.sf.saxon.s9api.XdmValue;

import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.TreeWriter;

/**
 *
 * @author ndw
 */
public class Parameters extends DefaultStep {
    private static final QName c_param_set = new QName("c", XProcConstants.NS_XPROC_STEP, "param-set");
    private static final QName c_param = new QName("c", XProcConstants.NS_XPROC_STEP, "param");
    private static final QName cx_item = new QName("cx", XProcConstants.NS_CALABASH_EX, "item");
    private static final QName _name = new QName("name");
    private static final QName _namespace = new QName("namespace");
    private static final QName _value = new QName("value");
    private static final QName _type = new QName("type");
    private WritablePipe result = null;
    Hashtable<QName,RuntimeValue> parameters = new Hashtable<QName,RuntimeValue> ();

    /** Creates a new instance of Count */
    public Parameters(XProcRuntime runtime, XAtomicStep step) {
        super(runtime,step);
    }

    public void setOutput(String port, WritablePipe pipe) {
        result = pipe;
    }

    public void setParameter(String port, QName name, RuntimeValue value) {
        parameters.put(name, value);
    }

    public void setParameter(QName name, RuntimeValue value) {
        parameters.put(name, value);
    }

    public void reset() {
        result.resetWriter(stepContext);
    }

    public void gorun() throws SaxonApiException {
        super.gorun();

        TreeWriter treeWriter = new TreeWriter(runtime);
        treeWriter.startDocument(step.getNode().getBaseURI());
        treeWriter.addStartElement(c_param_set);
        treeWriter.startContent();

        for (QName param : parameters.keySet()) {
            String value = parameters.get(param).getStringValue().getStringValue();
            treeWriter.addStartElement(c_param);
            treeWriter.addAttribute(_name, param.getLocalName());
            if (param.getNamespaceURI() != null && !"".equals(param.getNamespaceURI())) {
                treeWriter.addAttribute(_namespace, param.getNamespaceURI());
            } else {
                // I'm not really sure about this...
                treeWriter.addAttribute(_namespace, "");
            }

            if (runtime.getAllowGeneralExpressions()) {
                XdmValue xdmvalue = parameters.get(param).getValue();
                XdmAtomicValue atom = null;
                if (xdmvalue.size() == 1) {
                    XdmItem item = xdmvalue.itemAt(0);
                    if (item.isAtomicValue()) {
                        atom = (XdmAtomicValue) item;
                    }
                }

                if (atom != null && xdmvalue.size() == 1) {
                    treeWriter.addAttribute(_value, value);
                    treeWriter.startContent();
                } else {
                    treeWriter.startContent();
                    XdmSequenceIterator iter = xdmvalue.iterator();
                    while (iter.hasNext()) {
                        XdmItem next = iter.next();
                        QName type = next.isAtomicValue() ? ((XdmAtomicValue) next).getPrimitiveTypeName() : null;

                        treeWriter.addStartElement(cx_item);

                        if (type != null) {
                            if ("http://www.w3.org/2001/XMLSchema".equals(type.getNamespaceURI())) {
                                treeWriter.addAttribute(_type, type.getLocalName());
                            } else {
                                treeWriter.addAttribute(_type, type.getClarkName());
                            }
                        }

                        if (next.isAtomicValue()) {
                            treeWriter.addAttribute(_value, next.getStringValue());
                            treeWriter.startContent();
                        } else {
                            treeWriter.startContent();
                            treeWriter.addSubtree((XdmNode) next);
                        }
                        
                        treeWriter.addEndElement();
                    }

                }
            } else {
                treeWriter.addAttribute(_value, value);
                treeWriter.startContent();
            }
            treeWriter.addEndElement();
        }

        treeWriter.addEndElement();
        treeWriter.endDocument();

        result.write(stepContext,treeWriter.getResult());
    }
}

