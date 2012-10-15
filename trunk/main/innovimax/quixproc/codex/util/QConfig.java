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

import java.io.File;
import java.net.URI;

import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.util.URIUtils;

public class QConfig {  
  
  public final static int MODE_RUN_IT = 0;  
  public final static int MODE_STREAM_ALL = 1; 
  public final static int MODE_DOM_ALL = 2; 
  
  public final static int TRACE_NO = 0;  
  public final static int TRACE_WAIT = 1; 
  public final static int TRACE_ALL = 2; 
  
  private Tracing tracer = null;
  private Waiting waiter = null;
  private Spying spy = null;
  private IEQuiXPath quixpath = null;
  private int traceMode = 0;
  private int runMode = 0;
  private File baseDir = null;
    
  public void setTracer(Tracing tracer) {    
    this.tracer = tracer;
  }      
  
  public Tracing getTracer() {    
    return tracer;
  }   

  public void setWaiter(Waiting waiter) {    
    this.waiter = waiter;
  }      
  
  public Waiting getWaiter() {    
    return waiter;
  }      
  
  public void setSpy(Spying spy) {    
    this.spy = spy;
  }      
  
  public Spying getSpy() {    
    return spy;
  }      
  
  public void setTraceMode(int traceMode) {    
    this.traceMode = traceMode;
  }      
  
  public int getTraceMode() {    
    return traceMode;
  }   
  
  public void setRunMode(int runMode) {    
    this.runMode = runMode;
  }      
  
  public void setQuiXPath(IEQuiXPath quixpath) {
    this.quixpath = quixpath;
  }
  
  public IEQuiXPath getQuiXPath() {
    return this.quixpath;
  }
  public int getRunMode() {    
    return runMode;
  }     
    
  public boolean isTraceNo() {
    return traceMode == TRACE_NO;
  }  
             
  public boolean isTraceWait() {
    return traceMode == TRACE_WAIT;
  }  
    
  public boolean isTraceAll() {
    return traceMode == TRACE_ALL;
  }
    
  public boolean isRunIt() {
    return runMode == MODE_RUN_IT;
  }  
             
  public boolean isStreamAll() {
    return runMode == MODE_STREAM_ALL;
  }  
    
  public boolean isDOMAll() {
    return runMode == MODE_DOM_ALL;
  }   
  
  public void setBaseDir(File baseDir) {    
    this.baseDir = baseDir;
  }             
  
  public URI getBaseURI(URI baseURI) {    
    if (baseDir != null) {
      return baseDir.toURI();
    }
    return baseURI;
  }      
        
  public URI resolveURI(URI baseURI, String href) {    
    if (baseDir != null && (baseURI.getScheme() == null || baseURI.getScheme().equals("file"))) {
      if (href != null) {
        return baseDir.toURI().resolve(URIUtils.encode(href));
      }
      return baseDir.toURI();
    } 
    if (href != null) {
      return baseURI.resolve(URIUtils.encode(href));
    }
    return baseURI;    
  }      
  
  public void start(XProcRuntime runtime) {
    tracer.setRuntime(runtime);    
    waiter.setRuntime(runtime);    
    if (isStreamAll()) {
      tracer.debug("Running in stream-all mode", true);
    } else if (isDOMAll()) {
      tracer.debug("Running in dom-all mode", true);
    } else {    
      tracer.debug("Running in run-it mode", true);
    }  
  }    
  
  public void startSpying() { spy.start(!isDOMAll()); }      
  
  public void stopSpying() { spy.stop(); }         
  
}
