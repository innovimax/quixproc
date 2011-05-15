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
package com.xmlcalabash.core;

import com.xmlcalabash.runtime.XStep;
import com.xmlcalabash.util.TreeWriter;

import java.util.Stack;
import java.util.Vector;
import java.util.List;
import java.net.URI;
import java.net.URISyntaxException;

import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.QName;

public class XProcData {
    private Stack<StackFrame> stack = null;
    private XProcRuntime runtime = null;

    public XProcData(XProcRuntime runtime) {
        this.runtime = runtime;
        stack = new Stack<StackFrame> ();
    }

   // Innovimax: desactivated code    
    public void openFrame(XStep step) {        
        int pos = 1;
        int size = 1;

        if (stack.size() > 0) {
            pos = stack.peek().iterPos;
            size = stack.peek().iterSize;
        }

        StackFrame frame = new StackFrame();
        frame.iterPos = pos;
        frame.iterSize = size;

        stack.push(frame);
        stack.peek().step = step;
    }

    // Innovimax: desactivated function
    public void closeFrame() { 
        stack.pop();
    }

    // Innovimax: desactivated function
    public int getDepth() {
        //if (true) throw new RuntimeException("XProcData.getDepth");
        //return stack.size();
        // Innovimax: to preserve correct cases
        return 1; 
    }
    
    // Innovimax: desactivated function
    public XStep getStep() {        
        if (stack.size() == 0) {
            return null;
        } else {
            return stack.peek().step;
        }        
    }

    // Innovimax: desactivated function
    public void setIterationPosition(int pos) {
        if (true) throw new RuntimeException("XProcData.setIterationPosition");
        stack.peek().iterPos = pos;        
    }

    // Innovimax: desactivated function
    public int getIterationPosition() {
        if (true) throw new RuntimeException("XProcData.getIterationPosition");
        return stack.peek().iterPos;        
    }

    // Innovimax: desactivated function
    public void setIterationSize(int size) {
        if (true) throw new RuntimeException("XProcData.setIterationSize");
        stack.peek().iterSize = size;        
    }

    // Innovimax: desactivated function
    public int getIterationSize() {
        if (true) throw new RuntimeException("XProcData.getIterationSize");
        return stack.peek().iterSize;        
    }

    private boolean tryGroup(XStep step) {
        if (XProcConstants.p_group.equals(step.getType())) {
            XdmNode node = step.getNode();
            return XProcConstants.p_try.equals(node.getParent().getNodeName());
        }
        return false;
    }

    // Innovimax: desactivated function
    public boolean catchError(XdmNode error) {
        if (true) throw new RuntimeException("XProcData.catchError");
        // Errors accumulate on the nearest p:try/p:group ancestor because that's where we
        // can read them. Note, however, that errors raised in a p:catch are NOT
        // part of the parent p:try but rather the grandparent.
        int pos = stack.size() - 1;
        if (stack.size() > 0 && XProcConstants.p_catch.equals(stack.peek().step.getType())) {
            pos = pos - 2;
        }
        while (pos >= 0 && !tryGroup(stack.get(pos).step)) {
            pos = pos - 1;
        }
        if (pos >= 0) {
            stack.get(pos).errors.add(error);
            return true;
        } else {
            return false;
        }
    }

    // Innovimax: desactivated function
    public List<XdmNode> errors() {
        if (true) throw new RuntimeException("XProcData.errors");
        // Errors accumulate on the nearest p:try/p:group ancestor
        int pos = stack.size() - 1;
        while (pos >= 0 && !tryGroup(stack.get(pos).step)) {
            pos = pos - 1;
        }
        if (pos >= 0) {
            return stack.get(pos).errors;
        } else {
            return new Stack<XdmNode> ();
        }
    }
    
    private class StackFrame {
        public XStep step = null;
        public int iterPos = 1;
        public int iterSize = 1;
        public Vector<XdmNode> errors = new Vector<XdmNode> ();

        public StackFrame() {
            // nop;
        }
    }
}
