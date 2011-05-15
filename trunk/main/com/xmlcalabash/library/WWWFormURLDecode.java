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

import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.util.TreeWriter;
import com.xmlcalabash.util.TypeUtils;
import com.xmlcalabash.io.WritablePipe;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.QName;
import com.xmlcalabash.runtime.XAtomicStep;

public class WWWFormURLDecode extends DefaultStep {
    public static final QName _value = new QName("", "value");
    public static final QName _name = new QName("", "name");
    public static final QName c_paramset = new QName("c",XProcConstants.NS_XPROC_STEP,"param-set");
    public static final QName c_param = new QName("c", XProcConstants.NS_XPROC_STEP, "param");
    private WritablePipe result = null;

    /** Creates a new instance of FormURLDecode */
    public WWWFormURLDecode(XProcRuntime runtime, XAtomicStep step) {
        super(runtime,step);
    }

    public void setOutput(String port, WritablePipe pipe) {
        result = pipe;
    }

    public void reset() {
        result.resetWriter(stepContext);
    }

    public void gorun() throws SaxonApiException {
        super.gorun();

        String value = getOption(_value).getString();

        TreeWriter tree = new TreeWriter(runtime);
        tree.startDocument(step.getNode().getBaseURI());
        tree.addStartElement(c_paramset);
        tree.startContent();

        String[] params = value.split("&");
        for (int idx = 0; idx < params.length; idx++) {
            String p = params[idx];
            int pos = p.indexOf("=");
            if (pos > 0) {
                String name = p.substring(0, pos);
                String val = p.substring(pos+1);

                try {
                    TypeUtils.checkType(runtime, name, XProcConstants.xs_NCName, null);
                } catch (XProcException e) {
                    throw XProcException.stepError(61);
                }

                tree.addStartElement(c_param);
                tree.addAttribute(_name, name);
                tree.addAttribute(_value, decode(val));
                tree.startContent();
                tree.addEndElement();
            } else {
                throw new XProcException(step.getNode(), "Badly formatted parameters");
            }
        }

        tree.addEndElement();
        tree.endDocument();
        result.write(stepContext, tree.getResult());
    }

    private String decode(String val) {
        int pos;
        String result = "";

        while ((pos = val.indexOf("%")) >= 0) {
            result += val.substring(0, pos);

            try {
                String digits = val.substring(pos+1,pos+3);
                int dec = Integer.parseInt(digits, 16);
                char ch = (char) dec;
                result += ch;
            } catch (StringIndexOutOfBoundsException ex) {
                throw new XProcException("Badly formatted parameters", ex);
            } catch (NumberFormatException ex) {
                throw new XProcException("Badly formatted parameters", ex);
            }

            val = val.substring(pos+3);
        }

        result += val;
        return result;
    }
}
