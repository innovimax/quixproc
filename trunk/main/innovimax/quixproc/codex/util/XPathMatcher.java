/*
QuiXProc: efficient evaluation of XProc Pipelines.
Copyright (C) 2011 Innovimax
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

import com.quixpath.exceptions.QuiXPathException;
import com.quixpath.interfaces.IQuiXPath;
import com.quixpath.interfaces.IQuiXPathExpression;

import innovimax.quixproc.datamodel.MatchEvent;
import innovimax.quixproc.datamodel.IStream;
import innovimax.quixproc.util.MatchException;
import innovimax.quixproc.util.MatchHandler;
import innovimax.quixproc.util.MatchProcess;
import innovimax.quixproc.util.MatchQueue;

public class XPathMatcher implements Runnable {
    private EventReader reader = null;
    private MatchHandler callback = null;
    private MatchProcess process = null;
    
    private static class QuiXPathMatcher implements MatchProcess {
      private final MatchQueue queue;
      private final String xpath;
      private final boolean matchSubtree;
      private IQuiXPathExpression expression;
      private QuiXPathMatcher(IQuiXPath quixpath, String xpath, boolean matchSubtree) {
      this.xpath = xpath;
      try {
        this.expression = quixpath.compile(xpath);        
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
            while(stream.hasNext()) {
              this.queue.push(stream.next());
            }
          }
        } catch (QuiXPathException e) {
          throw new MatchException(e);
        }        
      }
      @Override
      public boolean hasEvent() { return !queue.empty(); }    
      @Override
      public MatchEvent pullEvent() { return queue.pull(); }
      
    }
    
    public XPathMatcher(IQuiXPath quixpath, EventReader reader, MatchHandler callback, String xpath, boolean multiplex) {          
        this.reader = reader;
        this.callback = callback;
        process = new QuiXPathMatcher(quixpath, xpath,multiplex);
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
        }       
        catch (Exception e) {    
            callback.errorProcess(e);                    
        }         
    }
 
}

