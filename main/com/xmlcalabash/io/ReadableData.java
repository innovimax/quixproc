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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;

import org.json.JSONTokener;

import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.model.Step;
import com.xmlcalabash.util.Base64;
import com.xmlcalabash.util.HttpUtils;
import com.xmlcalabash.util.JSONtoXML;
import com.xmlcalabash.util.TreeWriter;
// Innovimax: new import
// Innovimax: new import
// Innovimax: new import
// Innovimax: new import
// Innovimax: new import

/**
 *
 * @author ndw
 */
public class ReadableData implements ReadablePipe {
    protected String contentType = null;    
    public static final QName _contentType = new QName("","content-type");
    public static final QName c_contentType = new QName("c",XProcConstants.NS_XPROC_STEP, "content-type");
    public static final QName _encoding = new QName("","encoding");
    public static final QName c_encoding = new QName("c",XProcConstants.NS_XPROC_STEP, "encoding");    
    private QName wrapper = null;
    private String uri = null;
    private String serverContentType = null;
    private XProcRuntime runtime = null;
    private DocumentSequence documents = null;
    private Step reader = null;

    /** Creates a new instance of ReadableDocument */    
    public ReadableData(XProcRuntime runtime, QName wrapper, String uri, String contentType) {
        this.runtime = runtime;
        this.uri = uri;
        this.wrapper = wrapper;
        this.contentType = contentType;
    }
    
    public void canReadSequence(boolean sequence) {
        // nop; always false
    }
    
    public boolean readSequence() {
        return false;
    }              
    
    // =======================================================================

    protected URI getDataUri(String uri) {
        try {
            return new URI(uri);
        } catch (URISyntaxException use) {
            throw new XProcException(use);
        }
    }

    protected InputStream getStream(URI uri) {
        try {
            URL url = uri.toURL();
            URLConnection connection = url.openConnection();
            serverContentType = connection.getContentType();
            return connection.getInputStream();
        } catch (IOException ioe) {
            throw new XProcException(XProcConstants.dynamicError(29), ioe);
        }
    }

    protected String getContentType() {
        return serverContentType;
    }

    // =======================================================================

    private boolean isText(String contentType, String charset) {
        return ("application/xml".equals(contentType)
                || contentType.endsWith("+xml")
                || contentType.startsWith("text/")
                || "utf-8".equals(charset));
    }

    private String parseContentType(String contentType) {
        if (contentType == null) {
            return null;
        }

        int pos = contentType.indexOf(";");
        if (pos > 0) {
            String type = contentType.substring(0,pos).trim();
            return type;
        } else {
            return contentType;
        }
    }

    private String parseCharset(String contentType) {
        String charset = HttpUtils.getCharset(contentType);

        if (charset != null) {
            return charset.toLowerCase();
        }

        return null;
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
    
    // Innovimax: new function
    public void initialize(StepContext stepContext) {                    
      if (!c_init.done(stepContext.curChannel)) {
            readData(stepContext);                        
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
        runtime.getTracer().debug(null,stepContext,-1,this,null,"    DATA > TRY READING...");             
        initialize(stepContext);
                
        PipedDocument doc = null;                
        if (moreDocuments(stepContext)) {
            doc = documents.get(stepContext.curChannel, c_pos.get(stepContext.curChannel));
            c_pos.increment(stepContext.curChannel);
            runtime.getTracer().debug(null,stepContext,-1,this,doc,"    DATA > READ ");            
        } else {
            runtime.getTracer().debug(null,stepContext,-1,this,null,"    DATA > NO MORE DOCUMENT");            
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
    private void readData(StepContext stepContext) {
        runtime.getTracer().debug(null,stepContext,-1,this,null,"    DATA > LOADING...");   
        
        if (documents==null) {
            documents = new DocumentSequence(runtime);
        }

        c_init.close(stepContext.curChannel);        
        
        if (uri == null) {
            documents.addChannel(stepContext.curChannel);           
            runtime.getTracer().debug(null,stepContext,-1,this,null,"    DATA > NOTHING");                    
        } else {
            if (doc == null) {
                String userContentType = parseContentType(contentType);
                String userCharset = parseCharset(contentType);
                URI dataURI = getDataUri(uri);
        
                TreeWriter tree = new TreeWriter(runtime);
                tree.startDocument(dataURI);
        
                InputStream stream;
        
                try {
                    stream = getStream(dataURI);
                    String serverContentType = getContentType();
        
                    if ("content/unknown".equals(serverContentType) && contentType != null) {
                        // pretend...
                        serverContentType = contentType;
                    }
        
                    String serverBaseContentType = parseContentType(serverContentType);
                    String serverCharset = parseCharset(serverContentType);
        
                    if (serverCharset != null) {
                        // HACK! Make the content type here consistent with the content type returned
                        // from the http-request tests, just to make the test suite results more
                        // consistent.
                        serverContentType = serverBaseContentType + "; charset=\"" + serverCharset + "\"";
                    }
        
                    // If the user specified a charset and the server did not and it's a file: URI,
                    // assume the user knows best.
                    // FIXME: provide some way to override this!!!
        
                    String charset = serverCharset;
                    if ("file".equals(dataURI.getScheme())
                            && serverCharset == null
                            && serverBaseContentType.equals(userContentType)) {
                        charset = userCharset;
                    }
        
                    if (runtime.transparentJSON() && HttpUtils.jsonContentType(contentType)) {
                        if (charset == null) {
                            // FIXME: Is this right? I think it is...
                            charset = "UTF-8";
                        }
                        InputStreamReader reader = new InputStreamReader(stream, charset);
                        JSONTokener jt = new JSONTokener(reader);
                        XdmNode jsonDoc = JSONtoXML.convert(runtime.getProcessor(), jt, runtime.jsonFlavor());
                        tree.addSubtree(jsonDoc);
                    } else {
                        tree.addStartElement(wrapper);
                        if (XProcConstants.c_data.equals(wrapper)) {
                            if ("content/unknown".equals(serverContentType)) {
                                tree.addAttribute(_contentType, "application/octet-stream");
                            } else {
                                tree.addAttribute(_contentType, serverContentType);
                            }
                            if (!isText(serverContentType, charset)) {
                                tree.addAttribute(_encoding, "base64");
                            }
                        } else {
                            if ("content/unknown".equals(serverContentType)) {
                                tree.addAttribute(c_contentType, "application/octet-stream");
                            } else {
                                tree.addAttribute(c_contentType, serverContentType);
                            }
                            if (!isText(serverContentType, charset)) {
                                tree.addAttribute(c_encoding, "base64");
                            }
                        }
                        tree.startContent();
        
                        if (isText(serverContentType, charset)) {
                            BufferedReader bufread;
                            if (charset == null) {
                                // FIXME: Is this right? I think it is...
                                charset = "UTF-8";
                            }
                            BufferedReader bufreader = new BufferedReader(new InputStreamReader(stream, charset));
                            int maxlen = 4096 * 3;
                            char[] chars = new char[maxlen];
                            int read = bufreader.read(chars, 0, maxlen);
                            while (read >= 0) {
                                if (read > 0) {
                                    String data = new String(chars, 0, read);
                                    tree.addText(data);
                                }
                                read = bufreader.read(chars, 0, maxlen);
                            }
                            bufreader.close();
                        } else {
                            // Fill the buffer each time so that we get an even number of base64 lines
                            int maxlen = 4096 * 3;
                            byte[] bytes = new byte[maxlen];
                            int pos = 0;
                            int readlen = maxlen;
                            boolean done = false;
                            while (!done) {
                                int read = stream.read(bytes, pos, readlen);
                                if (read >= 0) {
                                    pos += read;
                                    readlen -= read;
                                } else {
                                    done = true;
                                }
        
                                if ((readlen == 0) || done) {
                                    String base64 = Base64.encodeBytes(bytes, 0, pos);
                                    tree.addText(base64 + "\n");
                                    pos = 0;
                                    readlen = maxlen;
                                }
                            }
                            stream.close();
                        }
                        tree.addEndElement();
                    }
                } catch (IOException ioe) {
                    throw new XProcException(XProcConstants.dynamicError(29), ioe);
                }        
                
                tree.endDocument();     
                           
                doc = tree.getResult();                                              
            }
            
            documents.newPipedDocument(stepContext.curChannel, doc);
            runtime.getTracer().debug(null,stepContext,-1,this,null,"    DATA > LOADED");             
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
    private Logger logger = Logger.getLogger(this.getClass().getName());
    private int pos = 0;    

    public void resetReader() {
        pos = 0;
    }

    public void setReader(Step step) {
        reader = step;
    }

    public boolean moreDocuments() {
        DocumentSequence docs = ensureDocuments();
        return pos < docs.size();
    }

    public boolean closed() {
        return true;
    }

    public int documentCount() {
        DocumentSequence docs = ensureDocuments();
        return docs.size();
    }   
    
    public DocumentSequence documents() {
        return ensureDocuments();
    }       

    public XdmNode read() throws SaxonApiException {
        DocumentSequence docs = ensureDocuments();
        XdmNode doc = docs.get(pos++);
        if (reader != null) {
            runtime.finest(null, reader.getNode(), reader.getName() + " read '" + (doc == null ? "null" : doc.getBaseURI()) + "' from " + this);
        }
        return doc;
    }    
        
    private DocumentSequence ensureDocuments() {
        if (documents != null) {
            return documents;
        }
            
        documents = new DocumentSequence(runtime);

        if (uri == null) {
            return documents;
        }

        String userContentType = parseContentType(contentType);
        String userCharset = parseCharset(contentType);
        URI dataURI = getDataUri(uri);

        TreeWriter tree = new TreeWriter(runtime);
        tree.startDocument(dataURI);

        InputStream stream;

        try {
            stream = getStream(dataURI);
            String serverContentType = getContentType();

            if ("content/unknown".equals(serverContentType) && contentType != null) {
                // pretend...
                serverContentType = contentType;
            }

            String serverBaseContentType = parseContentType(serverContentType);
            String serverCharset = parseCharset(serverContentType);

            if (serverCharset != null) {
                // HACK! Make the content type here consistent with the content type returned
                // from the http-request tests, just to make the test suite results more
                // consistent.
                serverContentType = serverBaseContentType + "; charset=\"" + serverCharset + "\"";
            }

            // If the user specified a charset and the server did not and it's a file: URI,
            // assume the user knows best.
            // FIXME: provide some way to override this!!!

            String charset = serverCharset;
            if ("file".equals(dataURI.getScheme())
                    && serverCharset == null
                    && serverBaseContentType.equals(userContentType)) {
                charset = userCharset;
            }

            if (runtime.transparentJSON() && HttpUtils.jsonContentType(contentType)) {
                if (charset == null) {
                    // FIXME: Is this right? I think it is...
                    charset = "UTF-8";
                }
                InputStreamReader reader = new InputStreamReader(stream, charset);
                JSONTokener jt = new JSONTokener(reader);
                XdmNode jsonDoc = JSONtoXML.convert(runtime.getProcessor(), jt, runtime.jsonFlavor());
                tree.addSubtree(jsonDoc);
            } else {
                tree.addStartElement(wrapper);
                if (XProcConstants.c_data.equals(wrapper)) {
                    if ("content/unknown".equals(serverContentType)) {
                        tree.addAttribute(_contentType, "application/octet-stream");
                    } else {
                        tree.addAttribute(_contentType, serverContentType);
                    }
                    if (!isText(serverContentType, charset)) {
                        tree.addAttribute(_encoding, "base64");
                    }
                } else {
                    if ("content/unknown".equals(serverContentType)) {
                        tree.addAttribute(c_contentType, "application/octet-stream");
                    } else {
                        tree.addAttribute(c_contentType, serverContentType);
                    }
                    if (!isText(serverContentType, charset)) {
                        tree.addAttribute(c_encoding, "base64");
                    }
                }
                tree.startContent();

                if (isText(serverContentType, charset)) {
                    BufferedReader bufread;
                    if (charset == null) {
                        // FIXME: Is this right? I think it is...
                        charset = "UTF-8";
                    }
                    BufferedReader bufreader = new BufferedReader(new InputStreamReader(stream, charset));
                    int maxlen = 4096 * 3;
                    char[] chars = new char[maxlen];
                    int read = bufreader.read(chars, 0, maxlen);
                    while (read >= 0) {
                        if (read > 0) {
                            String data = new String(chars, 0, read);
                            tree.addText(data);
                        }
                        read = bufreader.read(chars, 0, maxlen);
                    }
                    bufreader.close();
                } else {
                    // Fill the buffer each time so that we get an even number of base64 lines
                    int maxlen = 4096 * 3;
                    byte[] bytes = new byte[maxlen];
                    int pos = 0;
                    int readlen = maxlen;
                    boolean done = false;
                    while (!done) {
                        int read = stream.read(bytes, pos, readlen);
                        if (read >= 0) {
                            pos += read;
                            readlen -= read;
                        } else {
                            done = true;
                        }

                        if ((readlen == 0) || done) {
                            String base64 = Base64.encodeBytes(bytes, 0, pos);
                            tree.addText(base64 + "\n");
                            pos = 0;
                            readlen = maxlen;
                        }
                    }
                    stream.close();
                }
                tree.addEndElement();
            }
        } catch (IOException ioe) {
            throw new XProcException(XProcConstants.dynamicError(29), ioe);
        }

        tree.endDocument();

        documents.add(tree.getResult());
        return documents;
    }         
*/
}


