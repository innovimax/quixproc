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
package innovimax.quixproc.codex.drivers.open;

import innovimax.quixproc.codex.drivers.QuiXProc;
import innovimax.quixproc.codex.util.IEQuiXPath;
import innovimax.quixproc.codex.util.QConfig;
import innovimax.quixproc.codex.util.Spying;
import innovimax.quixproc.codex.util.open.QuiXPathImpl;
import innovimax.quixproc.codex.util.open.Spy;
import innovimax.quixproc.codex.util.open.Tracer;
import innovimax.quixproc.codex.util.open.Waiter;

import java.io.File;

import com.quixpath.interfaces.IQuiXPath;

public class QuiXProcB extends QuiXProc
{  
  public QuiXProcB(boolean called)          
  {
    super(called);
  } 
    
  public static void main(String[] args)
  {    
    exec(args, false, null);
  }     
  
  public static Spying call(String[] args, File baseDir) {
    return exec(args, true, baseDir);
  }    

  private static Spying exec(String[] args, boolean called, File baseDir) {    
    QuiXProcB driver = new QuiXProcB(called); 
    Tracer tracer = new Tracer();
    Waiter waiter = new Waiter();
    Spy spy = new Spy();
    IEQuiXPath quixpath = new QuiXPathImpl();
    QConfig config = new QConfig();
    config.setTracer(tracer);
    config.setWaiter(waiter);
    config.setSpy(spy);
    config.setQuiXPath(quixpath);
    config.setTraceMode(QConfig.TRACE_NO);
    config.setRunMode(QConfig.MODE_DOM_ALL);
    config.setBaseDir(baseDir);
    driver.run(args, config);
    return spy;
  }
  
}
