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

import innovimax.quixproc.codex.util.QConfig;
import innovimax.quixproc.codex.util.open.QuiXPathImpl;
import innovimax.quixproc.codex.util.open.Spy;
import innovimax.quixproc.codex.util.open.Tracer;
import innovimax.quixproc.codex.util.open.Waiter;

import com.quixpath.interfaces.IQuiXPath;
import com.xmlcalabash.drivers.RunTestReport;

public class RunTestC
{  
  /*
   * main entry
   */ 
     
  public static void main(String[] args)
  {
    try {
      Tracer tracer = new Tracer();
      Waiter waiter = new Waiter();
      Spy spy = new Spy();
      IQuiXPath quixpath = new QuiXPathImpl();  
      QConfig config = new QConfig();
      config.setTracer(tracer);
      config.setWaiter(waiter);
      config.setSpy(spy);
      config.setQuiXPath(quixpath);
      config.setTraceMode(QConfig.TRACE_NO);
      config.setRunMode(QConfig.MODE_RUN_IT);
      RunTestReport.run(args, config);
    } catch (Exception e) {
      e.printStackTrace();
    }    
  }
  
}
