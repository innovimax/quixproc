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
import innovimax.quixproc.codex.util.Waiting;
import innovimax.quixproc.codex.util.shared.ChannelDocuments;
import innovimax.quixproc.codex.util.shared.DocumentList;
import innovimax.quixproc.util.shared.ChannelClosed;

import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import net.sf.saxon.s9api.XdmNode;

import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.model.Log;
// Innovimax: new import
// Innovimax: new import
// Innovimax: new import
// Innovimax: new import
// Innovimax: new import
// Innovimax: new import
// Innovimax: new import
// Innovimax: new import
// Innovimax: new import

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Oct 2, 2008
 * Time: 6:56:32 AM
 * To change this template use File | Settings | File Templates.
 */
public class DocumentSequence {
    protected static final String logger = "com.xmlcalabash.io.documentsequence";
    private XProcRuntime runtime = null;
    private Vector<XdmNode> documents = new Vector<XdmNode>();
    private boolean closed = false;
    private static int idCounter = 0;
    private int id = 0;
    private PipeLogger outputlog = null;
    private int readerCount = 0;

    public DocumentSequence(XProcRuntime xproc) {
        runtime = xproc;
        id = idCounter++;
        //runtime.finest(logger, null, "Created document-sequence #" + id);
    }

    public void addReader() {
        readerCount++;
        //System.err.println(this + ": " + readerCount);
    }

    public int getReaderCount() {
        return readerCount;
    }

    public void setLogger(Log log) {
        if (log != null) {
            outputlog = new PipeLogger(runtime, log);
        }
    }
    
    public String toString() {
        return "[document-sequence #" + id + " (" + documents.size() + " docs)]";
    }
    
    //*************************************************************************
    //*************************************************************************        
    //*************************************************************************
    // INNOVIMAX IMPLEMENTATION
    //*************************************************************************
    //*************************************************************************
    //*************************************************************************             
    
    private ChannelDocuments documentsMap = new ChannelDocuments();  // Innovimax: new property
    private ChannelClosed closedMap = new ChannelClosed();  // Innovimax: new property
    private Set<ReadablePipe> readers = new HashSet<ReadablePipe>();  // Innovimax: new property
    
    // Innovimax: new function
    public void addReader(ReadablePipe reader) {                   
        if (!readers.contains(reader)) {        
            readers.add(reader);               
            if (runtime.getQConfig().isTraceAll()) {
              runtime.getTracer().err("DOCSEQ #"+id+" HAS "+readers.size()+" READERS AFTER REGISTERING "+reader);
            }
        }
    }     
    
    // Innovimax: new function
    public void reset(int channel) {
        DocumentList documents = documentsMap.get(channel);      
        if (documents != null) {
          documents.clear();
        }
        closedMap.put(channel, false);
        if (outputlog != null) {
            outputlog.stopLogging();
        }        

    }    

    // Innovimax: new function    
    public StepContext checkChannel(StepContext stepContext) {        
        if (stepContext.curChannel != stepContext.altChannel) {            
            Waiting waiter = runtime.newWaiterInstance(null,stepContext.curChannel,null,null,"    PIPE > WAITING CHANNEL IS CREATED..."+this);
            while (true) {         
                if (documentsMap.containsKey(stepContext.curChannel)) {
                    return stepContext;
                } else if (documentsMap.containsKey(stepContext.altChannel)) {
                    StepContext newContext = new StepContext(stepContext);
                    newContext.curChannel = stepContext.altChannel;                    
                    return newContext;
                } else {                
                    waiter.check();                    
                    Thread.yield();
                }                       
            }                      
        }   
        return stepContext;          
    }
    
    // Innovimax: new function
    public void addChannel(int channel) {                
        addSequence(channel);   
    }            
    
    // Innovimax: new function
    private DocumentList addSequence(int channel) {                
        DocumentList documents = documentsMap.get(channel);
        if (documents == null) { 
            synchronized(this) {
                documents = new DocumentList();                
                documentsMap.put(channel, documents);
                closedMap.put(channel, false);                
            }
        }    
        return documents;
    }       
    
    // Innovimax: new function
    private void add(int channel, PipedDocument document) {                        
        DocumentList documents = addSequence(channel);                
        boolean error = false;
        Boolean closed = closedMap.get(channel);        
        if (closed != null) {  
            error = closed.booleanValue();            
        }                           
        if (error) {
            throw new XProcException("You can't add a document to a closed DocumentSequence.");
        }
        if (!document.isDocument()) {
            throw XProcException.dynamicError(1);
        }
        documents.add(document);
    }

    // Innovimax: new function  
    public void close(int channel) {                        
        addSequence(channel);
        closedMap.put(channel, true);   
    }    
    
    // Innovimax: new function  
    public boolean closed(int channel) {              
        Boolean closed = closedMap.get(channel);              
        if (closed != null) {  
            return closed.booleanValue();    
        }
        return false;             
    }
    
    // Innovimax: new function
    public int size(int channel) {        
        DocumentList documents = documentsMap.get(channel);
        if (documents != null) {  
            return documents.size();    
        }
        return 0;
    }        

    // Innovimax: new function   
    public PipedDocument get(int channel, int index) {        
        DocumentList documents = documentsMap.get(channel);      
        if (documents != null) {
            if (index < documents.size()) {
                PipedDocument doc = documents.get(index);                            
                return doc;
            }
            return null;
        }
        return null;
    }

    // Innovimax: new function   
    public PipedDocument newPipedDocument(int channel) {
        PipedDocument document = new PipedDocument(runtime, readers.size());
        add(channel, document); 
        return document;
    }

    // Innovimax: new function   
    public PipedDocument newPipedDocument(int channel, XdmNode result) {
        PipedDocument document = new PipedDocument(runtime, readers.size(), result);
        add(channel, document);
        return document;
    }

    //*************************************************************************
    //*************************************************************************        
    //*************************************************************************
    // INNOVIMAX DEPRECATION
    //*************************************************************************
    //*************************************************************************
    //*************************************************************************    
/*                 
    public void add(XdmNode document) {
        if (closed) {
            throw new XProcException("You can't add a document to a closed DocumentSequence.");
        } else {
            if (!S9apiUtils.isDocument(document)) {
                throw XProcException.dynamicError(1);
            }
            
            //runtime.finest(logger, null, "Wrote " + (document == null ? "null" : document.getBaseURI()) + " to " + toString());
            documents.add(document);
            if (outputlog != null) {
                outputlog.log(document);
            }
        }
    }

    public XdmNode get(int count) {
        if (count < documents.size()) {
            XdmNode doc = documents.get(count);
            //runtime.finest(logger, null, "Read " + (doc == null ? "null" : doc.getBaseURI()) + " from " + toString());
            return doc;
        } else {
            return null;
        }
    }

    public void close() {
        readerCount--;
        closed = true;
        if (outputlog != null) {
            outputlog.stopLogging();
        }
    }

    public boolean closed() {
        return closed;
    }

    public int size() {
        return documents.size();
    }

    public void reset() {
        documents.clear();
        closed = false;
        if (outputlog != null) {
            outputlog.stopLogging();
        }
    }
*/
}
