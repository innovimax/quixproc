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
import java.util.Vector;

import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;

import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.ProcessMatch;
import com.xmlcalabash.util.ProcessMatchingNodes;

/**
 *
 * @author ndw
 */
public class StringReplace extends DefaultStep implements ProcessMatchingNodes {
    private static final QName _match = new QName("", "match");
    private static final QName _replace = new QName("", "replace");
    private ReadablePipe source = null;
    private WritablePipe result = null;
    private ProcessMatch matcher = null;
    private RuntimeValue replace = null;
    private Hashtable<String,String> rns = new Hashtable<String,String> ();
    private static Hashtable<QName,RuntimeValue> atomicStepsGetNoInScopeOptions = new Hashtable<QName,RuntimeValue> ();

    /** Creates a new instance of StringReplace */
    public StringReplace(XProcRuntime runtime, XAtomicStep step) {
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


    public void gorun() throws SaxonApiException {
        super.gorun();

        RuntimeValue match = getOption(_match);
        replace = getOption(_replace);
        for (String prefix : replace.getNamespaceBindings().keySet()) {
            rns.put(prefix, replace.getNamespaceBindings().get(prefix));
        }

        matcher = new ProcessMatch(runtime, this);
        matcher.match(source.read(stepContext), match);

        result.write(stepContext,matcher.getResult());
    }

    public boolean processStartDocument(XdmNode node) throws SaxonApiException {
        return true;
    }

    public void processEndDocument(XdmNode node) throws SaxonApiException {
        // nop?
    }

    public boolean processStartElement(XdmNode node) throws SaxonApiException {
        String newValue = computeReplacement(node);
        matcher.addText(newValue);
        return false;
    }

    public void processEndElement(XdmNode node) throws SaxonApiException {
        // nop?
    }

    public void processText(XdmNode node) throws SaxonApiException {
        String newValue = computeReplacement(node);
        matcher.addText(newValue);
    }

    public void processComment(XdmNode node) throws SaxonApiException {
        String newValue = computeReplacement(node);
        matcher.addText(newValue);
    }

    public void processPI(XdmNode node) throws SaxonApiException {
        String newValue = computeReplacement(node);
        matcher.addText(newValue);
    }

    public void processAttribute(XdmNode node) throws SaxonApiException {
        String newValue = computeReplacement(node);
        matcher.addAttribute(node, newValue);
    }

    private String computeReplacement(XdmNode node) {
        Vector<XdmItem> values = evaluateXPath(node, rns, replace.getString(), atomicStepsGetNoInScopeOptions);
        String newValue = "";
        for (XdmItem item : values) {
            newValue += item.getStringValue();
        }
        return newValue;
    }

}

