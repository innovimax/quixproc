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
package innovimax.quixproc.codex.util;

import innovimax.quixproc.codex.io.AggregatePipe;
import innovimax.quixproc.datamodel.IStream;
import innovimax.quixproc.datamodel.QuixEvent;
import innovimax.quixproc.datamodel.QuixException;
import innovimax.quixproc.util.SpyHandler;

import java.util.List;

import com.xmlcalabash.io.ReadablePipe;

public class EventReader {
    private PipedDocument doc = null;    
    private ReadablePipe pipe = null;        
    private StepContext stepContext = null;      
    private List<ReadablePipe> pipes = null;                
    private final SpyHandler spy;       
    private IStream<QuixEvent> stream = null;
    private int index = 0;
    private boolean closed = false;    
    private boolean startSequence = false;    
    private boolean endSequence = false;      
          
    public EventReader(PipedDocument doc, SpyHandler spy) { 
        this.doc = doc;         
        this.spy = spy; 
        if (doc==null) {                        
          closed = true;
        } else {
          stream = doc.registerReader();          
        }
    }      

    public EventReader(StepContext stepContext, ReadablePipe pipe, SpyHandler spy) { 
        this.stepContext = stepContext;        
        this.pipe = pipe;                    
        this.spy = spy;           
        doc = pipe.readAsStream(stepContext);                          
        if (doc!=null) {                     
            stream = doc.registerReader();            
        }        
        startSequence = true;
    }    
    
    public EventReader(StepContext stepContext, List<ReadablePipe> pipes, SpyHandler spy) { 
        this.stepContext = stepContext;         
        this.pipes = pipes;          
        this.spy = spy;   
        if (pipes.size() > 0) {
            pipe = pipes.get(index++);              
            doc = pipe.readAsStream(stepContext);            
            if (doc!=null) {                    
               stream = doc.registerReader();
            }
            startSequence = true;
        } else {
          // Innovimax: need to throw en error ?
          closed = true;
        }
    }        
    
    public int pipeIndex() {
        return index-1; 
    }
   
    public boolean hasEvent() throws QuixException {  
        if (closed) { 
            return false;
        }
        if (startSequence || endSequence) {
            return true;
        }
        if (doc != null && stream.hasNext()) {                            
            return true;
        }
        if (pipe != null) {            
            if (pipe.moreDocuments(stepContext)) {                
                doc = pipe.readAsStream(stepContext); 
                if (doc != null) {                       
                  stream = doc.registerReader();
                }
                return hasEvent();
            }
            if (pipes != null && index < pipes.size()) {
               pipe = pipes.get(index++);
               doc = pipe.readAsStream(stepContext); 
               if (doc != null) {                 
                 stream = doc.registerReader();
                }
               return hasEvent();
            }
            endSequence = true;                                          
            return true;
        }
        closed = true;
        return false;
    }             
    
    public QuixEvent nextEvent() throws QuixException {
        if (hasEvent()) {
            if (startSequence) {
                startSequence = false;
                return QuixEvent.getStartSequence();
            } 
            if (endSequence) {
                endSequence = false;                
                closed = true;               
                return QuixEvent.getEndSequence();
            }            
            QuixEvent event = stream.next();            
            switch(event.getType()) {
                case START_DOCUMENT:
                    if ( spy!= null) spy.spyStartDocument();
                    break;
                case END_DOCUMENT:
                    if (spy != null) spy.spyEndDocument();          
                    break;
            }      
            return event;
        }
        return null;
    }       
        
    /**
     * for debug only
     */  
     
    public ReadablePipe pipe() {
        if (pipe instanceof AggregatePipe) {
            return ((AggregatePipe)pipe).currentPipe();
        }
        return pipe;
    } 
    
    public PipedDocument document() {
        return doc;
    }         
           
//    public int eventCount() {
//        return doc.eventCount();        
//    }                
        
    public int documentId() {
        return doc.getId();        
    }                   
}

