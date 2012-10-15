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

import java.util.List;
import java.util.Stack;
import java.util.Vector;

import net.sf.saxon.s9api.XdmNode;

import com.xmlcalabash.runtime.XStep;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Oct 20, 2009
 * Time: 9:25:07 AM
 * To change this template use File | Settings | File Templates.
 */
public class XProcData {
    private Stack<StackFrame> stack = null;
    private XProcRuntime runtime = null;

    public XProcData(XProcRuntime runtime) {
        this.runtime = runtime;
        stack = new Stack<StackFrame> ();
    }

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

    public void closeFrame() {
        stack.pop();
    }

    // Innovimax: modified function
    public int getDepth() {
        //if (true) throw new RuntimeException("XProcData.getDepth");      
        // Innovimax: to preserve correct cases  
        //return stack.size();        
        return 1;            
    }
        
    public XStep getStep() {
        if (stack.size() == 0) {
            return null;
        } else {
            return stack.peek().step;
        }
    }

    public void setIterationPosition(int pos) {
        stack.peek().iterPos = pos;
    }

    public int getIterationPosition() {
        return stack.peek().iterPos;
    }

    public void setIterationSize(int size) {
        stack.peek().iterSize = size;
    }

    public int getIterationSize() {
        return stack.peek().iterSize;
    }

    private boolean tryGroup(XStep step) {
        if (XProcConstants.p_group.equals(step.getType())) {
            XdmNode node = step.getNode();
            return XProcConstants.p_try.equals(node.getParent().getNodeName());
        }
        return false;
    }

    public boolean catchError(XdmNode error) {
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

    public List<XdmNode> errors() {
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
