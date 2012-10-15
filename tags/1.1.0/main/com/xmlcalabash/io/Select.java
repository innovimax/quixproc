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

package com.xmlcalabash.io;

import innovimax.quixproc.codex.util.PipedDocument;
import innovimax.quixproc.codex.util.StepContext;
import innovimax.quixproc.codex.util.shared.ChannelReader;
import innovimax.quixproc.util.shared.ChannelInit;
import innovimax.quixproc.util.shared.ChannelPosition;

import java.util.Iterator;

import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XPathCompiler;
import net.sf.saxon.s9api.XPathExecutable;
import net.sf.saxon.s9api.XPathSelector;
import net.sf.saxon.s9api.XdmDestination;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;

import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.model.NamespaceBinding;
import com.xmlcalabash.model.Step;
import com.xmlcalabash.util.S9apiUtils;
// Innovimax: new import  
// Innovimax: new import  
// Innovimax: new import  
// Innovimax: new import  
// Innovimax: new import  

/**
 *
 * Ideally, I'd like this code to perform the selections in a lazy fashion, but that's
 * hard because it has to be possible to answer questions about how many documents
 * will be returned. So for now, I'm just doing it all up front.
 *
 * @author ndw
 */
public class Select implements ReadablePipe {
    private ReadablePipe source = null;
    private String select = null;
    private XdmNode context = null;
    private DocumentSequence documents = null;
    private XPathSelector selector = null;
    private XProcRuntime runtime = null;    
    private Step reader = null;
    
    /** Creates a new instance of Select */
    public Select(XProcRuntime runtime, ReadablePipe readFrom, String xpathExpr, XdmNode xpathContext) {
        source = readFrom;
        select = xpathExpr;
        context = xpathContext;
        this.runtime = runtime;
        documents = new DocumentSequence(runtime);
    }

    public void canReadSequence(boolean sequence) {
        // nop; always true
    }

    public boolean readSequence() {
        return true;
    }
    
    //*************************************************************************
    //*************************************************************************        
    //*************************************************************************
    // INNOVIMAX IMPLEMENTATION
    //*************************************************************************
    //*************************************************************************
    //************************************************************************* 

    private ChannelPosition c_pos = new ChannelPosition();  // Innovimax: new property
    private ChannelInit c_init = new ChannelInit(); // Innovimax: new property    
    private ChannelReader c_reader = new ChannelReader();  // Innovimax: new property  
    
    // Innovimax: new function
    public void initialize(StepContext stepContext) {            
      if (!c_init.done(stepContext.curChannel)) {
            readSource(stepContext);                        
        }    
    }    
    
    // Innovimax: new function
    public void resetReader(StepContext stepContext) {    
        c_pos.reset(stepContext.curChannel);
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
        return !closed(newContext) || c_pos.get(stepContext.curChannel) < documents.size(newContext.curChannel);
    }

    // Innovimax: new function
    public int documentCount(StepContext stepContext) {
        StepContext newContext = documents.checkChannel(stepContext);
        initialize(newContext);
        return documents.size(newContext.curChannel);
    }    
    
    // Innovimax: new function
    public DocumentSequence documents(StepContext stepContext) {
        initialize(stepContext);
        return documents;
    }      

    // Innovimax: new function
    public XdmNode read(StepContext stepContext) throws SaxonApiException {         
        PipedDocument doc = readAsStream(stepContext);
        if (doc!=null) {
            return doc.getNode();
        }
        return null;
    } 
    
    // Innovimax: new function
    public PipedDocument readAsStream(StepContext stepContext) {
        StepContext newContext = documents.checkChannel(stepContext);
        runtime.getTracer().debug(null,newContext,-1,this,null,"    SELC > TRY READING...");  
        initialize(newContext);

        PipedDocument doc = null;                
        if (moreDocuments(newContext)) {
            doc = documents.get(newContext.curChannel, c_pos.get(newContext.curChannel));
            c_pos.increment(newContext.curChannel);
            runtime.getTracer().debug(null,newContext,-1,this,doc,"    SELC > READ ");            
        } else {
            runtime.getTracer().debug(null,newContext,-1,this,null,"    SELC > NO MORE DOCUMENT");            
        }  

        if (reader != null) {
            runtime.finest(null, reader.getNode(), reader.getName() + " read '" + (doc == null ? "null" : doc.getBaseURI()) + "' from " + this);
        }

        return doc;
    } 
    
    // Innovimax: new function
    public String sequenceInfos() {        
        return documents.toString();
    }          
    
    // Innovimax: new function
    private void readSource(StepContext stepContext) {
        runtime.getTracer().debug(null,stepContext,-1,this,null,"    SELC > LOADING...");  
              
        c_init.close(stepContext.curChannel);              
        try {
            NamespaceBinding bindings = new NamespaceBinding(runtime,context);
            XPathCompiler xcomp = runtime.getProcessor().newXPathCompiler();
            xcomp.setBaseURI(context.getBaseURI());
            for (String prefix : bindings.getNamespaceBindings().keySet()) {
                xcomp.declareNamespace(prefix, bindings.getNamespaceBindings().get(prefix));
            }

            XPathExecutable xexec = xcomp.compile(select);
            selector = xexec.load();
            // FIXME: Set getVariables
        } catch (SaxonApiException sae) {
            throw new XProcException(sae);
        }

        while (source.moreDocuments(stepContext)) {
            // Ok, time to go looking for things to select from.
            try {
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
                    } catch (ClassCastException cce) {
                        throw new XProcException (context, "Select matched non-node!?");
                    }
                    XdmDestination dest = new XdmDestination();
                    S9apiUtils.writeXdmValue(runtime, node, dest, node.getBaseURI());

                    XdmNode sdoc = dest.getXdmNode();

                    if (reader != null) {
                        runtime.finest(null, reader.getNode(), reader.getName() + " select wrote '" + (sdoc == null ? "null" : sdoc.getBaseURI()) + "' to " + documents);
                    }

                    documents.newPipedDocument(stepContext.curChannel, sdoc);
                }
            } catch (SaxonApiException sae) {
                throw new XProcException(sae);
            }
        }
        
        // Innovimax : close documents
        documents.close(stepContext.curChannel);
        runtime.getTracer().debug(null,stepContext,-1,this,null,"    SELC > LOADED");          
    }   
    
    //*************************************************************************
    //*************************************************************************        
    //*************************************************************************
    // INNOVIMAX DEPRECATION
    //*************************************************************************
    //*************************************************************************
    //*************************************************************************  
/*    
    private boolean initialized = false;
    private int docindex = 0;    
    private Logger logger = Logger.getLogger(this.getClass().getName());        
    
    private void readSource() {
        initialized = true;
        try {
            NamespaceBinding bindings = new NamespaceBinding(runtime,context);
            XPathCompiler xcomp = runtime.getProcessor().newXPathCompiler();
            xcomp.setBaseURI(context.getBaseURI());
            for (String prefix : bindings.getNamespaceBindings().keySet()) {
                xcomp.declareNamespace(prefix, bindings.getNamespaceBindings().get(prefix));
            }

            XPathExecutable xexec = xcomp.compile(select);
            selector = xexec.load();
            // FIXME: Set getVariables
        } catch (SaxonApiException sae) {
            throw new XProcException(sae);
        }

        while (source.moreDocuments(stepContext)) {
            // Ok, time to go looking for things to select from.
            try {
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
                    } catch (ClassCastException cce) {
                        throw new XProcException (context, "Select matched non-node!?");
                    }
                    XdmDestination dest = new XdmDestination();
                    S9apiUtils.writeXdmValue(runtime, node, dest, node.getBaseURI());

                    XdmNode sdoc = dest.getXdmNode();

                    if (reader != null) {
                        runtime.finest(null, reader.getNode(), reader.getName() + " select wrote '" + (sdoc == null ? "null" : sdoc.getBaseURI()) + "' to " + documents);
                    }

                    documents.add(sdoc);
                }
            } catch (SaxonApiException sae) {
                throw new XProcException(sae);
            }
        }
    }
    
    public void resetReader() {
        docindex = 0;
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

    public DocumentSequence documents() {
        return documents;
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
