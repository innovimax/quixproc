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

import java.net.URI;
import java.util.HashSet;
import java.util.Vector;

import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmDestination;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmNodeKind;
import net.sf.saxon.s9api.XdmValue;

import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.model.Step;
import com.xmlcalabash.util.S9apiUtils;
// Innovimax: new import  
// Innovimax: new import  
// Innovimax: new import  
// Innovimax: new import  
// Innovimax: new import  

/**
 *
 * @author ndw
 */
public class ReadableInline implements ReadablePipe {    
    private XProcRuntime runtime = null;
    private DocumentSequence documents = null;
    private boolean readSeqOk = false;    
    private Step reader = null;

    /** Creates a new instance of ReadableInline */
    // Innovimax: modified constructor     
    public ReadableInline(XProcRuntime runtime, Vector<XdmValue> nodes, HashSet<String> excludeNS) {
        this.runtime = runtime;
        documents = new DocumentSequence(runtime);
        
        // Innovimax: deprecated code
        //readInline();
        
        // Innovimax: properties required for stream mode
        this.nodes = nodes;
        this.excludeNS = excludeNS;            
    }
    
    public void canReadSequence(boolean sequence) {
        readSeqOk = sequence;
    }

    public boolean readSequence() {
        return readSeqOk;
    }
    
    public String toString() {
        return "readableinline " + documents;
    }
    
    //*************************************************************************
    //*************************************************************************        
    //*************************************************************************
    // INNOVIMAX IMPLEMENTATION
    //*************************************************************************
    //*************************************************************************
    //************************************************************************* 

    private XdmNode doc = null;  // Innovimax: new property
    private ChannelPosition c_pos = new ChannelPosition();  // Innovimax: new property
    private ChannelInit c_init = new ChannelInit(); // Innovimax: new property    
    private ChannelReader c_reader = new ChannelReader();  // Innovimax: new property  
    private Vector<XdmValue> nodes = null;  // Innovimax: new property 
    private HashSet<String> excludeNS = null;  // Innovimax: new property     
    
    // Innovimax: new function
    public void initialize(StepContext stepContext) {                    
      if (!c_init.done(stepContext.curChannel)) {
            readInline(stepContext);                        
        }    
    }
        
    // Innovimax: new function
    public void resetReader(StepContext stepContext) {
        c_pos.reset(stepContext.curChannel);        
        c_init.reset(stepContext.curChannel);        
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
        runtime.getTracer().debug(null,stepContext,-1,this,null,"    INLI > TRY READING..."); 
        initialize(stepContext);
                
        PipedDocument doc = null;                
        if (moreDocuments(stepContext)) {
            doc = documents.get(stepContext.curChannel, c_pos.get(stepContext.curChannel));
            c_pos.increment(stepContext.curChannel);
            runtime.getTracer().debug(null,stepContext,-1,this,doc,"    INLI > READ ");            
        } else {
            runtime.getTracer().debug(null,stepContext,-1,this,null,"    INLI > NO MORE DOCUMENT");            
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
    private void readInline(StepContext stepContext) { 
        runtime.getTracer().debug(null,stepContext,-1,this,null,"    INLI > LOADING...");           
        
        c_init.close(stepContext.curChannel);
        
        if (doc == null) {                  
            XdmDestination dest = new XdmDestination();
            XdmNode p_inline = null;
    
            if (nodes.size() > 0) {
                p_inline = ((XdmNode) nodes.get(0)).getParent();
            }
    
            // Find the document element so we can get the base URI
            XdmNode node = null;
            for (int pos = 0; pos < nodes.size() && node == null; pos++) {
                if (((XdmNode) nodes.get(pos)).getNodeKind() == XdmNodeKind.ELEMENT) {
                    node = (XdmNode) nodes.get(pos);
                }
            }
    
            if (node == null) {
                throw XProcException.dynamicError(1, p_inline, "Invalid inline content");
            }
    
            // If the document element of the inline document has a relative xml:base, then we have
            // to be careful *not* to resolve it now. Otherwise, it'll get resolved *twice* if someone
            // calls p:make-absolute-uris on it.
            URI baseURI = null;
            if (node.getAttributeValue(XProcConstants.xml_base) == null) {
                baseURI = node.getBaseURI();
            } else {
                baseURI = node.getParent().getBaseURI();
            }
    
            try {
                S9apiUtils.writeXdmValue(runtime, nodes, dest, baseURI);
                doc = dest.getXdmNode();    
                doc = S9apiUtils.removeNamespaces(runtime, doc, excludeNS, true);
                runtime.finest(null, null, "Instantiate a ReadableInline");             
            } catch (SaxonApiException sae) {
                throw new XProcException(sae);
            }        
        }
        
        documents.newPipedDocument(stepContext.curChannel, doc);
        
        // close documents
        documents.close(stepContext.curChannel);
        runtime.getTracer().debug(null,stepContext,-1,this,null,"    INLI > LOADED");           
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
    private int pos = 0;
    
    public void resetReader() {
        pos = 0;
    }
    
    public boolean moreDocuments() {
        return pos < documents.size();
    }

    public boolean closed() {
        return true;
    }

    public int documentCount() {
        return documents.size();
    }

    public DocumentSequence documents() {
        return documents;
    }

    public void setReader(Step step) {
        reader = step;
    }

    public XdmNode read() throws SaxonApiException {
        XdmNode doc = documents.get(pos++);

        if (reader != null) {
            runtime.finest(null, reader.getNode(), reader.getName() + " read '" + (doc == null ? "null" : doc.getBaseURI()) + "' from " + this);
        }
        
        return doc;
    }

    private void readInline() {         
        XdmDestination dest = new XdmDestination();
        XdmNode p_inline = null;

        if (nodes.size() > 0) {
            p_inline = ((XdmNode) nodes.get(pos)).getParent();
        }

        // Find the document element so we can get the base URI
        XdmNode node = null;
        for (int pos = 0; pos < nodes.size() && node == null; pos++) {
            if (((XdmNode) nodes.get(pos)).getNodeKind() == XdmNodeKind.ELEMENT) {
                node = (XdmNode) nodes.get(pos);
            }
        }

        if (node == null) {
            throw XProcException.dynamicError(1, p_inline, "Invalid inline content");
        }

        // If the document element of the inline document has a relative xml:base, then we have
        // to be careful *not* to resolve it now. Otherwise, it'll get resolved *twice* if someone
        // calls p:make-absolute-uris on it.
        URI baseURI = null;
        if (node.getAttributeValue(XProcConstants.xml_base) == null) {
            baseURI = node.getBaseURI();
        } else {
            baseURI = node.getParent().getBaseURI();
        }

        try {
            S9apiUtils.writeXdmValue(runtime, nodes, dest, baseURI);
            XdmNode doc = dest.getXdmNode();

            doc = S9apiUtils.removeNamespaces(runtime, doc, excludeNS, true);
            runtime.finest(null, null, "Instantiate a ReadableInline");
            documents.add(doc);
        } catch (SaxonApiException sae) {
            throw new XProcException(sae);
        }
    }
*/
}
