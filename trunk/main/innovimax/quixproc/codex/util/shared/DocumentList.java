/*
QuiXProc: efficient evaluation of XProc Pipelines.
Copyright (C) 2011-2012 Innovimax
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
package innovimax.quixproc.codex.util.shared;


import innovimax.quixproc.codex.util.PipedDocument;

import java.util.ArrayList;
import java.util.List;

public class DocumentList {  
  
    private List<PipedDocument> documents = null;    
    
    public DocumentList() {    
        documents = new ArrayList<PipedDocument>();          
    }    
    
    public void add(PipedDocument document) {
        synchronized(documents) {
            documents.add(document);
        }
    }  
    
    public PipedDocument get(int index) {
        synchronized(documents) {
            return documents.get(index);            
        }          
    }   
    
    public int size() {
        synchronized(documents) {      
            return documents.size();
        }
    }    
    
    public void clear() {
        synchronized(documents) {      
            documents.clear();
        }
    }                
        
}