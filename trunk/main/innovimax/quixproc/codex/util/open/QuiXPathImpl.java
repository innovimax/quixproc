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
package innovimax.quixproc.codex.util.open;

import innovimax.quixproc.codex.util.IEQuiXPath;
import innovimax.quixproc.datamodel.IStream;
import innovimax.quixproc.datamodel.MatchEvent;
import innovimax.quixproc.datamodel.QuixEvent;
import innovimax.quixproc.datamodel.QuixValue;
import net.sf.saxon.s9api.Processor;

import com.quixpath.exceptions.QuiXPathException;
import com.quixpath.exceptions.UnsupportedQueryException;
import com.quixpath.interfaces.IQuiXPathExpression;
import com.quixpath.interfaces.context.IStaticContext;
import com.quixpath.internal.interfaces.impl.AbstractQuiXPathExpression;

public class QuiXPathImpl implements IEQuiXPath {
  com.quixpath.internal.interfaces.impl.QuiXPathImpl proxy;
  public QuiXPathImpl() {
    this.proxy = new com.quixpath.internal.interfaces.impl.QuiXPathImpl();
  }
  
  @Override
  public IQuiXPathExpression compile(String xpathQuery, IStaticContext context, boolean canUseTree) throws UnsupportedQueryException {

    return this.proxy.compile(xpathQuery, context, canUseTree);
  }
 
  @Override
  public IStream<MatchEvent> update(IQuiXPathExpression expression, QuixEvent event) throws QuiXPathException {
    return this.proxy.update(expression, event);
  }

  @Override
  public void setProcessor(Processor processor) {
    AbstractQuiXPathExpression.setProcessor(processor);        
  }

  @Override
  public QuixValue evaluate(IQuiXPathExpression expression, IStream<QuixEvent> stream) throws QuiXPathException {
    return this.proxy.evaluate(expression, stream);
  }

}