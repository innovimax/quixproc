/*
QuiXProc: efficient evaluation of XProc Pipelines.
Copyright (C) 2011 Innovimax
Charles Foster
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

package com.xmlcalabash.extensions.xmlunit;

import java.util.Iterator;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.ByteArrayOutputStream;

import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.library.DefaultStep;
import com.xmlcalabash.util.TreeWriter;
import net.sf.saxon.s9api.*;

import com.xmlcalabash.runtime.XAtomicStep;

// ---- XML Unit dependencies ----
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import junit.framework.AssertionFailedError;
// -------------------------------

import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import org.xml.sax.SAXException;

public class Compare extends DefaultStep
{
	private static final QName c_result = new QName("c", XProcConstants.NS_XPROC_STEP, "result");

	private static final QName _fail_if_not_equal        = new QName("","fail-if-not-equal");
	private static final QName _compare_unmatched        = new QName("","compare-unmatched");
	private static final QName _ignore_comments          = new QName("","ignore-comments");
	private static final QName _ignore_whitespace        = new QName("","ignore-whitespace");
	private static final QName _normalize                = new QName("","normalize");
	private static final QName _normalize_whitespace     = new QName("","normalize-whitespace");
	private static final QName _ignore_diff_between_text_and_cdata = new QName("","ignore-diff-between-text-and-cdata");

	private static final boolean default_compare_unmatched        = false;
	private static final boolean default_ignore_comments          = false;
	private static final boolean default_ignore_whitespace        = false;
	private static final boolean default_normalize                = false;
	private static final boolean default_normalize_whitespace     = false;
	private static final boolean default_ignore_diff_between_text_and_cdata = false;

	private ReadablePipe source = null;
	private ReadablePipe alternate = null;
	private WritablePipe result = null;

	static
	{
		XMLUnit.setTransformerFactory("net.sf.saxon.TransformerFactoryImpl");
		XMLUnit.setXPathFactory("net.sf.saxon.xpath.XPathFactoryImpl");
		// XMLUnit.setXSLTVersion("2.0"); // XML Unit has been tested with XSLT version "1.0"
	}

	/**
	* Creates a new instance of XML Unit Compare
	*/
	public Compare(XProcRuntime runtime, XAtomicStep step) {
		super(runtime, step);
	}

	public void setInput(String port, ReadablePipe pipe) {
		if("source".equals(port))
			source = pipe;
		else
			alternate = pipe;
	}

	public void setOutput(String port, WritablePipe pipe) {
		result = pipe;
	}

	public void reset() {
		source.resetReader(stepContext);
		result.resetWriter(stepContext);
	}

	private String getXMLDocument(XdmNode saxonNode) throws SaxonApiException
	{
		Serializer serializer = makeSerializer();

		Processor qtproc = runtime.getProcessor();
		XQueryCompiler xqcomp = qtproc.newXQueryCompiler();
		XQueryExecutable xqexec = xqcomp.compile(".");
		XQueryEvaluator xqeval = xqexec.load();
		xqeval.setContextItem(saxonNode);

		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		serializer.setOutputStream(stream);
		xqeval.setDestination(serializer);
		xqeval.run();

		try {
			return stream.toString("UTF-8");
		} catch (UnsupportedEncodingException uee) {
			// This can't happen...
			throw new XProcException(uee);
		}
	}

	public void gorun() throws SaxonApiException {
		super.gorun();

		String sourceDoc = getXMLDocument(source.read(stepContext));
		String alternateDoc = getXMLDocument(alternate.read(stepContext));

		boolean same = false;

		try
		{
			XMLUnit.setCompareUnmatched(getOption(_compare_unmatched, default_compare_unmatched));
			XMLUnit.setIgnoreComments(getOption(_ignore_comments, default_ignore_comments));
			XMLUnit.setIgnoreDiffBetweenTextAndCDATA(getOption(_ignore_diff_between_text_and_cdata, default_ignore_diff_between_text_and_cdata));
			XMLUnit.setIgnoreWhitespace(getOption(_ignore_whitespace, default_ignore_whitespace));
			XMLUnit.setNormalize(getOption(_normalize, default_normalize));

			XMLAssert.assertXMLEqual(sourceDoc, alternateDoc);

			same = true;
		}
		catch(AssertionFailedError e) { }
		catch(SAXException e) { throw new SaxonApiException(e.getMessage()); }
		catch(IOException e) { throw new SaxonApiException(e.getMessage()); }
		catch(Exception e) { throw new SaxonApiException(e.getMessage());  }

		if (!same && getOption(_fail_if_not_equal, false)) {
			throw XProcException.stepError(19);
		}

		TreeWriter treeWriter = new TreeWriter(runtime);
		treeWriter.startDocument(step.getNode().getBaseURI());
		treeWriter.addStartElement(c_result);
		treeWriter.startContent();
		treeWriter.addText(String.valueOf(same));
		treeWriter.addEndElement();
		treeWriter.endDocument();

		result.write(stepContext, treeWriter.getResult());
	}
}

