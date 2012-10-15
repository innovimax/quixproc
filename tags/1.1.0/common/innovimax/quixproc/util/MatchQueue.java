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
package innovimax.quixproc.util;

import innovimax.quixproc.datamodel.IStream;
import innovimax.quixproc.datamodel.MatchEvent;

import java.util.LinkedList;

public class MatchQueue extends AbstractQueue
{    
  private static long curMatchCount = 0;
  private static long maxMatchCount = 0;
  private LinkedList<MatchEvent> queue = null;  

  public MatchQueue() { queue = new LinkedList<MatchEvent>(); }

  public void push(MatchEvent match) { 
    increaseCurrentCount();
    curMatchCount++;
    if (curMatchCount>maxMatchCount) { maxMatchCount++; }        
    queue.add(match); 
  }  
  
  public void push(IStream<MatchEvent> stream) {
    // TODO
  }
  
  public MatchEvent pull() { 
    decreaseCurrentCount(); 
    curMatchCount--;    
    return queue.poll(); 
  }    
  
  public boolean empty() { return (queue.size()==0); }  
  
  public void clear() { queue.clear(); }     
    
  public static long getCurrentCount() { return curMatchCount; }   
  public static long getMaxiCount() { return maxMatchCount; }  
  
  public static void resetMaxiCount() { 
    maxMatchCount = 0;
    curMatchCount = 0;
  }      
  
}