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

import java.util.Vector;

import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmNode;

import com.xmlcalabash.core.XProcConfiguration;
import com.xmlcalabash.core.XProcRuntime;

/**
 *
 * @author ndw
 */
public class Serialization extends SourceArtifact {
    String port = null;
    private XProcConfiguration config = null;

    boolean byteOrderMark;
    Vector<QName> cdataSectionElements;
    String doctypePublic;
    String doctypeSystem;
    String encoding;
    boolean escapeURIAttributes;
    boolean includeContentType;
    boolean indent;
    String mediaType;
    QName method;
    String normalizationForm;
    boolean omitXMLDeclaration;
    String standalone;
    boolean undeclarePrefixes;
    String version;

    /** Creates a new instance of Serialization */
    public Serialization(XProcRuntime xproc, XdmNode node) {
        super(xproc,node);

        config = xproc.getConfiguration();

        byteOrderMark = defValue("byte-order-mark", false);
        cdataSectionElements = null; // FIXME: support cdata-section-elements
        doctypePublic = defValue("doctype-public", (String) null);
        doctypeSystem = defValue("doctype-system", (String) null);
        encoding = defValue("encoding", (String) null);
        escapeURIAttributes = defValue("escape-uri-attributes", false);
        includeContentType = defValue("include-content-type", false);
        indent = defValue("indent", false);
        mediaType = defValue("media-type", (String) null);
        method = new QName("",defValue("method", "xml"));
        normalizationForm = defValue("normalization-form", (String) null);
        omitXMLDeclaration = defValue("omit-xml-declaration", true);
        standalone = defValue("standalone", "omit");
        undeclarePrefixes = defValue("undeclare-prefixes", false);
        version = defValue("version", (String) null);
    }

    private String defValue(String name, String defVal) {
        if (config.serializationOptions.containsKey(name))
            return config.serializationOptions.get(name);
        else {
            return defVal;
        }
    }

    private boolean defValue(String name, boolean defVal) {
        if (config.serializationOptions.containsKey(name))
            return "true".equals(config.serializationOptions.get(name));
        else {
            return defVal;
        }
    }

    public void setPort(String port) {
        this.port = port;
    }
    
    public String getPort() {
        return port;
    }

    public void setByteOrderMark(boolean byteOrderMark) {
        this.byteOrderMark = byteOrderMark;
    }

    public boolean getByteOrderMark() {
        return byteOrderMark;
    }

    public void setCdataSectionElements(Vector<QName> cdataSectionElements) {
        this.cdataSectionElements = cdataSectionElements;
    }

    public Vector<QName> getCdataSectionElements() {
        return cdataSectionElements;
    }

    public void setDoctypePublic(String doctypePublic) {
        this.doctypePublic = doctypePublic;
    }

    public String getDoctypePublic() {
        return doctypePublic;
    }

    public void setDoctypeSystem(String doctypeSystem) {
        this.doctypeSystem = doctypeSystem;
    }

    public String getDoctypeSystem() {
     return doctypeSystem;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEscapeURIAttributes(boolean escapeURIAttributes) {
        this.escapeURIAttributes = escapeURIAttributes;
    }

    public boolean getEscapeURIAttributes() {
        return escapeURIAttributes;
    }

    public void setIncludeContentType(boolean includeContentType) {
        this.includeContentType = includeContentType;
    }

    public boolean getIncludeContentType() {
        return includeContentType;
    }

    public void setIndent(boolean indent) {
        this.indent = indent;
    }

    public boolean getIndent() {
        return indent;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMethod(QName method) {
        this.method = method;
    }

    public QName getMethod() {
        return method;
    }
   
    public void setNormalizationForm(String normalizationForm) {
        this.normalizationForm = normalizationForm;
    }

    public String getNormalizationForm() {
        return normalizationForm;
    }

    public void setOmitXMLDeclaration(boolean omitXMLDeclaration) {
        this.omitXMLDeclaration = omitXMLDeclaration;
    }

    public boolean getOmitXMLDeclaration() {
        return omitXMLDeclaration;
    }

    public void setStandalone(String standalone) {
        this.standalone = standalone;
    }

    public String getStandalone() {
        return standalone;
    }

    public void setUndeclarePrefixes(boolean undeclarePrefixes) {
        this.undeclarePrefixes = undeclarePrefixes;
    }

    public boolean getUndeclarePrefixes() {
        return undeclarePrefixes;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }
}
