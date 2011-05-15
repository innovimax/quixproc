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

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcConstants;
import net.sf.saxon.lib.CollectionURIResolver;
import net.sf.saxon.s9api.*;
import net.sf.saxon.Configuration;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.TreeWriter;
import com.xmlcalabash.util.CollectionResolver;
import com.xmlcalabash.util.Base64;
import com.xmlcalabash.util.S9apiUtils;

public class XQuery extends DefaultStep {
    private static final QName _content_type = new QName("content-type");

    private ReadablePipe source = null;
    private Hashtable<QName,RuntimeValue> params = new Hashtable<QName,RuntimeValue> ();
    private ReadablePipe query = null;
    private WritablePipe result = null;
    
    public XQuery(XProcRuntime runtime, XAtomicStep step) {
        super(runtime,step);
    }

    public void setInput(String port, ReadablePipe pipe) {
        if ("source".equals(port)) {
            source = pipe;
        } else if ("query".equals(port)) {
            query = pipe;
        }
    }

    public void setOutput(String port, WritablePipe pipe) {
        result = pipe;
    }

    public void setParameter(QName name, RuntimeValue value) {
        params.put(name, value);
    }

    public void reset() {
        query.resetReader(stepContext);
        result.resetWriter(stepContext);
    }

    public void gorun() throws SaxonApiException {
        super.gorun();

        // FIXME: Deal with (doubly) escaped markup correctly...

        Vector<XdmNode> defaultCollection = new Vector<XdmNode> ();
        while (source.moreDocuments(stepContext)) {
            defaultCollection.add(source.read(stepContext));
        }

        XdmNode document = null;
        if (defaultCollection.size() > 0) {
            document = defaultCollection.firstElement();
        }

        XdmNode root = S9apiUtils.getDocumentElement(query.read(stepContext));
        String queryString = null;

        if ((XProcConstants.c_data.equals(root.getNodeName())
             && "application/octet-stream".equals(root.getAttributeValue(_content_type)))
            || "base64".equals(root.getAttributeValue(_encoding))) {
            byte[] decoded = Base64.decode(root.getStringValue());
            queryString = new String(decoded);
        } else {
            queryString = root.getStringValue();
        }

        Configuration config = runtime.getProcessor().getUnderlyingConfiguration();

        CollectionURIResolver collectionResolver = config.getCollectionURIResolver();

        config.setCollectionURIResolver(new CollectionResolver(runtime, defaultCollection, collectionResolver));

        Processor qtproc = runtime.getProcessor();
        XQueryCompiler xqcomp = qtproc.newXQueryCompiler();
        xqcomp.setBaseURI(root.getBaseURI());
        XQueryExecutable xqexec = xqcomp.compile(queryString);
        XQueryEvaluator xqeval = xqexec.load();
        if (document != null) {
            xqeval.setContextItem(document);
        }

        for (QName name : params.keySet()) {
            RuntimeValue v = params.get(name);
            if (runtime.getAllowGeneralExpressions()) {
                xqeval.setExternalVariable(name, v.getValue());
            } else {
                xqeval.setExternalVariable(name, new XdmAtomicValue(v.getString()));
            }

        }

        Iterator<XdmItem> iter = xqeval.iterator();
        while (iter.hasNext()) {
            XdmItem item = iter.next();
            if (item.isAtomicValue()) {
                throw new XProcException(step.getNode(), "Not expecting atomic values back from XQuery!");
            }
            XdmNode node = (XdmNode) item;

            if (node.getNodeKind() != XdmNodeKind.DOCUMENT) {
                // Make a document for this node...is this the right thing to do?
                TreeWriter treeWriter = new TreeWriter(runtime);
                treeWriter.startDocument(step.getNode().getBaseURI());
                treeWriter.addSubtree(node);
                treeWriter.endDocument();
                node = treeWriter.getResult();
            }
            
            result.write(stepContext, node);
        }

        config.setCollectionURIResolver(collectionResolver);
    }
}
