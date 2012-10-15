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
package innovimax.quixproc.codex.library;

import innovimax.quixproc.codex.util.PipedDocument;
import innovimax.quixproc.datamodel.QuixEvent;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import net.sf.saxon.s9api.QName;

import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.library.DefaultStep;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.URIUtils;

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
    
    private PipedDocument out = null;     
    
    public DirectoryList(XProcRuntime runtime, XAtomicStep step) {    
        super(runtime,step);
    }
    
    public void setOutput(String port, WritablePipe pipe) {
        result = pipe;
    } 
    
    public void reset() {        
        // nop
    }      
   
    public void gorun() {             
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
        RuntimeValue value = getOption(_include_filter);
        if (value != null) {
            inclFilter = value.getString();
        }
        value = getOption(_exclude_filter);
        if (value != null) {
            exclFilter = value.getString();            
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

        try {           
            out = result.newPipedDocument(stepContext.curChannel);    
            out.append(QuixEvent.getStartDocument(step.getNode().getBaseURI().toASCIIString()));   
            out.append(QuixEvent.getStartElement(c_directory.toString(), c_directory.getNamespaceURI()));                         
            out.append(QuixEvent.getAttribute(_name.toString(), _name.getNamespaceURI(), dirname));
            out.append(QuixEvent.getAttribute(XProcConstants.xml_base.toString(), XProcConstants.xml_base.getNamespaceURI(), dir.toURI().toASCIIString()));            
                    
            File[] contents = dir.listFiles();                                      
            for (File file : contents) {
                boolean use = true;
                String filename = file.getName();         
                if (inclFilter != null) {
                    use = filename.matches(inclFilter);                   
                }   
                if (exclFilter != null) {
                    use = use && !filename.matches(exclFilter);                   
                }   
                if (use) {
                    if (file.isDirectory()) {                        
                        out.append(QuixEvent.getStartElement(c_directory.toString(), c_directory.getNamespaceURI()));                         
                        out.append(QuixEvent.getAttribute(_name.toString(), _name.getNamespaceURI(), file.getName()));
                        out.append(QuixEvent.getEndElement(c_directory.toString(), c_directory.getNamespaceURI())); 
                    } else if (file.isFile()) {
                        out.append(QuixEvent.getStartElement(c_file.toString(), c_file.getNamespaceURI()));                         
                        out.append(QuixEvent.getAttribute(_name.toString(), _name.getNamespaceURI(), file.getName()));
                        out.append(QuixEvent.getEndElement(c_file.toString(), c_file.getNamespaceURI())); 
                    } else {
                        out.append(QuixEvent.getStartElement(c_other.toString(), c_other.getNamespaceURI()));                         
                        out.append(QuixEvent.getAttribute(_name.toString(), _name.getNamespaceURI(), file.getName()));
                        out.append(QuixEvent.getEndElement(c_other.toString(), c_other.getNamespaceURI()));                      
                    }
                } else if (showExcluded) {
                    out.append(QuixEvent.getComment(" excluded: " + file.getName() + " ")); 
                }  
            }                                                
            
            out.append(QuixEvent.getEndElement(c_directory.toString(), c_directory.getNamespaceURI()));             
            out.append(QuixEvent.getEndDocument(step.getNode().getBaseURI().toASCIIString()));   
            
        } catch (Exception e) {
            throw new XProcException(e);      
        }
    }
  
}

