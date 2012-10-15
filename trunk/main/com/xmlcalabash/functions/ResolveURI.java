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
package com.xmlcalabash.functions;

import java.net.URI;
import java.net.URISyntaxException;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.ExtensionFunctionCall;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.SingletonIterator;
import net.sf.saxon.tree.tiny.TinyDocumentImpl;
import net.sf.saxon.value.AnyURIValue;
import net.sf.saxon.value.SequenceType;

import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.runtime.XCompoundStep;
import com.xmlcalabash.runtime.XStep;

//
// The contents of this file are subject to the Mozilla Public License Version 1.0 (the "License");
// you may not use this file except in compliance with the License. You may obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by Norman Walsh are Copyright (C) Mark Logic Corporation. All Rights Reserved.
//
// Contributor(s): Norman Walsh.
//

/**
 * Implementation of the XSLT system-property() function
 */

public class ResolveURI extends ExtensionFunctionDefinition {
    private static StructuredQName funcname = new StructuredQName("p", XProcConstants.NS_XPROC, "resolve-uri");
    private ThreadLocal<XProcRuntime> tl_runtime = new ThreadLocal<XProcRuntime>() {
        protected synchronized XProcRuntime initialValue() {
            return null;
        }
    };
    
    private XProcRuntime runtime = null; // Innovimax: new property    

    protected ResolveURI() {
        // you can't call this one
    }

    // Innovimax: modified constructor
    public ResolveURI(XProcRuntime runtime) {
        // Innovimax: desactivate ThreadLocal 
        //tl_runtime.set(runtime);
        this.runtime = runtime; 
    }

    public StructuredQName getFunctionQName() {
        return funcname;
    }

    public int getMinimumNumberOfArguments() {
        return 1;
    }

    public int getMaximumNumberOfArguments() {
        return 2;
    }

    public SequenceType[] getArgumentTypes() {
        return new SequenceType[]{SequenceType.SINGLE_STRING, SequenceType.OPTIONAL_STRING};
    }

    public SequenceType getResultType(SequenceType[] suppliedArgumentTypes) {
        return SequenceType.SINGLE_ATOMIC;
    }

    public boolean dependsOnFocus() {
        return true;
    }

    public ExtensionFunctionCall makeCallExpression() {
        return new ResolveURICall();
    }

    private class ResolveURICall extends ExtensionFunctionCall {
        // Innovimax: modified function      
        public SequenceIterator call(SequenceIterator[] arguments, XPathContext context) throws XPathException {
            SequenceIterator iter = arguments[0];
            String relativeURI = iter.next().getStringValue();

            // Innovimax: desactivate ThreadLocal
            //XProcRuntime runtime = tl_runtime.get();    
            XStep step = runtime.getXProcData().getStep();
            // FIXME: this can't be the best way to do this...
            // step == null in use-when
            if (step != null && !(step instanceof XCompoundStep)) {
                throw XProcException.dynamicError(23);
            }

            String baseURI = null;
            if (arguments.length > 1) {
                iter = arguments[1];
                baseURI = iter.next().getStringValue();
            } else {
                baseURI = runtime.getStaticBaseURI().toASCIIString();
                try {
                    // FIXME: TinyDocumentImpl? Surely we can do better than that!
                    Item item = context.getContextItem();
                    baseURI = ((TinyDocumentImpl) item).getBaseURI();
                } catch (Exception e) {
                    // nop
                }
            }

            // Michael Kay's done the heavy lifting and made the method public, so let's just
            // do that!
            try {
                URI abs = net.sf.saxon.functions.ResolveURI.makeAbsolute(relativeURI, baseURI);
                String resolvedURI = abs.toASCIIString();
                return SingletonIterator.makeIterator(new AnyURIValue(resolvedURI));
            } catch (URISyntaxException use) {
                throw new XProcException(use);
            }
        }
    }
}