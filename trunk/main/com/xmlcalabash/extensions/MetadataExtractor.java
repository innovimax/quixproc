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

package com.xmlcalabash.extensions;

import com.drew.imaging.jpeg.JpegMetadataReader;
import com.drew.imaging.jpeg.JpegProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.Tag;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.library.DefaultStep;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.TreeWriter;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;

public class MetadataExtractor extends DefaultStep {
    private static final QName _href = new QName("","href");
    private final static QName c_metadata = new QName("c", XProcConstants.NS_XPROC_STEP, "metadata");
    private final static QName c_tag = new QName("c", XProcConstants.NS_XPROC_STEP, "tag");
    private final static QName _dir = new QName("", "dir");
    private final static QName _type = new QName("", "type");
    private final static QName _name = new QName("", "name");
    private final static QName _error = new QName("", "error");

    private WritablePipe result = null;

    /**
     * Creates a new instance of Identity
     */
    public MetadataExtractor(XProcRuntime runtime, XAtomicStep step) {
        super(runtime,step);
    }

    public void setOutput(String port, WritablePipe pipe) {
        result = pipe;
    }

    public void reset() {
        result.resetWriter(stepContext);
    }

    public void gorun() throws SaxonApiException {
        super.gorun();

        URI href = getOption(_href).getBaseURI().resolve(getOption(_href).getString());

        try {
            URL url = href.toURL();
            URLConnection connection = url.openConnection();
            InputStream stream = connection.getInputStream();
            Metadata metadata = JpegMetadataReader.readMetadata(stream);

            TreeWriter tree = new TreeWriter(runtime);
            tree.startDocument(step.getNode().getBaseURI());
            tree.addStartElement(c_metadata);
            tree.addAttribute(_href, href.toASCIIString());
            tree.startContent();

            // iterate through metadata directories
            Iterator directories = metadata.getDirectoryIterator();
            while (directories.hasNext()) {
                Directory directory = (Directory) directories.next();
                String dir = directory.getName();
                Iterator tags = directory.getTagIterator();
                while (tags.hasNext()) {
                    Tag tag = (Tag) tags.next();

                    tree.addStartElement(c_tag);
                    tree.addAttribute(_dir, dir);
                    tree.addAttribute(_type, tag.getTagTypeHex());
                    tree.addAttribute(_name, tag.getTagName());

                    String value = "";
                    try {
                        value = tag.getDescription();
                    } catch (MetadataException me) {
                        tree.addAttribute(_error, me.toString());
                    }

                    // Bah humbug...I don't see an easy way to tell if ti's a date/time
                    if (value.matches("^\\d\\d\\d\\d:\\d\\d:\\d\\d \\d\\d:\\d\\d:\\d\\d$")) {
                        String iso = value.substring(0, 4) + "-" + value.substring(5, 7) + "-" + value.substring(8, 10)
                                + "T" + value.substring(11,19);
                        value = iso;
                    }

                    tree.startContent();
                    tree.addText(value);
                    tree.addEndElement();
                }
            }

            tree.addEndElement();
            tree.endDocument();
            result.write(stepContext, tree.getResult());
        } catch (IOException ioe) {
            throw new XProcException(ioe);
        } catch (JpegProcessingException e) {
            throw new XProcException(e);
        }
    }
}