/*
QuiXProc: efficient evaluation of XProc Pipelines.
Copyright (C) 2011-2012 Innovimax
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
package innovimax.quixproc.codex.functions;

import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.runtime.XForEach;

public class Functions {
      
    public static String check(XAtomicStep step, String select) {                 
        select = select.trim();        
        if (!select.startsWith("'")) {            
            select = checkIterationPosition(step, select);
            select = checkIterationSize(step, select);
            select = checkStepAvailable(select);
            select = checkValueAvailable(select);                                 
        }        
        return select;
    }

    private  static String checkIterationPosition(XAtomicStep step, String select) {    
        if (select.contains("p:iteration-position()")) {
             if (select.equals("p:iteration-position()")) {
                 select = "'"+step.getContext().iterationPos+"'";
             } else {                 
                 select = select.replace("p:iteration-position()",Integer.toString(step.getContext().iterationPos));                 
             }
         } 
         return select;
    }          
    
    private static String checkIterationSize(XAtomicStep step, String select) {    
        if (select.contains("p:iteration-size()")) {
             if (step instanceof XForEach) {
               throw new RuntimeException("p:iteration-size is not available in p:for-each");
             }
             if (select.equals("p:iteration-size()")) {
                 select = "'"+step.getContext().iterationSize+"'";
             } else {
                 select = select.replace("p:iteration-size()",Integer.toString(step.getContext().iterationSize));
             }
         } 
         return select;
    } 
    
    private static String checkStepAvailable(String select) {   
        if (select.contains("p:step-available()")) { 
          throw new RuntimeException("p:step-available is not implemented");
        }
        return select;
    } 
        
    private static String checkValueAvailable(String select) {    
        if (select.contains("p:value-available()")) { 
          throw new RuntimeException("p:value-available is not implemented");
        }
        return select;
    }   
        
}