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
package innovimax.quixproc.codex.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SpyUtils {      
      
    public static String formatDateTime(Date date) { return formatDate(date,"dd/MM/yyyy hh:mm:ss"); }
    public static String formatDate(Date date) { return formatDate(date,"dd/MM/yyyy"); }  
           
    public static String formatDate(Date date, String format) {
      if (date==null) { return null; }  
      SimpleDateFormat sdf = new SimpleDateFormat(format,Locale.FRANCE);
      return sdf.format(date);
    }        
    
    public static String displaySize(long size) {
      long giga = 0;
      long mega = 0;
      long kilo = 0;        
      long rest = 0;
      if (size > 1073741824) {
        giga = size / 1073741824;
        size = size % 1073741824;
      }
      if (size > 1048576) {
        mega = size / 1048576;
        size = size % 1048576;
      }  
      if (size > 1024) {
        kilo = size / 1024;   
        rest = size % 1024;       
      } else {
        rest = size;       
      }                           
      String s = "";
      if (giga > 0) { s += giga+"G "; }
      if (mega > 0) { s += mega+"M "; }
      if (kilo > 0) { s += kilo+"K "; }
      if (rest > 0) { s += rest+"b "; }
      if (s.equals("")) { s = "0b"; }
      return s.trim();  
    }
    
    public static String displayTime(long time) {
      long hre = 0;
      long min = 0;
      long sec = 0;    
      long rest = 0;    
      if (time > 3600000) {
        hre = time / 3600000;
        time = time % 3600000;
      }        
      if (time > 60000) {
        min = time / 60000;
        time = time % 60000;
      }                
      if (time > 1000) {
        sec = time / 1000;
        rest = time % 1000;
      } else {
        rest = time;
      }         
      String s = "";        
      if (hre > 0)  { s += hre+"h "; }
      if (min > 0)  { s += min+"m "; }
      if (sec > 0)  { s += sec+"s "; }
      if (rest > 0) { s += rest+"ms"; }
      if (s.equals("")) { s = "0ms"; }
      return s.trim();
    }
    
    public static String displayCount(long count) {
      DecimalFormatSymbols dfs = new DecimalFormatSymbols();
      dfs.setDecimalSeparator(',');
      dfs.setGroupingSeparator('.');    
      DecimalFormat df = new DecimalFormat("#,##0",dfs);    
      return df.format(count);    
    }     
                
}