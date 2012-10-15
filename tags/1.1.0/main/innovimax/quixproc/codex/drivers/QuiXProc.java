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
package innovimax.quixproc.codex.drivers;

import innovimax.quixproc.codex.util.QConfig;
import innovimax.quixproc.codex.util.Spying;
import innovimax.quixproc.util.ExitException;

import java.io.File;
import java.util.Vector;

public abstract class QuiXProc
{    
  private boolean called = false;
  
  public QuiXProc(boolean called)          
  {
    this.called = called;  
  }     
  
  protected Spying run(String[] args, File baseDir)
  {    
    try {        
        return EngineA.run(checkArgs(args), baseDir);       
    }
    catch (ExitException e) {
      if (called) {
        throw e;
      }
      System.exit(e.getStatus());
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }    
          
  protected void run(String[] args, QConfig config)
  {    
    try {        
        EngineB.run(checkArgs(args), config);       
    }
    catch (ExitException e) {
      if (called) {
        throw e;
      }
      System.exit(e.getStatus());
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }         
  
  private String[] checkArgs(String[] args) 
  {       
    Vector<String> newArgs = new Vector<String>();
    int pos = 0;
    while (pos < args.length) {      
      String arg = args[pos++];      
      if (arg.startsWith("-rI") || arg.startsWith("--run-it")) continue;              
      if (arg.startsWith("-rS") || arg.startsWith("--stream-all")) continue;                        
      if (arg.startsWith("-rD") || arg.startsWith("--dom-all")) continue;                
      if (arg.startsWith("-tN") || arg.startsWith("--trace-no")) continue;
      if (arg.startsWith("-tW") || arg.startsWith("--trace-wait")) continue;          
      if (arg.startsWith("-tA") || arg.startsWith("--trace-all")) continue;      
      if (arg.startsWith("-D") || arg.startsWith("--debug")) continue;      
      newArgs.add(arg);                
    }          
    String[] array = new String[newArgs.size()];
    int index = 0;
    for (String arg : newArgs) {
      array[index++] = arg;
    }
    return array;      
  }   
}
