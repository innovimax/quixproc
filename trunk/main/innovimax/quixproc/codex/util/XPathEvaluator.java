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
import innovimax.quixproc.util.EvalException;
import innovimax.quixproc.util.EvalProcess;
import innovimax.quixproc.util.QuiXPathEvaluator;

import java.util.Map;

import javax.xml.namespace.QName;

import com.xmlcalabash.core.XProcException;

public class XPathEvaluator {
    private EventReader reader = null;     
    private EvalProcess process = null;        
    
    public XPathEvaluator(EventReader reader, String xpath, Map<QName,QuixValue> variables, Map<String,String> namespaces) { 
        this.reader = reader;     
        process = new QuiXPathEvaluator(xpath,variables,namespaces);        
    }    
   
    public String exec() {          
        try {                                                   
            while (reader.hasEvent()) {
              process.pushEvent(reader.nextEvent());
              if (process.hasValue()) {                                
                return process.getValue().getString();
              }   
              Thread.yield();
            }  
            while (!process.hasValue()) {   
              Thread.yield();
            }                                 
            return process.getValue().getString();
        } 
        catch (EvalException e) {
            if (e.getMessage().contains("Undeclared variable")) {
                throw XProcException.dynamicError(23, e.getMessage());
            } 
            throw new XProcException(e);
        }
        catch (Exception e) { throw new XProcException(e); }
    }
    
}

