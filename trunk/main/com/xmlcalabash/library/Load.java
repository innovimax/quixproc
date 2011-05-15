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

import java.net.URI;

import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcConstants;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.QName;

import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.model.RuntimeValue;

public class Load extends DefaultStep {
    protected static final String logger = "org.xproc.library.load";
    private static final QName _href = new QName("href");
    private static final QName _dtd_validate = new QName("dtd-validate");
    private static final QName err_XD0011 = new QName("err", XProcConstants.NS_XPROC_ERROR, "XD0011");

    private WritablePipe result = null;
    private URI href = null;

    /**
     * Creates a new instance of Load
     */
    public Load(XProcRuntime runtime, XAtomicStep step) {
        super(runtime,step);
    }

    public void setOutput(String port, WritablePipe pipe) {
        result = pipe;
    }

    public void reset() {
        result.resetWriter(stepContext);
    }

    // Innovimax: modified function
    public void gorun() throws SaxonApiException {
        super.gorun();
        
        RuntimeValue href = getOption(_href);
        // Innovimax: external base uri
        //String baseURI = href.getBaseURI().toASCIIString();        
        String baseURI = runtime.getQConfig().getBaseURI(href.getBaseURI()).toASCIIString();         
        
        if (runtime.getSafeMode() && baseURI.startsWith("file:")) {
            throw XProcException.dynamicError(21);
        }
        
        // Innovimax: statistics                                  
        URI hrefURI = runtime.getQConfig().resolveURI(href.getBaseURI(), href.getString()); 
        java.io.File input = new java.io.File(hrefURI);          
        if (input.exists()) { totalFileSize += input.length(); }          
        
        boolean validate = getOption(_dtd_validate, false);
        try {
            XdmNode doc = runtime.parse(href.getString(), baseURI, validate);            
            result.write(stepContext, doc);
        } catch (XProcException e) {            
            e.printStackTrace();
            if (err_XD0011.equals(e.getErrorCode())) {
                throw XProcException.stepError(11, "Could not load " + href.getString() + " (" + baseURI + ") dtd-validate=" + validate);
            }
            throw e;
        } catch (Exception e) {            
            throw new XProcException(e);
        }
    }
    
    //*************************************************************************
    //*************************************************************************        
    //*************************************************************************
    // INNOVIMAX IMPLEMENTATION
    //*************************************************************************
    //*************************************************************************
    //*************************************************************************      

    // Innovimax: statistics  
    private static long totalFileSize = 0;              
    public static long getTotalFileSize() { return totalFileSize; }     
    public static void resetTotalFileSize() { totalFileSize = 0; }  
        
}

