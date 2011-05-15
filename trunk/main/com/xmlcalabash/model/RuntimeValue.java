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

package com.xmlcalabash.model;

import java.net.URI;
import java.util.Hashtable;
import java.util.Vector;

import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.ItemTypeFactory;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmSequenceIterator;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.s9api.ItemType;
import net.sf.saxon.value.StringValue;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;

public class RuntimeValue {
    private Vector<XdmItem> generalValue = null;
    private String value = null;
    private XdmNode node = null;
    private ComputableValue val = null;
    private boolean initialized = false;
    private Hashtable<String,String> nsBindings = null;

    public RuntimeValue() {
        // nop; returns an uninitialized value
    }

    public RuntimeValue(String value, XdmNode node) {
        this.value = value;
        this.node = node;
        initialized = true;

        nsBindings = new Hashtable<String,String> ();
        XdmSequenceIterator nsIter = node.axisIterator(Axis.NAMESPACE);
        while (nsIter.hasNext()) {
            XdmNode ns = (XdmNode) nsIter.next();
            QName nodeName = ns.getNodeName();
            String uri = ns.getStringValue();

            if (nodeName == null) {
                // Huh?
                nsBindings.put("", uri);
            } else {
                String localName = nodeName.getLocalName();
                nsBindings.put(localName,uri);
            }
        }
    }

    public RuntimeValue(String value, XdmNode node, Hashtable<String,String> nsBindings) {
        this.value = value;
        this.node = node;
        this.nsBindings = nsBindings;
        initialized = true;
    }

    public RuntimeValue(String value, Vector<XdmItem> generalValue, XdmNode node, Hashtable<String,String> nsBindings) {
        this.value = value;
        this.generalValue = generalValue;
        this.node = node;
        this.nsBindings = nsBindings;
        initialized = true;
    }

    public RuntimeValue(String value) {
        this.value = value;
        initialized = true;
    }

    /*
    public void setComputableValue(ComputableValue value) {
        val = value;
        initialized = true;
    }
    */

    public boolean initialized() {
        return initialized;
    }

    public XdmAtomicValue getUntypedAtomic(XProcRuntime runtime) {
        try {
            ItemTypeFactory itf = new ItemTypeFactory(runtime.getProcessor());
            ItemType untypedAtomic = itf.getAtomicType(new QName(NamespaceConstant.SCHEMA, "xs:untypedAtomic"));
            XdmAtomicValue val = new XdmAtomicValue(value, untypedAtomic);
            return val;
        } catch (SaxonApiException sae) {
            throw new XProcException(sae);
        }
    }

    public String getString() {
        return value;
    }

    public boolean hasGeneralValue() {
        return generalValue != null;
    }

    public XdmValue getValue() {
        if (generalValue == null) {
            throw new XProcException(node, "Unexpexted null value in getValue()");
        }
        if (generalValue.size() == 1) {
            return generalValue.get(0);
        } else {
            return new XdmValue(generalValue);
        }
    }

    public StringValue getStringValue() {
        return new StringValue(value);
    }

    public QName getQName() {
        // FIXME: Check the type
        // TypeUtils.checkType(runtime, value, )
        if (value.contains(":")) {
            return new QName(value, node);
        } else {
            return new QName("", value);
        }
    }

    public XdmNode getNode() {
        return node;
    }
    
    public URI getBaseURI() {
        return node.getBaseURI();
    }

    public Hashtable<String,String> getNamespaceBindings() {
        return nsBindings;
    }

    public boolean getBoolean() {
        if ("true".equals(value) || "1".equals(value)) {
            return true;
        } else if ("false".equals(value) || "0".equals(value)) {
            return false;
        } else {
            throw new XProcException(node, "Non boolean string: " + value);
        }
    }

    public int getInt() {
        int result = Integer.parseInt(value);
        return result;
    }

    public XdmSequenceIterator getNamespaces() {
        return node.axisIterator(Axis.NAMESPACE);
    }
    
    //*************************************************************************
    //*************************************************************************        
    //*************************************************************************
    // INNOVIMAX IMPLEMENTATION
    //*************************************************************************
    //*************************************************************************
    //*************************************************************************        
    
    // Innovimax: new function
    public Object clone() {
        RuntimeValue clone = new RuntimeValue();       
        clone.cloneInstantiation(generalValue, value, node, nsBindings);
        return clone;
    }        
    
    // Innovimax: new function
    public void cloneInstantiation(Vector<XdmItem> generalValue, String value, XdmNode node, Hashtable<String,String> nsBindings) {
        if (generalValue != null) {
            this.generalValue = new Vector<XdmItem>(generalValue);
        }
        this.value = value;
        this.node = node;          
        if (nsBindings != null) {  
            this.nsBindings = new Hashtable<String,String>(nsBindings);
        }
    }           
}
