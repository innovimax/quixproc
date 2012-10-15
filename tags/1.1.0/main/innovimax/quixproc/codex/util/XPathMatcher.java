/*
QuiXProc: efficient evaluation of XProc Pipelines.
Copyright (C) 2011-2012 Innovimax
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

import innovimax.quixproc.datamodel.IStream;
import innovimax.quixproc.datamodel.MatchEvent;
import innovimax.quixproc.datamodel.QuixException;
import innovimax.quixproc.util.MatchException;
import innovimax.quixproc.util.MatchHandler;
import innovimax.quixproc.util.MatchProcess;
import innovimax.quixproc.util.MatchQueue;
import net.sf.saxon.s9api.Processor;

import com.quixpath.exceptions.QuiXPathException;
import com.quixpath.interfaces.IQuiXPathExpression;

public class XPathMatcher implements Runnable {
  private EventReader  reader   = null;
  private MatchHandler callback = null;
  private MatchProcess process  = null;

  private static class QuiXPathMatcher implements MatchProcess {
    private final MatchQueue    queue;
    private final String        xpath;
    private final boolean       matchSubtree;
    private final Processor     processor;
    private IQuiXPathExpression expression;

    private QuiXPathMatcher(Processor processor, IEQuiXPath quixpath, String xpath, boolean matchSubtree, boolean canUseTree) {
      this.xpath = xpath;
      this.processor = processor;
      try {
        this.expression = quixpath.compile(xpath, new SimpleStaticContext(), canUseTree);
      } catch (QuiXPathException e) {
        e.printStackTrace();
      }
      this.matchSubtree = matchSubtree;
      this.queue = new MatchQueue();
    }

    @Override
    public void pushEvent(MatchEvent event) throws MatchException {
      try {
        IStream<MatchEvent> stream = this.expression.update(event.getEvent());
        if (stream != null) {
          while (stream.hasNext()) {
            this.queue.push(stream.next());
          }
        }
      } catch (QuiXPathException e) {
        throw new MatchException(e);
      } catch (QuixException e) {
        throw new MatchException(e);
      }
    }

    @Override
    public boolean hasEvent() {
      return !queue.empty();
    }

    @Override
    public MatchEvent pullEvent() {
      return queue.pull();
    }

  }

  public XPathMatcher(Processor processor, IEQuiXPath quixpath, EventReader reader, MatchHandler callback, String xpath, boolean multiplex, boolean canUseTree) {
    this.reader = reader;
    this.callback = callback;
    if (!xpath.startsWith("/")) {
      xpath = "//" + xpath; // DEBUG : A MODIFIER
    }
    process = new QuiXPathMatcher(processor, quixpath, xpath, multiplex, canUseTree);
  }

  public void run() {
    try {
      callback.startProcess();
      while (reader.hasEvent()) {
        process.pushEvent(new MatchEvent(reader.nextEvent()));
        while (process.hasEvent()) {
          callback.processEvent(process.pullEvent());
          Thread.yield();
        }
      }
      while (process.hasEvent()) {
        callback.processEvent(process.pullEvent());
        Thread.yield();
      }
      callback.endProcess();
    } catch (Exception e) {
      callback.errorProcess(e);
    }
  }

}
