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

package com.xmlcalabash.io;

import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.SaxonApiException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.model.Step;
import com.xmlcalabash.util.XPointer;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.logging.Logger;
import java.util.Vector;


import innovimax.quixproc.codex.util.PipedDocument;
import innovimax.quixproc.codex.util.StepContext;
import innovimax.quixproc.codex.util.shared.ChannelReader;
import innovimax.quixproc.util.shared.ChannelInit;
import innovimax.quixproc.util.shared.ChannelPosition;

public class ReadableDocument implements ReadablePipe {
    private DocumentSequence documents = null;
    private String base = null;
    private String uri = null;
    private XProcRuntime runtime = null;
    private XdmNode node = null;
    private Step reader = null;
    private Pattern pattern = null;

    public ReadableDocument(XProcRuntime runtime) {
        // This is an empty document sequence (p:empty)
        this.runtime = runtime;
        documents = new DocumentSequence(runtime);
    }

    /** Creates a new instance of ReadableDocument */
    public ReadableDocument(XProcRuntime runtime, XdmNode node, String uri, String base, String mask) {
        this.runtime = runtime;
        this.node = node;
        this.uri = uri;
        this.base = base;

        if (mask != null) {
            pattern = Pattern.compile(mask);
        }

        documents = new DocumentSequence(runtime);
    }

    public void canReadSequence(boolean sequence) {
        // nop; always false
    }
    
    public DocumentSequence documents() {
        return documents;
    }    
    
    private class RegexFileFilter implements FileFilter {
        Pattern pattern = null;

        public RegexFileFilter(Pattern p) {
            this.pattern = p;
        }

        public boolean accept(File pathname) {
            Matcher matcher = pattern.matcher(pathname.getName());
            return matcher.matches();
        }
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
            readDoc(stepContext);                        
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
        initialize(stepContext);
        return documents.closed(stepContext.curChannel);
    }        

    // Innovimax: new function
    public boolean moreDocuments(StepContext stepContext) {        
        initialize(stepContext);
        return !closed(stepContext) || c_pos.get(stepContext.curChannel) < documents.size(stepContext.curChannel);
    }    

    // Innovimax: new function
    public int documentCount(StepContext stepContext) {        
        initialize(stepContext);
        return documents.size(stepContext.curChannel);
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
        runtime.getTracer().debug(null,stepContext,-1,this,null,"    DOCU > TRY READING...");             
        initialize(stepContext);
         
        PipedDocument doc = null;                
        if (moreDocuments(stepContext)) {
            doc = documents.get(stepContext.curChannel, c_pos.get(stepContext.curChannel));
            c_pos.increment(stepContext.curChannel);
            runtime.getTracer().debug(null,stepContext,-1,this,doc,"    DOCU > READ ");            
        } else {
            runtime.getTracer().debug(null,stepContext,-1,this,null,"    DOCU > NO MORE DOCUMENT");            
        }        
           
        if (reader != null) {
            runtime.finest(null, reader.getNode(), reader.getName() + " select read '" + (doc == null ? "null" : doc.getBaseURI()) + "' from " + this);
        }

        return doc;
    }
    
    // Innovimax: new function
    private void readDoc(StepContext stepContext) {        
        runtime.getTracer().debug(null,stepContext,-1,this,null,"    DOCU > LOADING...");          
        
        XdmNode doc;

        c_init.close(stepContext.curChannel);        
        if (uri != null) {            
            try {
                // What if this is a directory?
                String fn = uri;
                if (fn.startsWith("file:")) {
                    fn = fn.substring(5);
                    if (fn.startsWith("///")) {
                        fn = fn.substring(2);
                    }
                }

                File f = new File(fn);
                if (f.isDirectory()) {
                    if (pattern == null) {
                        pattern = Pattern.compile("^.*\\.xml$");
                    }
                    for (File file : f.listFiles(new RegexFileFilter(pattern))) {
                        doc = runtime.parse(file.getCanonicalPath(), base);                        
                        documents.newPipedDocument(stepContext.curChannel,  doc);
                    }
                } else {
                    doc = runtime.parse(uri, base);

                    if (fn.contains("#")) {
                        int pos = fn.indexOf("#");
                        String ptr = fn.substring(pos+1);

                        if (ptr.matches("^[\\w]+$")) {
                            ptr = "element(" + ptr + ")";
                        }

                        XPointer xptr = new XPointer(ptr);
                        Vector<XdmNode> nodes = xptr.selectNodes(runtime, doc, xptr.xpathEquivalent(), xptr.xpathNamespaces());

                        if (nodes.size() == 1) {
                            doc = nodes.get(0);
                        } else if (nodes.size() != 0) {
                            throw new XProcException(node, "XPointer matches more than one node!?");
                        }
                    }                    
                    documents.newPipedDocument(stepContext.curChannel, doc);                                                                                      
                    runtime.getTracer().debug(null,stepContext,-1,this,null,"    DOCU > LOADED");             
                }
            } catch (Exception except) {
                throw XProcException.dynamicError(11, node, except, "Could not read: " + uri);
            }            
        } else {
            documents.addChannel(stepContext.curChannel); 
            runtime.getTracer().debug(null,stepContext,-1,this,null,"    DOCU > NOTHING");                                    
        }
        
        // close documents        
        documents.close(stepContext.curChannel);                            
    }
    
    //*************************************************************************
    //*************************************************************************        
    //*************************************************************************
    // INNOVIMAX DEPRECATION
    //*************************************************************************
    //*************************************************************************
    //*************************************************************************  
/*
    private int pos = 0;
    private boolean readDoc = false;    
    
    public void resetReader() {
        pos = 0;
        // 6 Feb 2009: removed "readDoc = false;" because we don't want to re-read the document
        // if this happens in a loop. We just want to reset ourselves back to the beginning.
        // A readable document can only have a single doc, so it should be ok.
    }

    public void setReader(Step step) {
        reader = step;
    }

    public boolean moreDocuments() {
        if (!readDoc) {
            readDoc();
        }
        return pos < documents.size();
    }

    public boolean closed() {
        return true;
    }

    public int documentCount() {
        return documents.size();
    }    
    
    public XdmNode read() throws SaxonApiException {
        if (!readDoc) {
            readDoc();
        }

        XdmNode doc = documents.get(pos++);

        if (reader != null) {
            runtime.finest(null, reader.getNode(), reader.getName() + " select read '" + (doc == null ? "null" : doc.getBaseURI()) + "' from " + this);
        }

        return doc;
    }

    private void readDoc() {
        XdmNode doc;

        readDoc = true;
        if (uri != null) {
            try {
                // What if this is a directory?
                String fn = uri;
                if (fn.startsWith("file:")) {
                    fn = fn.substring(5);
                    if (fn.startsWith("///")) {
                        fn = fn.substring(2);
                    }
                }

                File f = new File(fn);
                if (f.isDirectory()) {
                    if (pattern == null) {
                        pattern = Pattern.compile("^.*\\.xml$");
                    }
                    for (File file : f.listFiles(new RegexFileFilter(pattern))) {
                        doc = runtime.parse(file.getCanonicalPath(), base);
                        documents.add(doc);
                    }
                } else {
                    doc = runtime.parse(uri, base);

                    if (fn.contains("#")) {
                        int pos = fn.indexOf("#");
                        String ptr = fn.substring(pos+1);

                        if (ptr.matches("^[\\w]+$")) {
                            ptr = "element(" + ptr + ")";
                        }

                        XPointer xptr = new XPointer(ptr);
                        Vector<XdmNode> nodes = xptr.selectNodes(runtime, doc, xptr.xpathEquivalent(), xptr.xpathNamespaces());

                        if (nodes.size() == 1) {
                            doc = nodes.get(0);
                        } else if (nodes.size() != 0) {
                            throw new XProcException(node, "XPointer matches more than one node!?");
                        }
                    }

                    documents.add(doc);
                }
            } catch (Exception except) {
                throw XProcException.dynamicError(11, node, except, "Could not read: " + uri);
            }
        }
    }
*/
}
