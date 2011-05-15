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

import com.xmlcalabash.util.Base64;
import com.xmlcalabash.util.TreeWriter;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.S9apiUtils;
import com.thaiopensource.validate.SchemaReader;
import com.thaiopensource.validate.ValidateProperty;
import com.thaiopensource.validate.ValidationDriver;
import com.thaiopensource.validate.prop.rng.RngProperty;
import com.thaiopensource.validate.auto.AutoSchemaReader;
import com.thaiopensource.validate.rng.CompactSchemaReader;
import com.thaiopensource.xml.sax.ErrorHandlerImpl;
import com.thaiopensource.util.PropertyMapBuilder;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;

public class ValidateJing extends DefaultStep {
    private static final QName _assert_valid = new QName("", "assert-valid");
    private static final QName _dtd_attribute_values = new QName("", "dtd-attribute-values");
    private static final QName _dtd_id_idref_warnings = new QName("", "dtd-id-idref-warnings");
    private static final QName _encoding = new QName("encoding");

    private ReadablePipe source = null;
    private ReadablePipe schemaSource = null;
    private WritablePipe result = null;
    private URI docBaseURI = null;

    /** Creates a new instance of Delete */
    public ValidateJing(XProcRuntime runtime, XAtomicStep step) {
        super(runtime,step);
    }

    public void setInput(String port, ReadablePipe pipe) {
        if ("source".equals(port)) {
            source = pipe;
        } else if ("schema".equals(port)) {
            schemaSource = pipe;
        }
    }

    public void setOutput(String port, WritablePipe pipe) {
        result = pipe;
    }

    public void reset() {
        source.resetReader(stepContext);
        result.resetWriter(stepContext);
    }

    public void gorun() throws SaxonApiException {
        super.gorun();

        boolean assertValid = getOption(_assert_valid,true);
        boolean checkIdRefs = getOption(_dtd_id_idref_warnings,false);
        boolean dtdAugment  = getOption(_dtd_attribute_values,false);

        ErrorHandler eh = new RNGErrorHandler();
        PropertyMapBuilder properties = new PropertyMapBuilder();
        properties.put(ValidateProperty.ERROR_HANDLER, eh);

        if (checkIdRefs) {
            RngProperty.CHECK_ID_IDREF.add(properties);
        }
        
        XdmNode doc = source.read(stepContext);
        XdmNode schema = schemaSource.read(stepContext);
        XdmNode root = S9apiUtils.getDocumentElement(schema);

        docBaseURI = doc.getBaseURI();

        SchemaReader sr = null;

        boolean compact = XProcConstants.c_data.equals(root.getNodeName());

        String contentType = root.getAttributeValue(XProcConstants.c_content_type);
        if (contentType != null) {
            compact |= contentType.startsWith("text/") || contentType.equals("application/relax-ng-compact-syntax");
        }

        InputSource schemaInputSource = null;

        if (compact) {
            // Compact syntax
            sr = CompactSchemaReader.getInstance();

            // Grotesque hack!
            StringReader srdr = new StringReader(compactSchema(root));
            schemaInputSource = new InputSource(srdr);
            schemaInputSource.setSystemId(root.getBaseURI().toASCIIString());
        } else {
            // XML syntax
            sr = new AutoSchemaReader();
            schemaInputSource = S9apiUtils.xdmToInputSource(runtime, schema);
        }

        ValidationDriver driver = new ValidationDriver(properties.toPropertyMap(), sr);
        try {
            if (driver.loadSchema(schemaInputSource)) {
                InputSource din = S9apiUtils.xdmToInputSource(runtime, doc);
                if (!driver.validate(din)) {
                    if (assertValid) {
                        throw XProcException.stepError(53);
                    }
                }
            } else {
                throw new XProcException(step.getNode(), "Error loading schema");
            }
        } catch (SAXParseException e) {
            if (assertValid) {
                throw XProcException.stepError(53);
            }
        } catch (SAXException e) {
            throw new XProcException("SAX Exception", e);
        } catch (IOException e) {
            throw new XProcException("IO Exception", e);
        }

        result.write(stepContext, doc); // At the moment, we don't get any augmentation
    }

    private String compactSchema(XdmNode doc) {
        if ("base64".equals(doc.getAttributeValue(_encoding))) {
            byte[] decoded = Base64.decode(doc.getStringValue());
            String s = new String(decoded);
            return s;
        } else {
            return doc.getStringValue();
        }
    }

    class RNGErrorHandler implements ErrorHandler {
        public void fatalError(SAXParseException e) throws SAXException {
            error(e);
        }

        public void error(SAXParseException e) throws SAXException {
            TreeWriter treeWriter = new TreeWriter(runtime);
            treeWriter.startDocument(docBaseURI);
            treeWriter.addStartElement(XProcConstants.c_error);
            treeWriter.startContent();

            treeWriter.addText(e.toString());

            treeWriter.addEndElement();
            treeWriter.endDocument();

            step.reportError(treeWriter.getResult());
            throw e;
        }

        public void warning( SAXParseException e ) {
            // ignore warnings
        }
    }
}
