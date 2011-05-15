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

package com.xmlcalabash.library;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.util.TreeWriter;
import com.xmlcalabash.util.URIUtils;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.model.RuntimeValue;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;

import com.xmlcalabash.runtime.XAtomicStep;

public class DirectoryList extends DefaultStep {
    private static final QName _name = new QName("", "name");
    private static final QName _path = new QName("", "path");
    private static final QName _include_filter = new QName("", "include-filter");
    private static final QName _exclude_filter = new QName("", "exclude-filter");
    private static final QName c_directory = new QName("c", XProcConstants.NS_XPROC_STEP, "directory");
    private static final QName c_file = new QName("c", XProcConstants.NS_XPROC_STEP, "file");
    private static final QName c_other  = new QName("c", XProcConstants.NS_XPROC_STEP, "other");
    private static final QName px_show_excluded = new QName(XProcConstants.NS_CALABASH_EX, "show-excluded");
    private WritablePipe result = null;
    private String path = ".";
    private String inclFilter = null;
    private String exclFilter = null;

    /** Creates a new instance of DirectoryList */
    public DirectoryList(XProcRuntime runtime, XAtomicStep step) {
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

        if (runtime.getSafeMode()) {
            throw XProcException.dynamicError(21);
        }

        if (getOption(_path) != null) {
            URI pathbase = getOption(_path).getBaseURI();
            String pathstr = URIUtils.encode(getOption(_path).getString());
            path = pathbase.resolve(pathstr).toASCIIString();
        } else {
            path = step.getNode().getBaseURI().resolve(".").toASCIIString();
        }

        runtime.fine(null, step.getNode(), "path: " + path);

        RuntimeValue value = getOption(_include_filter);
        if (value != null) {
            inclFilter = value.getString();
            runtime.fine(null, step.getNode(), "include: " + inclFilter);
        }
        value = getOption(_exclude_filter);
        if (value != null) {
            exclFilter = value.getString();
            runtime.fine(null, step.getNode(), "exclude: " + exclFilter);
        }

        File dir = URIUtils.getFile(path);
        String dirname = null;

        try {
            dir = dir.getCanonicalFile();
            dirname = dir.getName();
        } catch (IOException ioe) {
            throw new XProcException(ioe);
        }

        if (!dir.isDirectory()) {
            throw XProcException.stepError(17);
        }

        if (!dir.canRead()) {
            throw XProcException.stepError(12);
        }

        boolean showExcluded = "true".equals(step.getExtensionAttribute(px_show_excluded));

        TreeWriter tree = new TreeWriter(runtime);
        tree.startDocument(step.getNode().getBaseURI());
        tree.addStartElement(c_directory);
        tree.addAttribute(_name, dirname);
        tree.addAttribute(XProcConstants.xml_base, dir.toURI().toASCIIString());
        tree.startContent();

        File[] contents = dir.listFiles();

        for (File file : contents) {
            boolean use = true;
            String filename = file.getName();

            runtime.fine(null, step.getNode(), "name: " + filename);

            if (inclFilter != null) {
                use = filename.matches(inclFilter);
                runtime.fine(null, step.getNode(), "include: " + use);
            }

            if (exclFilter != null) {
                use = use && !filename.matches(exclFilter);
                runtime.fine(null, step.getNode(), "exclude: " + !use);
            }

            if (use) {
                if (file.isDirectory()) {
                    tree.addStartElement(c_directory);
                    tree.addAttribute(_name, file.getName());
                    tree.addEndElement();
                    finest(step.getNode(), "Including directory: " + file.getName());
                } else if (file.isFile()) {
                    tree.addStartElement(c_file);
                    tree.addAttribute(_name, file.getName());
                    tree.addEndElement();
                    finest(step.getNode(), "Including file: " + file.getName());
                } else {
                    tree.addStartElement(c_other);
                    tree.addAttribute(_name, file.getName());
                    tree.addEndElement();
                    finest(step.getNode(), "Including other: " + file.getName());
                }
            } else if (showExcluded) {
                tree.addComment(" excluded: " + file.getName() + " ");
                finest(step.getNode(), "Excluding: " + file.getName());
            }
        }

        tree.addEndElement();
        tree.endDocument();

        result.write(stepContext, tree.getResult());
    }
}

