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

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XPathCompiler;
import net.sf.saxon.s9api.XPathExecutable;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.sxpath.XPathDynamicContext;
import net.sf.saxon.sxpath.XPathExpression;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.BooleanValue;

import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.DocumentSequenceIterator;

/**
 *
 * @author ndw
 */
public class SplitSequence extends DefaultStep {
    private static final QName _test = new QName("", "test");
    private static final QName _initial_only = new QName("", "initial-only");
    private ReadablePipe source = null;
    private WritablePipe matched = null;
    private WritablePipe notMatched = null;
    private boolean initialOnly = false;

    /** Creates a new instance of SplitSequence */
    public SplitSequence(XProcRuntime runtime, XAtomicStep step) {
        super(runtime,step);
    }

    public void setInput(String port, ReadablePipe pipe) {
        source = pipe;
    }

    public void setOutput(String port, WritablePipe pipe) {
        if ("matched".equals(port)) {
            matched = pipe;
        } else {
            notMatched = pipe;
        }
    }

    public void reset() {
        source.resetReader(stepContext);
        matched.resetWriter(stepContext);
        notMatched.resetWriter(stepContext);
    }

    public void gorun() throws SaxonApiException {
        super.gorun();

        RuntimeValue test = getOption(_test);
        
        // Innovimax: last() is not available here
        if (test.getString().contains("last()")) {
            throw new RuntimeException("last() is not available in p:split-sequence @test");
        }
                
        initialOnly = getOption(_initial_only, false);
        boolean stillOk = true;

        int count = 0;
        while (source.moreDocuments(stepContext)) {
            count++;
            source.read(stepContext);
        }
        source.resetReader(stepContext);

        DocumentSequenceIterator xsi = new DocumentSequenceIterator(); // See below
        xsi.setLast(count);

        int pos = 0;
        while (source.moreDocuments(stepContext)) {
            XdmNode doc = source.read(stepContext);
            pos++;

            Item item = null;

            try {
                XPathCompiler xcomp = runtime.getProcessor().newXPathCompiler();
                xcomp.setBaseURI(step.getNode().getBaseURI());

                for (String prefix : test.getNamespaceBindings().keySet()) {
                    xcomp.declareNamespace(prefix, test.getNamespaceBindings().get(prefix));
                }

                XPathExecutable xexec = xcomp.compile(test.getString());

                // From Michael Kay: http://markmail.org/message/vkb2vaq2miylgndu
                //
                // Underneath the s9api XPathExecutable is a net.sf.saxon.sxpath.XPathExpression.

                XPathExpression xexpr = xexec.getUnderlyingExpression();

                // Call createDynamicContext() on this to get an XPathDynamicContext object;

                XPathDynamicContext xdc = xexpr.createDynamicContext(doc.getUnderlyingNode());

                // call getXPathContextObject() on that to get the underlying XPathContext.

                XPathContext xc = xdc.getXPathContextObject();

                // Then call XPathContext.setCurrentIterator()
                // to supply a SequenceIterator whose current() and position() methods return
                // the context item and position respectively. If there's any risk that the
                // expression will call the last() method, then it's simplest to make your
                // iterator's getProperties() return LAST_POSITION_FINDER, and implement the
                // LastPositionFinder interface, in which case last() will be implemented by
                // calling the iterator's getLastPosition() method. (Otherwise last() is
                // implemented by calling getAnother() to clone the iterator and calling next()
                // on the clone until the end of the sequence is reached).

                xsi.setPosition(pos);
                xsi.setItem(doc.getUnderlyingNode());
                xc.setCurrentIterator(xsi);

                // Then evaluate the expression by calling iterate() on the
                // net.sf.saxon.sxpath.XPathExpression object.

                SequenceIterator results = xexpr.iterate(xdc);
                // FIXME: What if the expression returns a sequence?
                item = results.next();
            } catch (XPathException xe) {
                throw new XProcException(xe);
            }

            //
            //  Good luck!
            //

            // FIXME: Compute effective boolean value in a more robust way
            boolean pass = false;
            if (item instanceof BooleanValue) {
                pass = ((BooleanValue) item).getBooleanValue();
            } else {
                pass = (item != null);
            }

            stillOk = stillOk && pass;

            if (pass && (!initialOnly || stillOk)) {
                matched.write(stepContext,doc);
            } else {
                notMatched.write(stepContext,doc);
            }
        }
    }
}

