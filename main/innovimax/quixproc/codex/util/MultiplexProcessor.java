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

import innovimax.quixproc.datamodel.MatchEvent;
import innovimax.quixproc.datamodel.QuixEvent;
import innovimax.quixproc.util.MatchQueue;

import java.util.LinkedList;
import java.util.List;

import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.DocumentSequence;
import com.xmlcalabash.io.ReadablePipe;

public class MultiplexProcessor
{
  private XProcRuntime runtime = null; 
  private StepContext stepContext = null;     
  private ReadablePipe in = null;
  private DocumentSequence out = null;   
  private String key = null;
    
  private boolean matched = false;
  private boolean sequenced = false;
  private String baseURI = null; // document base URI  
  private PipedDocument document = null;     
  private boolean startSequence = false;
  private List<MatchQueue> mxStack = new LinkedList<MatchQueue>(); // muliplex stack events (write)
  private List<MatchQueue> mxEvents = new LinkedList<MatchQueue>(); // muliplex channels events (read)
  private int mxDepht = 0; // multiplex depth (write)
  private int mxChannel = 0; // multiplex channel (read)   
  private MatchEvent mxStop = new MatchEvent(null); // multiplex stop channel
          
  public MultiplexProcessor(XProcRuntime runtime, StepContext stepContext, ReadablePipe in, DocumentSequence out, String key)
  {  
    this.runtime = runtime;
    this.stepContext = stepContext;      
    this.in = in;
    this.out = out;            
    this.key = key;    
  } 
 
  public void processEvent(MatchEvent match) throws Exception
  {          
    QuixEvent event = match.getEvent();          
    if (event.isStartSequence()) {         
      runtime.getTracer().debug(null,stepContext,-1,in,null,"      "+key+" > INPUT > START SEQUENCE");         
      sequenced = true;
    } else if (event.isEndSequence()) {                    
      runtime.getTracer().debug(null,stepContext,-1,in,null,"      "+key+" > INPUT > END SEQUENCE");                    
    } else if (event.isStartDocument()) {
      runtime.getTracer().debug(null,stepContext,-1,in,null,"      "+key+" > INPUT > START DOCUMENT");                 
    } else if (event.isEndDocument()) {
      runtime.getTracer().debug(null,stepContext,-1,in,null,"      "+key+" > INPUT > END DOCUMENT");                    
    }     
    pushToMultiplexList(match);
    processQueueEvents();                  
  }  
  
  private void processQueueEvents() throws Exception
  {       
    MatchEvent match = null;    
    if (mxChannel<mxEvents.size()) {
      MatchQueue mxQueue = mxEvents.get(mxChannel);        
      match = mxQueue.pull(); 
      if (match!=null) {
        //if (match.isMatched()) { System.out.println("PULL FROM "+(mxChannel+1)+" > "+match.getEvent());  }          
        if (match==mxStop) { mxChannel++; }        
        else if (match.getEvent().isEndSequence()) { 
          mxChannel++; 
          mxEvents.clear(); 
        }        
        else if (match.getEvent().isEndDocument()&&!sequenced) {           
          mxChannel++; 
          mxEvents.clear(); 
        }        
      }      
    }      
    while (match!=null) {
      QuixEvent event = match.getEvent();
      if (event!=null) {
        switch (event.getType()) {
          case START_SEQUENCE :                    
            runtime.getTracer().debug(null,stepContext,-1,in,null,"      "+key+" > OUTPUT > BYPASS START SEQUENCE");  
            startSequence = true;
            break;
          case END_SEQUENCE :
            runtime.getTracer().debug(null,stepContext,-1,in,null,"      "+key+" > OUTPUT > BYPASS END SEQUENCE");           
            out.close(stepContext.curChannel);
            break;              
          case START_DOCUMENT :
            baseURI = event.asStartDocument().getURI();                       
            runtime.getTracer().debug(null,stepContext,-1,in,null,"      "+key+" > OUTPUT > BASE URI="+baseURI+" MX="+(mxChannel+1));                  
            break;
          case END_DOCUMENT :
            if (!startSequence) {
              out.close(stepContext.curChannel);
            }
            break;
          case START_ELEMENT :                
            if (match.isMatched()) {                             
              document = out.newPipedDocument(stepContext.curChannel);              
              runtime.getTracer().debug(null,stepContext,-1,in,null,"      "+key+" > OUTPUT > START DOCUMENT MX="+(mxChannel+1));
              document.append(QuixEvent.getStartDocument(baseURI));
              matched = true;
            }
            if (matched) { document.append(event); }                                                            
            break;
          case END_ELEMENT :                       
            if (matched) {
              if (match.isMatched()) {   
                runtime.getTracer().debug(null,stepContext,-1,in,null,"      "+key+" > OUTPUT > END DOCUMENT MX="+(mxChannel+1));
                document.append(event);                                                    
                document.append(QuixEvent.getEndDocument(baseURI)); 
                document.close();  
                matched = false;                                          
              } else {
                document.append(event);                          
              }                      
            }             
            break; 
          default :                
            if (match.isMatched()) { throw XProcException.dynamicError(16); }
            if (matched) { document.append(event); }
            break;               
        }
      }
      //match.clear();  // care : many references in multiplex                   
      match = null;
      if (mxChannel<mxEvents.size()) {
        MatchQueue mxQueue = mxEvents.get(mxChannel);        
        match = mxQueue.pull();    
        if (match!=null) { 
          //if (match.isMatched()) { System.out.println("PULL FROM "+(mxChannel+1)+" > "+match.getEvent());  }
          if (match==mxStop) { mxChannel++; }        
          else if (match.getEvent().isEndSequence()) { 
            mxChannel++; 
            mxEvents.clear(); 
          }        
          else if (match.getEvent().isEndDocument()&&!sequenced) {           
            mxChannel++; 
            mxEvents.clear(); 
          }        
        }        
      }  
      Thread.yield();          
    }             
  }
  
  private void pushToMultiplexList(MatchEvent match) throws Exception 
  {   
    //System.out.println(">>> PUSH EVENT "+match.getEvent()); 
    QuixEvent event = match.getEvent();       
    if (match.isMatched()&&event.isStartElement()) { mxDepht++; }
    int wmxDepht = 1;
    if (mxDepht>0) { wmxDepht = mxDepht; }    
    for (int mxLevel=1; mxLevel<=wmxDepht; mxLevel++) {                
      MatchEvent match2 = null;
      MatchQueue mxQueue = null;
      if (mxLevel<=mxStack.size()) { mxQueue = mxStack.get(mxLevel-1); }            
      if (mxQueue==null) { 
        mxQueue = new MatchQueue();        
        mxStack.add(mxQueue);     
        mxEvents.add(mxQueue);     
        //System.out.println("INCREASE STACK LEVEL TO "+mxStack.size());
        //System.out.println("INCREASE EVENTS CHANNEL TO "+mxEvents.size());          
      }          
      if (match.isMatched()&&(mxLevel<wmxDepht)) {
        match2 = new MatchEvent(match.getEvent());
        match2.setMatched(false);
        mxQueue.push(match2);            
      } else {            
        mxQueue.push(match);
      }
      //if (match.isMatched()) { System.out.println("PUSH TO LEVEL "+mxLevel+" > "+match.getEvent()); }                
      if (match.isMatched()&&event.isEndElement()&&(mxLevel==wmxDepht)) {
        //System.out.println("PUSH TO LEVEL "+mxLevel+" > STOP"); 
        mxQueue.push(mxStop);               
        if (mxStack.size()>0) {          
          mxStack.remove(mxStack.size()-1);           
          //System.out.println("DECREASE STACK LEVEL TO "+mxStack.size());                    
        }
        mxDepht--;                          
      } 
    }
  }  
        
}

