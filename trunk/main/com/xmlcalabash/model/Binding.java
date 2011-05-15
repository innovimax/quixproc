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

public abstract class Binding extends SourceArtifact {
    public static final int NO_BINDING = 0;
    public static final int PIPE_NAME_BINDING = 1;
    public static final int INLINE_BINDING = 2;
    public static final int DOCUMENT_BINDING = 3;
    public static final int PIPE_BINDING = 4;
    public static final int EMPTY_BINDING = 5;
    public static final int STDIO_BINDING = 6;
    public static final int ERROR_BINDING = 7;
    public static final int DATA_BINDING = 8;

    protected int bindingType = NO_BINDING;

    /** Creates a new instance of Binding */
    public Binding(XProcRuntime xproc, XdmNode node) {
        super(xproc, node);
    }

    public int getBindingType() {
        return bindingType;
    }
    
    abstract protected void dump(int depth);
}
