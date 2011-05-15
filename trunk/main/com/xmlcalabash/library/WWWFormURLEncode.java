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

package com.xmlcalabash.library;

import java.io.UnsupportedEncodingException;
import java.util.Vector;

import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.util.ProcessMatch;
import com.xmlcalabash.util.ProcessMatchingNodes;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.model.RuntimeValue;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import com.xmlcalabash.runtime.XAtomicStep;

public class WWWFormURLEncode extends DefaultStep implements ProcessMatchingNodes {
    private ReadablePipe source = null;
    private WritablePipe result = null;
    private Vector<Tuple> params = new Vector<Tuple> ();
    private static final QName _match = new QName("", "match");
    private ProcessMatch matcher = null;
    private String encoded = "";

    /** Creates a new instance of FormURLEncode.
     */
    public WWWFormURLEncode(XProcRuntime runtime, XAtomicStep step) {
        super(runtime,step);
    }

    public void setInput(String port, ReadablePipe pipe) {
        source = pipe;
    }

    public void setOutput(String port, WritablePipe pipe) {
        result = pipe;
    }

    public void setParameter(QName name, RuntimeValue value) {
        int pos = -1;
        int count = -1;
        for (Tuple t : params) {
            count++;
            if (name.equals(t.name)) {
                pos = count;
            }
        }

        if (pos >= 0) {
            params.remove(pos);
        }

        params.add(new Tuple(name, value));
    }

    public void reset() {
        source.resetReader(stepContext);
        result.resetWriter(stepContext);
    }

    public void gorun() throws SaxonApiException {
        super.gorun();

        for (Tuple t : params) {
            if (!"".equals(encoded)) {
                encoded += "&";
            }
            encoded += t.name.getLocalName() + "=" + encode(t.value.getString());
        }
        
        matcher = new ProcessMatch(runtime, this);
        matcher.match(source.read(stepContext), getOption(_match));

        if (source.moreDocuments(stepContext)) {
            throw XProcException.dynamicError(6);
        }

        result.write(stepContext, matcher.getResult());
    }
    
    public boolean processStartDocument(XdmNode node) throws SaxonApiException {
        return true;
    }

    public void processEndDocument(XdmNode node) throws SaxonApiException {
        // nop?
    }

    public boolean processStartElement(XdmNode node) throws SaxonApiException {
        matcher.addText(encoded);
        return false;
    }

    public void processEndElement(XdmNode node) throws SaxonApiException {
        // nop?
    }

    public void processText(XdmNode node) throws SaxonApiException {
        matcher.addText(encoded);
    }

    public void processComment(XdmNode node) throws SaxonApiException {
        matcher.addComment(encoded);
    }

    public void processPI(XdmNode node) throws SaxonApiException {
        matcher.addPI(node.getNodeName().getLocalName(),encoded);
    }

    public void processAttribute(XdmNode node) throws SaxonApiException {
        matcher.addAttribute(node.getNodeName(), encoded);
    }

    private String encode(String src) {
        String genDelims = ":/?#[]@";
        String subDelims = "!$'()*+,;="; // N.B. NO &!
        String unreserved = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-._~";
        String okChars = genDelims + subDelims + unreserved;

        String encoded = "";
        
        try {
            byte[] bytes = src.getBytes("UTF-8");
            for (int pos = 0; pos < bytes.length; pos++) {
                if (okChars.indexOf(bytes[pos]) >= 0) {
                    encoded += (char) bytes[pos];
                } else {
                    encoded += String.format("%%%02X", bytes[pos]);
                }
            }
        } catch (UnsupportedEncodingException uee) {
            // This can't happen for UTF-8!
        }

        return encoded;
    }

    private class Tuple {
        public QName name;
        public RuntimeValue value;
        public Tuple(QName name, RuntimeValue value) {
            this.name = name;
            this.value = value;
        }
    }
}
