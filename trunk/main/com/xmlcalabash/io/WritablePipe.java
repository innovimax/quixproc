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
import net.sf.saxon.s9api.XdmNode;

import com.xmlcalabash.model.Step;
// Innovimax: new import
// Innovimax: new import

/**
 *
 * @author ndw
 */
public interface WritablePipe {
    public void canWriteSequence(boolean sequence);    
    public boolean writeSequence();
    
    //*************************************************************************
    // INNOVIMAX IMPLEMENTATION
    //*************************************************************************               
        
    public void resetWriter(StepContext stepContext);    
    public Step getWriter(StepContext stepContext);            
    public void setWriter(StepContext stepContext, Step step);     
    public void write(StepContext stepContext, XdmNode document);         
    public void close(StepContext stepContext);    
     
    public void addChannel(int channel);
    public PipedDocument newPipedDocument(int channel);  
    
    //*************************************************************************
    // INNOVIMAX DEPRECATION
    //*************************************************************************              
/*        
    public void resetWriter();    
    public void write(XdmNode document);             
    public void setWriter(Step step);      
    public void close();    
*/     
}
