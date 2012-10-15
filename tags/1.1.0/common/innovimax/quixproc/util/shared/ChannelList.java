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

import java.util.ArrayList;
import java.util.List;

public class ChannelList {  
  
    private List<Integer> channels = null;    
    
    public ChannelList() {    
        channels = new ArrayList<Integer>();          
    }    
    
    public void add(int channel) {
        synchronized(channels) {
            channels.add(channel);
        }
    }  
    
    public int get(int index) {
        synchronized(channels) {
            return channels.get(index).intValue();            
        }          
    }   
    
    public int size() {
        synchronized(channels) {      
            return channels.size();
        }
    }    
    
    public void clear() {
        synchronized(channels) {         
            channels.clear();
        }
    }                
        
}