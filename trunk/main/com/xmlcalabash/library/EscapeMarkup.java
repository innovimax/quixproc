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

import java.io.ByteArrayOutputStream;

import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmNodeKind;

import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.RelevantNodes;
import com.xmlcalabash.util.S9apiUtils;
import com.xmlcalabash.util.TreeWriter;

/**
 *
 * @author ndw
 */
public class EscapeMarkup extends DefaultStep {
    private ReadablePipe source = null;
    private WritablePipe result = null;

    /** Creates a new instance of EscapeMarkup */
    public EscapeMarkup(XProcRuntime runtime, XAtomicStep step) {
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

        Serializer serializer = makeSerializer();

        XdmNode doc = source.read(stepContext);

        TreeWriter tree = new TreeWriter(runtime);
        tree.startDocument(doc.getBaseURI());
        for (XdmNode child : new RelevantNodes(runtime, doc, Axis.CHILD)) {
            if (child.getNodeKind() == XdmNodeKind.COMMENT) {
                tree.addComment(child.getStringValue());
            } else if (child.getNodeKind() == XdmNodeKind.PROCESSING_INSTRUCTION) {
                tree.addPI(child.getNodeName().getLocalName(), child.getStringValue());
            } else if (child.getNodeKind() == XdmNodeKind.TEXT) {
                tree.addText(child.getStringValue());
            } else {
                tree.addStartElement(child);
                tree.addAttributes(child);
                tree.startContent();

                // Serialize the *whole* thing, then strip off the start and end tags, because
                // otherwise namespace fixup messes with the namespace bindings
                ByteArrayOutputStream outstr = new ByteArrayOutputStream();
                serializer.setOutputStream(outstr);
                S9apiUtils.serialize(runtime, child, serializer);
                String data = outstr.toString();

                data = data.replaceAll("^<.*?>",""); // Strip off the start tag...
                data = data.replaceAll("<[^<>]*?>$",""); // Strip off the end tag

                tree.addText(data);
                tree.addEndElement();
            }
        }
        tree.endDocument();

        result.write(stepContext,tree.getResult());
    }
}
