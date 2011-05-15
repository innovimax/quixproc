/*
QuiXProc: efficient evaluation of XProc Pipelines.
Copyright (C) 2011 Innovimax
2008-2011 Mark Logic Corporation.
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
package innovimax.quixproc.codex.util.open;

import innovimax.quixproc.codex.util.Spying;

public class Spy implements Spying {      
      
    public void start(String[] args) { /* NOP */ }   
    public void exec() { /* NOP */ }
    public void stop() { /* NOP */ }  
    public long getCompileTime() { return 0; }
    public long getRunningTime() { return 0; }
    public long getFileInSize() { return 0; }
    public long getFileOutSize() { return 0; }
    public long getStartMemory() { return 0; }
    public long getMaximumMemory() { return 0; }
    public long getLostMemory() { return 0; }
    public long getTotalEventCount() { return 0; }
    public long getDocEventCount() { return 0; }
    public long getAttrEventCount() { return 0; }
                  
}