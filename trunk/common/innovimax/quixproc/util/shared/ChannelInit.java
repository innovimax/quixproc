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
package innovimax.quixproc.util.shared;

import java.util.Map;
import java.util.HashMap;

public class ChannelInit {
    private Map<Integer, Boolean> map = null;

    public ChannelInit() {
        map = new HashMap<Integer, Boolean>();
        map.put(0, false);
    }
    
    public boolean done(int channel) {
        synchronized(map) {        
            Boolean loaded = map.get(channel);
            if (loaded != null) {
                return loaded.booleanValue();
            }
            return false;            
        }
    }
    
    public void reset(int channel) {        
        synchronized(map) {       
            map.put(channel, false);
        }
    }     

    public void close(int channel) {        
        synchronized(map) {       
            map.put(channel, true);        
        }
    }         
}
