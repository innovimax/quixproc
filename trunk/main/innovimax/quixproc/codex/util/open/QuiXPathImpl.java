package innovimax.quixproc.codex.util.open;

import net.sf.saxon.s9api.Processor;
import innovimax.quixproc.codex.util.IEQuiXPath;
import innovimax.quixproc.datamodel.MatchEvent;
import innovimax.quixproc.datamodel.QuixEvent;
import innovimax.quixproc.datamodel.IStream;

import com.quixpath.exceptions.QuiXPathException;
import com.quixpath.exceptions.UnsupportedQueryException;
import com.quixpath.interfaces.IQuiXPath;
import com.quixpath.interfaces.IQuiXPathExpression;
import com.quixpath.internal.interfaces.impl.AbstractQuiXPathExpression;

public class QuiXPathImpl implements IEQuiXPath {
  com.quixpath.internal.interfaces.impl.QuiXPathImpl proxy;
  public QuiXPathImpl() {
    this.proxy = new com.quixpath.internal.interfaces.impl.QuiXPathImpl();
  }
  
  @Override
  public IQuiXPathExpression compile(String xpathQuery) throws UnsupportedQueryException {

    return this.proxy.compile(xpathQuery);
  }
 
  @Override
  public IStream<MatchEvent> update(IQuiXPathExpression expression, QuixEvent event) throws QuiXPathException {
    return this.proxy.update(expression, event);
  }

  @Override
  public void setProcessor(Processor processor) {
    AbstractQuiXPathExpression.setProcessor(processor);        
  }

}