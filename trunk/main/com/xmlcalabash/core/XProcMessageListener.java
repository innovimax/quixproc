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
package com.xmlcalabash.core;

import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmNode;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Dec 18, 2009
 * Time: 8:10:48 AM
 * To change this template use File | Settings | File Templates.
 */
public interface XProcMessageListener {
    public void error(XProcRunnable step, XdmNode node, String message, QName code);
    public void error(Throwable exception);
    public void warning(XProcRunnable step, XdmNode node, String message);
    public void warning(Throwable exception);
    public void info(XProcRunnable step, XdmNode node, String message);
    public void fine(XProcRunnable step, XdmNode node, String message);
    public void finer(XProcRunnable step, XdmNode node, String message);
    public void finest(XProcRunnable step, XdmNode node, String message);
}
