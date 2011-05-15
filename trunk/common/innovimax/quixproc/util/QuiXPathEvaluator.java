/*
QuiXProc: efficient evaluation of XProc Pipelines.
Copyright (C) 2011 Innovimax
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
package innovimax.quixproc.util;

import java.util.Map;

import javax.xml.namespace.QName;
import innovimax.quixproc.datamodel.*;

public class QuiXPathEvaluator implements IEvalProcess {   
  /*
   *  properties
   */ 
     
  private String xpath = null;    
  private Map<QName,QuixValue> variables = null;   // Map<QName,Value> 
  private Map<String,String> namespaces = null;  // Map<Prefix,URI>    
  private boolean checked = false;      
  private QuixValue value = new QuixValue("");  
  private boolean computed = false;    
  
  /*
   *  constructor
   */  
    
  public QuiXPathEvaluator(String xpath, Map<QName,QuixValue> variables, Map<String,String> namespaces) {         
    this.xpath = xpath.trim();        
    this.namespaces = namespaces;
    this.variables = variables;
  }    
     
  public void pushEvent(QuixEvent event) throws EvalException {  
    checkXPath();
    if (!computed) {      
        switch(event.getType()) {
            case START_DOCUMENT: 
                break;
            case END_DOCUMENT:
          computed = true;
                break;
            case START_ELEMENT:                           
                break;
            case END_ELEMENT:                             
                break;
            case ATTRIBUTE:     
                // Innovimax: for Innovimax debuging (to remove)
                if (xpath.contains(event.asAttribute().getLocalName())) {
                    value = new QuixValue(event.asAttribute().getValue());
                    computed = true;
                }
                break;
            case TEXT:             
                break;
            case PI:                           
                break;
            case COMMENT:          
                break;
        }
    }
  }
    
  public boolean hasValue() {
    return computed;
  }    
    
  public QuixValue getValue() { return value; }    
  
  public void reset()    
  {
    checked = false;
    value = new QuixValue("");
    computed = false;
  }  
  
  private void checkXPath() throws EvalException
  {       
    if (checked) { return; }    
    checked = true;
    // Innovimax: for Innovimax debuging (to remove)    
    if (xpath.startsWith("$")) {            
      String name = xpath.substring(1);      
      if (variables.containsKey(new QName(name))) {
        value = variables.get(new QName(name));        
        computed = true;
      } else {
        throw new EvalException("Undeclared variable in XPath expression: $"+name);
      }
    }    
  }    
    
}

