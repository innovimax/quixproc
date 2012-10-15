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

import com.xmlcalabash.config.JaxpConfigurer;
import com.xmlcalabash.config.JingConfigurer;
import com.xmlcalabash.config.SaxonConfigurer;
import com.xmlcalabash.config.XMLCalabashConfigurer;
import com.xmlcalabash.config.XProcConfigurer;
import com.xmlcalabash.core.XProcRuntime;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: 9/1/11
 * Time: 8:48 AM
 * To change this template use File | Settings | File Templates.
 */
public class DefaultXProcConfigurer implements XProcConfigurer {
    private static final JaxpConfigurer defJaxpConfigurer = new DefaultJaxpConfigurer();
    private static final JingConfigurer defJingConfigurer = new DefaultJingConfigurer();
    private static final SaxonConfigurer defSaxonConfigurer = new DefaultSaxonConfigurer();
    private XMLCalabashConfigurer configurer = null;

    public DefaultXProcConfigurer(XProcRuntime runtime) {
        configurer = new DefaultXMLCalabashConfigurer(runtime);
    }

    public XMLCalabashConfigurer getXMLCalabashConfigurer() {
        return configurer;
    }

    public SaxonConfigurer getSaxonConfigurer() {
        return defSaxonConfigurer;
    }

    public JingConfigurer getJingConfigurer() {
        return defJingConfigurer;
    }

    public JaxpConfigurer getJaxpConfigurer() {
        return defJaxpConfigurer;
    }
}
