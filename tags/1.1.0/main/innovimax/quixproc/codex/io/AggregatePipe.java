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
package innovimax.quixproc.codex.io;

import innovimax.quixproc.codex.util.PipedDocument;
import innovimax.quixproc.codex.util.StepContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.saxon.s9api.XdmNode;

import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.DocumentSequence;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.model.Step;

public class AggregatePipe implements ReadablePipe {
    private static int idCounter = 0;
    private int id = 0;
    private XProcRuntime runtime = null;
    private DocumentSequence documents = null;      
    private boolean readSeqOk = false;  
    private Map<String, Step> readers = new HashMap<String, Step>();
    private Map<Integer, List<ReadablePipe>> c_pipes = new HashMap<Integer, List<ReadablePipe>>();       
    private Map<Integer, Integer> c_index = new HashMap<Integer, Integer>();       
    private ReadablePipe currentPipe = null;
    
    public AggregatePipe(XProcRuntime xproc) {
        runtime = xproc;
        documents = new DocumentSequence(xproc);         
        id = idCounter++;        
    }    
    
    public void aggregates(StepContext stepContext, ReadablePipe pipe) {
        List<ReadablePipe> pipes = c_pipes.get(new Integer(stepContext.curChannel));
        if (pipes == null) {
          pipes = new ArrayList<ReadablePipe>();
          c_pipes.put(new Integer(stepContext.curChannel), pipes);
          c_index.put(new Integer(stepContext.curChannel), new Integer(0));
        }
        pipes.add(pipe);
    }
    
    public DocumentSequence documents(StepContext stepContext) {
        // Innovimax : always empty for the moment... to complete !
        return documents;
    }    
        
    public void initialize(StepContext stepContext) {    
        // nop
    }    
            
    public void setReader(StepContext stepContext, Step step) {
        readers.put(Integer.toString(stepContext.curChannel),step);
    }      
        
    public Step getReader(StepContext stepContext) {
        return readers.get(Integer.toString(stepContext.curChannel));
    }    

    public void canReadSequence(boolean sequence) {
        readSeqOk = sequence;
    }     
    
    public boolean readSequence() {
        return readSeqOk;
    }    
    
    public boolean closed(StepContext stepContext) {                        
        List<ReadablePipe> pipes = c_pipes.get(new Integer(stepContext.curChannel));
        if (pipes != null) {      
            for (ReadablePipe pipe : pipes) {                
                if (!pipe.closed(stepContext)) {
                    return false;
                }            
            }  
        }      
        return true;
    }    
    
    public boolean moreDocuments(StepContext stepContext) {                        
        PipedDocument doc = null;      
        if (currentPipe != null && currentPipe.moreDocuments(stepContext)) {                  
          return true;
        }                   
        List<ReadablePipe> pipes = c_pipes.get(new Integer(stepContext.curChannel));        
        int index = c_index.get(new Integer(stepContext.curChannel)).intValue();                                 
        if (pipes != null && index < pipes.size()) {                 
          currentPipe = pipes.get(index++);                   
          c_index.put(new Integer(stepContext.curChannel),new Integer(index)); 
          return moreDocuments(stepContext);
        }
        return false;    
    }
    
    public int documentCount(StepContext stepContext) {        
        int count = 0;
        List<ReadablePipe> pipes = c_pipes.get(new Integer(stepContext.curChannel));
        if (pipes != null) {        
            for (ReadablePipe pipe : pipes) {
                count += pipe.documentCount(stepContext);
            }    
        }    
        return count;
    }
    
    public XdmNode read(StepContext stepContext) {         
        PipedDocument doc = readAsStream(stepContext);
        if (doc!=null) {
            return doc.getNode();
        }
        return null;
    }        
    
    public PipedDocument readAsStream(StepContext stepContext) {                
        if (currentPipe != null && currentPipe.moreDocuments(stepContext)) {        
            return currentPipe.readAsStream(stepContext); 
        }      
        List<ReadablePipe> pipes = c_pipes.get(new Integer(stepContext.curChannel));        
        int index = c_index.get(new Integer(stepContext.curChannel)).intValue(); 
        if (pipes != null && index < pipes.size()) {                 
          currentPipe = pipes.get(index++);   
          c_index.put(new Integer(stepContext.curChannel),new Integer(index)); 
          return readAsStream(stepContext);
        }
        return null;
    }     
    
    public ReadablePipe currentPipe() {
        return currentPipe;
    }      

    public String toString() {
        return "[aggregate-pipe #" + id + "] (" + documents + ")";
    }  
    
    public void resetReader(StepContext stepContext) {
        List<ReadablePipe> pipes = c_pipes.get(new Integer(stepContext.curChannel));
        if (pipes != null) {        
            for (ReadablePipe pipe : pipes) {
                pipe.resetReader(stepContext);
            }    
        } 
        c_index.put(new Integer(stepContext.curChannel), new Integer(0));
        currentPipe = null;        
    }      
        
    public String sequenceInfos() {        
        return documents.toString();
    }       
                         
}

