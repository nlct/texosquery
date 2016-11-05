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
 * Main class. Supports Java 1.5+.
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
