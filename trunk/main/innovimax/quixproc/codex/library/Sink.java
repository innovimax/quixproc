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
package innovimax.quixproc.codex.library;

import innovimax.quixproc.codex.util.EventReader;

import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.library.DefaultStep;
import com.xmlcalabash.runtime.XAtomicStep;


public class Sink extends DefaultStep {
    private ReadablePipe source = null; 
    
    public Sink(XProcRuntime runtime, XAtomicStep step) {    
        super(runtime,step);
    }
    
    public void setInput(String port, ReadablePipe pipe) {
        source = pipe;
    }
    
    public void reset() {        
        // nop
    }      
   
    public void gorun() {    
        try {                                 
            EventReader evr = new EventReader(source.readAsStream(stepContext), null);   
            while (evr.hasEvent()) {
              evr.nextEvent(); 
            }                                    
        }         
        catch (Exception e) {            
            throw new XProcException(e);      
        }      
    }

}

