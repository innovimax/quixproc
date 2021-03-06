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

import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;

import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.TreeWriter;

/**
 *
 * @author ndw
 */
public class Count extends DefaultStep {
    private static final QName c_result = new QName("c", XProcConstants.NS_XPROC_STEP, "result");
    private static final QName _limit = new QName("limit");
    private ReadablePipe source = null;
    private WritablePipe result = null;
    
    /** Creates a new instance of Count */
    public Count(XProcRuntime runtime, XAtomicStep step) {
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

        String limitStr = getOption(_limit).getString();
        int limit = 0;

        try {
            limit = Integer.parseInt(limitStr);
        } catch (NumberFormatException nfe) {
            throw XProcException.dynamicError(19, "The limit on p:count must be an integer");
        }

        boolean done = false;
        int count = 0;
        while (!done && source.moreDocuments(stepContext)) {
            XdmNode node = source.read(stepContext);
            count++;
            done = (limit > 0 && count == limit);
        }

        TreeWriter tree = new TreeWriter(runtime);
        tree.startDocument(step.getNode().getBaseURI());
        tree.addStartElement(c_result);
        tree.startContent();
        tree.addText("" + count);
        tree.addEndElement();
        tree.endDocument();
        result.write(stepContext,tree.getResult());
    }
}
