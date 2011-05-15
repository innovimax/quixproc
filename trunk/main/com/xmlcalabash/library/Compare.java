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

import java.util.Iterator;

import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.util.TreeWriter;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XPathCompiler;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XPathExecutable;
import net.sf.saxon.s9api.XPathSelector;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmAtomicValue;

import com.xmlcalabash.runtime.XAtomicStep;

public class Compare extends DefaultStep {
    private static final QName c_result = new QName("c", XProcConstants.NS_XPROC_STEP, "result");
    private static final QName doca = new QName("","doca");
    private static final QName docb = new QName("","docb");
    private static final QName _fail_if_not_equal = new QName("","fail-if-not-equal");
    private ReadablePipe source = null;
    private ReadablePipe alternate = null;
    private WritablePipe result = null;

    /**
     * Creates a new instance of Compare
     */
    public Compare(XProcRuntime runtime, XAtomicStep step) {
        super(runtime,step);
    }

    public void setInput(String port, ReadablePipe pipe) {
        if ("source".equals(port)) {
            source = pipe;
        } else {
            alternate = pipe;
        }
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

        XdmNode sdoc = source.read(stepContext);
        XdmNode adoc = alternate.read(stepContext);

        XPathCompiler xcomp = runtime.getProcessor().newXPathCompiler();
        xcomp.declareVariable(doca);
        xcomp.declareVariable(docb);

        XPathExecutable xexec = xcomp.compile("deep-equal($doca,$docb)");
        XPathSelector selector = xexec.load();

        selector.setVariable(doca,sdoc);
        selector.setVariable(docb,adoc);

        Iterator<XdmItem> values = selector.iterator();
        XdmAtomicValue item = (XdmAtomicValue) values.next();
        boolean same = item.getBooleanValue();
        if (!same && getOption(_fail_if_not_equal,false)) {
            throw XProcException.stepError(19);
        }

        TreeWriter treeWriter = new TreeWriter(runtime);
        treeWriter.startDocument(step.getNode().getBaseURI());
        treeWriter.addStartElement(c_result);
        treeWriter.startContent();
        treeWriter.addText(""+same);
        treeWriter.addEndElement();
        treeWriter.endDocument();

        result.write(stepContext, treeWriter.getResult());
    }
}

