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

package com.xmlcalabash.model;

import net.sf.saxon.s9api.XdmNode;
import com.xmlcalabash.core.XProcRuntime;

public class Input extends Port {
    private String select = null;
    private boolean debugReader = false;
    private boolean debugWriter = false;
    private boolean parameterInput = false;
    private int position = 0;

    /** Creates a new instance of Input */
    public Input(XProcRuntime xproc, XdmNode node) {
        super(xproc, node);
    }

    public void setSelect(String select) {
        this.select = select;
    }

    public String getSelect() {
        return select;
    }

    public void setParameterInput() {
        parameterInput = true;
    }
    
    public void setParameterInput(boolean value) {
        parameterInput = value;
    }

    public boolean getParameterInput() {
        return parameterInput;
    }

    public void setPosition(int pos) {
        position = pos;
    }
    
    public int getPosition() {
        return position;
    }

    public void setDebugReader(boolean debug) {
        debugReader = debug;
    }
    
    public void setDebugWriter(boolean debug) {
        debugWriter = debug;
    }
    
    public boolean getDebugReader() {
        return debugReader;
    }

    public boolean getDebugWriter() {
        return debugWriter;
    }
    
    protected void dump(int depth) {
        String indent = "";
        for (int count = 0; count < depth; count++) {
            indent += " ";
        }

        System.err.println(indent + "input " + getPort());
        for (Binding binding : getBinding()) {
            binding.dump(depth+2);
        }
    }

    @Override
    public String toString() {
        if (getStep() == null) {
            return "[input " + getPort() + " on null]";
        } else {
            return "[input " + getPort() + " on " + getStep().getName() + "]";
        }
    }        
    
}
