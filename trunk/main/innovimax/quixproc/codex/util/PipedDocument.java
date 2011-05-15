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

import innovimax.quixproc.datamodel.DOMConverter;
import innovimax.quixproc.datamodel.EventConverter;
import innovimax.quixproc.datamodel.QuixEvent;
import innovimax.quixproc.datamodel.IStream;
import innovimax.quixproc.datamodel.shared.IQueue;
import innovimax.quixproc.datamodel.shared.ISimpleQueue;
import innovimax.quixproc.datamodel.shared.SmartAppendQueue;

import java.net.URI;

import net.sf.saxon.s9api.XdmNode;

import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.util.S9apiUtils;

public class PipedDocument implements ISimpleQueue<QuixEvent>{
  private static int idCounter = 0;
  //
  private final XProcRuntime runtime;
  private final int readerCount;
  private final int id;
  //
  private URI baseURI = null;
  private XdmNode node = null;
  private boolean closed = false;
  private IQueue<QuixEvent> events = null;
//  private QuixStream domNodeReader = null;

  
  private PipedDocument(int readerCount, XProcRuntime runtime) {
    this.runtime = runtime;    
    this.id = idCounter++;
    this.readerCount = readerCount;    
//    if (readerCount == 0) Thread.dumpStack();
  }
  
  public PipedDocument(XProcRuntime runtime, int readerCount, XdmNode node) {
    this(readerCount, runtime);
    this.node = node;
    this.baseURI = node.getBaseURI();
  }

  public PipedDocument(XProcRuntime runtime, int readerCount) {
    this(readerCount, runtime);
    this.events = newQuixEventQueue(readerCount);
//    this.domNodeReader = events.registerReader();
  }

  private final static IQueue<QuixEvent> newQuixEventQueue(int readerCount) {
    // TODO moz V1 : put the one who works less good
    IQueue<QuixEvent> qeq = new SmartAppendQueue<QuixEvent>();
    qeq.setReaderCount(readerCount);
    return qeq;
  }

  public int getId() {
    return id;
  }

  public void setBaseURI(URI baseURI) {
    this.baseURI = baseURI;
  }

  public URI getBaseURI() {
    return baseURI;
  }

  public boolean isDocument() {
    if (node != null) { return S9apiUtils.isDocument(node); }
    return true;
  }

  /**
   * DOM pipe management
   */

  public boolean isDOMPipe() {
    return (node != null);
  }

  public XdmNode getNode() {
    if (node == null) {
      node = streamToNode();
    }
    return node;
  }

  public void setNode(XdmNode node) {
    this.node = node;
  }

  /**
   * Stream pipe management
   */

  public boolean isStreamedPipe() {
    return (events != null);
  }

  public synchronized IStream<QuixEvent> registerReader() {
    if (events == null) { return nodeToStream(); }
    return events.registerReader();
  }

  public void append(QuixEvent event) {
    // System.out.println("PipedDocument.addEvent : "+event);
    events.append(event);
  }

  public void close() {
    closed = true;
    events.close();
  }

  public boolean isClosed() {
    return closed;
  }

  /**
   * conversion management
   */

  private XdmNode streamToNode() {
    runtime.getTracer().debug(null, null, -1, null, this, "    PDOC > EXEC CONVERT-DOM ROUTINE");
    DOMConverter converter = new DOMConverter(runtime.getProcessor().newDocumentBuilder(), events.registerReader());
    return converter.exec();
  }

  private IStream<QuixEvent> nodeToStream() {
    events = newQuixEventQueue(readerCount);
    IStream<QuixEvent> result = events.registerReader();
    runtime.getTracer().debug(null, null, -1, null, this, "    PDOC > RUN CONVERT-EVT THREAD");
    final PipedDocument doc = this;
    EventConverter converter = new EventConverter(doc, doc.getNode()) {
      public void startProcess() {
        runtime.getTracer().debug(null, null,-1,null,doc,"    CONVERT-EVT > START THREAD");    
    }
      public void endProcess() {
          runtime.getTracer().debug(null, null,-1,null,doc,"    CONVERT-EVT > END THREAD");        
      }             

    };
    Thread t = new Thread(converter);
    t.start();
    return result;
  }

}
