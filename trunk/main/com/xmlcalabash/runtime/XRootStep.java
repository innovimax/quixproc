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
package com.xmlcalabash.runtime;

import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.model.*;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmNode;

import java.util.Hashtable;
import java.util.Vector;

public class XRootStep extends XCompoundStep {
    private Vector<XdmNode> errors = new Vector<XdmNode> ();

    public XRootStep(XProcRuntime runtime) {
        super(runtime, null, null);
    }

    public DeclareStep getDeclaration(QName stepType) {
        return runtime.getBuiltinDeclaration(stepType);
    }

    public Hashtable<QName,RuntimeValue> getInScopeOptions() {
        return new Hashtable<QName,RuntimeValue> ();
    }

/*
    public void addVariable(QName name, RuntimeValue value) {
        throw new XProcException("The root step can't have getVariables!");
    }
*/

    public RuntimeValue getVariable(QName name) {
        throw new XProcException("The root step doesn't have getVariables!");
    }

    public ReadablePipe getBinding(String stepName, String portName) {
        throw new XProcException("No in-scope binding for " + portName + " on " + stepName);
    }

/*
    public void instantiate(DeclareStep step) {
        throw new XProcException("The root step can't be instantiated!");
    }
*/
    
    // Innovimax: replaced by gorun()
    //public void run() {    
    public void gorun() {
        throw new XProcException("The root step can't be run!");
    }

    public void reportError(XdmNode doc) {
        errors.add(doc);
    }
    
    //*************************************************************************
    //*************************************************************************        
    //*************************************************************************
    // INNOVIMAX IMPLEMENTATION
    //*************************************************************************
    //*************************************************************************
    //*************************************************************************
    
    // Innovimax: new function
    public Object clone() {
        XRootStep clone = new XRootStep(runtime);
        super.cloneStep(clone);       
        clone.cloneInstantiation(errors);        
        return clone;
    }              
    
    // Innovimax: new function
    private void cloneInstantiation(Vector<XdmNode> errors) {
        this.errors.addAll(errors);        
    }      
    
}