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
package com.xmlcalabash.util;

import net.sf.saxon.lib.CollectionURIResolver;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.Item;
import net.sf.saxon.s9api.XdmNode;

import java.util.Vector;
import java.util.logging.Logger;
import java.net.URI;
import java.net.URISyntaxException;

import com.xmlcalabash.core.XProcRuntime;
import net.sf.saxon.tree.iter.ArrayIterator;

public class CollectionResolver implements CollectionURIResolver {
    XProcRuntime runtime = null;
    Vector<XdmNode> docs = null;
    CollectionURIResolver chainedResolver = null;
    protected Logger logger = Logger.getLogger(this.getClass().getName());

    public CollectionResolver(XProcRuntime runtime, Vector<XdmNode> docs, CollectionURIResolver chainedResolver) {
        this.runtime = runtime;
        this.docs = docs;
        this.chainedResolver = chainedResolver;
    }

    public SequenceIterator resolve(String href, String base, XPathContext context) throws XPathException {
        runtime.finest(null, null, "Collection: " + href + " (" + base + ")");
        if (href == null) {
            Item[] array = new Item[docs.size()];
            for (int pos = 0; pos < docs.size(); pos++) {
                array[pos] = docs.get(pos).getUnderlyingNode();
            }
            return new ArrayIterator(array);
        } else {
            try {
                URI hrefuri;

                if (base == null) {
                    hrefuri = new URI(href);
                } else {
                    hrefuri = new URI(base).resolve(href);
                }
                Vector<XdmNode> docs = runtime.getCollection(hrefuri);
                if (docs != null) {
                    Item[] items = new Item[docs.size()];
                    for (int pos = 0; pos < docs.size(); pos++) {
                        items[pos] = docs.get(pos).getUnderlyingNode();
                    }
                    return new ArrayIterator(items);
                }
            } catch (URISyntaxException use) {
                runtime.finest(null, null, "URI Syntax exception resolving collection URI: " + href + " (" + base + ")");
            }

            return chainedResolver.resolve(href,base,context);
        }
    }
}
