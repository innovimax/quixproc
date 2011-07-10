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
package innovimax.quixproc.codex.util;

import innovimax.quixproc.datamodel.MatchEvent;
import innovimax.quixproc.datamodel.QuixEvent;
import innovimax.quixproc.util.MatchHandler;
import innovimax.quixproc.util.MatchQueue;
import innovimax.quixproc.codex.util.MultiplexProcessor;

import java.util.Hashtable;

import net.sf.saxon.s9api.QName;

import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.DocumentSequence;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.model.RuntimeValue;

public class DocumentSelector implements MatchHandler 
{
  private XProcRuntime runtime = null;  
  private ReadablePipe in = null;
  private DocumentSequence out = null;
  private String xpath = null;    
  private Hashtable<String,String> nsBindings = null;
  private Hashtable<QName,RuntimeValue> globals = null; 
  private StepContext stepContext = null;  
  private boolean matched = false;
  private boolean sequenced = false;
  private String baseURI = null; // document base URI  
  private PipedDocument document = null;    
  private MultiplexProcessor mxProcess = null;  
          
  public DocumentSelector(XProcRuntime runtime, StepContext stepContext, ReadablePipe in, DocumentSequence out, String xpath, Hashtable<String,String> nsBindings, Hashtable<QName,RuntimeValue> globals) 
  {  
    this.runtime = runtime;
    this.stepContext = stepContext;   
    this.in = in;
    this.out = out;
    this.xpath = xpath;        
    this.nsBindings = nsBindings;
    this.globals = globals;    
    mxProcess = new MultiplexProcessor(runtime, stepContext, in, out, "SELECT");
  }    
 
  public void exec() 
  { 
    try {
      EventReader evr = new EventReader(stepContext, in, null);         
      XPathMatcher xmatch = new XPathMatcher(runtime.getQConfig().getQuiXPath(), evr, this, xpath, true);
      Thread t = new Thread(xmatch);                         
      t.start();              
    } 
    catch (Exception e) {            
      throw new XProcException(e);      
    }
  }  
  
  /** 	  
   * match handler interface
   */  
   
  public void startProcess() 
  {
    runtime.getTracer().debug(null,stepContext,-1,in,null,"      SELECT > START THREAD");
  }
  
  public void endProcess() 
  {
    runtime.getTracer().debug(null,stepContext,-1,in,null,"      SELECT > END THREAD");
  }     
  
  public void errorProcess(Exception e) 
  {    
      if (e instanceof RuntimeException) {
          throw (RuntimeException)e;
      }
      throw new RuntimeException(e);
  }     

  public void processEvent(MatchEvent match) 
  { 
    try {  
      mxProcess.processEvent(match);          
    }         
    catch (Exception e) { throw new XProcException(e); }         
  }  
        
}

