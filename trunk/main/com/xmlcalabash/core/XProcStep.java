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

import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.model.RuntimeValue;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Oct 8, 2008
 * Time: 7:43:09 AM
 * To change this template use File | Settings | File Templates.
 */
public interface XProcStep extends XProcRunnable {
    public void setInput(String port, ReadablePipe pipe);
    public void setOutput(String port, WritablePipe pipe);
    public void setParameter(QName name, RuntimeValue value);
    public void setParameter(String port, QName name, RuntimeValue value);
    public void setOption(QName name, RuntimeValue value);
    
   //*************************************************************************
    // INNOVIMAX IMPLEMENTATION
    //*************************************************************************  

    public boolean isRunning();      
    public void setStreamed(boolean streamed);      
    public void setStreamAll(boolean streamAll);      
}
