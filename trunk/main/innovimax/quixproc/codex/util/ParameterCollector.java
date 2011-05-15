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

import java.util.List;

import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.runtime.XPipeline;

public class ParameterCollector implements Runnable {        
    private XProcRuntime runtime = null;  
    private XPipeline pipeline = null;      
    private List<DocumentCollector> inCollectors = null;                   
    private boolean running = true;    
    
    public ParameterCollector(XProcRuntime runtime, XPipeline pipeline, List<DocumentCollector> inCollectors) {
        this.runtime = runtime;
        this.pipeline = pipeline;
        this.inCollectors = inCollectors;        
    }
    
    public void run() {                    
        try {             
           startProcess();            
           while (collectorRunning(inCollectors)) {            
               Thread.yield();
           }    
           pipeline.collectParameters();
           endProcess();            
           running = false;                    
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {          
            throw new RuntimeException(e);
        }           
    }
    
    public boolean isRunning() {
        return running;
    } 
    
    private boolean collectorRunning(List<DocumentCollector> collectors) {
        for (DocumentCollector collector : collectors) {
            if (collector.isRunning()) {
                return true;
            }
        }       
        return false; 
    }      
    
    /** 	  
     * collect handler interface
     */ 
     
    private void startProcess() {
        runtime.getTracer().debug(pipeline,null,-1,null,null,"    COLLECT-PARAM > START THREAD");    
    }
    
    private void endProcess() {
        runtime.getTracer().debug(pipeline,null,-1,null,null,"    COLLECT-PARAM > END THREAD");
    }        
    
}