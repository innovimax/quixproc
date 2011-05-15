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

import java.util.Map;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.util.ProcessMatchingNodes;
import com.xmlcalabash.util.ProcessMatch;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.model.RuntimeValue;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import com.xmlcalabash.runtime.XAtomicStep;

public class Delete extends DefaultStep implements ProcessMatchingNodes {
    private static final QName _match = new QName("", "match");
    private ReadablePipe source = null;
    private WritablePipe result = null;
    private Map<QName, RuntimeValue> inScopeOptions = null;
    private String matchPattern = null;

    /** Creates a new instance of Delete */
    public Delete(XProcRuntime runtime, XAtomicStep step) {
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

        ProcessMatch matcher = null;

        matcher = new ProcessMatch(runtime, this);
        matcher.match(source.read(stepContext), getOption(_match));

        XdmNode tree = matcher.getResult();
        result.write(stepContext, tree);
    }

    public boolean processStartDocument(XdmNode node) throws SaxonApiException {
        return false;
    }

    public void processEndDocument(XdmNode node) throws SaxonApiException {
        // nop, deleted
    }

    public boolean processStartElement(XdmNode node) throws SaxonApiException {
        return false;
    }

    public void processAttribute(XdmNode node) throws SaxonApiException {
        // nop, delete the attribute
    }

    public void processEndElement(XdmNode node) throws SaxonApiException {
        // nop, deleted
    }

    public void processText(XdmNode node) throws SaxonApiException {
        // nop, delete the node
    }

    public void processComment(XdmNode node) throws SaxonApiException {
        // nop, delete the node
    }

    public void processPI(XdmNode node) throws SaxonApiException {
        // nop, delete the node
    }
}
