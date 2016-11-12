package com.dickimawbooks.texosquery;

import java.util.Locale;
import java.util.Locale.Builder;
import java.io.Serializable;

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
      Locale locale = Locale.forLanguageTag(languageTag);

      // Locale.forLanguageTag() doesn't recognise
      // numeric regions. So test for a numeric region.

      String region = locale.getCountry();

      try
      {
         region = getRegionAlpha2Code(Integer.parseInt(region));
      }
      catch (NumberFormatException e)
      {
         // region isn't numeric, so we don't need to do anything
         // else
         return locale;
      }

      Locale.Builder builder = new Locale.Builder();
      builder.setLocale(locale);
      builder.setRegion(region);

      return builder.build();
   }

   /**
    * Gets the language tag for the given locale.
    * @param locale The locale or null for the default locale
    * @return The language tag
    */
   @Override
   public String getLanguageTag(Locale locale)
   {
      if (locale == null)
      {
         locale = Locale.getDefault();
      }

      return locale.toLanguageTag();
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
