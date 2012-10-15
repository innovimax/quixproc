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

import java.util.Vector;

import net.sf.saxon.s9api.XdmNode;

import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.DocumentSequence;
import com.xmlcalabash.io.Pipe;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.model.Input;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Oct 7, 2008
 * Time: 7:38:44 AM
 * To change this template use File | Settings | File Templates.
 */
public class XInput {
    private XProcRuntime runtime = null;
    private String port = null;
    private XdmNode node = null;
    private boolean sequenceOk = false;
    private boolean isParameters = false;
    private Vector<ReadablePipe> readers = null;
    private WritablePipe writer = null;
    private DocumentSequence documents = null;

    public XInput(XProcRuntime runtime, Input input) {
        this.runtime = runtime;
        node = input.getNode();
        port = input.getPort();
        sequenceOk = input.getSequence();
        isParameters = input.getParameterInput();
        readers = new Vector<ReadablePipe> ();
    }

    public String getPort() {
        return port;
    }

    public XdmNode getNode() {
        return node;
    }

    public ReadablePipe getReader() {
        if (documents == null) {
            documents = new DocumentSequence(runtime);
        }
        ReadablePipe pipe = new Pipe(runtime, documents);
        pipe.canReadSequence(sequenceOk);
        readers.add(pipe);
        return pipe;
    }

    public WritablePipe getWriter() {
        if (writer != null) {
            throw new XProcException(node, "Attempt to create two writers for the same input.");
        }
        if (documents == null) {
            documents = new DocumentSequence(runtime);
        }
        writer = new Pipe(runtime, documents);
        return writer;
    }

    public boolean getSequence() {
        return sequenceOk;
    }

    public boolean getParameters() {
        return isParameters;
    }
}
