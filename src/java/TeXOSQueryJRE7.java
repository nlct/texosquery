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
 * Main class. Supports Java 1.7.
 * @author Nicola Talbot
 * @version 1.2
 * @since 1.2
 */
public class TeXOSQueryJRE7 extends TeXOSQuery
{
   public TeXOSQueryJRE7()
   {
      super("texosquery");
   }

    /**
     * Gets the script for the given locale.
     * @param locale The locale
     * @return The language script associated with the given locale or null if not available
     */
   @Override
   public String getScript(Locale locale)
   {
      return locale.getScript();
   }


   /**
    * Gets the locale from the given language tag.
    * @param languageTag The language tag
    * @return The locale that closest matches the language tag
    */
   @Override
   public Locale getLocale(String languageTag)
   {
      return Locale.forLanguageTag(languageTag);
   }

   /**
    * Main method.
    * @param args Command line arguments.
    */
   public static void main(String[] args)
   {
      (new TeXOSQueryJRE7()).processArgs(args);
   }
}
