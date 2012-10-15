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

package com.xmlcalabash.model;

import net.sf.saxon.s9api.XdmNode;

import com.xmlcalabash.core.XProcRuntime;

/**
 *
 * @author ndw
 */
public class Port extends EndPoint {
    private String port = null;
    private boolean sequence = false;
    private boolean primary = false;
    private boolean setPrimary = false;
    
    public Port(XProcRuntime xproc, XdmNode node) {
        super(xproc,node);
    }
    
    public void setPort(String port) {
        this.port = port;
    }
    
    public String getPort() {
        return port;
    }

    public void setSequence(String sequence) {
        this.sequence = "true".equals(sequence);
    }

    public void setSequence(boolean sequence) {
        this.sequence = sequence;
    }

    public boolean getSequence() {
        return sequence;
    }

    public void setPrimary(String primary) {
        if (primary != null) {
            if ("true".equals(primary)) {
                setPrimary(true);
            } else if ("false".equals(primary)) {
                setPrimary(false);
            } else {
                throw new UnsupportedOperationException("Primary '" + primary + "' not allowed; must be 'true' or 'false'");
            }
        }
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
        setPrimary = true;
    }

    public boolean getPrimary() {
        return primary;
    }
    
    public boolean getPrimarySet() {
        return setPrimary;
    }

    public String toString() {
        if (getStep() == null) {
            return "[port " + port + " on null]";
        } else {
            return "[port " + port + " on " + getStep().getName() + "]";
        }
    }        
}
