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

import net.sf.saxon.expr.LastPositionFinder;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;

public class DocumentSequenceIterator implements SequenceIterator, LastPositionFinder {
    int position = 0;
    int last = 0;
    Item item = null;

    public void setPosition(int position) {
        this.position = position;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public void setLast(int last) {
        this.last = last;
    }

    public int getLastPosition() throws XPathException {
        return last;
    }

    public Item next() throws XPathException {
        throw new UnsupportedOperationException("Don't know what to do for next() on DocumentSequenceIterator");
    }

    public Item current() {
        return item;
    }

    public int position() {
        return position;
    }

    public void close() {
        // ???
    }

    public SequenceIterator getAnother() throws XPathException {
        throw new UnsupportedOperationException("Don't know what to do for getAnother() on DocumentSequenceIterator");
    }

    public int getProperties() {
        return SequenceIterator.LAST_POSITION_FINDER;
    }
}
