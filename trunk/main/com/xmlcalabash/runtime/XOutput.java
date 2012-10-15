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
import com.xmlcalabash.model.Log;
import com.xmlcalabash.model.Output;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Oct 7, 2008
 * Time: 8:00:21 AM
 * To change this template use File | Settings | File Templates.
 */
public class XOutput {
    private DocumentSequence documents = null;
    private XProcRuntime runtime = null;
    private String port = null;
    private XdmNode node = null;
    private boolean sequenceOk = false;
    private WritablePipe writer = null;
    private WritablePipe inputWriter = null;
    private Vector<ReadablePipe> readers = null;

    public XOutput(XProcRuntime runtime, Output output) {
        this.runtime = runtime;
        node = output.getNode();
        port = output.getPort();
        sequenceOk = output.getSequence();
        documents = new DocumentSequence(runtime);
        readers = new Vector<ReadablePipe> ();
    }

    public void setLogger(Log log) {
        documents.setLogger(log);
    }

    public XdmNode getNode() {
        return node;
    }

    public String getPort() {
        return port;
    }

    public boolean getSequence() {
        return sequenceOk;
    }

    public ReadablePipe getReader() {
        ReadablePipe pipe = new Pipe(runtime, documents);
        readers.add(pipe);
        return pipe;
    }

    public WritablePipe getWriter() {
        if (writer != null) {
            throw new XProcException(node, "Attempt to create two writers for the same output.");
        }
        if (inputWriter != null) {
            writer = inputWriter;
        } else {
            writer = new Pipe(runtime, documents);
        }
        writer.canWriteSequence(sequenceOk);
        return writer;
    }
}
