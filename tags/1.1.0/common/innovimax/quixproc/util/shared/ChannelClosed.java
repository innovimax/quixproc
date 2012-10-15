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
package innovimax.quixproc.util.shared;

import java.util.HashMap;
import java.util.Map;

public class ChannelClosed {  
  
    private Map<Integer, Boolean> map = null;
    
    public ChannelClosed() {    
        map = new HashMap<Integer, Boolean>();
    }    
    
    public void put(int channel, boolean closed) {
        synchronized(map) {
            map.put(channel, closed);
        }
    }  
    
    public Boolean get(int index) {
        synchronized(map) {
            return map.get(index);              
        }          
    }   
    
    public int size() {
        synchronized(map) {      
            return map.size();
        }
    }    
    
    public void clear() {
        synchronized(map) {         
            map.clear();
        }
    }                
        
}