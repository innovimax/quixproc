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

import java.util.HashSet;
import java.util.Hashtable;

import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmNode;

import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcStep;
import com.xmlcalabash.model.DeclareStep;
import com.xmlcalabash.model.Option;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.model.Step;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.runtime.XPipeline;
import com.xmlcalabash.util.TypeUtils;

public class OptionsCalculator implements Runnable {  
    private final static int TYPE_ATOMIC = 1;
    private final static int TYPE_OTHERS = 2;    
    
    class Counter {
        private int count = 0;        
        void setCount(int count) { this.count = count; }
        int getCount() { return count; }
        void decrease() { count--; }
    }
  
    private int type = TYPE_ATOMIC;
    private XProcRuntime runtime = null;
    private XAtomicStep astep = null;
    private XProcStep xstep = null;
    private Step step = null;      
    private DeclareStep decl = null;    
    private Hashtable<QName, RuntimeValue> inScopeOptions = null;
    private Hashtable<QName, RuntimeValue> optionsPassedIn = null;    
    private XPipeline calledPipeline = null;
    private HashSet<QName> calledOpts = null;    
    private int channel = 0;
    private Counter counter = null;   
    private QName name = null;   
    private Option option = null;
    private ErrorHandler errorHandler = null;
    
    public OptionsCalculator(XProcRuntime runtime, XAtomicStep astep, XProcStep xstep, Step step, Hashtable<QName,RuntimeValue> inScopeOptions) { 
        this(TYPE_ATOMIC, runtime, astep, xstep, step, inScopeOptions, null, null, null, null, null, null);        
    }
    
    public OptionsCalculator(XProcRuntime runtime, XAtomicStep astep, Step step, Hashtable<QName,RuntimeValue> inScopeOptions) { 
        this(TYPE_OTHERS, runtime, astep, null, step, inScopeOptions, null, null, null, null, null, null);        
    }    
    
    public OptionsCalculator(XProcRuntime runtime, XAtomicStep astep, Step step, Hashtable<QName,RuntimeValue> inScopeOptions, Hashtable<QName,RuntimeValue> optionsPassedIn) { 
        this(TYPE_OTHERS, runtime, astep, null, step, inScopeOptions, optionsPassedIn, null, null, null, null, null);        
    }      
    
    public OptionsCalculator(XProcRuntime runtime, XAtomicStep astep, Step step, Hashtable<QName,RuntimeValue> inScopeOptions, XPipeline calledPipeline, HashSet<QName> calledOpts) { 
        this(TYPE_OTHERS, runtime, astep, null, step, inScopeOptions, null, calledPipeline, calledOpts, null, null, null);        
    }      
      
    public OptionsCalculator(int type, XProcRuntime runtime, XAtomicStep astep, XProcStep xstep, Step step, Hashtable<QName,RuntimeValue> inScopeOptions, Hashtable<QName,RuntimeValue> optionsPassedIn, XPipeline calledPipeline, HashSet<QName> calledOpts, Counter counter, QName name, Option option) { 
        this.type = type;
        this.runtime = runtime;
        this.astep = astep;        
        this.xstep = xstep;              
        this.step = step;              
        this.decl = step.getDeclaration();      
        this.inScopeOptions = inScopeOptions;  
        this.optionsPassedIn = optionsPassedIn;                  
        this.calledPipeline = calledPipeline;
        this.calledOpts = calledOpts;
        this.channel = astep.getContext().curChannel;    
        this.counter = counter;                     
        this.name = name;
        this.option = option;        
    }    
   
    public void exec() { exec(null); }
      
    public void exec(ErrorHandler errorHandler) {       
        this.errorHandler = errorHandler;
        counter = new Counter();
        counter.setCount(step.getOptions().size());
        for (QName name : step.getOptions()) {
            this.name = name;          
            option = step.getOption(name);                        
            if (optionsPassedIn != null && optionsPassedIn.containsKey(name)) {            
                assign(optionsPassedIn.get(name));
            } else {   
                String select = option.getSelect();             
                if (option.getRequired() && select == null) {
                    throw XProcException.staticError(18, option.getNode(), "No value provided for required option \"" + option.getName() + "\"");
                } 
                if (select == null) {
                    assign(new RuntimeValue());
                } else {     
                    if (VariableEvaluator.isDynamic(select)) {                      
                        /*OptionsCalculator c = new OptionsCalculator(type, runtime, astep, xstep, step, inScopeOptions, optionsPassedIn, calledPipeline, calledOpts, counter, name, option);
                        Thread t = new Thread(c);             
                        runtime.getTracer().debug(astep,null,-1,null,null,"  STEP > RUN EVAL THREAD '"+name+"'");         
                        t.start();*/
                        assign(astep.computeVariable(option));
                    } else {
                        assign(astep.computeVariable(option));
                    }
                }
            }
        }              
        Waiting waiter = runtime.newWaiterInstance(astep,channel,null,null,"  STEP > WAITING OPTIONS EVAL..."); 
        while (counter.getCount() > 0) {
            checkErrors();
            waiter.check();
            Thread.yield();
        }
        runtime.getTracer().debug(astep,null,-1,null,null,"  STEP > OPTIONS EVAL TERMINATED");                 
    }
    
    public void run() {    
        try {       
            runtime.getTracer().debug(astep,null,-1,null,null,"    EVAL > START THREAD '"+name+"'");                   
            assign(astep.computeVariable(option));            
            runtime.getTracer().debug(astep,null,-1,null,null,"    EVAL > END THREAD '"+name+"'");                               
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {          
            throw new RuntimeException(e);
        }                
    }        
    
    private void assign(RuntimeValue value) {                
        switch(type) {
            case TYPE_ATOMIC:
                Option optionDecl = decl.getOption(name);
                String typeName = optionDecl.getType();
                XdmNode declNode = optionDecl.getNode();
                if (typeName != null && declNode != null) {
                    if (typeName.contains("|")) {
                        TypeUtils.checkLiteral(value.getString(), typeName);
                    } else {
                        QName type = new QName(typeName, declNode);
                        TypeUtils.checkType(runtime, value.getString(),type,option.getNode());
                    }
                }
                xstep.setOption(name, value);
                break;
            case TYPE_OTHERS:            
                astep.setOption(name, value);
                if (calledPipeline != null && calledOpts.contains(name)) {
                    calledPipeline.passOption(name, value);
                }                
                break;
        }                                        
        inScopeOptions.put(name, value);         
        counter.decrease();       
    }        
        
    private void checkErrors() {
      if (errorHandler != null) errorHandler.checkError();
    }           
    
}

