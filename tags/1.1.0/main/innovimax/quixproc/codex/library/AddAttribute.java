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
package innovimax.quixproc.codex.library;

import innovimax.quixproc.codex.util.EventReader;
import innovimax.quixproc.codex.util.PipedDocument;
import innovimax.quixproc.codex.util.XPathMatcher;
import innovimax.quixproc.datamodel.MatchEvent;
import innovimax.quixproc.datamodel.QuixEvent;
import innovimax.quixproc.datamodel.stream.NamespaceContextFilter;
import innovimax.quixproc.util.MatchHandler;

import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;

import net.sf.saxon.s9api.QName;

import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.library.DefaultStep;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.runtime.XAtomicStep;

public class AddAttribute extends DefaultStep implements MatchHandler {

  private static final QName        _match               = new QName("", "match");
  private static final QName        _attribute_name      = new QName("", "attribute-name");
  private static final QName        _attribute_value     = new QName("", "attribute-value");
  private static final QName        _attribute_prefix    = new QName("", "attribute-prefix");
  private static final QName        _attribute_namespace = new QName("", "attribute-namespace");
    
  private ReadablePipe              source               = null;
  private WritablePipe              result               = null;
  private RuntimeValue              match                = null;
  private String                    attrName             = null;
  private String                    attrValue            = null;
  private String                    attrPrefix           = null;
  private String                    attrNS               = null;
  private PipedDocument             out                  = null;
  private boolean                   matched              = false;

  private javax.xml.namespace.QName attrQName            = null;
  private List<QuixEvent.Attribute> attrs                = new ArrayList<QuixEvent.Attribute>();

  public AddAttribute(XProcRuntime runtime, XAtomicStep step) {
    super(runtime, step);
  }

  public void setInput(String port, ReadablePipe pipe) {
    source = pipe;
  }

  public void setOutput(String port, WritablePipe pipe) {
    result = pipe;
  }

  public void reset() {
    // nop
  }

  public void gorun() {    
    match = getOption(_match);
    RuntimeValue name = getOption(_attribute_name);
    attrName = name.getString();
    attrValue = getOption(_attribute_value).getString();
    attrPrefix = getOption(_attribute_prefix, (String) null);
    attrNS = getOption(_attribute_namespace, (String) null);    

    if (attrPrefix != null && attrNS == null) { throw XProcException.dynamicError(34, "You can't specify a prefix without a namespace"); }

    if (attrNS != null && attrName.contains(":")) { throw XProcException.dynamicError(34, "You can't specify a namespace if the new-name contains a colon"); }

    if (attrName.contains(":")) {
      QName qname = new QName(attrName, name.getNode());
      attrName = qname.getLocalName();
      attrPrefix = qname.getPrefix();
      attrQName = new javax.xml.namespace.QName(qname.getNamespaceURI(), attrName, attrPrefix);
    } else {
      attrQName = new javax.xml.namespace.QName(attrNS, attrName, attrPrefix == null ? "" : attrPrefix);
    }    

    if ("xmlns".equals(attrQName.getLocalPart())
            || "xmlns".equals(attrQName.getPrefix())
            || XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(attrQName.getNamespaceURI())
            || (!"xml".equals(attrQName.getPrefix()) && XMLConstants.XML_NS_URI.equals(attrQName.getNamespaceURI()))
            || ("xml".equals(attrQName.getPrefix()) && !XMLConstants.XML_NS_URI.equals(attrQName.getNamespaceURI()))) {
        throw XProcException.stepError(59);
    }        

    try {
      out = result.newPipedDocument(stepContext.curChannel);
      EventReader evr = new EventReader(source.readAsStream(stepContext), null);
      XPathMatcher xmatch = new XPathMatcher(runtime.getProcessor(), runtime.getQConfig().getQuiXPath(), evr, this, match.getString(), false, !streamAll);
      Thread t = new Thread(xmatch);
      runtime.getTracer().debug(step, null, -1, source, null, "  ADDATT > RUN MATCH THREAD");
      running = true;
      t.start();
    } catch (Exception e) {
      throw new XProcException(e);
    }
  }

  /**
   * match handler interface
   */

  public void startProcess() {
    runtime.getTracer().debug(step, null, -1, source, null, "    MATCH > START THREAD");
  }

  public void endProcess() {
    runtime.getTracer().debug(step, null, -1, source, null, "    MATCH > END THREAD");
    if (!out.isClosed()) { throw new XProcException("Thread concurrent error : unclosed renamed document"); }
    running = false;
  }

  public void errorProcess(Exception e) {
    if (e instanceof RuntimeException) { throw (RuntimeException) e; }
    throw new RuntimeException(e);
  }

  private NamespaceContextFilter<QuixEvent> namespaceContext = new NamespaceContextFilter<QuixEvent>(null);

  public void processEvent(MatchEvent match) {
    try {
      boolean close = false;
      QuixEvent event = match.getEvent();
      namespaceContext.process(event);      
      switch (event.getType()) {
        case START_SEQUENCE:
          break;
        case END_SEQUENCE:
          close = true;
          break;
        case START_DOCUMENT:
          runtime.getTracer().debug(step, null, -1, source, null, "    MATCH > START DOCUMENT");
          if (match.isMatched()) { throw XProcException.stepError(13); }
          break;
        case END_DOCUMENT:
          runtime.getTracer().debug(step, null, -1, source, null, "    MATCH > END DOCUMENT");
          if (match.isMatched()) { throw XProcException.stepError(13); }
          break;
        case START_ELEMENT:
          addAttribute();
          if (match.isMatched()) {
            matched = true;
          }
          break;
        case END_ELEMENT:
          addAttribute();
          break;
        case ATTRIBUTE:
          if (match.isMatched()) {
            // attribute is matched : not allowed
            throw XProcException.stepError(23);
          } else if (matched) {
            // current parent element is matched
            if (event.asAttribute().getQName().equals(attrQName)) {
              // same attribute so removed
            } else {
              attrs.add(event.asAttribute());
            }
            event = null;
          }
          break;
        case PI:
          addAttribute();
          if (match.isMatched()) { throw XProcException.stepError(23); }
          break;
        case COMMENT:
          addAttribute();
          if (match.isMatched()) { throw XProcException.stepError(23); }
          break;
        case TEXT:
          addAttribute();
          if (match.isMatched()) { throw XProcException.stepError(23); }
          break;
        case NAMESPACE:
          // copy the namespace declaration : they won't be modified
          break;
      }
      // match.clear();
      if (event != null) {        
        out.append(event);
      }
      if (close) {
        out.close();
      }
    } catch (XProcException e) {
      throw e;
    } catch (Exception e) {
      e.printStackTrace();
      throw new XProcException(e);
    }
  }
  
  private void addAttribute() {
    if (matched) {
      // all the namespaces declaration are out
      // we should add a namespace declaration
      String newPrefix = attrQName.getPrefix();      
      if (attrQName.getNamespaceURI() != null && !"".equals(attrQName.getNamespaceURI())) {                    
        boolean needNamespace = true;
        // If the requested prefix is already bound to something else, drop it
        String uri = this.namespaceContext.getURI(newPrefix);                
        if (uri != null) {
          // is the URI the same hence we keep the prefix
          if (uri.equals(attrQName.getNamespaceURI())) {
            // the prefix is already mapped to the good namespace
            needNamespace = false; 
          } else {
            newPrefix = null; 
          }
        }                             
        // If there isn't a prefix, we have to make one up
        if (newPrefix.equals("")) {          
          int acount = 0;
          newPrefix = "_0";
          boolean done = false;
          while (!done) {
            acount++;
            newPrefix = "_" + acount;
            if (this.namespaceContext.getURI(newPrefix) == null) done = true;
          }                                        
        }
        if (needNamespace) {
          QuixEvent namespaceEvent = QuixEvent.getNamespace(newPrefix, attrQName.getNamespaceURI());          
          out.append(namespaceEvent);          
        }
      }
      for (QuixEvent.Attribute attr : attrs) {
        out.append(attr);        
      }
      QuixEvent attrEvent = QuixEvent.getAttribute(attrName, attrQName.getNamespaceURI(), newPrefix, attrValue);
      out.append(attrEvent);      
      attrs.clear();
      matched = false;
    }
  }  

}
