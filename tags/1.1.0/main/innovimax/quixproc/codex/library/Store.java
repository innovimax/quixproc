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
package innovimax.quixproc.codex.library;


import innovimax.quixproc.codex.util.EventReader;
import innovimax.quixproc.datamodel.QuixEvent;
import innovimax.quixproc.util.ReadingException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.URI;

import net.sf.saxon.s9api.QName;

import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.library.DefaultStep;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.TreeWriter;

public class Store extends DefaultStep {
    private static final QName _href = new QName("href");
    private static final QName _encoding = new QName("encoding");
    private static final QName c_encoding = new QName("c", XProcConstants.NS_XPROC_STEP, "encoding");
    private static final QName cx_decode = new QName("cx", XProcConstants.NS_CALABASH_EX, "decode");

    private ReadablePipe source = null;
    private WritablePipe result = null;     
    private File output = null;
    private PrintWriter out = null;   
    private boolean first = true;
    private boolean open = false;     
    
    // innovimax: statistics  
    private static long totalFileSize = 0;      
    
    public Store(XProcRuntime runtime, XAtomicStep step) {    
        super(runtime,step);
    }
    
    public void setInput(String port, ReadablePipe pipe) {
        source = pipe;
    }

    public void setOutput(String port, WritablePipe pipe) {
        result = pipe;
    }
    
    public void reset() {        
        // nop
    }      

    public void gorun() {   
      
        if (runtime.getSafeMode()) {
            throw XProcException.dynamicError(21);
        }

        
        URI href = null;
        RuntimeValue hrefOpt = getOption(_href);           
        
        if (hrefOpt != null) {
            href = runtime.getQConfig().resolveURI(hrefOpt.getBaseURI(), hrefOpt.getString()); 
        } else {
            href = runtime.getQConfig().getBaseURI(step.getNode().getBaseURI());  
        }                        
                
        output = new File(href);
        runtime.getTracer().debug(step,null,-1,source,null,"  STORE > WRITE XML FILE "+output.getAbsolutePath());          
                                    
        try {                   
            // Innovimax: must do a better implementation !
            out = new PrintWriter(new BufferedWriter(new FileWriter(output)));          
            
            EventReader reader = new EventReader(source.readAsStream(stepContext), null);
            while (reader.hasEvent()) {              
               processEvent(reader.nextEvent(), reader.pipe());
               if (reader.hasEvent()) {
                 processEvent(reader.nextEvent(), reader.pipe());                       
               }
               Thread.yield();          
           }                                              
        } catch (Exception e) {
            throw new XProcException(e);      
        }          

        TreeWriter tree = new TreeWriter(runtime);
        tree.startDocument(step.getNode().getBaseURI());
        tree.addStartElement(XProcConstants.c_result);
        tree.startContent();
        tree.addText(href.toString());
        tree.addEndElement();
        tree.endDocument();           
        result.write(stepContext, tree.getResult());     
    }
    
    /** 	  
     * reading handler interface
     */          
    
    private void processEvent(QuixEvent event, ReadablePipe in) throws ReadingException { 
        try {       
            switch(event.getType()) {                
              case START_SEQUENCE:                    
                runtime.getTracer().debug(step,null,-1,null,null,"  STORE > START SEQUENCE");        
                break;
                case START_DOCUMENT:                    
                    runtime.getTracer().debug(step,null,-1,null,null,"  STORE > START DOCUMENT");        
                    break;
                case END_DOCUMENT:                    
                    runtime.getTracer().debug(step,null,-1,null,null,"  STORE > END DOCUMENT"); 
                    out.flush();
                    out.close();
                    // innovimax: statistics                      
                    totalFileSize += output.length();                    
                    break;
                case END_SEQUENCE:
                  runtime.getTracer().debug(step,null,-1,null,null,"  STORE > END SEQUENCE"); 
                  break;
                case START_ELEMENT:                  
                    if (first) {                      
                        //out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                        //out.println();                         
                        first = false;
                    } else if (open) {
                        out.write(">");
                        open = false;
                    }                       
                    out.write("<"+event.asStartElement().getFullName()); 
                    open = true;                    
                    break;
                case END_ELEMENT:            
                   if (open) {
                        out.write(">");
                        open = false;
                    }                         
                    out.write("</"+event.asEndElement().getFullName()+">");
                    //out.println();            
                    break;
                case NAMESPACE :
                    out.write(" xmlns");
                    String prefix = event.asNamespace().getPrefix();
                    if (prefix.length() > 0) {
                      out.write(":"+prefix);
                    }
                    out.write("=\""+event.asNamespace().getURI());
                    break;
                case ATTRIBUTE:                  
                    out.write(" ");
                    out.write(event.asAttribute().getFullName()+"=\""+event.asAttribute().getValue()+"\"");
                    break;
                case TEXT:       
                    if (open) {
                        out.write(">");
                        open = false;
                    }
                    out.write(event.asText().getData());
                    break;
                case PI:                         
                    break;
                case COMMENT:                              
                    break;
            }
        } catch (Exception e) {            
            throw new ReadingException(e); 
        }          
    }        
    
    // innovimax: statistics  
    public static long getTotalFileSize() { return totalFileSize; }       
    public static void resetTotalFileSize() { totalFileSize = 0; }       
}

