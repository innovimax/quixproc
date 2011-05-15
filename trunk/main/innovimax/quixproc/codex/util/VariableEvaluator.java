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

import innovimax.quixproc.datamodel.QuixValue;
import innovimax.quixproc.util.SpyHandler;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Vector;

import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmItem;

import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.model.ComputableValue;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.runtime.XAtomicStep;

public class VariableEvaluator implements SpyHandler {
    private XProcRuntime runtime = null;  
    private XAtomicStep step = null;
    private PipedDocument doc = null;    
    private ComputableValue var = null;    
    private Hashtable<String,String> nsBindings = null;
    private Hashtable<QName,RuntimeValue> globals = null;      
    private int channel = 0;
    
    public VariableEvaluator(XProcRuntime runtime, XAtomicStep step, PipedDocument doc, ComputableValue var, Hashtable<String,String> nsBindings, Hashtable<QName,RuntimeValue> globals) {          
        this.runtime = runtime;
        this.step = step;
        this.doc = doc;    
        this.var = var;        
        this.nsBindings = nsBindings;
        this.globals = globals;
        this.channel = step.getContext().curChannel;
    }    
   
    public Vector<XdmItem> exec() { 
        Vector<XdmItem> results = new Vector<XdmItem> ();        
        String select = var.getSelect();         
        if (doc != null && !isDynamic(select)) {
           // consume unused reader
           doc.registerReader().close();            
        }
        select = XPathUtils.checkFunctions(step, select);                                
        if (!isDynamic(select)) {                      
            String value = getValue(select);       
            runtime.getTracer().debug(step,null,-1,null,null,"    EVAL > '"+var.getName()+"' STATIC COMPUTED TO ["+value+"] FOR SELECT ["+select+"]"); 
            results.add(new XdmAtomicValue(value));            
        } else {
            HashMap<javax.xml.namespace.QName, QuixValue> variables = new HashMap<javax.xml.namespace.QName, QuixValue> ();                
            for (QName qName : globals.keySet()) {
                RuntimeValue rv = globals.get(qName);
                if (rv.initialized()) {
                    variables.put(javaQName(qName), new QuixValue(rv.getString()));
                }
            }           
            EventReader evr = new EventReader(doc, this);                                    
            XPathEvaluator evaluator = new XPathEvaluator(evr, select, variables, nsBindings);
            String value = evaluator.exec();
            runtime.getTracer().debug(step,null,-1,null,null,"    EVAL > '"+var.getName()+"' DYNAMIC COMPUTED TO ["+value+"] FOR SELECT ["+select+"]"); 
            results.add(new XdmAtomicValue(value));
        }
        return results;
    }
    
    private javax.xml.namespace.QName javaQName(QName qName)
    {
      return new javax.xml.namespace.QName(qName.getNamespaceURI(),qName.getLocalName(),qName.getPrefix());
    }
    
    public static boolean isDynamic(String select) {        
        boolean dynamic = true;    
        // Innovimax : need to be completed...
        if (select.startsWith("'")) { dynamic = false; }
        return dynamic;
    }    
    
    private String getValue(String select) {        
        String value = select;
        if (value != null) {
            value = value.substring(1); 
            value = value.substring(0,value.length()-1);
        }
        return value;
    }
           
    
    /** 	  
     * spy handler interface
     */  
  
    public void spyStartDocument() {               
        runtime.getTracer().debug(step,null,channel,null,null,"    EVAL > START DOCUMENT '"+var.getName()+"'");     
    }     
    
    public void spyEndDocument() {        
        runtime.getTracer().debug(step,null,channel,null,null,"    EVAL > END DOCUMENT '"+var.getName()+"'");     
    }  
    
}

