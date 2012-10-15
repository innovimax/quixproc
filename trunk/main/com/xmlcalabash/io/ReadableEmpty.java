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

package com.xmlcalabash.io;

import innovimax.quixproc.codex.util.PipedDocument;
import innovimax.quixproc.codex.util.StepContext;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;

import com.xmlcalabash.model.Step;
// Innovimax: new import
// Innovimax: new import

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Nov 19, 2009
 * Time: 4:00:20 PM
 * To change this template use File | Settings | File Templates.
 */
public class ReadableEmpty implements ReadablePipe {
    public void canReadSequence(boolean sequence) {
        // nop;
    }
    
    public boolean readSequence() {
        return false;
    }    
    
    //*************************************************************************
    //*************************************************************************        
    //*************************************************************************
    // INNOVIMAX IMPLEMENTATION
    //*************************************************************************
    //*************************************************************************
    //************************************************************************* 
    
    // Innovimax: new function
    public void initialize(StepContext context) {    
        // nop
    }    
    
    // Innovimax: new function
    public void resetReader(StepContext context) {
        // nop
    }       
   
    // Innovimax: new function
    public void setReader(StepContext context, Step step) {
        // nop
    }      
    
    // Innovimax: new function
    public Step getReader(StepContext context) {
        return null;
    }    
    
    // Innovimax: new function
    public boolean closed(StepContext context) {
        return true;
    }         

    // Innovimax: new function
    public boolean moreDocuments(StepContext context) {
        return false;
    }

    // Innovimax: new function
    public int documentCount(StepContext context) {
        return 0;
    }   
    
    // Innovimax: new function
    public DocumentSequence documents(StepContext context) {
        return null;
    }       
    
    // Innovimax: new function
    public XdmNode read(StepContext context) throws SaxonApiException {         
        return null;
    } 
    
    // Innovimax: new function
    public PipedDocument readAsStream(StepContext context) {
        return null;    
    }  
    
    // Innovimax: new function
    public String sequenceInfos() {        
        return "";
    }         
    
    //*************************************************************************
    //*************************************************************************        
    //*************************************************************************
    // INNOVIMAX DEPRECATION
    //*************************************************************************
    //*************************************************************************
    //*************************************************************************      
/*
    public XdmNode read() throws SaxonApiException {
        return null;    }

    public void setReader(Step step) {
        // nop
    }

    public void resetReader() {
        // nop
    }

    public boolean moreDocuments() {
        return false;
    }

    public boolean closed() {
        return false;
    }

    public int documentCount() {
        return 0;
    }

    public DocumentSequence documents() {
        return null;
    }
*/    
}
