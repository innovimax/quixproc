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
package com.xmlcalabash.functions;

import com.xmlcalabash.runtime.XCompoundStep;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.ExtensionFunctionCall;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.om.SequenceIterator;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.runtime.XStep;
import net.sf.saxon.tree.iter.SingletonIterator;
import net.sf.saxon.value.Int64Value;
import net.sf.saxon.value.SequenceType;

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
 * Implementation of the XProc p:iteration-position function
 */

public class IterationPosition extends ExtensionFunctionDefinition {
    private XProcRuntime runtime;
    private static StructuredQName funcname = new StructuredQName("p", XProcConstants.NS_XPROC, "iteration-position");

    protected IterationPosition() {
        // you can't call this one
    }

    public IterationPosition(XProcRuntime runtime) {
        this.runtime = runtime;
    }

    public StructuredQName getFunctionQName() {
        return funcname;
    }

    public int getMinimumNumberOfArguments() {
        return 0;
    }

    public int getMaximumNumberOfArguments() {
        return 0;
    }

    public SequenceType[] getArgumentTypes() {
        return new SequenceType[]{SequenceType.OPTIONAL_ATOMIC};
    }

    public SequenceType getResultType(SequenceType[] suppliedArgumentTypes) {
        return SequenceType.SINGLE_ATOMIC;
    }

    public ExtensionFunctionCall makeCallExpression() {
        return new IterationPositionCall();
    }

    private class IterationPositionCall extends ExtensionFunctionCall {
        public SequenceIterator call(SequenceIterator[] arguments, XPathContext context) throws XPathException {
          
            // Innovimax: desactivated function
            if (true) throw new RuntimeException("IterationPosition is desactivated");
                      
            XStep step = runtime.getXProcData().getStep();
            // FIXME: this can't be the best way to do this...
            if (!(step instanceof XCompoundStep)) {
                throw XProcException.dynamicError(23);
            }
            return SingletonIterator.makeIterator(
                    new Int64Value(runtime.getXProcData().getIterationPosition()));
        }
    }
}