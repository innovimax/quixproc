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
package com.xmlcalabash.util;

import javax.xml.transform.TransformerException;

import net.sf.saxon.lib.StandardErrorListener;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Dec 9, 2009
 * Time: 7:13:02 AM
 *
 * The StepErrorListener puts errors on the p:catch error port and passes the error along to the next
 * listener. If you want the next listener to just drop messages on the floor, use this one.
 *
 */
public class SilentErrorListener extends StandardErrorListener {
    public SilentErrorListener() {
        super();
    }

    public void error(TransformerException exception) throws TransformerException {
        // what, me, worry?
    }

    public void fatalError(TransformerException exception) throws TransformerException {
        // what, me, worry?
    }

    public void warning(TransformerException exception) throws TransformerException {
        // what, me, worry?
    }
}