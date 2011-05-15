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

import java.util.Hashtable;

import net.sf.saxon.s9api.QName;

import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.model.Step;
import com.xmlcalabash.model.Variable;
import com.xmlcalabash.runtime.XAtomicStep;

public class VariablesCalculator implements Runnable {      
    class Counter {
        private int count = 0;        
        void setCount(int count) { this.count = count; }
        int getCount() { return count; }
        void decrease() { count--; }
    }
  
    private XProcRuntime runtime = null;
    private XAtomicStep astep = null;    
    private Step step = null;       
    private Hashtable<QName, RuntimeValue> inScopeOptions = null;    
    private int channel = 0;
    private Counter counter = null;   
    private QName name = null;   
    private Variable variable = null;
    private ErrorHandler errorHandler = null;
    
    public VariablesCalculator(XProcRuntime runtime, XAtomicStep astep, Step step, Hashtable<QName,RuntimeValue> inScopeOptions) { 
        this(runtime, astep, step, inScopeOptions, null, null, null);
    }    
    
    public VariablesCalculator(XProcRuntime runtime, XAtomicStep astep, Step step, Hashtable<QName,RuntimeValue> inScopeOptions, Counter counter, QName name, Variable variable) {               
        this.runtime = runtime;
        this.astep = astep;                
        this.step = step;           
        this.inScopeOptions = inScopeOptions;                  
        this.channel = astep.getContext().curChannel;
        this.counter = counter;                     
        this.name = name;
        this.variable = variable;        
    }    
   
    public void exec() { exec(null); }
      
    public void exec(ErrorHandler errorHandler) {               
        this.errorHandler = errorHandler;         
        counter = new Counter();
        counter.setCount(step.getVariables().size());
        for (Variable variable : step.getVariables()) {            
            name = variable.getName();                 
            String select = variable.getSelect();
            if (select == null) {                
                assign(new RuntimeValue());
            } else {                
                if (VariableEvaluator.isDynamic(select)) {                                           
                    //VariablesCalculator c = new VariablesCalculator(runtime, astep, step, inScopeOptions, counter, name, variable);
                    //Thread t = new Thread(c);             
                    //runtime.getTracer().debug(astep,null,-1,null,null,"  STEP > RUN EVAL THREAD '"+name+"'");         
                    //t.start();
                    assign(astep.computeVariable(variable));
                } else {                                        
                    assign(astep.computeVariable(variable));
                }
            }
        }                 
        runtime.getWaiter().initialize(astep,channel,null,null,"  STEP > WAITINGS VARIABLE EVAL..."); 
        while (counter.getCount() > 0) {            
            checkErrors();
            runtime.getWaiter().check();            
            Thread.yield();
        }        
        runtime.getTracer().debug(astep,null,-1,null,null,"  STEP > VARIABLES EVAL TERMINATED"); 
    }
    
    public void run() {                    
        try {       
            runtime.getTracer().debug(astep,null,-1,null,null,"    EVAL > START THREAD '"+name+"'");       
            assign(astep.computeVariable(variable));
            runtime.getTracer().debug(astep,null,-1,null,null,"    EVAL > END THREAD '"+name+"'");                         
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {          
            throw new RuntimeException(e);
        }                
    }        
    
    private void assign(RuntimeValue value) {        
        inScopeOptions.put(name, value);                 
        counter.decrease();       
    }        
        
    private void checkErrors() {
      if (errorHandler != null) errorHandler.checkError();
    }       
    
}

