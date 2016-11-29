/*
    Copyright (C) 2016 Nicola L.C. Talbot
    www.dickimaw-books.com

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
*/
package com.dickimawbooks.texosquery;

import java.io.BufferedReader;
import java.util.Locale;
import java.util.Calendar;
import java.text.DecimalFormatSymbols;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Main class. Supports Java 5 onwards.
 * @author Nicola Talbot
 * @version 1.2
 * @since 1.2
 */
public class TeXOSQueryJRE5 extends TeXOSQuery
{
   public TeXOSQueryJRE5()
   {
      super("texosquery-jre5");
   }

    /**
     * Main method.
     * @param args Command line arguments.
     */
   public static void main(String[] args)
   {
      (new TeXOSQueryJRE5()).processArgs(args);
   }
}
