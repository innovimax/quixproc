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
package com.xmlcalabash.extensions;

import java.util.HashSet;

import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;

import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.library.DefaultStep;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.S9apiUtils;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Oct 8, 2008
 * Time: 7:44:07 AM
 * To change this template use File | Settings | File Templates.
 */

public class NamespaceDelete extends DefaultStep {
    private static final QName _prefixes = new QName("","prefixes");
    private ReadablePipe source = null;
    private WritablePipe result = null;

    /**
     * Creates a new instance of NamespaceDelete
     */
    public NamespaceDelete(XProcRuntime runtime, XAtomicStep step) {
        super(runtime,step);
    }

    public void setInput(String port, ReadablePipe pipe) {
        source = pipe;
    }

    public void setOutput(String port, WritablePipe pipe) {
        result = pipe;
    }

    public void reset() {
        source.resetReader(stepContext);
        result.resetWriter(stepContext);
    }

    // Innovimax: modified function
    public void gorun() throws SaxonApiException {
        super.gorun();

        HashSet<String> excludeUris = S9apiUtils.excludeInlinePrefixes(step.getNode(), getOption(_prefixes).getString());
         
        while (source.moreDocuments(stepContext)) {
            XdmNode doc = source.read(stepContext);
            runtime.finest(this, step.getNode(), "Namespace-delete step " + step.getName() + " read " + doc.getDocumentURI());
            doc = S9apiUtils.removeNamespaces(runtime, doc, excludeUris, false);
            result.write(stepContext,doc);
        }
        
        // Innovimax: close pipe
        result.close(stepContext);        
    }

  
}