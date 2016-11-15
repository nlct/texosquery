package com.dickimawbooks.texosquery;

import java.util.Locale;
import java.util.Locale.Builder;
import java.util.Calendar;
import java.util.Map;

/**
 * Main class. Supports Java 1.8.
 * @author Nicola Talbot
 * @version 1.2
 * @since 1.2
 */
public class TeXOSQueryJRE8 extends TeXOSQuery
{
   public TeXOSQueryJRE8()
   {
      super("texosquery-jre8");
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

   /** Gets the standalone month names for the locale data.
    * These are only available for Java 8, so just return the 
    * month names used in the date format instead.
    * @param cal The calendar
    * @param locale The locale
    * @return month names
    */
   public String getStandaloneMonths(Calendar cal, Locale locale)
   {
      Map<String,Integer> map = cal.getDisplayNames(Calendar.MONTH,
        Calendar.LONG_STANDALONE, locale);

      // Is the map order? Not sure so save in an array

      String[] names = new String[12];

      for (Map.Entry<String,Integer> entry : map.entrySet())
      {
         int idx = entry.getValue().intValue();

         if (idx < 12)
         {// Java has a 13th month that we're ignoring
            names[idx] = entry.getKey();
         }
      }

      StringBuilder builder = new StringBuilder();

      for (int i = 0; i < names.length; i++)
      {
         builder.append(String.format("{%s}", escapeText(names[i])));
      }

      return builder.toString();
   }

   /** Gets the standalone short month names for the locale data.
    * @param cal The calendar
    * @param locale The locale
    * @return month names
    */
   public String getStandaloneShortMonths(Calendar cal, Locale locale)
   {
      Map<String,Integer> map = cal.getDisplayNames(Calendar.MONTH,
        Calendar.SHORT_STANDALONE, locale);

      // Is the map order? Not sure so save in an array

      String[] names = new String[12];

      for (Map.Entry<String,Integer> entry : map.entrySet())
      {
         int idx = entry.getValue().intValue();

         if (idx < names.length)
         {
            names[idx] = entry.getKey();
         }
      }

      StringBuilder builder = new StringBuilder();

      for (int i = 0; i < names.length; i++)
      {
         builder.append(String.format("{%s}", names[i]));
      }

      return builder.toString();
   }


   /** Gets the standalone week day names for the locale data.
    * @param cal The calendar
    * @param locale The locale
    * @return week day names
    */
   public String getStandaloneWeekdays(Calendar cal, Locale locale)
   {
      Map<String,Integer> map = cal.getDisplayNames(Calendar.DAY_OF_WEEK,
        Calendar.LONG_STANDALONE, locale);

      String[] names = new String[7];

      for (Map.Entry<String,Integer> entry : map.entrySet())
      {
         switch (entry.getValue().intValue())
         {
            case Calendar.MONDAY:
              names[0] = entry.getKey();
            break;
            case Calendar.TUESDAY:
              names[1] = entry.getKey();
            break;
            case Calendar.WEDNESDAY:
              names[2] = entry.getKey();
            break;
            case Calendar.THURSDAY:
              names[3] = entry.getKey();
            break;
            case Calendar.FRIDAY:
              names[4] = entry.getKey();
            break;
            case Calendar.SATURDAY:
              names[5] = entry.getKey();
            break;
            case Calendar.SUNDAY:
              names[6] = entry.getKey();
            break;
         }
      }

      StringBuilder builder = new StringBuilder();

      for (int i = 0; i < names.length; i++)
      {
         builder.append(String.format("{%s}", names[i]));
      }

      return builder.toString();
   }

   /** Gets the standalone short week day names for the locale data.
    * @param cal The calendar
    * @param locale The locale
    * @return week day names
    */
   public String getStandaloneShortWeekdays(Calendar cal, Locale locale)
   {
      Map<String,Integer> map = cal.getDisplayNames(Calendar.DAY_OF_WEEK,
        Calendar.SHORT_STANDALONE, locale);

      String[] names = new String[7];

      for (Map.Entry<String,Integer> entry : map.entrySet())
      {
         switch (entry.getValue().intValue())
         {
            case Calendar.MONDAY:
              names[0] = entry.getKey();
            break;
            case Calendar.TUESDAY:
              names[1] = entry.getKey();
            break;
            case Calendar.WEDNESDAY:
              names[2] = entry.getKey();
            break;
            case Calendar.THURSDAY:
              names[3] = entry.getKey();
            break;
            case Calendar.FRIDAY:
              names[4] = entry.getKey();
            break;
            case Calendar.SATURDAY:
              names[5] = entry.getKey();
            break;
            case Calendar.SUNDAY:
              names[6] = entry.getKey();
            break;
         }
      }

      StringBuilder builder = new StringBuilder();

      for (int i = 0; i < names.length; i++)
      {
         builder.append(String.format("{%s}", names[i]));
      }

      return builder.toString();
   }


   /**
    * Main method.
    * @param args Command line arguments.
    */
   public static void main(String[] args)
   {
      (new TeXOSQueryJRE8()).processArgs(args);
   }
}
