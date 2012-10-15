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
package innovimax.quixproc.codex.util;

public class StepContext { 
  
  public long threadId = 0;
  public int curChannel = 0;
  public int altChannel = 0;
  public int iterationPos = 1;
  public int iterationSize = 1;    
      
  public StepContext() { /* NOP */ }
  
  public StepContext(int channel) {
    curChannel = channel;
    altChannel = channel;
  }    
  
  public StepContext(StepContext stepContext) {
    curChannel = stepContext.curChannel;
    altChannel = stepContext.altChannel;
    iterationPos = stepContext.iterationPos;
    iterationSize = stepContext.iterationSize;
  }      
    
}