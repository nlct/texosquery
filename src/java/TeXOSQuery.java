package com.dickimawbooks.texosquery;

import java.io.BufferedReader;
import java.util.Locale;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.DecimalFormatSymbols;
import java.text.Format;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.DateFormatSymbols;
import java.text.NumberFormat;
import java.text.DecimalFormat;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;

/**
 * Application functions. These methods need to be Java version 1.5
 * compatible. The 1.7 methods need to be in the TeXOSQueryJRE7 class
 * and the 1.8 methods in TeXOSQueryJRE8.
 *
 * Since this application is designed to be run from TeX, the output
 * needs to be easy to parse using TeX commands. For this reason,
 * most exceptions are caught and an empty string is returned. The
 * TeX code can then check for an empty value to determine failure.
 * There's a debug mode to print error messages to STDERR to
 * investigate the reason for failure.
 * @author Nicola Talbot
 * @version 1.2
 * @since 1.0
 */
public class TeXOSQuery implements Serializable
{
   /**
    * Constructor.
    * @param name The application name. 
    */ 
   public TeXOSQuery(String name)
   {
      this.name = name;
   }

   /**
    * Runs kpsewhich and returns the result. This is for single
    * argument lookups through kpsewhich, such as a file location
    * or variable value.
    * @param arg The argument to pass to kpsewhich
    * @return The result read from the first line of STDIN
    * @since 1.2
    */
   protected String kpsewhich(String arg)
      throws IOException,InterruptedException
   {
      // Create and start the process.
      Process process = 
        new ProcessBuilder("kpsewhich", arg).start();

      int exitCode = process.waitFor();

      String line = null;

      if (exitCode == 0)
      {
         // kpsewhich completed with exit code 0.
         // Read STDIN to find the result.
                
         InputStream stream = process.getInputStream();
                    
         if (stream == null)
         {
            throw new IOException(String.format(
             "Unable to open input stream from \"kpsewhich '%s'\" process",
             arg));
         }

         BufferedReader reader = null;

         try
         {
            reader = new BufferedReader(new InputStreamReader(stream));

            // only read a single line, nothing further is required
            // for a variable or file location query.
            line = reader.readLine();
         }
         finally
         {
            if (reader != null)
            {
               reader.close();
            }
         }
      }
      else
      {
         // kpsewhich failed.

         throw new IOException(String.format(
           "\"kpsewhich '%s'\" failed with exit code %d", arg, exitCode));
      }

      return line;
   }

    /**
     * Print message if in debug mode. Message is printed to STDERR
     * if the debug level is greater than or equal to the given level.
     * @param message Debugging message.
     * @param level Debugging level.
     * @since 1.2
     */
   public void debug(String message, int level)
   {
      if (debugLevel >= level)
      {
         System.err.println(String.format("%s: %s", name, message));
      }
   }

    /**
     * Print message if in debug mode. Message is printed to STDERR
     * if the debug level is greater than 0.
     * @param message Debugging message.
     * @since 1.2
     */
   public void debug(String message)
   {
      debug(message, 1);
   }
    
    /**
     * Print message and exception if in debug mode. Message is printed to
     * STDERR if the debug level is greater than or equal to the given level.
     * The exception may be null. If not null, the exception message
     * is printed.
     * @param message Debugging message.
     * @param excpt Exception.
     * @param msgLevel Debugging level for message.
     * @param traceLevel Debugging level for stack trace.
     * @since 1.2
     */
   public void debug(String message, Throwable excpt, int msgLevel,
      int traceLevel)
   {
      debug(message, msgLevel);

      if (excpt != null)
      {
         debug(excpt.getMessage(), msgLevel);

         if (debugLevel >= traceLevel)
         {
            excpt.printStackTrace();
         }
      }
   }

    /**
     * Print message and exception if in debug mode. The message
     * level is 1 and the trace level is 2.
     * @param message Debugging message.
     * @param excpt Exception.
     * @since 1.2
     */
   public void debug(String message, Throwable excpt)
   {
      debug(message, excpt, 1, 2);
   }

    /**
     * Checks if file is in or below the given directory. This might
     * be easier with java.nio.file.Path etc but that requires Java
     * 1.7, so use the old-fashioned method.
     * @param file The file being checked
     * @param dir The directory being searched
     * @return true if found
     * @since 1.2
     */
   protected boolean isFileInTree(File file, File dir)
    throws IOException
   {
      if (file == null || dir == null) return false;

      file = file.getCanonicalFile();
      dir = dir.getCanonicalFile();

      File parent = file.getParentFile();

      while (parent != null)
      {
         if (parent.equals(dir))
         {
            return true;
         }

         parent = parent.getParentFile();
      }

      return false;
   }

    /**
     * Queries if the given file may be read according to
     * openin_any. Since the user may not require any of the file
     * access functions, the openin variable is only set the first
     * time this method is used to reduce unnecessary overhead.
     * kpsewhich is used to lookup the value of openin_any, which
     * may have one of the following values: a (any), r (restricted,
     * no hidden files) or p (paranoid, as restricted and no parent
     * directories and no absolute paths except under $TEXMFOUTPUT)
     * @param file The file to be checked
     * @return true if read-access allowed
     * @since 1.2
     */
   public boolean isReadPermitted(File file)
   {
      // if file doesn't exist, it can't be read
      if (file == null || !file.exists())
      {
         return false;
      }

      try
      {
         if (openin == OPENIN_UNSET)
         {
            //First time this method has been called. Use kpsewhich
            //to determine the value.

            try
            {
               String result = kpsewhich("-var-value=openin_any");

               if ("a".equals(result))
               {
                  openin=OPENIN_A;
               }
               else if ("r".equals(result))
               {
                  openin=OPENIN_R;
               }
               else if ("p".equals(result))
               {
                  openin=OPENIN_P;
               }
               else
               {
                  // This shouldn't occur, but just in case...
                  debug(String.format("Invalid openin_any value '%s'",
                     result));
                  openin = OPENIN_P;
               }
            }
            catch (Exception e)
            {
               // kpsewhich failed, assume paranoid
               debug("Can't determine openin value, assuming 'p'", e);
               openin = OPENIN_P;
            }

            // Now find TEXMFOUTPUT if set (only need this with the
            // paranoid setting)

            if (openin == OPENIN_P)
            {
               String path = null;

               try
               {
                  path = System.getenv("TEXMFOUTPUT");
               }
               catch (SecurityException e)
               {
                  debug("Can't query TEXMFOUTPUT", e);
               }

               if (path != null && !"".equals(path))
               {
                  texmfoutput = new File(fromTeXPath(path));

                  if (!texmfoutput.exists())
                  {
                     debug(String.format(
                           "TEXMFOUTPUT (%s) doesn't exist, ignoring",
                           texmfoutput.toString()));
                     texmfoutput = null;
                  }
                  else if (!texmfoutput.isDirectory())
                  {
                     debug(String.format(
                           "TEXMFOUTPUT (%s) isn't a directory, ignoring",
                           texmfoutput.toString()));
                     texmfoutput = null;
                  }
                  else if (!texmfoutput.canRead())
                  {
                     debug(String.format(
                           "TEXMFOUTPUT (%s) doesn't have read permission, ignoring",
                           texmfoutput.toString()));
                     texmfoutput = null;
                  }
               }
            }
         }

         // Now check if the given file can be read according to the
         // openin setting.

         switch (openin)
         {
            case OPENIN_A: 
              // any file can be read as long as the OS allows it
               return file.canRead(); 
            case OPENIN_P:
              // paranoid check

              if (isFileInTree(file, texmfoutput))
              {
                 // file under TEXMFOUTPUT, so it's okay as long
                 // as it has read permission
                 return file.canRead();
              }

              // does the file have an absolute path?

              if (file.isAbsolute())
              {
                 debug(String.format(
                   "Read access for file '%s' forbidden by openin_any=%c (has absolute path outside TEXMFOUTPUT)",
                   file, openin));
                 return false;
              }

              // is the file outside the cwd?
              File cwd = new File(getSystemProperty("user.dir", "."));

              if (file.getParentFile() != null && !isFileInTree(file, cwd))
              {
                 // disallow going to parent directories

                 debug(String.format(
                   "Read access for file '%s' forbidden by openin_any=%c (outside cwd path)",
                   file, openin));
                 return false;
              }

            // no break, fall through to restricted check
            case OPENIN_R:

              if (file.isHidden())
              {
                 // hidden file so not permitted
                 debug(String.format(
                   "Read access for file '%s' forbidden by openin_any=%c (hidden file)",
                   file, openin));
                 return false;
              }

            break;
            default:
              // this shouldn't happen, but just in case...
              debug(String.format("Invalid openin value %d", openin));
              // don't allow, something's gone badly wrong
              return false;
         }

         // return read access
         return file.canRead();
      }
      catch (Exception e)
      {
         // Catch all exceptions
         debug(String.format("isReadPermitted(%s) failed", file), e);

         // Can't permit read if something's gone wrong here.
         return false;
      }
   }

    /**
     * Gets the given system property or the default value.
     * Returns empty if the property isn't set or can't be accessed.
     * @param propName The property name
     * @param defValue The default value
     * @return The property value or the default if unavailable
     * @since 1.2
     */
   public String getSystemProperty(String propName, String defValue)
   {
      try
      {
         return System.getProperty(propName, defValue);
      }
      catch (SecurityException e)
      {
         // The security manager doesn't permit access to this property.

         debug(String.format("unable to access '%s' property", propName), e);
         return defValue;
      }
   }

    /**
     * Escapes problematic characters from string.
     * Except for the hash, TeX's special characters shouldn't need escaping.
     * The definition of \\TeXOSQuery in texosquery.tex changes the category
     * code for the standard special characters (and a few others) except 
     * hash, curly braces and backslash. 
     * 
     * Some of the methods in this class return TeX code. Those
     * returned values shouldn't be escaped as it would interfere
     * with the code, so just use this method on information
     * directly obtained from Java.
     *
     * \\TeXOSQuery locally defines \\bks (literal backslash), 
     * \\lbr (literal left brace), \\rbr (literal right brace), 
     * \\hsh (literal hash), \\grv (literal grave), \\lspc (literal
     * space, catcode 12), \\spc (regular space), \\csq (close single quote),
     * \\dqt (double quote) and \\osq (open single quote).
     *
     * This should take care of any insane file-naming schemes, such
     * as "<tt>bad file name#1.tex</tt>", "<tt>stupid {file} name.tex</tt>",
     * "<tt>spaced    out  file #2.tex</tt>", "<tt>file's stupid name.tex</tt>"
     *
     * The regular space \\spc guards against a space occurring after
     * a character that needs to be converted to a control sequence.
     *
     * To help protect against input encoding problems, non-ASCII
     * characters are wrapped in \\wrp. \\TeXOSQuery locally redefines
     * this to \\@texosquery@nonascii@wrap which may be used to
     * provide some protection or conversion, if required.
     *
     * @param string Input string.
     * @param isRegularText true if the string represents text (for example, 
     * month names), set to false if string is something literal,
     * such as a file name.
     * @return The processed string
     * @since 1.2
     */
   public String escapeSpChars(String string, boolean isRegularText)
   {
      if (compatible < 2)
      {
         return escapeHash(string);
      }

      StringBuilder builder = new StringBuilder();

      // This iterates over Unicode characters so we can't use a simple
      // i++ increment. The offset is obtained from Character.charCount
      for (int i = 0, n = string.length(); i < n; )
      {
         int codepoint = string.codePointAt(i);
         i += Character.charCount(codepoint);

         builder.append(escapeSpChars(codepoint, isRegularText));
      }

      return builder.toString();
   }

    /**
     * Escapes file name. This should already have had the directory
     * divider changed to a forward slash where necessary.
     * @param filename Input string.
     * @return String with characters escaped.
     * @since 1.2
     */
   public String escapeFileName(String filename)
   {
      return escapeSpChars(filename, false);
   }

    /**
     * Escapes regular text.
     * @param string Input string.
     * @return String with characters escaped.
     * @since 1.2
     */
   public String escapeText(String string)
   {
      return escapeSpChars(string, true);
   }

    /**
     * Escapes regular text.
     * @param codepoint Input Unicode character.
     * @return String with characters escaped.
     * @since 1.2
     */
   public String escapeText(int codepoint)
   {
      return escapeSpChars(codepoint, true);
   }

    /**
     * Escapes character include punctuation.
     * \\TeXOSQuery sets the catcode to 12 for the following:
     * <pre>- _ ^ ~ $ &amp; . / : " ' ; %</pre>
     * so we don't need to worry about them when processing 
     * file names, but some of them will need conversion for regular
     * text.
     * @param codePoint Input code point.
     * @param isRegularText true if the character is in a string representing
     * text, set to false if string is a file name etc
     * @return String with character escaped.
     * @since 1.2
     */
   public String escapeSpChars(int codepoint, boolean isRegularText)
   {
      switch (codepoint)
      {
         case '\\': return "\\bks ";
         case '{': return "\\lbr ";
         case '}': return "\\rbr ";
         case '#': return isRegularText ? "\\#" : "\\hsh ";
         case '_': return isRegularText ? "\\_" : "_";
         case '\'': return isRegularText ? "\\csq " : "'";
         case '`': return isRegularText ? "\\osq " : "\\grv ";
         case '"': return isRegularText ? "\\dqt " : "\"";
         case ' ': return isRegularText ? "\\spc " : "\\lspc ";
         default:

           if (codepoint >= 32 && codepoint <= 126)
           {
              return String.format("%c", codepoint);
           }
           else
           {
              return String.format("\\wrp{%c}", codepoint);
           }
      }
   }

    /**
     * Escapes any hashes in input string.
     * Now only used if compatibility level is less than 2 (pre
     * texosquery version 1.2).
     * @param string Input string.
     * @return String with hash escaped.
     */
   public static String escapeHash(String string)
   {
      return string.replaceAll("#", "\\\\#");
   }

    /**
     * Escapes hash from input character.
     * @param c Input character.
     * @return String with hash escaped.
     */
   public static String escapeHash(char c)
   {
      return String.format("%s", c == '#' ? "\\#" : c);
   }

    /**
     * Gets the OS name. As far as I can tell, the "os.name"
     * property should return a string that just contains Basic
     * Latin upper or lower case letters, so we don't need to worry
     * about special characters.
     * @return The OS name as string.
     */
   public String getOSname()
   {
      return getSystemProperty("os.name", "");
   }

    /**
     * Gets the OS architecture. As with the OS name, this shouldn't
     * contain any special characters.
     * @return The OS architecture as string.
     */
   public String getOSarch()
   {
      return getSystemProperty("os.arch", "");
   }

    /**
     * Gets the OS version. This may contain an underscore, but we
     * don't need to worry about that as \\TeXOSQuery changes the
     * category code for <tt>_</tt> before parsing the result of texosquery.
     * @return The OS version as string.
     */
   public String getOSversion()
   {
      return getSystemProperty("os.version", "");
   }

    /**
     * Converts the filename string to TeX path. Since this is designed to work
     * within TeX, backslashes in paths need to be replaced with forward
     * slashes.
     * @param filename The filename string.
     * @return TeX path.
     */
   public String toTeXPath(String filename)
   {
      if (filename == null)
      {
         // This shouldn't happen, but just in case...
         try
         {
            // throw so we can get a stack trace for debugging
            throw new NullPointerException();
         }
         catch (NullPointerException e)
         {
            debug("null file name passed to toTeXPath()", e);
         }

         return "";
      }

      // If the OS uses backslash as the directory divider,
      // convert all backslashes to forward slashes. The Java regex
      // means that we need four backslashes to represent a single literal
      // backslash.

      if (File.separatorChar == BACKSLASH)
      {
         filename = filename.replaceAll("\\\\", "/");
      }

      return escapeFileName(filename);
   }

    /**
     * Converts the TeX path back to the OS representation.
     * @param filename The filename string.
     * @return The OS representation.
     */
   public String fromTeXPath(String filename)
   {
      if (filename == null)
      {
         // This shouldn't happen, but just in case...
         try
         {
            throw new NullPointerException();
         }
         catch (NullPointerException e)
         {
            debug("null file name passed to fromTeXPath()", e);
         }

         return "";
      }

      // Unescape hash
      filename = filename.replaceAll("\\#", "#");

      // If the OS uses backslash as the directory divider,
      // convert all forward slashes to backslashes. Again we need
      // four backslashes to represent a single literal backslash.

      if (File.separatorChar == BACKSLASH)
      {
         filename = filename.replaceAll("/", "\\\\");
      }

      return filename;
   }

    /**
     * Gets a file representation from a filename string. If the
     * provided file doesn't have a parent and if it's not found in the
     * current directory, kpsewhich will be used to locate it on
     * TeX's path. The provided file name is assumed to have been
     * passed through commands provided by texosquery.tex so the
     * directory dividers should be forward slashes, even if the OS
     * uses backslashes. The returned file may not exist. Any method
     * that uses this method needs to check for existence.
     * @param filename Filename string.
     * @return File representation 
     * @since 1.2
     */
   public File fileFromTeXPath(String filename)
   {
      // Convert from TeX to the OS path representation.
      filename = fromTeXPath(filename);

      File file = new File(filename);

      if (!file.exists() && file.getParent() == null)
      {
         // If the file doesn't exist and it doesn't have a parent
         // directory, use kpsewhich to find it.

         try
         {
            String result = kpsewhich(filename);

            if (result != null && !"".equals(result))
            {
               file = new File(fromTeXPath(result));
            }
         }
         catch (Exception exception)
         {
            // Catch all exceptions
            debug(String.format("kpsewhich couldn't find the file '%s'",
                                filename),
                  exception);

            // The File object will be returned even though the file
            // can't be found.
         }
      }

      return file;
   }

    /**
     * Gets the user's home directory.
     * @return The user home as string.
     */
   public String getUserHome()
   {
      File dir = new File(getSystemProperty("user.home", ""));

      if (!isReadPermitted(dir))
      {
         debug("read access not permitted for the home directory");
         return "";
      }

      // The resulting path needs to be converted to a TeX path.
      return toTeXPath(dir.getAbsolutePath());
   }

    /**
     * Gets the current working directory.
     * @return The current working directory.
     */
   public String getCwd()
   {
      File dir = new File(getSystemProperty("user.dir", "."));

      if (!isReadPermitted(dir))
      {
         // perhaps the current directory is hidden?
         debug("read access not permitted for the current directory");
         return "";
      }

      // The resulting path needs to be converted to a TeX path.
      return toTeXPath(dir.getAbsolutePath());
   }

    /**
     * Gets the temporary directory.
     * @return Temporary directory.
     */
   public String getTmpDir()
   {
      String filename = getSystemProperty("java.io.tmpdir", "");

      if ("".equals(filename))
      {
         // Not set
         return "";
      }

      File dir = new File(filename);

      if (!isReadPermitted(dir))
      {
         debug(String.format("read access not permitted for '%s'", dir));
         return "";
      }

      // The resulting path needs to be converted to a TeX path.
      return toTeXPath(filename);
   }

   /**
    * Gets the week year for the given calendar.
    * Calendar.getWeekYear() was added to Java 7, so this defaults
    * to the year instead. This method needs to be overridden in
    * TeXOSQueryJRE7 and TeXOSQueryJRE8.
    * @return The week year
    * @since 1.2
    */ 
   public int getWeekYear(Calendar cal)
   {
      return cal.get(Calendar.YEAR);
   }

   /**
    * Gets all the date-time data for the current date-time. 
    * @return data in format that can be read by \\texosqueryfmtdatetime
    * @since 1.2
    */ 
   public String getDateTimeData()
   {
      Calendar cal = Calendar.getInstance();
      cal.setTimeInMillis(now.getTime());

      int hourH = cal.get(Calendar.HOUR_OF_DAY);

      int hourk = (hourH == 0 ? 24 : hourH);

      int hourK = cal.get(Calendar.HOUR);

      int hourh = (hourK == 0 ? 12 : hourK);

      TimeZone timeZone = cal.getTimeZone();
      boolean isDaylightSaving = timeZone.inDaylightTime(now);

      int timezoneoffset = cal.get(Calendar.ZONE_OFFSET);

      if (isDaylightSaving)
      {
         timezoneoffset += cal.get(Calendar.DST_OFFSET);
      }

      // convert from offset millisec to hours and minutes
      // (ignore left-over seconds and milliseconds)

      int tzm = timezoneoffset/60000;

      int tzh = tzm/60;

      tzm = tzm % 60;

      return String.format(
       "{%d}{%d}{%d}{%d}{%d}{%d}{%d}{%d}{%d}{%d}{%d}{%d}{%d}{%d}{%d}{%d}{%d}{%d}{{%d}{%d}{%s}{%d}}",
       cal.get(Calendar.ERA),
       cal.get(Calendar.YEAR),
       getWeekYear(cal),
       cal.get(Calendar.MONTH)+1,
       cal.get(Calendar.WEEK_OF_YEAR),
       cal.get(Calendar.WEEK_OF_MONTH),
       cal.get(Calendar.DAY_OF_YEAR),
       cal.get(Calendar.DAY_OF_MONTH),
       cal.get(Calendar.DAY_OF_WEEK_IN_MONTH),
       cal.get(Calendar.DAY_OF_WEEK),
       cal.get(Calendar.AM_PM),
       hourH, hourk, hourK, hourh,
       cal.get(Calendar.MINUTE),
       cal.get(Calendar.SECOND),
       cal.get(Calendar.MILLISECOND),
       tzh, tzm, timeZone.getID(), 
       isDaylightSaving ? 1 : 0);
   }

   /**
    * Get the time zone names for the given locale.
    * The data for each zone is provided in the form
    * {id}{short name}{long name}{short dst name}\marg{long dst name}
    * @param localeTag The locale 
    * @return list of zone information for the locale
    * @since 1.2
    */ 

   public String getTimeZones(String localeTag)
   {
      Locale locale;

      if (localeTag == null || "".equals(localeTag))
      {
         locale = Locale.getDefault();
      }
      else
      {
         locale = getLocale(localeTag);
      }

      StringBuilder builder = new StringBuilder();

      String[] zones = TimeZone.getAvailableIDs();

      for (int i = 0; i < zones.length; i++)
      {
         TimeZone tz = TimeZone.getTimeZone(zones[i]);

         builder.append(String.format("{{%s}{%s}{%s}{%s}{%s}}",
          escapeFileName(tz.getID()), 
          escapeText(tz.getDisplayName(false, TimeZone.SHORT, locale)),
          escapeText(tz.getDisplayName(false, TimeZone.LONG, locale)),
          escapeText(tz.getDisplayName(true, TimeZone.SHORT, locale)),
          escapeText(tz.getDisplayName(true, TimeZone.LONG, locale))));
      }

      return builder.toString();
   }

    /**
     * Gets the current date in PDF format. (The same format as
     * \pdfcreationdate.)
     * @return The current date.
     */
   public String pdfnow()
   {
      Calendar cal = Calendar.getInstance();
      cal.setTimeInMillis(now.getTime());

      return pdfDate(cal);
   }

    /**
     * Gets the date in PDF format.
     * @param calendar A calendar object.
     * @return Date in PDF format.
     */
   public String pdfDate(Calendar calendar)
   {
       String tz = String.format("%1$tz", calendar);

       // Need to ensure "D" has category code 12

       return String.format(
               "%s:%2$tY%2$tm%2td%2$tH%2$tM%2$tS%3$s'%4$s'",
               compatible < 2 ? "D" : "\\pdfd ",
               calendar,
               tz.substring(0, 3),
               tz.substring(3)
       );
   }

   /**
    * Gets the date of a file in PDF format.
    * @param file File.
    * @return The date in PDF format.
    */
   public String pdfDate(File file)
   {
      try
      {
         if (!file.exists())
         {
            debug(String.format(
                 "Unable to get timestamp for file '%s' (no such file)",
                 file.toString()));
            return "";
         }

         if (!isReadPermitted(file))
         {
            debug(String.format("No read access for '%s'", file));
            return "";
         }
        
         long millisecs = file.lastModified();
            
         if (millisecs > ZERO)
         {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(millisecs);

            return pdfDate(calendar);
         }

         // I/O error has occurred (already checked for file
         // existence and read permission).
         debug(String.format(
               "Unable to get timestamp for file '%s' (I/O error)",
               file.toString()));
      }
      catch (Exception exception)
      {
         // Catch all possible exceptions, including security
         // exception.

         debug(String.format(
              "Unable to get timestamp for file '%s'",
              file.toString()),
              exception);
      }

      // Unsuccessful
      return "";
   }

    /**
     * Gets the file length in bytes.
     * @param file The file.
     * @return The length as a string.
     */
   public String getFileLength(File file)
   {
      try
      {
         if (!file.exists())
         {
            debug(String.format(
              "Unable to get the size of file '%s' (no such file)",
              file.toString()));
            return "";
         }
        
         if (!isReadPermitted(file))
         {
            debug(String.format("No read access for '%s'", file));
            return "";
         }
        
         long length = file.length();

         if (length > ZERO)
         {
            return String.format("%d", length);
         }

         // I/O error has occurred (already checked for file
         // existence and read access).
         debug(String.format(
               "Unable to get the size of file '%s' (I/O error)",
               file.toString()));
      }
      catch (Exception exception)
      {
         // Catch all possible exceptions, including security
         // exceptions.

         debug(String.format("Unable to get the size of file '%s'",
               file.toString()),
               exception);
      }

      // Unsuccessful
      return "";
   }

    /**
     * Gets the list of files from a directory. This uses
     * getFilterFileList to filter out files prohibited by the
     * openin_any setting.
     * @param separator Separator.
     * @param directory Directory.
     * @return List as a string.
     */
   public String getFileList(String separator, File directory)
   {
      return getFilterFileList(separator, ".*", directory);
   }

    /**
     * Gets a filtered list of files from directory.
     * This is pretty much the same as above but filters the file
     * list.
     * @param separator Separator.
     * @param regex Regular expression.
     * @param directory Directory.
     * @return Filtered list as string.
     */
   public String getFilterFileList(String separator,
            final String regex, File directory)
   {
      if (directory == null)
      {
         // shouldn't happen, but just in case...

         debug("Unable to list contents (null directory)");
         return "";
      }

      if (!directory.exists())
      {
         debug(String.format(
               "Unable to list contents of '%s' (no such directory)",
               directory.toString()));
         return "";
      }

      if (!directory.isDirectory())
      {
         debug(String.format(
               "Unable to list contents of '%s' (not a directory)",
               directory.toString()));
         return "";
      }

      if (!isReadPermitted(directory))
      {
         debug(String.format("No read access for '%s'", directory));
         return "";
      }
        
      if ((regex == null) || ("".equals(regex)))
      {
         // null or empty regular expression forbidden
         debug("Null or empty regular expression in getFilterFileList");
         return "";
      }

      StringBuilder builder = new StringBuilder();
        
      try
      {
         String[] list = directory.list(
            new FilenameFilter()
            {
               @Override
               public boolean accept(File dir, String name)
               {
                  File file = new File(dir, name);
 
                  if (!isReadPermitted(file))
                  {
                     debug(String.format("No read access for '%s'", file));
                     return false;
                  }

                  return name.matches(regex);
               }
            });

         if (list != null)
         {
            for (int i = 0; i < list.length; i++)
            {
               if (i > 0)
               {
                  builder.append(separator);
               }
                            
               if (list[i].contains(separator))
               {
                  builder.append(String.format("{%s}", escapeFileName(list[i])));
               }
               else
               {
                  builder.append(escapeFileName(list[i]));
               }
            }
                        
         }

         return builder.toString();
      }
      catch (Exception exception)
      {
         // Catch all possible exceptions
         debug(String.format("Unable to list contents of '%s' using regex '%s'",
               directory.toString(), regex),
               exception);
      }

      // Unsuccessful
      return "";
   }

    /**
     * Gets the file URI. This may contain % characters, but
     * \TeXOSQuery changes the category code before parsing the
     * result. We shouldn't have to worry about any other special
     * characters as they should be %-encoded.
     * @param file The file.
     * @return The URI.
     */
   public String fileURI(File file)
   {
      if (file == null)
      {
         // This shouldn't happen, but just in case...
         debug("null file passed to fileURI");
         return "";
      }

      if (!file.exists())
      {
         debug(String.format("can't obtain URI of file '%s' (no such file)",
            file.toString()));
         return "";
      }
        
      if (!isReadPermitted(file))
      {
         debug(String.format("No read access for '%s'", file));
         return "";
      }
        
      try
      {
         return file.toURI().toString();
      }
      catch (Exception exception)
      {
         debug(String.format("can't obtain URI of file '%s'", file.toString()),
          exception);
      }

      // Unsuccessful
      return "";
    }

    /**
     * Gets the full TeX file path name from File object.
     * @param file The file.
     * @return The path.
     */
   public String filePath(File file)
   {
      if (file == null)
      {
         // This shouldn't happen, but just in case...
         debug("null file passed to filePath");
         return "";
      }

      if (!file.exists())
      {
         debug(String.format(
           "can't obtain full file path for '%s' (no such file)",
           file.toString()));
         return "";
      }

      if (!isReadPermitted(file))
      {
          debug(String.format(
            "can't obtain full file path for '%s' (no read access)",
            file.toString()));
          return "";
      }

      try
      {
         return toTeXPath(file.getCanonicalPath());
      }
      catch (Exception exception)
      {
         debug(String.format(
           "can't obtain full file path for '%s'", file.toString()),
            exception);
      }

      // Unsuccessful
      return "";
    }

    /**
     * Gets the path for the file's parent.
     * @param file The file.
     * @return The path.
     * @since 1.1
     */
   public String parentPath(File file)
   {
      if (file == null)
      {
         // This shouldn't happen, but just in case...
         debug("null file passed to filePath");
         return "";
      }

      if (!file.exists())
      {
         debug(String.format(
           "can't obtain full file path for parent of '%s' (no such file)",
           file.toString()));
         return "";
      }

      if (!isReadPermitted(file))
      {
          debug(String.format(
            "can't obtain full file path for '%s' (no read access)",
            file.toString()));
          return "";
      }

      try
      {
         File parent = file.getCanonicalFile().getParentFile();

         if (parent == null)
         {
            // No parent? If getCanonicalFile fails it throws
            // exception, so no parent would presumably mean the
            // root directory. Not sure we should allow that so
            // return empty.
            return "";
         }

         return toTeXPath(parent.getAbsolutePath());

      } 
      catch (Exception exception)
      {
         debug(String.format(
           "can't obtain full file path for parent of '%s'", file.toString()),
           exception);
      }

      // Unsuccessful
      return "";
   }

   /**
    * Gets the script for the given locale. Java only introduced
    * support for language scripts in version 1.7, so this returns
    * null here. The Java 7 and 8 support needs to override this method.
    * @param locale The locale
    * @return The language script associated with the given locale or 
    * null if not available
    * @since 1.2
    */ 
   public String getScript(Locale locale)
   {
      return null;
   }

   /**
    * Gets the language tag for the given locale.
    * @param locale The locale or null for the default locale
    * @return The language tag
    * @since 1.2
    */ 
   public String getLanguageTag(Locale locale)
   {
      if (locale == null)
      {
         locale = Locale.getDefault();
      }

      String tag = locale.getLanguage();

      String country = locale.getCountry();

      if (country != null && !"".equals(country))
      {
         tag = String.format("%s-%s", tag, country);
      }

      String variant = locale.getVariant();

      if (variant != null && !"".equals(variant))
      {
         tag = String.format("%s-%s", tag, variant);
      }

      return tag;
   }

    /**
     * Gets a string representation of the provided locale.
     * @param locale The provided locale.
     * @return String representation.
     */
   public String getLocale(Locale locale)
   {
      return getLocale(locale, false);
   }

    /**
     * Gets a POSIX representation of the provided locale, converting the code
     * set if possible. If the boolean argument is true, this
     * attempts to convert the code set to a identifier that stands
     * a better chance of being recognised by inputenc.sty. For
     * example, UTF-8 will be converted to utf8. None of TeX's
     * special characters should occur in any of the locale
     * information.
     * @param locale The provided locale.
     * @param convertCodeset Boolean value to convert the code set.
     * @return String representation.
     */
   public String getLocale(Locale locale, boolean convertCodeset)
   {
      String identifier = "";

      if (locale == null)
      {
         // No locale provided, return empty string
         debug("null locale");
         return "";
      }

      String language = locale.getLanguage();

      if (language == null)
      {
          // No language provided for the locale. The language
          // part will be omitted from the returned string.
         debug("locale has no language", 3);
      }
      else
      {
         identifier = language;
      }

      String country = locale.getCountry();

      if (country == null || "".equals(country))
      {
         // No country is associated with the locale. The
         // country part will be omitted from the returned
         // string.
         debug("locale has no region", 3);
      }
      else
      {
         if ("".equals(identifier))
         {
            // The identifier hasn't been set (no language
            // provided), so just set it to the country code.
            identifier = country;
         }
         else
         {
            // Append the country code to the identifier.
            identifier = identifier.concat("-").concat(country);
         }
      }

      String codeset = getCodeSet(convertCodeset);

      identifier = identifier.concat(".").concat(codeset);

      // Find the script if available. This is used as the modifier part
      // but it's better to use a language tag if the script is
      // needed.

      String script = getScript(locale);

      if (script == null || "".equals(script))
      {
         // Script information is missing. Ignore it.
         debug("no script available for locale", 3);
      }
      else
      {
         // Append the script. This will be a four letter string 
         // (if it's not empty).
         identifier = identifier.concat("@").concat(script);
      }

      return identifier;
   }

   /**
    * Gets default file encoding.
    * @param convertCodeset If true convert codeset to fit
    * inputenc.sty
    * @return the file encoding.
    * @since 1.2
    */ 
   public String getCodeSet(boolean convertCodeset)
   {
      // Get the OS default file encoding or "UTF-8" if not set.

      String codeset = getSystemProperty("file.encoding", "UTF-8");

      // The codeset should not be null here as a default has
      // been provided if the property is missing.

      if (convertCodeset)
      {
         // If conversion is required, change to lower case
         // and remove any hyphens.
         codeset = codeset.toLowerCase().replaceAll("-", "");
      }

      return codeset;
   }

   /**
    * Gets the two-letter alpha region code from the numeric code.
    * (Java doesn't seem to recognise the numeric codes.)
    * @param ISO 3166-1 numeric code
    * @return ISO 3166-1 alpha code
    * @since 1.2
    */ 
   public String getRegionAlpha2Code(int code)
   {
      switch (code)
      {
         case 4: return "AF";
         case 8: return "AL";
         case 10: return "AQ";
         case 12: return "DZ";
         case 16: return "AS";
         case 20: return "AD";
         case 24: return "AO";
         case 28: return "AG";
         case 31: return "AZ";
         case 32: return "AR";
         case 36: return "AU";
         case 40: return "AT";
         case 44: return "BS";
         case 48: return "BH";
         case 50: return "BD";
         case 51: return "AM";
         case 52: return "BB";
         case 56: return "BE";
         case 60: return "BM";
         case 64: return "BT";
         case 68: return "BO";
         case 70: return "BA";
         case 72: return "BW";
         case 74: return "BV";
         case 76: return "BR";
         case 84: return "BZ";
         case 86: return "IO";
         case 90: return "SB";
         case 92: return "VG";
         case 96: return "BN";
         case 100: return "BG";
         case 104: return "MM";
         case 108: return "BI";
         case 112: return "BY";
         case 116: return "KH";
         case 120: return "CM";
         case 124: return "CA";
         case 132: return "CV";
         case 136: return "KY";
         case 140: return "CF";
         case 144: return "LK";
         case 148: return "TD";
         case 152: return "CL";
         case 156: return "CN";
         case 158: return "TW";
         case 162: return "CX";
         case 166: return "CC";
         case 170: return "CO";
         case 174: return "KM";
         case 175: return "YT";
         case 178: return "CG";
         case 180: return "CD";
         case 184: return "CK";
         case 188: return "CR";
         case 191: return "HR";
         case 192: return "CU";
         case 196: return "CY";
         case 203: return "CZ";
         case 204: return "BJ";
         case 208: return "DK";
         case 212: return "DM";
         case 214: return "DO";
         case 218: return "EC";
         case 222: return "SV";
         case 226: return "GQ";
         case 231: return "ET";
         case 232: return "ER";
         case 233: return "EE";
         case 234: return "FO";
         case 238: return "FK";
         case 239: return "GS";
         case 242: return "FJ";
         case 246: return "FI";
         case 248: return "AX";
         case 250: return "FR";
         case 254: return "GF";
         case 258: return "PF";
         case 260: return "TF";
         case 262: return "DJ";
         case 266: return "GA";
         case 268: return "GE";
         case 270: return "GM";
         case 275: return "PS";
         case 276: return "DE";
         case 288: return "GH";
         case 292: return "GI";
         case 296: return "KI";
         case 300: return "GR";
         case 304: return "GL";
         case 308: return "GD";
         case 312: return "GP";
         case 316: return "GU";
         case 320: return "GT";
         case 324: return "GN";
         case 328: return "GY";
         case 332: return "HT";
         case 334: return "HM";
         case 336: return "VA";
         case 340: return "HN";
         case 344: return "HK";
         case 348: return "HU";
         case 352: return "IS";
         case 356: return "IN";
         case 360: return "ID";
         case 364: return "IR";
         case 368: return "IQ";
         case 372: return "IE";
         case 376: return "IL";
         case 380: return "IT";
         case 384: return "CI";
         case 388: return "JM";
         case 392: return "JP";
         case 398: return "KZ";
         case 400: return "JO";
         case 404: return "KE";
         case 408: return "KP";
         case 410: return "KR";
         case 414: return "KW";
         case 417: return "KG";
         case 418: return "LA";
         case 422: return "LB";
         case 426: return "LS";
         case 428: return "LV";
         case 430: return "LR";
         case 434: return "LY";
         case 438: return "LI";
         case 440: return "LT";
         case 442: return "LU";
         case 446: return "MO";
         case 450: return "MG";
         case 454: return "MW";
         case 458: return "MY";
         case 462: return "MV";
         case 466: return "ML";
         case 470: return "MT";
         case 474: return "MQ";
         case 478: return "MR";
         case 480: return "MU";
         case 484: return "MX";
         case 492: return "MC";
         case 496: return "MN";
         case 498: return "MD";
         case 499: return "ME";
         case 500: return "MS";
         case 504: return "MA";
         case 508: return "MZ";
         case 512: return "OM";
         case 516: return "NA";
         case 520: return "NR";
         case 524: return "NP";
         case 528: return "NL";
         case 531: return "CW";
         case 533: return "AW";
         case 534: return "SX";
         case 535: return "BQ";
         case 540: return "NC";
         case 548: return "VU";
         case 554: return "NZ";
         case 558: return "NI";
         case 562: return "NE";
         case 566: return "NG";
         case 570: return "NU";
         case 574: return "NF";
         case 578: return "NO";
         case 580: return "MP";
         case 581: return "UM";
         case 583: return "FM";
         case 584: return "MH";
         case 585: return "PW";
         case 586: return "PK";
         case 591: return "PA";
         case 598: return "PG";
         case 600: return "PY";
         case 604: return "PE";
         case 608: return "PH";
         case 612: return "PN";
         case 616: return "PL";
         case 620: return "PT";
         case 624: return "GW";
         case 626: return "TL";
         case 630: return "PR";
         case 634: return "QA";
         case 638: return "RE";
         case 642: return "RO";
         case 643: return "RU";
         case 646: return "RW";
         case 652: return "BL";
         case 654: return "SH";
         case 659: return "KN";
         case 660: return "AI";
         case 662: return "LC";
         case 663: return "MF";
         case 666: return "PM";
         case 670: return "VC";
         case 674: return "SM";
         case 678: return "ST";
         case 682: return "SA";
         case 686: return "SN";
         case 688: return "RS";
         case 690: return "SC";
         case 694: return "SL";
         case 702: return "SG";
         case 703: return "SK";
         case 704: return "VN";
         case 705: return "SI";
         case 706: return "SO";
         case 710: return "ZA";
         case 716: return "ZW";
         case 724: return "ES";
         case 728: return "SS";
         case 729: return "SD";
         case 732: return "EH";
         case 740: return "SR";
         case 744: return "SJ";
         case 748: return "SZ";
         case 752: return "SE";
         case 756: return "CH";
         case 760: return "SY";
         case 762: return "TJ";
         case 764: return "TH";
         case 768: return "TG";
         case 772: return "TK";
         case 776: return "TO";
         case 780: return "TT";
         case 784: return "AE";
         case 788: return "TN";
         case 792: return "TR";
         case 795: return "TM";
         case 796: return "TC";
         case 798: return "TV";
         case 800: return "UG";
         case 804: return "UA";
         case 807: return "MK";
         case 818: return "EG";
         case 826: return "GB";
         case 831: return "GG";
         case 832: return "JE";
         case 833: return "IM";
         case 834: return "TZ";
         case 840: return "US";
         case 850: return "VI";
         case 854: return "BF";
         case 858: return "UY";
         case 860: return "UZ";
         case 862: return "VE";
         case 876: return "WF";
         case 882: return "WS";
         case 887: return "YE";
         case 894: return "ZM";
      }

      // not recognised, return the code as a string
      return String.format("%d", code);
   }

   /**
    * Gets the locale from the given language tag. Since Java didn't
    * support BCP47 language tags until v1.7, we can't use
    * Locale.forLanguageTag(String) here. (The Java 7 and 8 support
    * will need to override this method.) Only parse for language
    * code, country code and variant. Grandfathered, irregular and private
    * tags not supported.
    * @param languageTag The language tag
    * @return The locale that closest matches the language tag
    * @since 1.2
    */ 
   public Locale getLocale(String languageTag)
   {
      // The BCP47 syntax is described in 
      // https://tools.ietf.org/html/bcp47#section-2.1
      // This is a match for a subset of the regular syntax.
      // Only the language tag, the region and the variant are
      // captured.
      // Note: named capturing groups was introduced in Java 7, so we
      // can't use them here.
      Pattern p = Pattern.compile(
        "(?:([a-z]{2,3}(?:-[a-z]{2,3})*))+(?:-[A-Z][a-z]{3})?(?:-([A-Z]{2}|[0-9]{3}))?(?:-([a-zA-Z0-9]{5,8}|[0-9][a-zA-Z0-9]{3}))?(?:-.)*");

      Matcher m = p.matcher(languageTag);

      if (m.matches())
      {
         String language = m.group(1);
         String region = m.group(2);
         String variant = m.group(3);

         try
         {
            region = getRegionAlpha2Code(Integer.parseInt(region));
         }
         catch (NumberFormatException e)
         {
            // ignore, alpha region code was supplied
         }

         // Language won't be null as the pattern requires it, but
         // the region and variant might be.

         if (region == null)
         {
            // There isn't a Locale constructor that allows a
            // variant without a region, so don't bother checking
            // variant for null here.

            return new Locale(language);
         }

         if (variant == null)
         {
            return new Locale(language, region);
         }

         return new Locale(language, region, variant);
      }

      debug(String.format("Can't parse language tag '%s'", languageTag));

      // strip anything to a hyphen and try that
      String[] split = languageTag.split("-", 1);

      return new Locale(split[0]);
   }

   /**
    * Gets all numerical information for the given locale. If the
    * given locale tag is null or empty, the default locale is used. The
    * information is returned with each item grouped to make it
    * easier to parse in TeX. This is an abridged version of
    * getLocaleData().
    * @param localeTag the tag identifying the locale or null for
    * the default locale
    * @return locale numerical information: language tag, 
    * number group separator, decimal separator, exponent separator,
    * grouping conditional (1 if locale uses number grouping
    * otherwise 0),
    * currency code (e.g. GBP), regional currency identifier (e.g. IMP),
    * currency symbol (e.g. \\wrp{£}), currency TeX code (e.g.
    * \\texosquerycurrency{pound}), monetary decimal separator.
    * @since 1.2
    */
   public String getNumericalInfo(String localeTag)
   {
       Locale locale;

       if (localeTag == null || "".equals(localeTag))
       {
          locale = Locale.getDefault();
       }
       else
       {
          locale = getLocale(localeTag);
       }

       DecimalFormatSymbols fmtSyms 
               = DecimalFormatSymbols.getInstance(locale);

       // ISO 4217 code
       String currencyCode = fmtSyms.getInternationalCurrencySymbol();

       // Currency symbol
       String currency = fmtSyms.getCurrencySymbol();

       // Check for known unofficial currency codes

       String localeCurrencyCode = currencyCode;

       String countryCode = locale.getCountry();

       if (countryCode != null && !"".equals(countryCode))
       {
          if (countryCode.equals("GG") || countryCode.equals("GGY")
           || countryCode.equals("831"))
          {// Guernsey
             localeCurrencyCode = "GGP";
             currency = "£";
          }
          else if (countryCode.equals("JE") || countryCode.equals("JEY")
           || countryCode.equals("832"))
          {// Jersey
             localeCurrencyCode = "JEP";
             currency = "£";
          }
          else if (countryCode.equals("IM") || countryCode.equals("IMN")
           || countryCode.equals("833"))
          {// Isle of Man
             localeCurrencyCode = "IMP";
             currency = "M£";
          }
          else if (countryCode.equals("KI") || countryCode.equals("KIR")
           || countryCode.equals("296"))
          {// Kiribati
             localeCurrencyCode = "KID";
             currency = "$";
          }
          else if (countryCode.equals("TV") || countryCode.equals("TUV")
           || countryCode.equals("798"))
          {// Tuvaluan
             localeCurrencyCode = "TVD";
             currency = "$";
          }
          // Transnistrian ruble omitted as it conflicts with ISO
          // 4217 so omitted. There's also no country code for
          // Transnistria. Other currencies don't have an associated
          // region code (for example, Somaliland) or don't have an
          // known unofficial currency (for example, Alderney).
          // code.
       }

       // Convert known Unicode currency symbols to commands that
       // may be redefined in TeX

       String texCurrency = getTeXCurrency(currency);

       NumberFormat numFormat = NumberFormat.getNumberInstance(locale);

       return String.format(
         "{%s}{%s}{%s}{%s}{%d}{%s}{%s}{%s}{%s}{%s}",
             getLanguageTag(locale),
             escapeText(fmtSyms.getGroupingSeparator()),
             escapeText(fmtSyms.getDecimalSeparator()),
             escapeText(fmtSyms.getExponentSeparator()), 
             numFormat.isGroupingUsed() ? 1 : 0,
             escapeText(currencyCode),
             escapeText(localeCurrencyCode),
             escapeText(currency),
             texCurrency,// already escaped
             escapeText(fmtSyms.getMonetaryDecimalSeparator()));
   }

   /**
    * Gets the currency with known symbols replaced by TeX commands
    * provided by texosquery.tex.
    * @param currency The original currency string 
    * @return The TeX version
    * @since 1.2
    */ 
   public String getTeXCurrency(String currency)
   {
      StringBuilder builder = new StringBuilder();

      for (int i = 0, n = currency.length(); i < n; )
      {
         int codepoint = currency.codePointAt(i);
         i += Character.charCount(codepoint);

         if (codepoint == 0x0024)
         {
            builder.append("\\texosquerycurrency{dollar}");
         }
         else if (codepoint == 0x00A2)
         {
            builder.append("\\texosquerycurrency{cent}");
         }
         else if (codepoint == 0x00A3)
         {
            builder.append("\\texosquerycurrency{pound}");
         }
         else if (codepoint == 0x00A4)
         {
            builder.append("\\texosquerycurrency{sign}");
         }
         else if (codepoint == 0x00A5)
         {
            builder.append("\\texosquerycurrency{yen}");
         }
         else if (codepoint == 0x20A0)
         {
            builder.append("\\texosquerycurrency{ecu}");
         }
         else if (codepoint == 0x20A1)
         {
            builder.append("\\texosquerycurrency{colon}");
         }
         else if (codepoint == 0x20A2)
         {
            builder.append("\\texosquerycurrency{cruzeiro}");
         }
         else if (codepoint == 0x20A3)
         {
            builder.append("\\texosquerycurrency{franc}");
         }
         else if (codepoint == 0x20A4)
         {
            builder.append("\\texosquerycurrency{lira}");
         }
         else if (codepoint == 0x20A5)
         {
            builder.append("\\texosquerycurrency{mill}");
         }
         else if (codepoint == 0x20A6)
         {
            builder.append("\\texosquerycurrency{naira}");
         }
         else if (codepoint == 0x20A7)
         {
            builder.append("\\texosquerycurrency{peseta}");
         }
         else if (codepoint == 0x20A8)
         {
            builder.append("\\texosquerycurrency{rupee}");
         }
         else if (codepoint == 0x20A9)
         {
            builder.append("\\texosquerycurrency{won}");
         }
         else if (codepoint == 0x20AA)
         {
            builder.append("\\texosquerycurrency{newsheqel}");
         }
         else if (codepoint == 0x20AB)
         {
            builder.append("\\texosquerycurrency{dong}");
         }
         else if (codepoint == 0x20AC)
         {
            builder.append("\\texosquerycurrency{euro}");
         }
         else if (codepoint == 0x20AD)
         {
            builder.append("\\texosquerycurrency{kip}");
         }
         else if (codepoint == 0x20AE)
         {
            builder.append("\\texosquerycurrency{tugrik}");
         }
         else if (codepoint == 0x20AF)
         {
            builder.append("\\texosquerycurrency{drachma}");
         }
         else if (codepoint == 0x20B0)
         {
            builder.append("\\texosquerycurrency{germanpenny}");
         }
         else if (codepoint == 0x20B1)
         {
            builder.append("\\texosquerycurrency{peso}");
         }
         else if (codepoint == 0x20B2)
         {
            builder.append("\\texosquerycurrency{guarani}");
         }
         else if (codepoint == 0x20B3)
         {
            builder.append("\\texosquerycurrency{austral}");
         }
         else if (codepoint == 0x20B4)
         {
            builder.append("\\texosquerycurrency{hryvnia}");
         }
         else if (codepoint == 0x20B5)
         {
            builder.append("\\texosquerycurrency{cedi}");
         }
         else if (codepoint == 0x20B6)
         {
            builder.append("\\texosquerycurrency{livretournois}");
         }
         else if (codepoint == 0x20B7)
         {
            builder.append("\\texosquerycurrency{spesmilo}");
         }
         else if (codepoint == 0x20B8)
         {
            builder.append("\\texosquerycurrency{tenge}");
         }
         else if (codepoint == 0x20B9)
         {
            builder.append("\\texosquerycurrency{rupee}");
         }
         else if (codepoint == 0x20BA)
         {
            builder.append("\\texosquerycurrency{turkishlira}");
         }
         else if (codepoint == 0x20BB)
         {
            builder.append("\\texosquerycurrency{nordicmark}");
         }
         else if (codepoint == 0x20BC)
         {
            builder.append("\\texosquerycurrency{manat}");
         }
         else if (codepoint == 0x20BD)
         {
            builder.append("\\texosquerycurrency{ruble}");
         }
         else
         {
            builder.append(escapeText(codepoint));
         }
      }

      return builder.toString();
   }

   /** Gets the standalone month names for the locale data.
    * These are only available for Java 8, so just return the 
    * month names used in the date format instead. The JRE8 version
    * needs to override this method.
    * @param cal The calendar
    * @param locale The locale
    * @return month names
    * @since 1.2
    */  
   public String getStandaloneMonths(Calendar cal, Locale locale)
   {
      // can't use Calendar.getDisplayName() as it's not available
      // with Java 5.
      DateFormatSymbols dateFmtSyms = DateFormatSymbols.getInstance(locale);

      StringBuilder monthNamesGroup = new StringBuilder();

      String[] names = dateFmtSyms.getMonths();

      for (int i = 0; i < 12; i++)
      {
         monthNamesGroup.append(String.format("{%s}", escapeText(names[i])));
      }

      return monthNamesGroup.toString();
   }

   /** Gets the standalone short month names for the locale data.
    * These are only available for Java 8, so just return the 
    * month names used in the date format instead. The JRE8 version
    * needs to override this method.
    * @param cal The calendar
    * @param locale The locale
    * @return short month names
    * @since 1.2
    */  
   public String getStandaloneShortMonths(Calendar cal, Locale locale)
   {
      // can't use Calendar.getDisplayName() as it's not available
      // with Java 5.
      DateFormatSymbols dateFmtSyms = DateFormatSymbols.getInstance(locale);

      StringBuilder shortMonthNamesGroup = new StringBuilder();

      String[] names = dateFmtSyms.getShortMonths();

      for (int i = 0; i < 12; i++)
      {
         shortMonthNamesGroup.append(String.format("{%s}", names[i]));
      }

      return shortMonthNamesGroup.toString();
   }

   /** Gets the standalone day names for the locale data.
    * These are only available for Java 8, so just return the 
    * names used in the date format instead. The JRE8 version
    * needs to override this method.
    * @param cal The calendar
    * @param locale The locale
    * @return day of week names
    * @since 1.2
    */  
   public String getStandaloneWeekdays(Calendar cal, Locale locale)
   {
      DateFormatSymbols dateFmtSyms = DateFormatSymbols.getInstance(locale);

      String[] names = dateFmtSyms.getWeekdays();

      // Be consistent with pgfcalendar:

      return String.format("{%s}{%s}{%s}{%s}{%s}{%s}{%s}",
          names[Calendar.MONDAY],
          names[Calendar.TUESDAY],
          names[Calendar.WEDNESDAY],
          names[Calendar.THURSDAY],
          names[Calendar.FRIDAY],
          names[Calendar.SATURDAY],
          names[Calendar.SUNDAY]);
   }

   /** Gets the standalone short day names for the locale data.
    * These are only available for Java 8, so just return the 
    * names used in the date format instead. The JRE8 version
    * needs to override this method.
    * @param cal The calendar
    * @param locale The locale
    * @return day of week names
    * @since 1.2
    */  
   public String getStandaloneShortWeekdays(Calendar cal, Locale locale)
   {
      DateFormatSymbols dateFmtSyms = DateFormatSymbols.getInstance(locale);

      String[] names = dateFmtSyms.getShortWeekdays();

      // Be consistent with pgfcalendar:

      return String.format("{%s}{%s}{%s}{%s}{%s}{%s}{%s}",
          names[Calendar.MONDAY],
          names[Calendar.TUESDAY],
          names[Calendar.WEDNESDAY],
          names[Calendar.THURSDAY],
          names[Calendar.FRIDAY],
          names[Calendar.SATURDAY],
          names[Calendar.SUNDAY]);
   }

   /**
    * Converts date/time pattern to a form that's easier for TeX to
    * parse. This replaces the placeholders with <tt>\\dtf{n}{c}</tt> where c
    * is the placeholder character and n is the number of
    * occurrences of c in the placeholder. (For example, 
    * "<tt>dd-MMM-yyyy</tt>" is  converted to
    * <tt>\\dtf{2}{d}-\\dtf{3}{M}-\\dtf{4}{y}</tt>). The 
    * query command \\TeXOSQuery in texosquery.tex will expand \\dtf
    * to the longer \\texosquerydtf to avoid conflict. This can then be
    * redefined as appropriate.
    * @param localeFormat The date/time pattern
    * @return TeX code
    * @since 1.2
    */ 
   public String formatDateTimePattern(Format localeFormat)
   {
      SimpleDateFormat fmt = null;

      try
      {
         fmt = (SimpleDateFormat)localeFormat;

         if (fmt == null)
         {
            throw new NullPointerException();
         }
      }
      catch (Exception e)
      {
         // this shouldn't happen
         debug(String.format("invalid argument '%s'", localeFormat), e);
         return "";
      }

      String pattern = fmt.toPattern();

      StringBuilder builder = new StringBuilder();

      int prev = 0;
      int fieldLen = 0;
      boolean inString = false;

      for (int i = 0, n = pattern.length(), offset=1; i < n; i = i+offset)
      {
         int codepoint = pattern.codePointAt(i);
         offset = Character.charCount(codepoint);

         int nextIndex = i+offset;
         int nextCodePoint = (nextIndex < n ? pattern.codePointAt(nextIndex):0);

         if (inString)
         {
            if (codepoint == '\'')
            {
               if (nextCodePoint != '\'')
               {
                  // reached the end of the string
                  builder.append('}');
                  inString = false;
               }
               else
               {
                  // literal '
                  builder.append("\\apo ");
                  i = nextIndex;
                  offset = Character.charCount(nextCodePoint);
               }
            }
            else
            {
               // still inside the string
               builder.append(escapeText(codepoint));
            }
         }
         else if (codepoint == prev)
         {
            fieldLen++;
         }
         else
         {
            switch (codepoint)
            {
               case '\'': // quote

                  if (prev != 0)
                  {
                     builder.append(String.format(
                         "\\dtf{%d}{%c}", fieldLen, prev));
                     prev = 0;
                     fieldLen = 0;
                  }

                  // start of the string
                  builder.append("\\str{");
                  inString = true;

               break;
               case 'G': // era
               case 'y': // year
               case 'Y': // week year
               case 'M': // month in year (context sensitive)
               case 'L': // month in year (standalone)
               case 'w': // week in year
               case 'W': // week in month
               case 'D': // day in year
               case 'd': // day in month
               case 'F': // day of week in month
               case 'E': // day name in week
               case 'u': // day number of week (1 = Mon)
               case 'a': // am/pm marker
               case 'H': // hour in day (0-23)
               case 'k': // hour in day (1-24)
               case 'K': // hour in am/pm (0-11)
               case 'h': // hour in am/pm (1-12)
               case 'm': // minute in hour
               case 's': // second in minute
               case 'S': // millisecond
               case 'z': // time zone (locale)
               case 'Z': // time zone (RFC 822)
               case 'X': // time zone (ISO 8601)
                 prev = codepoint;
                 fieldLen = 1;
               break;
               default:
                 // prev doesn't need escaping as it will be one
                 // of the above letter cases.

                 if (prev == 0)
                 {
                     builder.append(escapeText(codepoint));
                 }
                 else
                 {
                     builder.append(String.format(
                       "\\dtf{%d}{%c}%s", fieldLen, prev, 
                       escapeText(codepoint)));
                 }
                 prev = 0;
                 fieldLen = 0;
            }
         }
      }

      if (prev != 0)
      {
         builder.append(String.format(
           "\\dtf{%d}{%c}", fieldLen, prev));
      }

      return builder.toString();
   }

   /**
    * Converts numeric pattern to a form that's easier for TeX to parse. 
    * @param numFormat the numeric pattern
    * @return TeX code
    * @since 1.2
    */ 
   public String formatNumberPattern(Format numFormat)
   {
      DecimalFormat fmt = null;

      try
      {
         fmt = (DecimalFormat)numFormat;

         if (fmt == null)
         {
            throw new NullPointerException();
         }
      }
      catch (Exception e)
      {
         // this shouldn't happen
         debug(String.format("invalid argument '%s'", numFormat), e);
         return "";
      }

      String pattern = fmt.toPattern();

      // Is there a +ve;-ve sub-pattern pair?
      // This is a bit awkward as a semi-colon could appear
      // literally within a string.

      String positive = null;
      String negative = null;

      StringBuilder builder = new StringBuilder();
      boolean inString = false;

      for (int i = 0, n = pattern.length(), offset=1; i < n; i = i+offset)
      {
         int codepoint = pattern.codePointAt(i);
         offset = Character.charCount(codepoint);

         int nextIndex = i+offset;
         int nextCodePoint = (nextIndex < n ? pattern.codePointAt(nextIndex):0);

         if (inString)
         {
            if (codepoint == '\'')
            {
               builder.appendCodePoint(codepoint);

               if (nextCodePoint == '\'')
               {
                  // literal '
                  builder.appendCodePoint(nextCodePoint);
                  i = nextIndex;
                  offset = Character.charCount(nextCodePoint);
               }
               else
               {
                  inString = false;
               }
            }
            else
            {
               builder.appendCodePoint(codepoint);
            }
         }
         else if (codepoint == '\'')
         {
            inString = true;
            builder.appendCodePoint(codepoint);
         }
         else if (codepoint == ';')
         {
            if (positive == null)
            {
               positive = builder.toString();
               builder = new StringBuilder();
            }
            else
            {
               debug(String.format("too many ';' found in pattern '%s'", 
                     pattern));
            }
         }
         else
         {
            builder.appendCodePoint(codepoint);
         }
      }

      if (positive == null)
      {
         positive = builder.toString();
      }
      else if (builder.length() > 0)
      {
         negative = builder.toString();
      }

      if (negative == null)
      {
         return String.format("\\numfmt{%s}", 
           formatNumberSubPattern(positive));
      }
      else
      {
         return String.format("\\pmnumfmt{%s}{%s}", 
           formatNumberSubPattern(positive),
           formatNumberSubPattern(negative));
      }
   }

   /**
    * Converts the sub-pattern of a numeric format.
    * @param pattern The sub-pattern
    * @return TeX code
    * @since 1.2
    */ 
   private String formatNumberSubPattern(String pattern)
   {
      if (pattern == null || "".equals(pattern))
      {
         return "";
      }

      // Is this currency?

      Pattern p = Pattern.compile("(.*(?:[^'](?:'')+){0,1})(¤{1,2})(.*)");
      Matcher m = p.matcher(pattern);

      if (m.matches())
      {
         return formatCurrencyPattern(m.group(1), 
           (m.group(2).length() == 2), m.group(3));
      }

      // Is this a percentage?

      p = Pattern.compile("(.*(?:[^'](?:'')+){0,1})([%‰])(.*)");
      m = p.matcher(pattern);

      if (m.matches())
      {
         boolean percent = ("%".equals(m.group(2)));

         return formatPercentagePattern(m.group(1), m.group(3),
          percent ? "ppct" : "ppml", 
          percent ? "spct" : "spml");
      }

      // must be a number

      return formatNumericPattern(pattern);
   }

   /**
    * Converts the currency format.
    * @param pre The pre-symbol pattern
    * @param international Determines if the international currency
    * symbol should be used
    * @param post The post-symbol pattern
    * @return TeX code
    * @since 1.2
    */ 
   private String formatCurrencyPattern(String pre, boolean international,
      String post)
   {
      if (post == null || "".equals(post))
      {
         pre = formatNumericPattern(pre);

         // currency symbol is a suffix
         if (international)
         {
            return String.format("\\sicur{%s}{}", pre);
         }
         else
         {
            return String.format("\\scur{%s}{}", pre);
         }
      }
      else if (pre == null || "".equals(pre))
      {
         // currency symbol is a prefix

         post = formatNumericPattern(post);

         if (international)
         {
            return String.format("\\picur{%s}{}", post);
         }
         else
         {
            return String.format("\\pcur{%s}{}", post);
         }
      }
      else
      {
         // What do we do here? If pre contains '#' or '0' assume
         // a suffix currency otherwise a prefix currency.

         pre = formatNumericPattern(pre);
         post = formatNumericPattern(post);

         if (pre.matches(".*[0#].*"))
         {
            // suffix, pre is the number and post is trailing
            // text
            if (international)
            {
               return String.format("\\sicur{%s}{%s}", pre, post);
            }
            else
            {
               return String.format("\\scur{%s}{%s}", pre, post);
            }
         }
         else
         {
            // prefix, post is the number and pre is leading
            // text
            if (international)
            {
               return String.format("\\picur{%s}{%s}", post, pre);
            }
            else
            {
               return String.format("\\pcur{%s}{%s}", post, pre);
            }
         }
      }
   }

   /**
    * Converts percentage format.
    * @param pre The pre-symbol pattern
    * @param post The post-symbol pattern
    * @param prefixCs The control sequence name to use if the symbol is a
    * prefix
    * @param suffixCs The control sequence name to use if the symbol is a
    * suffix
    * @return TeX code
    * @since 1.2
    */ 
   private String formatPercentagePattern(String pre, String post,
     String prefixCs, String suffixCs)
   {
      if (post == null || "".equals(post))
      {
         pre = formatNumericPattern(pre);

         // symbol is a suffix

         return String.format("\\%s{%s}{}", suffixCs, pre);
      }
      else if (pre == null || "".equals(pre))
      {
         // symbol is a prefix

         post = formatNumericPattern(post);

         return String.format("\\%s{%s}{}", prefixCs, post);
      }
      else
      {
         pre = formatNumericPattern(pre);
         post = formatNumericPattern(post);

         if (pre.matches(".*[0#].*"))
         {
            // suffix, pre is the number and post is trailing
            // text

            return String.format("\\%s{%s}{%s}", suffixCs, pre, post);
         }
         else
         {
            // prefix, post is the number and pre is leading
            // text

            return String.format("\\%s{%s}{%s}", prefixCs, post, pre);
         }
      }
   }

   /**
    * Converts the numeric format.
    * @param pattern The sub-pattern
    * @return TeX code
    * @since 1.2
    */ 
   private String formatNumericPattern(String pattern)
   {
      if (pattern == null || "".equals(pattern))
      {
         return "";
      }

      // Split around exponent (if present)

      Pattern p = Pattern.compile("(.*(?:[^'](?:'')+?){0,1})(E.*)?");
      Matcher m = p.matcher(pattern);

      if (!m.matches())
      {
         debug(String.format(
             "Can't match number format sub-pattern '%s' against regexp \"%s\"",
              pattern, p));
         return "";
      } 

      String pre = m.group(1);
      String post = m.group(2);

      if (pre == null && post == null)
      {
         // empty pattern
         return "";
      }

      if (post == null)
      {
         return formatDecimalPattern(pre);
      }

      return String.format("\\sinumfmt{%s}{%s}",
        formatDecimalPattern(pre),
        formatIntegerPattern(post, true));
   }

   /**
    * Converts a decimal pattern.
    * @param pattern The pattern
    * @return TeX code
    * @since 1.2
    */ 
   private String formatDecimalPattern(String pattern)
   {
      // split on the decimal point (if present)

      Pattern p = Pattern.compile("(.*?(?:[^'](?:'')){0,1})(?:\\.(.*))?");

      Matcher m = p.matcher(pattern);

      if (!m.matches())
      {
         debug(String.format(
             "Can't match decimal pattern '%s' against regexp \"%s\"",
              pattern, p));
         return "";
      } 


      String pre = m.group(1);
      String post = m.group(2);

      if (pre == null && post == null)
      {
         // empty pattern
         return "";
      }

      if (post == null)
      {
         return formatIntegerPattern(pre, true);
      }

      return String.format("\\decfmt{%s}{%s}",
        formatIntegerPattern(pre, true),
        formatIntegerPattern(post, false));
   }


   /**
    * Converts an integer pattern. The aim here is to have a number
    * formatting command defined in TeX that will be passed a number
    * with either leading or trailing zeros padded to 10 digits.
    * TeX can't handle numbers higher than 2147483647, so any digits
    * in the pattern beyond that are discarded. This means defining
    * a command that effectively takes 10 arguments (with a bit of
    * trickery to get around the 9-arg maximum). Each digit can then
    * be rendered using either \dgt (always display the digit)
    * or \dgtnz (only display the digit if it isn't zero).
    * These short commands will be converted to longer ones that are
    * less likely to cause conflict when \TeXOSQuery is used.
    * @param pattern The pattern
    * @param leadPadding Determines if leading padding needs taking
    * into account
    * @return TeX code
    * @since 1.2
    */ 
   private String formatIntegerPattern(String pattern, boolean leadPadding)
   {
      boolean inString = false;

      int digitCount = 0;
      int groupCount = -1;

      // count the number of digits

      for (int i = 0, n = pattern.length(), offset=1; i < n; i = i+offset)
      {
         int codepoint = pattern.codePointAt(i);
         offset = Character.charCount(codepoint);

         int nextIndex = i+offset;
         int nextCodePoint = (nextIndex < n ? pattern.codePointAt(nextIndex):0);

         if (inString)
         {
            if (codepoint == '\'')
            {
               if (nextCodePoint != '\'')
               {
                  inString = false;
                  i = nextIndex;
                  offset = Character.charCount(nextCodePoint);
               }
            }
         }
         else if (codepoint == '\'')
         {
            inString = true;
         }
         else if (codepoint == '#' || codepoint == '0')
         {
            digitCount++;

            if (groupCount > -1) groupCount++;
         }
         else if (codepoint == ',')
         {
            groupCount=0;
         }
      }

      int digitIndex = (leadPadding ? MAX_DIGIT_FORMAT : 0);

      inString = false;

      StringBuilder builder = new StringBuilder();

      for (int i = 0, n = pattern.length(), offset=1; i < n; i = i+offset)
      {
         int codepoint = pattern.codePointAt(i);
         offset = Character.charCount(codepoint);

         int nextIndex = i+offset;
         int nextCodePoint = (nextIndex < n ? pattern.codePointAt(nextIndex):0);

         switch (codepoint)
         {
            case '\'':

              if (!inString)
              {
                 inString = true;

                 builder.append("\\str{");
              }
              else if (nextCodePoint == '\'')
              {
                 builder.append("\\apo ");
                 i = nextIndex;
                 offset = Character.charCount(nextCodePoint);
              }
              else
              {
                 builder.append("}");
                 inString = false;
              }
            break;
            case '0':

              if (leadPadding)
              {
                 if (digitIndex > MAX_DIGIT_FORMAT)
                 {
                    // too many digit markers in the pattern,
                    // discard
                 }
                 else if (digitIndex > digitCount)
                 {
                    // not enough digit markers in the pattern
                    // pad with #

                    for ( ; digitIndex > digitCount; digitIndex--)
                    {
                       builder.append("\\dgtnz ");

                       if (groupCount > 0 && ((digitIndex-1) % groupCount) == 0)
                       {
                          builder.append("\\ngp ");
                       }
                    }

                    builder.append("\\dgt ");
                 }
                 else
                 {
                    builder.append("\\dgt ");
                 }

                 digitIndex--;
              }
              else
              {
                 digitIndex++;

                 if (digitIndex > MAX_DIGIT_FORMAT)
                 {
                    // too many digit markers in the pattern,
                    // discard
                 }
                 else if (digitIndex == digitCount)
                 {
                    builder.append("\\dgt ");

                    // not enough digit markers in the pattern
                    // pad with #

                    for ( ; digitIndex < MAX_DIGIT_FORMAT; digitIndex++)
                    {
                       builder.append("\\dgtnz ");
                    }
                 }
                 else
                 {
                    builder.append("\\dgt ");
                 }
              }
            break;
            case '#':

              if (leadPadding)
              {
                 if (digitIndex > MAX_DIGIT_FORMAT)
                 {
                    // too many digit markers in the pattern,
                    // discard
                 }
                 else if (digitIndex > digitCount)
                 {
                    // not enough digit markers in the pattern
                    // pad with #

                    for ( ; digitIndex > digitCount; digitIndex--)
                    {
                       builder.append("\\dgtnz ");

                       if (groupCount > 0 && ((digitIndex-1) % groupCount) == 0)
                       {
                          builder.append("\\ngp ");
                       }
                    }

                    builder.append("\\dgtnz ");
                 }
                 else
                 {
                    builder.append("\\dgtnz ");
                 }

                 digitIndex--;
              }
              else
              {
                 digitIndex++;

                 if (digitIndex > MAX_DIGIT_FORMAT)
                 {
                    // too many digit markers in the pattern,
                    // discard
                 }
                 else if (digitIndex == digitCount)
                 {
                    builder.append("\\dgtnz ");

                    // not enough digit markers in the pattern
                    // pad with #

                    for ( ; digitIndex < MAX_DIGIT_FORMAT; digitIndex++)
                    {
                       builder.append("\\dgtnz ");
                    }
                 }
                 else
                 {
                    builder.append("\\dgtnz ");
                 }
              }
            break;
            case '-':
              builder.append("\\msg ");
            break;
            case ',':

              if (digitIndex <= digitCount)
              {
                 builder.append("\\ngp ");
              }

            break;
            default:
              builder.append(escapeText(codepoint));
         }
      }

      return builder.toString();
   }

   /**
    * Gets all available for the given locale. If the
    * given locale tag is null, the default locale is used. The
    * information is returned with grouping to make it
    * easier to parse in TeX. Since TeX has a nine-argument limit,
    * each block is in a sub-group (although this still exceeds nine
    * arguments). The standalone month names and day of week names are new
    * to Java 8, so we can't use it for earlier versions.
    * @param localeTag the language tag identifying the locale or null for
    * the default locale
    * @return locale data in grouped blocks:
    * <ol>
    * <li>language tag, language name, language name in given locale,
    * country name, country name in given locale, variant name,
    * variant name in given locale.
    * <li> full date, long date, medium date, short date,
    * first day of the week index.
    * <li> full date, long date, medium date, short date patterns.
    * <li> full time, long time, medium time, short time.
    * <li> full time, long time, medium time, short time patterns.
    * <li> weekday names.
    * <li> short weekday names.
    * <li> month names
    * <li> short month names.
    * <li> standalone week day names.
    * <li> standalone short week day names.
    * <li> standalone month names.
    * <li> standalone short month names.
    * <li> number group separator,
    * decimal separator, exponent separator, grouping flag, ISO 4217 currency
    * identifier (e.g. GBP), region currency identifier (usually the same as
    * the ISO 4217 code, but may be an unofficial currency code, such as IMP),
    * currency symbol (e.g. £), TeX currency symbol, monetary decimal separator,
    * percent symbol, per mill symbol.
    * <li> number format, integer format, currency format,
    * percent format.
    * </ol>
    * @since 1.2
    */
   public String getLocaleData(String localeTag)
   {
       Locale locale;

       if (localeTag == null || "".equals(localeTag))
       {
          locale = Locale.getDefault();
       }
       else
       {
          locale = getLocale(localeTag);
       }

       String languageName = locale.getDisplayLanguage();

       if (languageName == null)
       {
          languageName = "";
       }

       String localeLanguageName = locale.getDisplayLanguage(locale);

       if (localeLanguageName == null)
       {
          localeLanguageName = "";
       }

       String countryName = locale.getDisplayCountry();

       if (countryName == null)
       {
          countryName = "";
       }

       String localeCountryName = locale.getDisplayCountry(locale);

       if (localeCountryName == null)
       {
          localeCountryName = "";
       }

       String variantName = locale.getDisplayVariant();

       if (variantName == null)
       {
          variantName = "";
       }

       String localeVariantName = locale.getDisplayVariant(locale);

       if (localeVariantName == null)
       {
          localeVariantName = "";
       }

       String langRegionGroup = String.format("{%s}{%s}{%s}{%s}{%s}{%s}{%s}",
             getLanguageTag(locale),
             escapeText(languageName),
             escapeText(localeLanguageName),
             escapeText(countryName),
             escapeText(localeCountryName),
             escapeText(variantName),
             escapeText(localeVariantName));

       DateFormat dateFullFormat = DateFormat.getDateInstance(
        DateFormat.FULL, locale);

       DateFormat dateLongFormat = DateFormat.getDateInstance(
        DateFormat.LONG, locale);

       DateFormat dateMediumFormat = DateFormat.getDateInstance(
        DateFormat.MEDIUM, locale);

       DateFormat dateShortFormat = DateFormat.getDateInstance(
        DateFormat.SHORT, locale);

       DateFormat timeFullFormat = DateFormat.getTimeInstance(
        DateFormat.FULL, locale);

       DateFormat timeLongFormat = DateFormat.getTimeInstance(
        DateFormat.LONG, locale);

       DateFormat timeMediumFormat = DateFormat.getTimeInstance(
        DateFormat.MEDIUM, locale);

       DateFormat timeShortFormat = DateFormat.getTimeInstance(
        DateFormat.SHORT, locale);

       DateFormat dateTimeFullFormat = DateFormat.getDateTimeInstance(
        DateFormat.FULL, DateFormat.FULL, locale);

       DateFormat dateTimeLongFormat = DateFormat.getDateTimeInstance(
        DateFormat.LONG, DateFormat.LONG, locale);

       DateFormat dateTimeMediumFormat = DateFormat.getDateTimeInstance(
        DateFormat.MEDIUM, DateFormat.MEDIUM, locale);

       DateFormat dateTimeShortFormat = DateFormat.getDateTimeInstance(
        DateFormat.SHORT, DateFormat.SHORT, locale);

       // first day of the week index consistent with pgfcalendar
       // (0 = Monday, etc)
       int firstDay = 0;

       Calendar cal = Calendar.getInstance(locale);
       cal.setTimeInMillis(now.getTime());

       switch (cal.getFirstDayOfWeek())
       {
          case Calendar.MONDAY:
            firstDay = 0;
          break;
          case Calendar.TUESDAY:
            firstDay = 1;
          break;
          case Calendar.WEDNESDAY:
            firstDay = 2;
          break;
          case Calendar.THURSDAY:
            firstDay = 3;
          break;
          case Calendar.FRIDAY:
            firstDay = 4;
          break;
          case Calendar.SATURDAY:
            firstDay = 5;
          break;
          case Calendar.SUNDAY:
            firstDay = 6;
          break;
       }

       String dateGroup = String.format("{%s}{%s}{%s}{%s}{%d}",
             escapeText(dateFullFormat.format(now)),
             escapeText(dateLongFormat.format(now)),
             escapeText(dateMediumFormat.format(now)),
             escapeText(dateShortFormat.format(now)),
             firstDay);

       String dateFmtGroup = String.format("{%s}{%s}{%s}{%s}",
         formatDateTimePattern(dateFullFormat),
         formatDateTimePattern(dateLongFormat),
         formatDateTimePattern(dateMediumFormat),
         formatDateTimePattern(dateShortFormat));

       String timeGroup = String.format("{%s}{%s}{%s}{%s}",
             escapeText(timeFullFormat.format(now)),
             escapeText(timeLongFormat.format(now)),
             escapeText(timeMediumFormat.format(now)),
             escapeText(timeShortFormat.format(now)));

       String timeFmtGroup = String.format("{%s}{%s}{%s}{%s}",
         formatDateTimePattern(timeFullFormat),
         formatDateTimePattern(timeLongFormat),
         formatDateTimePattern(timeMediumFormat),
         formatDateTimePattern(timeShortFormat));

       String dateTimeGroup = String.format("{%s}{%s}{%s}{%s}",
             escapeText(dateTimeFullFormat.format(now)),
             escapeText(dateTimeLongFormat.format(now)),
             escapeText(dateTimeMediumFormat.format(now)),
             escapeText(dateTimeShortFormat.format(now)));

       String dateTimeFmtGroup = String.format("{%s}{%s}{%s}{%s}",
         formatDateTimePattern(dateTimeFullFormat),
         formatDateTimePattern(dateTimeLongFormat),
         formatDateTimePattern(dateTimeMediumFormat),
         formatDateTimePattern(dateTimeShortFormat));

       DateFormatSymbols dateFmtSyms = DateFormatSymbols.getInstance(locale);

       String[] names = dateFmtSyms.getWeekdays();

       // Be consistent with pgfcalendar:

       String weekdayNamesGroup = String.format(
          "{%s}{%s}{%s}{%s}{%s}{%s}{%s}",
           escapeText(names[Calendar.MONDAY]),
           escapeText(names[Calendar.TUESDAY]),
           escapeText(names[Calendar.WEDNESDAY]),
           escapeText(names[Calendar.THURSDAY]),
           escapeText(names[Calendar.FRIDAY]),
           escapeText(names[Calendar.SATURDAY]),
           escapeText(names[Calendar.SUNDAY]));

       names = dateFmtSyms.getShortWeekdays();

       String shortWeekdayNamesGroup = String.format(
          "{%s}{%s}{%s}{%s}{%s}{%s}{%s}",
           escapeText(names[Calendar.MONDAY]),
           escapeText(names[Calendar.TUESDAY]),
           escapeText(names[Calendar.WEDNESDAY]),
           escapeText(names[Calendar.THURSDAY]),
           escapeText(names[Calendar.FRIDAY]),
           escapeText(names[Calendar.SATURDAY]),
           escapeText(names[Calendar.SUNDAY]));

       StringBuilder monthNamesGroup = new StringBuilder();

       names = dateFmtSyms.getMonths();

       for (int i = 0; i < 12; i++)
       {
          // skip 13th month (Calendar.UNDECIMBER)
          monthNamesGroup.append(String.format("{%s}", escapeText(names[i])));
       }

       StringBuilder shortMonthNamesGroup = new StringBuilder();

       names = dateFmtSyms.getShortMonths();

       for (int i = 0; i < 12; i++)
       {
          shortMonthNamesGroup.append(String.format("{%s}", 
            escapeText(names[i])));
       }

       // Get numerical data (as with getNumericalInfo)
       DecimalFormatSymbols fmtSyms 
               = DecimalFormatSymbols.getInstance(locale);

       // ISO 4217 code
       String currencyCode = fmtSyms.getInternationalCurrencySymbol();

       // Currency symbol
       String currency = fmtSyms.getCurrencySymbol();

       // Check for known unofficial currency codes

       String localeCurrencyCode = currencyCode;

       String countryCode = locale.getCountry();

       if (countryCode != null && !"".equals(countryCode))
       {
          if (countryCode.equals("GG") || countryCode.equals("GGY")
           || countryCode.equals("831"))
          {// Guernsey
             localeCurrencyCode = "GGP";
             currency = "£";
          }
          else if (countryCode.equals("JE") || countryCode.equals("JEY")
           || countryCode.equals("832"))
          {// Jersey
             localeCurrencyCode = "JEP";
             currency = "£";
          }
          else if (countryCode.equals("IM") || countryCode.equals("IMN")
           || countryCode.equals("833"))
          {// Isle of Man
             localeCurrencyCode = "IMP";
             currency = "M£";
          }
          else if (countryCode.equals("KI") || countryCode.equals("KIR")
           || countryCode.equals("296"))
          {// Kiribati
             localeCurrencyCode = "KID";
             currency = "$";
          }
          else if (countryCode.equals("TV") || countryCode.equals("TUV")
           || countryCode.equals("798"))
          {// Tuvaluan
             localeCurrencyCode = "TVD";
             currency = "$";
          }
          // Transnistrian ruble omitted as it conflicts with ISO
          // 4217. There's also no country code for
          // Transnistria. Other currencies don't have an associated
          // region code (for example, Somaliland) or don't have a
          // known unofficial currency code (for example, Alderney).
       }

       // Convert known Unicode currency symbols to commands that
       // may be redefined in TeX

       String texCurrency = getTeXCurrency(currency);

       NumberFormat numFormat = NumberFormat.getNumberInstance(locale);
       NumberFormat intFormat = NumberFormat.getIntegerInstance(locale);
       NumberFormat curFormat = NumberFormat.getCurrencyInstance(locale);
       NumberFormat pcFormat = NumberFormat.getPercentInstance(locale);

       String numGroup = String.format(
         "{%s}{%s}{%s}{%d}{%s}{%s}{%s}{%s}{%s}{%s}{%s}",
             escapeText(fmtSyms.getGroupingSeparator()),
             escapeText(fmtSyms.getDecimalSeparator()),
             escapeText(fmtSyms.getExponentSeparator()), 
             numFormat.isGroupingUsed() ? 1 : 0,
             escapeText(currencyCode),
             escapeText(localeCurrencyCode),
             escapeText(currency),
             texCurrency,// already escaped
             escapeText(fmtSyms.getMonetaryDecimalSeparator()),
             escapeText(fmtSyms.getPercent()),
             escapeText(fmtSyms.getPerMill()));

       String numFmtGroup = String.format("{%s}{%s}{%s}{%s}",
         formatNumberPattern(numFormat),
         formatNumberPattern(intFormat),
         formatNumberPattern(curFormat),
         formatNumberPattern(pcFormat));

       return String.format(
          "{%s}{%s}{%s}{%s}{%s}{%s}{%s}{%s}{%s}{%s}{%s}{%s}{%s}{%s}{%s}{%s}{%s}",
             langRegionGroup,
             dateGroup,
             dateFmtGroup,
             timeGroup,
             timeFmtGroup,
             dateTimeGroup,
             dateTimeFmtGroup,
             weekdayNamesGroup,
             shortWeekdayNamesGroup,
             monthNamesGroup,
             shortMonthNamesGroup,
             getStandaloneWeekdays(cal, locale),
             getStandaloneShortWeekdays(cal, locale),
             getStandaloneMonths(cal, locale),
             getStandaloneShortMonths(cal, locale),
             numGroup, numFmtGroup);
   }

    /**
     * Prints the syntax usage.
     */
   protected void syntax()
   {
      System.out.println(String.format("Usage: %s [<options>] <actions>", name));

      System.out.println();
      System.out.println("Cross-platform OS query application");
      System.out.println("for use with TeX's shell escape.");
      System.out.println();
      System.out.println("Each query displays the result in a single line.");
      System.out.println("An empty string is printed if the requested");
      System.out.println("information is unavailable or not permitted.");
      System.out.println("Multiple actions group the results.");
      System.out.println();
      System.out.println("See the manual (texdoc texosquery) for further details.");

      System.out.println();
      System.out.println("Options:");
      System.out.println();

      System.out.println("-h or --help");
      System.out.println("\tDisplay this help message and exit.");
      System.out.println();

      System.out.println("-v or --version");
      System.out.println("\tDisplay version information and exit.");
      System.out.println();

      System.out.println("--nodebug");
      System.out.println("\tNo debugging messages (default)");
      System.out.println();

      System.out.println("--debug <n>");
      System.out.println("\tDisplay debugging messages on STDOUT.");
      System.out.println("\t<n> should be an integer:");
      System.out.println("\t0: no debugging (same as --nodebug)");
      System.out.println("\t1: basic debugging messages");
      System.out.println("\t2: additionally display stack trace.");
      System.out.println();

      System.out.println("--compatible <n>");
      System.out.println("\tCompatibility setting.");
      System.out.println("\t<n> should be \"latest\" (default) or an integer:");

      for (int i = 0; i < DEFAULT_COMPATIBLE; i++)
      {
         System.out.println(String.format("\t%d: version 1.%d", i, i));
      }

      System.out.println();
      System.out.println("General actions:");
      System.out.println();

      for (QueryAction action : AVAILABLE_ACTIONS)
      {
         if (action.getType() == QueryActionType.GENERAL_ACTION)
         {
            System.out.println(action.help());
         }
      }

      System.out.println();
      System.out.println("Locale actions:");
      System.out.println();

      for (QueryAction action : AVAILABLE_ACTIONS)
      {
         if (action.getType() == QueryActionType.LOCALE_ACTION)
         {
            System.out.println(action.help());
         }
      }

      System.out.println();
      System.out.println("File actions:");
      System.out.println();
      System.out.println("Paths should use / for the directory divider.");
      System.out.println("TeX's openin_any setting is checked before attempting");
      System.out.println("to access file information.");
      System.out.println();

      for (QueryAction action : AVAILABLE_ACTIONS)
      {
         if (action.getType() == QueryActionType.FILE_ACTION)
         {
            System.out.println(action.help());
         }
      }
   }

    /**
     * Prints the version.
     */
   protected void version()
   {
       System.out.println(String.format("%s %s %s", name, VERSION_NUMBER,
                VERSION_DATE));
       System.out.println("Copyright 2016 Nicola Talbot");
       System.out.println("License LPPL 1.3+ (http://ctan.org/license/lppl1.3)");
   }

    /**
     * Prints the information with optional grouping.
     * @param numActions Add grouping if actions &gt; 1
     * @param info Information to print
     * @since 1.2
     */ 
   protected void print(int numActions, String info)
   {
      if (compatible == 0)
      {
         // version 1.0 didn't use grouping
         System.out.println(info);
      }
      else
      {
         if (numActions > 1)
         {
            System.out.println(String.format("{%s}", info));
         }
         else
         {
            System.out.println(info);
         }
      }
   }

   /**
    * Find the action corresponding to the name (the command line
    * switch). Once the action has been found, a copy must be
    * returned since the same action may be used multiple times with
    * different arguments.
    * @param action The command line switch (either the short or long
    * form)
    * @return a copy of the predefined action or null if not found 
    * @since 1.2
    */ 
   private QueryAction getAction(String action)
   {
      for (int i = 0; i < AVAILABLE_ACTIONS.length; i++)
      {
         if (AVAILABLE_ACTIONS[i].isAction(action))
         {
            return AVAILABLE_ACTIONS[i].copy();
         }
      }

      return null;
   }

    /**
     * Process command line arguments.
     * @param args Command line arguments.
     * @since 1.2
     */
   public void processArgs(String[] args)
   {
      Vector<QueryAction> actions = new Vector<QueryAction>();

      for (int i = 0; i < args.length; i++)
      {
         QueryAction action = getAction(args[i]);

         if (action != null)
         {
            try
            {
               i = action.parseArgs(args, i)-1;

               actions.add(action);
            }
            catch (Throwable e)
            {
               System.err.println(e.getMessage());
               debug(e.getMessage(), e);
               System.exit(1);
            }
         }
         else if (args[i].equals("-h") || args[i].equals("--help"))
         {
            syntax();
            System.exit(0);
         }
         else if (args[i].equals("-v") || args[i].equals("--version"))
         {
            version();
            System.exit(0);
         }
         else if (args[i].equals("--nodebug"))
         {
            debugLevel = 0;
         }
         else if (args[i].equals("--debug"))
         {
            if (i == args.length-1)
            {
               debugLevel = 1;
            }
            else if (args[i+1].startsWith("-"))
            {
               // Negative debug value not permitted, so next argument
               // must be a switch.
               debugLevel = 1;
            }
            else
            {
               i++;

               try
               {
                  debugLevel = Integer.parseInt(args[i]);
               }
               catch (NumberFormatException e)
               {
                  System.err.println(String.format(
                    "Debug level '%s' not recognised", args[i]));
                  System.exit(1);
               }
            }
         }
         else if (args[i].equals("--compatible"))
         {
            if (i == args.length-1)
            {
               System.err.println("--compatible <level> expected");
               System.exit(1);
            }

            i++;

            if (args[i].equals("latest"))
            {
               compatible = DEFAULT_COMPATIBLE;
            }
            else
            {
               try
               {
                  compatible = Integer.parseInt(args[i]);
               }
               catch (NumberFormatException e)
               {
                  System.err.println(String.format(
                   "Invalid --compatible argument '%s'. (\"latest\" or %d to %d required)",
                   args[i], 0, DEFAULT_COMPATIBLE));
                  System.exit(1);
               }
            }
         }
         else
         {
             System.err.println(String.format(
               "Unknown option '%s'. Try %s --help", args[i], name));
             System.exit(1);
         }
      }

      int numActions = actions.size();

      if (numActions == 0)
      {
         System.err.println(String.format(
           "One or more actions required. Try %s --help", name));
         System.exit(1);
      }

      for (QueryAction action : actions)
      {
         try
         {
            print(numActions, action.doAction(compatible));
         }
         catch (Throwable e)
         {
            System.err.println(e.getMessage());
            debug(String.format("Action '%s' failed", action.getInvokedName()),
              e);
            System.exit(1);
         }
      }
   }

   private final QueryAction[] AVAILABLE_ACTIONS = new QueryAction[]
   {
      new QueryAction("cwd", "c", QueryActionType.FILE_ACTION, 
        "Display current working directory")
      {
         public String action()
         {
            return getCwd();
         }
      },
      new QueryAction("userhome", "m", QueryActionType.FILE_ACTION,
         "Display user's home directory")
      {
         public String action()
         {
            return getUserHome();
         }
      },
      new QueryAction("tmpdir", "t", QueryActionType.FILE_ACTION,
         "Display temporary directory")
      {
         public String action()
         {
            return getTmpDir();
         }
      },
      new QueryAction("osname", "o", QueryActionType.GENERAL_ACTION,
        "Display OS name")
      {
         public String action()
         {
            return getOSname();
         }
      },
      new QueryAction("osversion", "r", QueryActionType.GENERAL_ACTION, 
        "Display OS version")
      {
         public String action()
         {
            return getOSversion();
         }
      },
      new QueryAction("osarch", "a", QueryActionType.GENERAL_ACTION, 
        "Display OS architecture")
      {
         public String action()
         {
            return getOSarch();
         }
      },
      new QueryAction("pdfnow", "n", QueryActionType.GENERAL_ACTION, 
        "Display current date-time in PDF format")
      {
         public String action()
         {
            return pdfnow();
         }
      },
      new QueryAction("locale", "L", QueryActionType.LOCALE_ACTION,
         "Display POSIX locale information")
      {
         public String action()
         {
            return getLocale(Locale.getDefault());
         }
      },
      new QueryAction("locale-lcs", "l", QueryActionType.LOCALE_ACTION,
         "Display POSIX style locale information with lower case codeset")
      {
         public String action()
         {
            return getLocale(Locale.getDefault(), true);
         }
      },
      new QueryAction("codeset-lcs", "C", QueryActionType.GENERAL_ACTION, 
         "Lower case codeset with hyphens stripped", 2)
      {
         public String action()
         {
            return getCodeSet(true);
         }
      },
      new QueryAction("bcp47", "b", QueryActionType.LOCALE_ACTION,
         "Display locale as BCP47 tag", 2)
      {
         public String action()
         {
            return getLanguageTag(null);
         }
      },
      new QueryAction("numeric", "N", 1, 0, "[locale]",
          QueryActionType.LOCALE_ACTION,
          "Display locale numeric information", 2)
      {
         public String action()
         {
            return getNumericalInfo(getOptionalArgument(0));
         }
      },
      new QueryAction("locale-data", "D", 1, 0, "[locale]",
         QueryActionType.LOCALE_ACTION,
         "Display all available locale information", 2)
      {
         public String action()
         {
            return getLocaleData(getOptionalArgument(0));
         }
      },
      new QueryAction("date-time", "M", 
         QueryActionType.GENERAL_ACTION,
         "Display all the current date-time data", 2)
      {
         public String action()
         {
            return getDateTimeData();
         }
      },
      new QueryAction("time-zones", "Z", 1, 0, "[locale]",
         QueryActionType.LOCALE_ACTION,
         "Display all available time zone information", 2)
      {
         public String action()
         {
            return getTimeZones(getOptionalArgument(0));
         }
      },
      new QueryAction("pdfdate", "d", 0, 1, "<file>",
         QueryActionType.FILE_ACTION, 
         "Display date stamp of <file> in PDF format")
      {
         public String action()
         {
            return pdfDate(fileFromTeXPath(getRequiredArgument(0)));
         }
      },
      new QueryAction("filesize", "s", 0, 1, "<file>",
         QueryActionType.FILE_ACTION,
         "Display size of <file> in bytes")
      {
         public String action()
         {
            return getFileLength(fileFromTeXPath(getRequiredArgument(0)));
         }
      },
      new QueryAction("list", "i", 0, 2, "<sep> <dir>",
         QueryActionType.FILE_ACTION,
         "Display list of all files in <dir> separated by <sep>")
      {
         public String action()
         {
            return getFileList(getRequiredArgument(0),
              new File(fromTeXPath(getRequiredArgument(1))));
         }
      },
      new QueryAction("filterlist", "f", 0, 3, "<sep> <regex> <dir>",
         QueryActionType.FILE_ACTION, 
         "Display list of files in <dir> that fully match <regex> separated by <sep>")
      {
         public String action()
         {
            return getFilterFileList(
                  getRequiredArgument(0), 
                  getRequiredArgument(1), 
                  new File(fromTeXPath(getRequiredArgument(2))));
         }
      },
      new QueryAction("uri", "u", 0, 1, "<file>",
         QueryActionType.FILE_ACTION, "Display the URI of <file>")
      {
         public String action()
         {
            return fileURI(fileFromTeXPath(getRequiredArgument(0)));
         }
      },
      new QueryAction("path", "p", 0, 1, "<file>",
         QueryActionType.FILE_ACTION, "Display the canonical path of <file>")
      {
         public String action()
         {
            return filePath(fileFromTeXPath(getRequiredArgument(0)));
         }
      },
      new QueryAction("dirname", "e", 0, 1, "<file>",
         QueryActionType.FILE_ACTION,
         "Display the canonical path of the parent of <file>")
      {
         public String action()
         {
            return parentPath(fileFromTeXPath(getRequiredArgument(0)));
         }
      }
   };

   /**
    * Application name.
    */ 
   private String name;
    
   public static final int DEFAULT_COMPATIBLE=2;

   private static final String VERSION_NUMBER = "1.2";
   private static final String VERSION_DATE = "2016-11-23";
   private static final char BACKSLASH = '\\';
   private static final long ZERO = 0L;

   /**
    * Initialise current date-time for consistency.
    */ 

   private Date now = new Date();

   /**
    * openin_any settings
    */
   private static final char OPENIN_UNSET=0; // unset
   private static final char OPENIN_A='a'; // any
   private static final char OPENIN_R='r'; // restricted
   private static final char OPENIN_P='p'; // paranoid

   private char openin = OPENIN_UNSET;

   private File texmfoutput = null;

   /**
    * Debug level. (0 = no debugging, 1 or more print error messages to
    * STDERR, 2 or more include stack trace, 3 or more include
    * informational messages.)
    */
   private int debugLevel = 0;

   /**
    * Compatibility mode. Version 1.2 replaces escapeHash with
    * escapeSpChars, which switches to using \\hsh etc. Provide a
    * mode to restore the previous behaviour.
    */ 
   private int compatible = DEFAULT_COMPATIBLE;

   // TeX can only go up to 2147483647, so set the maximum number
   // of digits provided for the number formatter. 

   private static final int MAX_DIGIT_FORMAT=10;
}
