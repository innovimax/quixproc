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
package innovimax.quixproc.codex.util;

import java.util.Collections;
import java.util.Set;

import com.quixpath.interfaces.context.IStaticContext;

public class SimpleStaticContext implements IStaticContext {

  @Override
  public String getNamespaceURI(String prefix) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Set<String> getPrefixes() {
    return Collections.emptySet();
  }

}
