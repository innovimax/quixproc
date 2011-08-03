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
package com.xmlcalabash.runtime;

import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.DocumentSequence;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.util.S9apiUtils;
import com.xmlcalabash.model.NamespaceBinding;
import com.xmlcalabash.model.Step;
import com.xmlcalabash.model.RuntimeValue;

import java.util.Iterator;
import java.util.Hashtable;
import java.util.logging.Logger;

import net.sf.saxon.s9api.*;
import net.sf.saxon.sxpath.IndependentContext;

import innovimax.quixproc.codex.util.Waiting;

import innovimax.quixproc.codex.util.DocumentSelector;
import innovimax.quixproc.codex.util.PipedDocument;
import innovimax.quixproc.codex.util.StepContext;
import innovimax.quixproc.codex.util.shared.ChannelReader;
import innovimax.quixproc.util.shared.ChannelInit;
import innovimax.quixproc.util.shared.ChannelPosition;

public class XSelect implements ReadablePipe {    
    private ReadablePipe source = null;
    private String select = null;
    private XdmNode context = null;
    private DocumentSequence documents = null;
    private XPathSelector selector = null;
    private XProcRuntime runtime = null;    
    private Step reader = null;
    private XStep forStep = null;

    /** Creates a new instance of Select */
    public XSelect(XProcRuntime runtime, XStep forStep, ReadablePipe readFrom, String xpathExpr, XdmNode context) {
        source = readFrom;
        select = xpathExpr;
        this.runtime = runtime;
        this.context = context;
        documents = new DocumentSequence(runtime);
        this.forStep = forStep;
    }

    public void canReadSequence(boolean sequence) {
        // nop; always true
    }
    
    public DocumentSequence documents() {
        return documents;
    }

    public String toString() {
        return "xselect " + documents;
    }    
    
    // Innovimax: modified function
    //private void readSource() {    
    private void readSource(StepContext stepContext) {            
        // Innovimax : obsolete
        //initialized = true;       

        try {
            NamespaceBinding bindings = new NamespaceBinding(runtime,context);
            XPathCompiler xcomp = runtime.getProcessor().newXPathCompiler();
            IndependentContext icontext = (IndependentContext) xcomp.getUnderlyingStaticContext();

            Hashtable<QName, RuntimeValue> inScopeOptions = new Hashtable<QName, RuntimeValue> ();
            try {
                inScopeOptions = ((XCompoundStep) forStep).getInScopeOptions();
            } catch (ClassCastException cce) {
                // FIXME: Surely there's a better way to do this!!!
            }
            
            Hashtable<QName, RuntimeValue> boundOpts = new Hashtable<QName, RuntimeValue> ();
            for (QName name : inScopeOptions.keySet()) {
                RuntimeValue v = inScopeOptions.get(name);
                if (v.initialized()) {                    
                    boundOpts.put(name, v);
                }
            }

            for (QName varname : boundOpts.keySet()) {
                xcomp.declareVariable(varname);
            }

            for (String prefix : bindings.getNamespaceBindings().keySet()) {
                xcomp.declareNamespace(prefix, bindings.getNamespaceBindings().get(prefix));
            }

            XPathExecutable xexec = xcomp.compile(select);
            selector = xexec.load();

            for (QName varname : boundOpts.keySet()) {
                XdmAtomicValue avalue = boundOpts.get(varname).getUntypedAtomic(runtime);
                selector.setVariable(varname,avalue);
            }

        } catch (SaxonApiException sae) {
            if (S9apiUtils.xpathSyntaxError(sae)) {
                throw XProcException.dynamicError(23, context, "Invalid XPath expression: '" + select + "'.");
            } else {
                throw new XProcException(sae);
            }
        }

        // Innovimax: new interface        
        //while (source.moreDocuments()) {
        while (source.moreDocuments(stepContext)) {        
            // Ok, time to go looking for things to select from.
            try {
                // Innovimax: new interface
                //XdmNode doc = source.read();
                XdmNode doc = source.read(stepContext);

                if (reader != null) {
                    runtime.finest(null, reader.getNode(), reader.getName() + " select read '" + (doc == null ? "null" : doc.getBaseURI()) + "' from " + source);
                }

                selector.setContextItem(doc);

                Iterator iter = selector.iterator();
                while (iter.hasNext()) {
                    XdmItem item = (XdmItem) iter.next();
                    XdmNode node = null;
                    try {
                        node = (XdmNode) item;
                        if ((node.getNodeKind() != XdmNodeKind.ELEMENT)
                            && (node.getNodeKind() != XdmNodeKind.DOCUMENT)) {
                            throw XProcException.dynamicError(16);
                        }
                    } catch (ClassCastException cce) {
                        throw XProcException.dynamicError(16);
                    }
                    XdmDestination dest = new XdmDestination();
                    S9apiUtils.writeXdmValue(runtime, node, dest, node.getBaseURI());

                    XdmNode sdoc = dest.getXdmNode();

                    if (reader != null) {
                        runtime.finest(null, reader.getNode(), reader.getName() + " select wrote '" + (sdoc == null ? "null" : sdoc.getBaseURI()) + "' to " + documents);
                    }
                                        
                    // Innovimax: new interface                    
                    documents.newPipedDocument(stepContext.curChannel, sdoc);

                }
            } catch (SaxonApiException sae) {
                throw new XProcException(sae);
            }
        }
        
        // Innovimax: close channel        
        documents.close(stepContext.curChannel);        
    }
    
    //*************************************************************************
    //*************************************************************************        
    //*************************************************************************
    // INNOVIMAX IMPLEMENTATION
    //*************************************************************************
    //*************************************************************************
    //*************************************************************************

    private ChannelInit c_init = new ChannelInit(); // Innovimax: new property
    private ChannelPosition c_pos = new ChannelPosition();  // Innovimax: new property    
    private ChannelReader c_reader = new ChannelReader();  // Innovimax: new property      
    
    // Innovimax: new function
    public void initialize(StepContext stepContext) {        
        if (!c_init.done(stepContext.curChannel)) {
            readStreamSource(stepContext);
        }   
    }      
    
    // Innovimax: new function
    public void resetReader(StepContext stepContext) {                
        c_init.reset(stepContext.curChannel);
        c_pos.reset(stepContext.curChannel);
        source.resetReader(stepContext);
        documents.reset(stepContext.curChannel);   
    }       
    
    // Innovimax: new function
    public void setReader(StepContext stepContext, Step step) {
        c_reader.put(stepContext.curChannel, step);
    }      
    
    // Innovimax: new function
    public Step getReader(StepContext stepContext) {
        return c_reader.get(stepContext.curChannel);
    }
    
    // Innovimax: new function
    public boolean closed(StepContext stepContext) {
        StepContext newContext = documents.checkChannel(stepContext);
        initialize(newContext);
        return documents.closed(newContext.curChannel);
    }        

    // Innovimax: new function
    public boolean moreDocuments(StepContext stepContext) {
        StepContext newContext = documents.checkChannel(stepContext);
        initialize(newContext);
        boolean waiting = true;      
        Waiting waiter = runtime.newWaiterInstance(null,newContext.curChannel,this,null,"    XSEL > WAITING CHANNEL IS CLOSED..."); 
        while (waiting) {           
            if (c_pos.get(stepContext.curChannel) < documents.size(newContext.curChannel)) return true;                                         
            if (closed(newContext)) return false;                                
            waiter.check();
            Thread.yield();          
        }        
        return false;      
    }

    // Innovimax: new function
    public int documentCount(StepContext stepContext) {
        StepContext newContext = documents.checkChannel(stepContext);
        initialize(newContext);
        return documents.size(newContext.curChannel);
    }

    // Innovimax: new function
    public XdmNode read(StepContext stepContext) throws SaxonApiException {         
        PipedDocument doc = readAsStream(stepContext, true);
        if (doc!=null) {
            return doc.getNode();
        }
        return null;
    } 
    
    // Innovimax: new function
    public PipedDocument readAsStream(StepContext stepContext) {         
        return readAsStream(stepContext, false) ;
    }
    public PipedDocument readAsStream(StepContext stepContext, boolean dom) {         
        StepContext newContext = documents.checkChannel(stepContext);
        runtime.getTracer().debug(null,newContext,-1,this,null,"    XSEL > TRY READING...");                
        initialize(newContext);
        
        PipedDocument doc = null;                
        if (moreDocuments(newContext)) {
            doc = documents.get(newContext.curChannel, c_pos.get(stepContext.curChannel));
            Waiting waiter = runtime.newWaiterInstance(null,newContext.curChannel,this,null,"    XSEL > WAITING FOR DOCUMENT..."); 
            while (doc==null) {                    
                waiter.check();
                Thread.yield();
                doc = documents.get(newContext.curChannel, c_pos.get(stepContext.curChannel));
            }            
            c_pos.increment(stepContext.curChannel);
            runtime.getTracer().debug(null,newContext,-1,this,doc,"    XSEL > READ ");            
        } else {
            runtime.getTracer().debug(null,newContext,-1,this,null,"    XSEL > NO MORE DOCUMENT");            
        }        
                
        if (reader != null) {
            runtime.finest(null, reader.getNode(), reader.getName() + " read '" + (doc == null ? "null" : doc.getBaseURI()) + "' from " + this);
        }

        return doc;
    }
    
    // Innovimax: new function
    private void readStreamSource(StepContext stepContext) {              
        c_init.close(stepContext.curChannel);
        
        if (forStep.isStreamed()) {
            Hashtable<String,String> nsBindings = new NamespaceBinding(runtime,context).getNamespaceBindings();
    
            Hashtable<QName, RuntimeValue> globals = new Hashtable<QName, RuntimeValue> ();                               
            try {
                Hashtable<QName, RuntimeValue> inScopeOptions = ((XCompoundStep) forStep).getInScopeOptions();
                for (QName name : inScopeOptions.keySet()) {
                    RuntimeValue v = inScopeOptions.get(name);
                    if (v.initialized()) {                    
                        globals.put(name, v);
                    }
                }             
            } catch (ClassCastException ignored) {}            
                      
            DocumentSelector selector = new DocumentSelector(runtime, stepContext, source, documents, select, nsBindings, globals);        
            runtime.getTracer().debug(null,stepContext,-1,this,null,"    XSEL > RUN SELECT THREAD");
            selector.exec();            
        } else {        
            readSource(stepContext);
        }
    }            
    
    //*************************************************************************
    //*************************************************************************        
    //*************************************************************************
    // INNOVIMAX DEPRECATION
    //*************************************************************************
    //*************************************************************************
    //*************************************************************************      
/*
    private Logger logger = Logger.getLogger(this.getClass().getName());
    private boolean initialized = false;
    private int docindex = 0;
    
    public void resetReader() {
        docindex = 0;
        source.resetReader(stepContext);
        documents.reset();
        initialized = false;
    }

    public boolean moreDocuments() {
        if (!initialized) {
            readSource();
        }
        return docindex < documents.size();
    }

    public boolean closed() {
        return true;
    }

    public int documentCount() {
        if (!initialized) {
            readSource();
        }
        return documents.size();
    }

    public void setReader(Step step) {
        reader = step;
    }

    public XdmNode read () throws SaxonApiException {
        if (!initialized) {
            readSource();
        }

        XdmNode doc = null;
        if (moreDocuments()) {
            doc = documents.get(docindex++);
        }

        if (reader != null) {
            runtime.finest(null, reader.getNode(), reader.getName() + " read '" + (doc == null ? "null" : doc.getBaseURI()) + "' from " + this);
        }

        return doc;
    }
*/
}
