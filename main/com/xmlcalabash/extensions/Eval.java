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
package com.xmlcalabash.extensions;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmDestination;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmNodeKind;
import net.sf.saxon.s9api.XdmSequenceIterator;
import net.sf.saxon.s9api.XdmValue;

import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.library.DefaultStep;
import com.xmlcalabash.model.DeclareStep;
import com.xmlcalabash.model.Input;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.runtime.XInput;
import com.xmlcalabash.runtime.XLibrary;
import com.xmlcalabash.runtime.XPipeline;
import com.xmlcalabash.util.RelevantNodes;
import com.xmlcalabash.util.S9apiUtils;
import com.xmlcalabash.util.TreeWriter;

/**
 * Created by IntelliJ IDEA.
 * User: ndw
 * Date: Mar 15, 2009
 * Time: 5:22:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class Eval extends DefaultStep {
    protected final static QName cx_document = new QName("cx", XProcConstants.NS_CALABASH_EX, "document");
    protected final static QName cx_options = new QName("cx", XProcConstants.NS_CALABASH_EX, "options");
    protected final static QName cx_option = new QName("cx", XProcConstants.NS_CALABASH_EX, "option");
    private static final QName _port = new QName("port");
    private static final QName _detailed = new QName("detailed");
    private static final QName _step = new QName("step");
    private static final QName _name = new QName("name");
    private static final QName _value = new QName("value");

    private Vector<ReadablePipe> sources = new Vector<ReadablePipe> ();
    private ReadablePipe pipeline = null;
    private Hashtable<QName,RuntimeValue> params = new Hashtable<QName,RuntimeValue> ();
    private Vector<ReadablePipe> options = new Vector<ReadablePipe> ();
    private WritablePipe result = null;

    /**
     * Creates a new instance of Eval
     */
    public Eval(XProcRuntime runtime, XAtomicStep step) {
        super(runtime,step);
    }

    public void setInput(String port, ReadablePipe pipe) {
        if ("source".equals(port)) {
            sources.add(pipe);
        } else if ("pipeline".equals(port)) {
            if (pipeline != null) {
                throw new XProcException(step.getNode(), "You can't specify more than one pipeline.");
            } else {
                pipeline = pipe;
            }
        } else if ("options".equals(port)) {
            options.add(pipe);
        } else {
            throw new XProcException(step.getNode(), "Unexpected port: " + port);
        }
    }

    public void setOutput(String port, WritablePipe pipe) {
        result = pipe;
    }

    public void reset() {
        for (ReadablePipe pipe : sources) {
            pipe.resetReader(stepContext);
        }
        for (ReadablePipe pipe : options) {
            pipe.resetReader(stepContext);
        }
        pipeline.resetReader(stepContext);
        // What about parameters?
        result.resetWriter(stepContext);
    }

    public void setParameter(QName name, RuntimeValue value) {
        params.put(name, value);
    }

    public void gorun() throws SaxonApiException {
        super.gorun();

        XdmNode pipedoc = pipeline.read(stepContext);
        XdmNode piperoot = S9apiUtils.getDocumentElement(pipedoc);

        XProcRuntime innerRuntime = new XProcRuntime(runtime);

        QName stepName = getOption(_step, (QName) null);
        XPipeline pipeline = null;
        if (XProcConstants.p_pipeline.equals(piperoot.getNodeName())
                || XProcConstants.p_declare_step.equals(piperoot.getNodeName())) {
            if (stepName != null) {
                throw new XProcException(step.getNode(), "Step option can only be used when loading a p:library");
            }
            pipeline = innerRuntime.use(pipedoc);
        } else if (XProcConstants.p_library.equals(piperoot.getNodeName())) {
            XLibrary library = innerRuntime.useLibrary(piperoot);
            if (stepName == null) {
                pipeline = library.getFirstPipeline();
            } else {
                pipeline = library.getPipeline(stepName);
            }
        }

        Set<String> inputports = pipeline.getInputs();
        Set<String> outputports = pipeline.getOutputs();

        int inputCount = 0;
        for (String port : inputports) {
            XInput input = pipeline.getInput(port);
            if (input.getParameters()) {
                // nop; it's ok for these to be unbound
            } else {
                inputCount++;
            }
        }

        boolean detailed = getOption(_detailed, false);

        if (!detailed && (inputCount > 1 || outputports.size() > 1)) {
            throw new XProcException(step.getNode(), "You must specify detailed='true' to eval pipelines with multiple inputs or outputs");
        }

        DeclareStep decl = pipeline.getDeclareStep();
        String primaryin = null;
        Iterator<String> portiter = inputports.iterator();
        while (portiter.hasNext()) {
            String port = portiter.next();
            Input input = decl.getInput(port);
            if (!input.getParameterInput() && ((inputports.size() == 1 && !input.getPrimarySet()) || input.getPrimary())) {
                primaryin = port;
            }
        }

        Hashtable<String,Vector<XdmNode>> inputs = new Hashtable<String,Vector<XdmNode>> ();
        for (ReadablePipe pipe : sources) {
            while (pipe.moreDocuments(stepContext)) {
                String port = primaryin;
                XdmNode doc = pipe.read(stepContext);
                XdmNode root = S9apiUtils.getDocumentElement(doc);
                if (detailed && cx_document.equals(root.getNodeName())) {
                    port = root.getAttributeValue(_port);
                    // FIXME: support exclude-inline-prefixes
                    boolean seenelem = false;
                    XdmDestination dest = new XdmDestination();
                    Vector<XdmValue> nodes = new Vector<XdmValue> ();
                    XdmSequenceIterator iter = root.axisIterator(Axis.CHILD);
                    while (iter.hasNext()) {
                        XdmNode child = (XdmNode) iter.next();
                        if (child.getNodeKind() == XdmNodeKind.ELEMENT) {
                            if (seenelem) {
                                throw new IllegalArgumentException("Not a well-formed inline document");
                            }
                            seenelem = true;
                        }
                        nodes.add(child);
                    }

                    S9apiUtils.writeXdmValue(runtime, nodes, dest, root.getBaseURI());
                    doc = dest.getXdmNode();
                }

                if (port == null) {
                    throw new XProcException(step.getNode(), "You must use cx:document for pipelines with no primary input port");
                }

                if (!inputs.containsKey(port)) {
                    inputs.put(port, new Vector<XdmNode> ());
                }

                inputs.get(port).add(doc);
            }
        }

        for (String port : inputs.keySet()) {
            if (inputports.contains(port)) {
                pipeline.clearInputs(port);
                for (XdmNode node : inputs.get(port)) {
                    pipeline.writeTo(port, node);
                }
            } else {
                throw new XProcException(step.getNode(), "Eval pipeline has no input port named '" + port + "'");
            }
        }

        if (params != null) {
            for (QName name : params.keySet()) {
                pipeline.setParameter(name, params.get(name));
            }
        }

        for (ReadablePipe pipe : options) {
            while (pipe.moreDocuments(stepContext)) {
                XdmNode doc = pipe.read(stepContext);
                XdmNode root = S9apiUtils.getDocumentElement(doc);

                if (!cx_options.equals(root.getNodeName())) {
                    throw new XProcException(step.getNode(), "Options port must be a cx:options document.");
                }

                
                for (XdmNode opt : new RelevantNodes(runtime, root, Axis.CHILD)) {
                    if (opt.getNodeKind() != XdmNodeKind.ELEMENT || !cx_option.equals(opt.getNodeName())) {
                        throw new XProcException(step.getNode(), "A cx:options document must only contain cx:option elements");
                    }

                    String name = opt.getAttributeValue(_name);
                    QName qname = new QName(name, opt);

                    String value = opt.getAttributeValue(_value);

                    if (name == null || value == null) {
                        throw new XProcException(step.getNode(), "A cx:option element must have name and value attributes");
                    }

                    RuntimeValue runtimeValue = new RuntimeValue(value);
                    pipeline.passOption(qname, runtimeValue);
                }
            }
        }
        
        pipeline.gorun();        

        portiter = outputports.iterator();
        while (portiter.hasNext()) {
            String port = portiter.next();
            ReadablePipe rpipe = pipeline.readFrom(port);
            rpipe.canReadSequence(true);

            while (rpipe.moreDocuments(stepContext)) {
                XdmNode doc = rpipe.read(stepContext);

                TreeWriter tree = new TreeWriter(runtime);
                tree.startDocument(doc.getBaseURI());

                if (detailed) {
                    tree.addStartElement(cx_document);
                    tree.addAttribute(_port, port);
                    tree.startContent();
                    tree.addSubtree(doc);
                    tree.addEndElement();
                } else {
                    tree.addSubtree(doc);
                }

                tree.endDocument();
                result.write(stepContext,tree.getResult());
            }
        }
    }
}
