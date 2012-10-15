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
package com.xmlcalabash.runtime;

import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.model.Step;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Oct 13, 2008
 * Time: 7:23:25 PM
 * To change this template use File | Settings | File Templates.
 */
public class XGroup extends XCompoundStep {
    public XGroup(XProcRuntime runtime, Step step, XCompoundStep parent) {
          super(runtime, step, parent);
    }
    
    //*************************************************************************
    //*************************************************************************        
    //*************************************************************************
    // INNOVIMAX IMPLEMENTATION
    //*************************************************************************
    //*************************************************************************
    //************************************************************************* 
    
    private String status = "release"; // Innovimax: new property
    
    // Innovimax: new function
    public void lockOutput() {
        status = "lock";
    }
    
    // Innovimax: new function
    public void releaseOutput() {
        status = "release";
    }    
    
    // Innovimax: new function
    public void cancelOutput() {
        status = "cancel";
    }        
          
    // Innovimax: new function
    public boolean outputReleased() {
        return status.equals("release");
    }          
    
    // Innovimax: new function
    public boolean outputLocked() {
        return status.equals("lock");
    }          
    
    // Innovimax: new function
    public boolean outputCanceled() {
        return status.equals("cancel");
    }                            
                
    // Innovimax: new function
    public Object clone() {
        XGroup clone = new XGroup(runtime, step, parent);
        super.cloneStep(clone);       
        return clone;
    }       
    
}
