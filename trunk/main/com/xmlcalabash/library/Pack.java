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

import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.util.TreeWriter;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.QName;

import com.xmlcalabash.runtime.XAtomicStep;

public class Pack extends DefaultStep {
    protected static final String logger = "org.xproc.library.identity";
    private static final QName _wrapper = new QName("wrapper");
    private static final QName _wrapper_prefix = new QName("wrapper-prefix");
    private static final QName _wrapper_namespace = new QName("wrapper-namespace");
    private ReadablePipe source = null;
    private ReadablePipe alternate = null;
    private WritablePipe result = null;

    /**
     * Creates a new instance of Pack
     */
    public Pack(XProcRuntime runtime, XAtomicStep step) {
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

        RuntimeValue wrapperNameValue = getOption(_wrapper);
        String wrapperNameStr = wrapperNameValue.getString();
        String wpfx = getOption(_wrapper_prefix, (String) null);
        String wns = getOption(_wrapper_namespace, (String) null);

        if (wpfx != null && wns == null) {
            throw XProcException.dynamicError(34, "You can't specify a prefix without a namespace");
        }

        if (wns != null && wrapperNameStr.contains(":")) {
            throw XProcException.dynamicError(34, "You can't specify a namespace if the wrapper name contains a colon");
        }

        QName wrapper = null;
        if (wrapperNameStr.contains(":")) {
            wrapper = new QName(wrapperNameStr, wrapperNameValue.getNode());
        } else {
            wrapper = new QName(wpfx == null ? "" : wpfx, wns, wrapperNameStr);
        }

        while (source.moreDocuments(stepContext) || alternate.moreDocuments(stepContext)) {
            XdmNode sdoc = null;
            XdmNode adoc = null;
            if (source.moreDocuments(stepContext)) {
                sdoc = source.read(stepContext);
            }
            if (alternate.moreDocuments(stepContext)) {
                adoc = alternate.read(stepContext);
            }

            TreeWriter tree = new TreeWriter(runtime);
            tree.startDocument(step.getNode().getBaseURI());
            tree.addStartElement(wrapper);
            tree.startContent();
            if (sdoc != null) {
                tree.addSubtree(sdoc);
            }
            if (adoc != null) {
                tree.addSubtree(adoc);
            }
            tree.endDocument();
            result.write(stepContext, tree.getResult());
        }
        
        // Innovimax: close pipe
        result.close(stepContext);
    }
}

