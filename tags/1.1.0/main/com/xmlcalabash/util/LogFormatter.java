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

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Jul 30, 2009
 * Time: 7:28:44 AM
 * To change this template use File | Settings | File Templates.
 */
public class LogFormatter extends Formatter {

    public String format(LogRecord record) {
        return record.getLevel().getName() + ": " + record.getMessage() + "\n";
    }
}
