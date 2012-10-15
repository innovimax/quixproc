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

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Feb 26, 2010
 * Time: 1:51:25 PM
 * To change this template use File | Settings | File Templates.
 */
public class HttpUtils {

    /** Creates a new instance of HttpUtils */
    protected HttpUtils() {
    }

    public static String baseContentType(String contentType) {
        if (contentType != null && contentType.matches("(^.*)[ \t]*;.*$")) {
            return contentType.replaceAll("(^.*)[ \t]*;.*$", "$1");
        } else {
            return contentType;
        }
    }

    public static boolean xmlContentType(String contentType) {
        String baseType = HttpUtils.baseContentType(contentType);
        return baseType != null
                && ("application/xml".equals(baseType)
                    || "text/xml".equals(baseType)
                    || baseType.endsWith("+xml"));
    }

    public static boolean jsonContentType(String contentType) {
        String baseType = HttpUtils.baseContentType(contentType);
        return baseType != null
                && ("application/json".equals(baseType)
                    || "text/json".equals(baseType));
    }

    public static boolean textContentType(String contentType) {
        return contentType != null && contentType.startsWith("text/");
    }

    public static String getCharset(String contentType, String defaultCharset) {
        String charset = HttpUtils.getCharset(contentType);
        if (charset == null) {
            return defaultCharset;
        } else {
            return charset;
        }
    }

    public static String getCharset(String contentType) {
        String charset = null;

        if (contentType != null && contentType.matches("^.*;[ \t]*charset=([^ \t]+).*$")) {
            charset = contentType.replaceAll("^.*;[ \t]*charset=([^ \t]+).*$", "$1");
            if (charset.startsWith("\"") || charset.startsWith("'")) {
                charset = charset.substring(1,charset.length()-1);
            }
        }

        return charset;
    }
}
