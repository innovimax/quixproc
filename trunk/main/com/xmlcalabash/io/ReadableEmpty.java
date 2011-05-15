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

package com.xmlcalabash.io;

import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.SaxonApiException;
import com.xmlcalabash.model.Step;

import innovimax.quixproc.codex.util.PipedDocument;
import innovimax.quixproc.codex.util.StepContext;

public class ReadableEmpty implements ReadablePipe {
    public void canReadSequence(boolean sequence) {
        // nop;
    }
  
    public DocumentSequence documents() {
        return null;
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
    public XdmNode read(StepContext context) throws SaxonApiException {         
        return null;
    } 
    
    // Innovimax: new function
    public PipedDocument readAsStream(StepContext context) {
        return null;    
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
*/
}
