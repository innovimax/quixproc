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

import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.model.Step;
import net.sf.saxon.s9api.XdmNode;

import java.util.logging.Logger;

import innovimax.quixproc.codex.util.Waiting;

import innovimax.quixproc.codex.util.PipedDocument;
import innovimax.quixproc.codex.util.StepContext;
import innovimax.quixproc.codex.util.shared.ChannelReader;
import innovimax.quixproc.codex.util.shared.ChannelWriter;
import innovimax.quixproc.util.shared.ChannelPosition;

public class Pipe implements ReadablePipe, WritablePipe {    
    private static int idCounter = 0;
    private int id = 0;
    private XProcRuntime runtime = null;
    private DocumentSequence documents = null;    
    private boolean readSeqOk = false;
    private boolean writeSeqOk = false;
    private Step writer = null;
    private Step reader = null;

    /** Creates a new instance of Pipe */
    public Pipe(XProcRuntime xproc) {        
        runtime = xproc;
        documents = new DocumentSequence(xproc);
        documents.addReader();
        id = idCounter++;
    }

    public Pipe(XProcRuntime xproc, DocumentSequence seq) {        
        runtime = xproc;
        documents = seq;
        seq.addReader();
        id = ++idCounter;
    }
    
    public void canWriteSequence(boolean sequence) {
        writeSeqOk = sequence;
    }    
    
    public void canReadSequence(boolean sequence) {
        readSeqOk = sequence;
    }

    public DocumentSequence documents() {
        return documents;
    }

    public String toString() {
        return "[pipe #" + id + "] (" + documents + ")";
    }        
    
    //*************************************************************************
    //*************************************************************************        
    //*************************************************************************
    // INNOVIMAX IMPLEMENTATION
    //*************************************************************************
    //*************************************************************************
    //*************************************************************************        
    
    private static int c_index = 0;  // Innovimax: new property    
    private ChannelPosition c_pos = new ChannelPosition();  // Innovimax: new property       
    private ChannelReader c_reader = new ChannelReader();  // Innovimax: new property  
    private ChannelWriter c_writer = new ChannelWriter();  // Innovimax: new property      
    
    // Innovimax: new function
    public void initialize(StepContext stepContext) {    
        // nop
    }    
    
    // Innovimax: new function
    public void resetReader(StepContext stepContext) {                  
        c_pos.reset(stepContext.curChannel);        
    }     
    
    // Innovimax: new function
    public void resetWriter(StepContext stepContext) {                
        c_pos.reset(stepContext.curChannel);
        documents.reset(stepContext.curChannel);
    }         
    
    // Innovimax: new function
    public void setReader(StepContext stepContext, Step step) {
        c_reader.put(stepContext.curChannel, step);
    }      
    
    // Innovimax: new function
    public Step getReader(StepContext stepContext) {
        return c_reader.get(stepContext.curChannel);
    }  
            
    // Innovimax: new function
    public Step getWriter(StepContext stepContext) {
        return c_writer.get(stepContext.curChannel);
    }        
    
    // Innovimax: new function
    public void setWriter(StepContext stepContext, Step step) {
        c_writer.put(stepContext.curChannel, step);
    }     
    
    // Innovimax: new function
    public boolean closed(StepContext stepContext) {        
        StepContext newContext = documents.checkChannel(stepContext);
        return documents.closed(newContext.curChannel);
    }    
 
    // Innovimax: new function
    public boolean moreDocuments(StepContext stepContext) {         
        StepContext newContext = documents.checkChannel(stepContext);
        boolean more = true;
        boolean waiting = true;                        
        Waiting waiter = runtime.newWaiterInstance(null,newContext.curChannel,this,null,"    PIPE > WAITING CHANNEL IS CLOSED...X");
        while (waiting) {                    
            int pos = c_pos.get(newContext.curChannel);
            int size = documents.size(newContext.curChannel);                        
            if (pos < size) {
                waiting = false;
            } else {                
                boolean closed = closed(newContext);                
                if (closed) {
                    waiting = false;
                    // must do that for security if channel is closed after the first test
                    pos = c_pos.get(newContext.curChannel);
                    size = documents.size(newContext.curChannel);            
                    more = (pos < size);                                            
                } else {
                    waiter.check("("+pos+"/"+size+")");                    
                    Thread.yield();     
                }     
            }
        }        
        return more;
    }
    
    // Innovimax: new function
    public int documentCount(StepContext stepContext) {
        StepContext newContext = documents.checkChannel(stepContext);  
        return documents.size(newContext.curChannel);
    }       
    
    // Innovimax: new function
    public XdmNode read(StepContext stepContext) {         
        PipedDocument doc = readAsStream(stepContext);
        if (doc!=null) {
            return doc.getNode();
        }
        return null;
    }    

    // Innovimax: new function
    public PipedDocument readAsStream(StepContext stepContext) {                
        StepContext newContext = documents.checkChannel(stepContext);                          
        runtime.getTracer().debug(null,newContext,-1,this,null,"    PIPE > TRY READING...");                
        if (c_pos.get(newContext.curChannel) > 0 && !readSeqOk) {                  
            throw XProcException.dynamicError(6);
        }
                
        PipedDocument doc = null;                
        if (moreDocuments(newContext)) {
            doc = documents.get(newContext.curChannel, c_pos.get(newContext.curChannel));
            Waiting waiter = runtime.newWaiterInstance(null,newContext.curChannel,this,null,"    PIPE > WAITING FOR DOCUMENT..."); 
            while (doc==null) {                   
                waiter.check();
                Thread.yield();
                doc = documents.get(newContext.curChannel, c_pos.get(newContext.curChannel));
            }
            c_pos.increment(newContext.curChannel);                        
            runtime.getTracer().debug(null,newContext,-1,this,doc,"    PIPE > READ ");            
        } else {
            runtime.getTracer().debug(null,newContext,-1,this,null,"    PIPE > NO MORE DOCUMENT");            
            if (!readSeqOk) {
                throw XProcException.dynamicError(6, "no document appear on port.");
            }
        }
        
        if (reader != null) {
            runtime.finest(null, reader.getNode(), reader.getName() + " read '" + (doc == null ? "null" : doc.getBaseURI()) + "' from " + this);
        }               
                
        return doc;
    }
            
    // Innovimax: new function
    public void write(StepContext stepContext, XdmNode doc) {       
        PipedDocument document = documents.newPipedDocument(stepContext.curChannel, doc);
        runtime.getTracer().debug(null,stepContext,-1,this,document,"    PIPE > WRITE ");
        if (documents.size(stepContext.curChannel) > 1 && !writeSeqOk) {
            throw XProcException.dynamicError(7);
        }        
        if (!writeSeqOk) {                   
            documents.close(stepContext.curChannel);
        }            
    }          
    
    // Innovimax: new function
    public void close(StepContext stepContext) {             
        documents.close(stepContext.curChannel);                
    }    

    // Innovimax: new function
    public void addChannel(int channel) {      
        documents.addChannel(channel); 
    }    

    // Innovimax: new function
    public synchronized int nextChannel() {                
        return ++c_index;        
    }

    // Innovimax: new function
    @Override
    public PipedDocument newPipedDocument(int channel) {
      PipedDocument document = documents.newPipedDocument(channel); 
      runtime.getTracer().debug(null,null,channel,this,document,"    PIPE > WRITE ");
      if (documents.size(channel) > 1 && !writeSeqOk) {
          throw XProcException.dynamicError(7);
      }              
      if (!writeSeqOk) {                          
          documents.close(channel);            
      }      
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
    private Logger logger = Logger.getLogger(this.getClass().getName());
    private int pos = 0;
    
    public void setReader(Step step) {
        reader = step;
    }

    public void setWriter(Step step) {
        writer  = step;
    }

    public void resetReader() {
        pos = 0;
    }
    
    public void resetWriter() {
        documents.reset();
        pos = 0;
    }

    public boolean moreDocuments() {
        return pos < documents.size();
    }

    public boolean closed() {
        return documents.closed(stepContext);
    }

    public void close() {
        /*
        This causes problems in a for-each if the for-each never runs...
        Plus I think I"m catching this error higher up now.
        if (documents.size() == 0 && !writeSeqOk) {
            throw XProcException.dynamicError(7);
        }
        *//*
        documents.close(stepContext);
    }

    public int documentCount() {
        return documents.size();
    }

    public XdmNode read () {
        if (pos > 0 && !readSeqOk) {
            throw XProcException.dynamicError(6);
        }

        XdmNode doc = documents.get(pos++);

        if (reader != null) {
            runtime.finest(null, reader.getNode(), reader.getName() + " read '" + (doc == null ? "null" : doc.getBaseURI()) + "' from " + this);
        }
        
        return doc;
    }

    public void write(XdmNode doc) {
        if (writer != null) {
            runtime.finest(null, writer.getNode(), writer.getName() + " wrote '" + (doc == null ? "null" : doc.getBaseURI()) + "' to " + this);
        }
        documents.add(doc);

        if (documents.size() > 1 && !writeSeqOk) {
            throw XProcException.dynamicError(7);
        }
    }
*/
}

