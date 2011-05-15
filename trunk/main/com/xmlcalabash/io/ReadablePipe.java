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

public interface ReadablePipe {
    public void canReadSequence(boolean sequence);    
    public DocumentSequence documents();
                
    //*************************************************************************
    // INNOVIMAX IMPLEMENTATION
    //*************************************************************************         
    
    public void initialize(StepContext stepContext);
    public void resetReader(StepContext stepContext);            
    public Step getReader(StepContext stepContext);         
    public void setReader(StepContext stepContext, Step step);                  
    public boolean closed(StepContext stepContext);       
    public boolean moreDocuments(StepContext stepContext);     
    public int documentCount(StepContext stepContext);        
    public XdmNode read(StepContext stepContext) throws SaxonApiException;        
    public PipedDocument readAsStream(StepContext stepContext);          
    
    //*************************************************************************
    // INNOVIMAX DEPRECATION
    //*************************************************************************       
/*
    public void resetReader();
    public XdmNode read() throws SaxonApiException;
    public void setReader(Step step);
    public boolean moreDocuments();
    public boolean closed();
    public int documentCount();
*/
}
