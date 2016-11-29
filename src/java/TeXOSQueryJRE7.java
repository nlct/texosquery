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

import java.util.Locale;
import java.util.Locale.Builder;
import java.util.Calendar;
import java.io.Serializable;

/**
 * Main class. Supports Java 7 onwards.
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
    * Gets the week year for the given calendar.
    * @return The week year
    */
   @Override
   public int getWeekYear(Calendar cal)
   {
      try
      {
        return cal.isWeekDateSupported() ?
          cal.getWeekYear() : cal.get(Calendar.YEAR);
      }
      catch (UnsupportedOperationException e)
      {
         // shouldn't happen with the above conditional, but just in
         // case...

         debug(e.getMessage(), e);
         return cal.get(Calendar.YEAR);
      }
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
