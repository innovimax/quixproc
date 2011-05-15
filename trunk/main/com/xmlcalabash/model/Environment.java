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

package com.xmlcalabash.model;

import java.util.Vector;

public class Environment {
    private Vector<Step> visibleSteps = new Vector<Step> ();
    private Port defaultReadablePort = null;
    private Step pipeline = null;
    private Environment parent = null;

    // ignored namespaces are only used at parse time
    
    /** Creates a new instance of Environment */
    public Environment(Step pipeline) {
        this.pipeline = pipeline;
        visibleSteps.add(pipeline);
    }
    
    public Environment(Environment env) {
        parent = env;
        pipeline = env.pipeline;
        defaultReadablePort = env.defaultReadablePort;
    }

    public Environment getParent() {
        return parent;
    }

    protected void setPipeline(Pipeline pipe) {
        pipeline = pipe;
        addStep(pipe);
    }

    public void addStep(Step step) {
        visibleSteps.add(step);
    }
    
    public void setDefaultReadablePort(Port port) {
        defaultReadablePort = port;
    }
    
    public Port getDefaultReadablePort() {
        return defaultReadablePort;
    }
    
    public int countVisibleSteps(String stepName) {
        int count = 0;
        for (Step step : visibleSteps) {
            if (step.getName().equals(stepName)) {
                count++;
            }
        }
        
        if (parent != null) {
            count += parent.countVisibleSteps(stepName);
        }
        
        return count;
    }
    
    public Step visibleStep(String stepName) {
        for (Step step : visibleSteps) {
            if (step.getName().equals(stepName)) {
                return step;
            }
        }
        
        if (parent != null) {
            return parent.visibleStep(stepName);
        }
        
        return null;
    }    
    
    public Output readablePort(String stepName, String portName) {
        Step step = visibleStep(stepName);
        if (step != null) {
            return step.getOutput(portName);
        }
        return null;
    }
}
