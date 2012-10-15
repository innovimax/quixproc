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
package com.xmlcalabash.util;

import java.util.Hashtable;

import net.sf.saxon.event.SourceLocationProvider;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Nov 23, 2008
 * Time: 5:12:19 PM
 * To change this template use File | Settings | File Templates.
 */
public class XProcLocationProvider implements SourceLocationProvider {
    Hashtable<String, Integer> locationMap;
    Hashtable<Integer,String> idMap;
    int nextId;

    public XProcLocationProvider() {
        locationMap = new Hashtable<String,Integer> ();
        idMap = new Hashtable<Integer,String> ();
        nextId = 0;
    }

    public int allocateLocation(String uri) {
        if (locationMap.containsKey(uri)) {
            return locationMap.get(uri);
        } else {
            int id = nextId++;
            idMap.put(id,uri);
            locationMap.put(uri,id);
            return id;
        }
    }


    public String getSystemId(long locationId) {
        int locId = (int) locationId;
        if (idMap.containsKey(locId)) {
            return idMap.get(locId);
        } else {
            return null;
        }
    }

    public int getLineNumber(long locationId) {
        return 0;
    }

    public int getColumnNumber(long locationId) {
        return 0;
    }
}
