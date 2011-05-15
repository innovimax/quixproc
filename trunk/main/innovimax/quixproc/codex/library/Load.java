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
package innovimax.quixproc.codex.library;


import innovimax.quixproc.codex.util.PipedDocument;
import innovimax.quixproc.datamodel.QuixEvent;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import net.sf.saxon.s9api.QName;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.library.DefaultStep;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.runtime.XAtomicStep;

public class Load extends DefaultStep implements ContentHandler {
    private static final QName _href = new QName("href");  
    private static final QName _dtd_validate = new QName("dtd-validate");  
    private WritablePipe result = null;  
    
    private Locator locator = null;  
    private final StringBuffer charBuffer = new StringBuffer(); 
    private final boolean preserveSpace = false;    
    private PipedDocument out = null; 
    private String baseURI;
    private final List<QuixEvent> namespaceList = new ArrayList<QuixEvent>(); 
    
    // innovimax: statistics  
    private static long totalFileSize = 0;      
    
    public Load(XProcRuntime runtime, XAtomicStep step) {    
        super(runtime,step);
    }
    
    public void setOutput(String port, WritablePipe pipe) {
        result = pipe;
    }
    
    public void reset() {        
        // nop
    }    

    public void gorun() {             
        RuntimeValue href = getOption(_href);                
        baseURI = runtime.getQConfig().getBaseURI(href.getBaseURI()).toASCIIString();  
        boolean validate = getOption(_dtd_validate, false);             
        try {   
            URI hrefURI = runtime.getQConfig().resolveURI(href.getBaseURI(), href.getString()); 
            
            // innovimax: statistics                           
            java.io.File file = new java.io.File(hrefURI);                
            if (file.exists()) { totalFileSize += file.length(); }                        
            
            out = result.newPipedDocument(stepContext.curChannel);            
            InputSource input = new InputSource(hrefURI.toASCIIString());                                              
            XMLReader reader = XMLReaderFactory.createXMLReader();            
            reader.setContentHandler(this);
            reader.parse(input);                                        
            
        } catch (Exception e) {
            e.printStackTrace();
            throw XProcException.stepError(11, "Could not load " + href.getString() + " (" + baseURI + ") dtd-validate=" + validate);  
        }
    }          
    
    /** 	  
     * content handler interface
     */  
  
    public void setDocumentLocator(Locator locator) { 
        this.locator = locator; 
    }  
  
    public void startDocument() throws SAXException { 
        runtime.getTracer().debug(step,null,-1,null,null,"  LOAD > START DOCUMENT");        
        try {            
            QuixEvent event = QuixEvent.getStartDocument(baseURI);
            out.append(event);                      
        } catch (Exception e) {            
            error(e); 
        }                         
    }
  
    public void endDocument() throws SAXException {
        runtime.getTracer().debug(step,null,-1,null,null,"  LOAD > END DOCUMENT");        
        try {            
            processCharacters();
            QuixEvent event = QuixEvent.getEndDocument(baseURI);
            out.append(event);                      
            out.close();  
        } catch (Exception e) { 
            error(e); 
        }         
    }
    
    
    public void startPrefixMapping(String prefix, String uri) {
      // The mapping happen before the StartElement in SAX      
      // but probably we want them be better to be RIGHT AFTER the START_ELEMENT
      // but BEFORE ATTRIBUTE in order to be able to processs ATTRIBUTE content
      QuixEvent event = QuixEvent.getNamespace(prefix, uri);
      namespaceList.add(event);      
    }  
  
    public void endPrefixMapping(String prefix) { /* NOP */ }
  
    public void startElement(String namespace, String localName, String qName, Attributes atts) throws SAXException {
        try {            
            processCharacters();           
            QuixEvent event = QuixEvent.getStartElement(qName, namespace);
            out.append(event);
            for(QuixEvent e : namespaceList) {
              out.append(e);
            }
            // reset list
            namespaceList.clear();
            for (int i=0; i < atts.getLength(); i++) {
                QuixEvent ea = QuixEvent.getAttribute(atts.getQName(i),atts.getURI(i),atts.getValue(i));
                out.append(ea);            
            }                                          
        } catch (Exception e) { 
            error(e); 
        }    
    }
  
    public void endElement(String namespace, String localName, String qName) throws SAXException {        
        try {    
            processCharacters();                      
            QuixEvent event = QuixEvent.getEndElement(qName, namespace);
            out.append(event);                  
        } catch (Exception e) { 
            error(e); 
        }    
    }
  
    public void characters(char[] ch, int start, int length) throws SAXException {     
        try {               
            charBuffer.append(ch,start,length); 
        } catch (Exception e) { 
            error(e); 
        } 
    }  
  
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {     
        try {               
            charBuffer.append(ch,start,length);             
        } catch (Exception e) { 
            error(e); 
        } 
    }  
  
    public void processingInstruction(String target, String data) throws SAXException {    
        try {
            processCharacters();
            QuixEvent event = QuixEvent.getPI(target,data);
            out.append(event);             
        } catch (Exception e) { 
            error(e); 
        }
    }  
  
    public void skippedEntity(String name) { /* NOP */ }
  
    private void processCharacters() throws SAXException {       
        try {      
            if (charBuffer.length() > 0) {                                 
                int len = charBuffer.toString().trim().length(); 
                if ( len > 0 || preserveSpace) {    
                    QuixEvent event = QuixEvent.getText(charBuffer.toString());
                    out.append(event);                                                               
                }
                charBuffer.setLength(0);            
            }
        } catch (Exception e) { 
            error(e); 
        }             
    }    
    
    private void error(Exception e) throws SAXException {
      throw new SAXException(e);
    }     
    
    // innovimax: statistics  
    public static long getTotalFileSize() { return totalFileSize; }       
    public static void resetTotalFileSize() { totalFileSize = 0; }       
}

