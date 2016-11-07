package com.dickimawbooks.texosquery;

import java.io.BufferedReader;
import java.util.Locale;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.DecimalFormatSymbols;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.NumberFormat;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Application functions. These methods need to be Java version 1.5
 * compatible. The 1.7 methods need to be in the TeXOSQueryJRE7 class.
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
public class TeXOSQuery
{
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

            // Now find TEXMFOUTPUT if set
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
                  debug(String.format("TEXMFOUTPUT (%s) doesn't exist, ignoring",
                        texmfoutput.toString()));
                  texmfoutput = null;
               }
               else if (!texmfoutput.isDirectory())
               {
                  debug(String.format("TEXMFOUTPUT (%s) isn't a directory, ignoring",
                        texmfoutput.toString()));
                  texmfoutput = null;
               }
               else if (!texmfoutput.canRead())
               {
                  debug(String.format("TEXMFOUTPUT (%s) doesn't have read permission, ignoring",
                        texmfoutput.toString()));
                  texmfoutput = null;
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

              // is the file under TEXMFOUTPUT?

              if (texmfoutput != null)
              {
                 if (isFileInTree(file, texmfoutput))
                 {
                    // file under TEXMFOUTPUT, so it's okay as long
                    // as it has read permission
                    return file.canRead();
                 }
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

              // is the file hidden?

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
     */
   public String getSystemProperty(String propName, String defValue)
   {
      // This may cause a SecurityException if the security manager
      // doesn't permit access to this property.

      try
      {
         return System.getProperty(propName, defValue);
      }
      catch (SecurityException e)
      {
         debug(String.format("unable to access '%s' property", propName), e);
         return defValue;
      }
   }

    /**
     * Escapes hash from input string.
     * Exception for the hash, TeX's special characters shouldn't need escaping.
     * The definition of \TeXOSQuery in texosquery.tex changes the category
     * code for the standard special characters (and a few others) except 
     * hash, curly braces and backslash. 
     *
     * Any instance of { } or \ returned by any of the methods in this
     * class are considered to be intentional begin-group,
     * end-group or escape characters for processing by TeX.
     * They shouldn't occur with literal intent in any string returned 
     * by texosquery. (Directory dividers in paths returned by texosquery
     * use TeX's / notation.)
     *
     * Therefore the only TeX special character that needs escaping is the 
     * hash. (It may be contained in the OS version or an eccentric file name.)
     * This assumes the user isn't using an insane file naming
     * scheme with { } or \ as literal characters in any file names.
     * Is this a safe assumption? Is it possible that an OS name,
     * arch or version may contain these characters?
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
     * don't need to worry about that as \TeXOSQuery changes the
     * category code for _ before parsing the result of texosquery.
     * @return The OS version as string.
     */
   public String getOSversion()
   {
      return getSystemProperty("os.version", "");
   }

    /**
     * Gets the user home.
     * @return The user home as string.
     */
   public String getUserHome()
   {
      // The result path needs to be converted to a TeX path.
      return toTeXPath(getSystemProperty("user.home", ""));
   }

    /**
     * Converts the filename string to TeX path. Since this is designed to work
     * within TeX, backslashes in paths need to be replaced with forward
     * slashes. The hash character needs escaping just in case of
     * exotic file names.
     * @param filename The filename string.
     * @return TeX path.
     */
   public String toTeXPath(String filename)
   {
      String path = "";
        
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

      path = escapeHash(filename);
        
      return path;
   }

    /**
     * Converts the TeX path back to the original representation.
     * @param filename The filename string.
     * @return The original representation.
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
     * provided file doesn't have a parent, if it's not found in the
     * current directory, kpsewhich will be used to locate it on
     * TeX's path. The provided file name is assumed to have been
     * passed through commands provided by texosquery.tex so the
     * directory dividers should be forward slashes, even if the OS
     * uses backslashes. The returned file may not exist. Any method
     * that uses this method needs to check for existence.
     * @param filename Filename string.
     * @return File representation 
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

            if ((result != null) && (!"".equals(result)))
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
     * Gets the current working directory. (Does this need to check
     * if read is permitted here?) Doesn't seem to make much sense
     * to bar the current working directory.
     * @return The current working directory.
     */
   public String getCwd()
   {
      // The result path needs to be converted to a TeX path.
      return toTeXPath(getSystemProperty("user.dir", "."));
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
       return String.format(
               "D:%1$tY%1$tm%1td%1$tH%1$tM%1$tS%2$s'%3$s'",
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
         // existence).
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
         // existence).
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
     * Gets the list of files from a directory.
     * @param separator Separator.
     * @param directory Directory.
     * @return List as a string.
     */
   public String getFileList(String separator, File directory)
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
        
      try
      {
         StringBuilder builder = new StringBuilder();

         String[] list = directory.list();

         if (list != null)
         {
            for (int i = 0; i < list.length; i++)
            {
               if (i > 0)
               {
                  builder.append(separator);
               }
                        
               // Don't need toTeXPath as the path isn't included.
               // Just the base file name.
               // If the file name includes the list separator (who 
               // would do that?) group the name.

               if (list[i].contains(separator))
               {
                  builder.append(String.format("{%s}", escapeHash(list[i])));
               }
               else
               {
                  builder.append(escapeHash(list[i]));
               }
            }
         }
                
         return builder.toString();
      }
      catch (Exception exception)
      {
         // Catch all possible exceptions
         debug(String.format("Unable to list contents of '%s'",
               directory.toString()),
               exception);
      }

      // Unsuccessful
      return "";
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
         // null or empty regular expression so just list all files
         return getFileList(separator, directory);
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
                  builder.append(String.format("{%s}", escapeHash(list[i])));
               }
               else
               {
                  builder.append(escapeHash(list[i]));
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
     * null here. The JRE7 support needs to override this method.
     * @param locale The locale
     * @return The language script associated with the given locale or null if not available
     */ 
   public String getScript(Locale locale)
   {
      return null;
   }

   /**
    * Gets the language tag for the given locale.
    * @param locale The locale or null for the default locale
    * @return The language tag
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

      identifier = identifier.concat(".").concat(codeset);

      // Find the script if available. This is used as the modifier part. Is
      // there a standard for POSIX locale modifiers?

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
    * Gets the two-letter alpha region code from the numeric code.
    * @param ISO 3166-1 numeric code
    * @return ISO 3166-1 alpha code
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
    * support BCP47 language tags until v1.7, we have can't use
    * Locale.forLanguageTag(String) here. Only parse for language
    * code, country code and variant. Grandfathered, irregular and private
    * tags not supported.
    * @param languageTag The language tag
    * @return The locale that closest matches the language tag
    */ 
   public Locale getLocale(String languageTag)
   {
      // The BCP47 syntax is described in 
      // https://tools.ietf.org/html/bcp47#section-2.1
      // This is a match for a subset of the regular syntax.
      // Numeric country codes aren't recognised. Only the 
      // language tag, the region and the variant are
      // captured.
      // Note: named capturing groups introduced in Java 7, so we
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
            // ignore, alpha region code supplied
         }

         // Language won't be null as the pattern requires it.

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
    * given locale tag is null, the default locale is used. The
    * information is returned with each item grouped to make it
    * easier to parse in TeX. Note that Java doesn't support any of
    * the unofficial currency codes, such as IMP, that aren't
    * recognised in ISO 4217. Use getLocaleData() for unofficial
    * codes.
    * @param localeTag the tag identifying the locale or null for
    * the default locale
    * @return locale numerical information: number group separator,
    * decimal separator, exponent separator, international currency
    * identifier (e.g. GBP), currency symbol (e.g. £),
    * monetary decimal separator.
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

       return String.format("{%s}{%s}{%s}{%s}{%s}{%s}",
             escapeHash(fmtSyms.getGroupingSeparator()),
             escapeHash(fmtSyms.getDecimalSeparator()),
             escapeHash(fmtSyms.getExponentSeparator()), 
             escapeHash(fmtSyms.getInternationalCurrencySymbol()),
             escapeHash(fmtSyms.getCurrencySymbol()),
             escapeHash(fmtSyms.getMonetaryDecimalSeparator()));
   }

   /**
    * Gets the currency with known symbols replaced by TeX commands
    * provided by texosquery.tex.
    * @param currency The original currency string 
    * @return The TeX version
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
            builder.append("\\texosquerycurrencydollar ");
         }
         else if (codepoint == 0x00A2)
         {
            builder.append("\\texosquerycurrencycent ");
         }
         else if (codepoint == 0x00A3)
         {
            builder.append("\\texosquerycurrencypound ");
         }
         else if (codepoint == 0x00A4)
         {
            builder.append("\\texosquerycurrencysign ");
         }
         else if (codepoint == 0x00A5)
         {
            builder.append("\\texosquerycurrencyyen ");
         }
         else if (codepoint == 0x20A0)
         {
            builder.append("\\texosquerycurrencyecu ");
         }
         else if (codepoint == 0x20A1)
         {
            builder.append("\\texosquerycurrencycolon ");
         }
         else if (codepoint == 0x20A2)
         {
            builder.append("\\texosquerycurrencycruzeiro ");
         }
         else if (codepoint == 0x20A3)
         {
            builder.append("\\texosquerycurrencyfranc ");
         }
         else if (codepoint == 0x20A4)
         {
            builder.append("\\texosquerycurrencylira ");
         }
         else if (codepoint == 0x20A5)
         {
            builder.append("\\texosquerycurrencymill ");
         }
         else if (codepoint == 0x20A6)
         {
            builder.append("\\texosquerycurrencynaira ");
         }
         else if (codepoint == 0x20A7)
         {
            builder.append("\\texosquerycurrencypeseta ");
         }
         else if (codepoint == 0x20A8)
         {
            builder.append("\\texosquerycurrencyrupee ");
         }
         else if (codepoint == 0x20A9)
         {
            builder.append("\\texosquerycurrencywon ");
         }
         else if (codepoint == 0x20AA)
         {
            builder.append("\\texosquerycurrencynewsheqel ");
         }
         else if (codepoint == 0x20AB)
         {
            builder.append("\\texosquerycurrencydong ");
         }
         else if (codepoint == 0x20AC)
         {
            builder.append("\\texosquerycurrencyeuro ");
         }
         else if (codepoint == 0x20AD)
         {
            builder.append("\\texosquerycurrencykip ");
         }
         else if (codepoint == 0x20AE)
         {
            builder.append("\\texosquerycurrencytugrik ");
         }
         else if (codepoint == 0x20AF)
         {
            builder.append("\\texosquerycurrencydrachma ");
         }
         else if (codepoint == 0x20B0)
         {
            builder.append("\\texosquerycurrencygermanpenny ");
         }
         else if (codepoint == 0x20B1)
         {
            builder.append("\\texosquerycurrencypeso ");
         }
         else if (codepoint == 0x20B2)
         {
            builder.append("\\texosquerycurrencyguarani ");
         }
         else if (codepoint == 0x20B3)
         {
            builder.append("\\texosquerycurrencyaustral ");
         }
         else if (codepoint == 0x20B4)
         {
            builder.append("\\texosquerycurrencyhryvnia ");
         }
         else if (codepoint == 0x20B5)
         {
            builder.append("\\texosquerycurrencycedi ");
         }
         else if (codepoint == 0x20B6)
         {
            builder.append("\\texosquerycurrencylivretournois ");
         }
         else if (codepoint == 0x20B7)
         {
            builder.append("\\texosquerycurrencyspesmilo ");
         }
         else if (codepoint == 0x20B8)
         {
            builder.append("\\texosquerycurrencytenge ");
         }
         else if (codepoint == 0x20B9)
         {
            builder.append("\\texosquerycurrencyrupee ");
         }
         else if (codepoint == 0x20BA)
         {
            builder.append("\\texosquerycurrencyturkishlira ");
         }
         else if (codepoint == 0x20BB)
         {
            builder.append("\\texosquerycurrencynordicmark ");
         }
         else if (codepoint == 0x20BC)
         {
            builder.append("\\texosquerycurrencymanat ");
         }
         else if (codepoint == 0x20BD)
         {
            builder.append("\\texosquerycurrencyruble ");
         }
         else
         {
            builder.appendCodePoint(codepoint);
         }
      }

      return builder.toString();
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
      // can't use Calendar.getDisplayName() as it's not available
      // with Java 5.
      DateFormatSymbols dateFmtSyms = DateFormatSymbols.getInstance(locale);

      StringBuilder monthNamesGroup = new StringBuilder();

      String[] names = dateFmtSyms.getMonths();

      for (int i = 0; i < 12; i++)
      {
         monthNamesGroup.append(String.format("{%s}", names[i]));
      }

      return monthNamesGroup.toString();
   }

   /** Gets the standalone short month names for the locale data.
    * These are only available for Java 8, so just return the 
    * month names used in the date format instead.
    * @param cal The calendar
    * @param locale The locale
    * @return short month names
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
    * names used in the date format instead.
    * @param cal The calendar
    * @param locale The locale
    * @return day of week names
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
    * names used in the date format instead.
    * @param cal The calendar
    * @param locale The locale
    * @return day of week names
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
    * Gets all available for the given locale. If the
    * given locale tag is null, the default locale is used. The
    * information is returned with grouping to make it
    * easier to parse in TeX. Since TeX has a nine-argument limit,
    * each block is in a sub-group (although this still exceeds nine
    * arguments). The standalone month names and day of week names are new
    * to Java 8, so we can't use it for earlier versions.
    * @param localeTag the language tag identifying the locale or null for
    * the default locale
    * @return locale data. First group:
    * language tag, language name, language name in given locale,
    * country name, country name in given locale, variant name,
    * variant name in given locale.
    * Second group: full date, long date, medium date, short date,
    * first day of the week index.
    * Third group: full time, long time, medium time, short time.
    * Fourth group: weekday names.
    * Fifth group: short weekday names.
    * Sixth group: month names
    * Seventh group: short month names.
    * Eighth group: standalone week day names.
    * Ninth group: standalone short week day names.
    * Tenth group: standalone month names.
    * Eleventh group: standalone short month names.
    * Last group: number group separator,
    * decimal separator, exponent separator, grouping flag, ISO 4217 currency
    * identifier (e.g. GBP), region currency identifier (usually the same as
    * the ISO 4217 code, but may be an unofficial currency code, such as IMP),
    * currency symbol (e.g. £), TeX currency symbol, monetary decimal separator,
    * percent symbol, per mill symbol.
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
             languageName,
             localeLanguageName,
             countryName,
             localeCountryName,
             escapeHash(variantName),
             escapeHash(localeVariantName));

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
             dateFullFormat.format(now),
             dateLongFormat.format(now),
             dateMediumFormat.format(now),
             dateShortFormat.format(now),
             firstDay);

       String timeGroup = String.format("{%s}{%s}{%s}{%s}",
             timeFullFormat.format(now),
             timeLongFormat.format(now),
             timeMediumFormat.format(now),
             timeShortFormat.format(now));

       DateFormatSymbols dateFmtSyms = DateFormatSymbols.getInstance(locale);

       String[] names = dateFmtSyms.getWeekdays();

       // Be consistent with pgfcalendar:

       String weekdayNamesGroup = String.format(
          "{%s}{%s}{%s}{%s}{%s}{%s}{%s}",
           names[Calendar.MONDAY],
           names[Calendar.TUESDAY],
           names[Calendar.WEDNESDAY],
           names[Calendar.THURSDAY],
           names[Calendar.FRIDAY],
           names[Calendar.SATURDAY],
           names[Calendar.SUNDAY]);

       names = dateFmtSyms.getShortWeekdays();

       String shortWeekdayNamesGroup = String.format(
          "{%s}{%s}{%s}{%s}{%s}{%s}{%s}",
           names[Calendar.MONDAY],
           names[Calendar.TUESDAY],
           names[Calendar.WEDNESDAY],
           names[Calendar.THURSDAY],
           names[Calendar.FRIDAY],
           names[Calendar.SATURDAY],
           names[Calendar.SUNDAY]);

       StringBuilder monthNamesGroup = new StringBuilder();

       names = dateFmtSyms.getMonths();

       for (int i = 0; i < 12; i++)
       {
          // skip 13th month (Calendar.UNDECIMBER)
          monthNamesGroup.append(String.format("{%s}", names[i]));
       }

       StringBuilder shortMonthNamesGroup = new StringBuilder();

       names = dateFmtSyms.getShortMonths();

       for (int i = 0; i < 12; i++)
       {
          shortMonthNamesGroup.append(String.format("{%s}", names[i]));
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
          // 4217 so omitted. There's also no country code for
          // Transnistria. Other currencies don't have an associated
          // region code (for example, Somaliland) or don't have an
          // known unofficial currency (for example, Alderney).
          // code.
       }

       // Convert known Unicode currency symbols to commands that
       // may be redefined in TeX

       String texCurrency = getTeXCurrency(currency);

       String numGroup = String.format(
         "{%s}{%s}{%s}{%s}{%s}{%s}{%s}{%s}{%s}{%s}{%s}",
             escapeHash(fmtSyms.getGroupingSeparator()),
             escapeHash(fmtSyms.getDecimalSeparator()),
             escapeHash(fmtSyms.getExponentSeparator()), 
             NumberFormat.getNumberInstance(locale).isGroupingUsed(),
             currencyCode,
             localeCurrencyCode,
             escapeHash(currency),
             texCurrency,
             escapeHash(fmtSyms.getMonetaryDecimalSeparator()),
             escapeHash(fmtSyms.getPercent()),
             escapeHash(fmtSyms.getPerMill()));

       return String.format(
          "{%s}{%s}{%s}{%s}{%s}{%s}{%s}{%s}{%s}{%s}{%s}{%s}",
             langRegionGroup,
             dateGroup,
             timeGroup,
             weekdayNamesGroup,
             shortWeekdayNamesGroup,
             monthNamesGroup,
             shortMonthNamesGroup,
             getStandaloneWeekdays(cal, locale),
             getStandaloneShortWeekdays(cal, locale),
             getStandaloneMonths(cal, locale),
             getStandaloneShortMonths(cal, locale),
             numGroup);
   }

    /**
     * Prints the syntax usage.
     */
   protected void syntax()
   {
      System.out.println(String.format("Usage: %s <option>...", name));

      System.out.println();
      System.out.println("Cross-platform OS query application");
      System.out.println("for use with TeX's shell escape.");
      System.out.println();
      System.out.println("Each query displays the result in a single line.");
      System.out.println("A blank line is printed if the requested");
      System.out.println("information is unavailable or not permitted.");
      System.out.println();
      System.out.println("TeX's openin_any setting is checked before attempting");
      System.out.println("to access file information.");

      System.out.println();
      System.out.println("-h or --help\tDisplay this help message and exit");
      System.out.println("-v or --version\tDisplay version information and exit");
      System.out.println("--nodebug\tNo debugging messages (default)");
      System.out.println("--debug <n>\tDisplay debugging messages on STDOUT.");
      System.out.println("\t<n> should be an integer:");
      System.out.println("\t0: no debugging (same as --nodebug)");
      System.out.println("\t1: basic debugging messages");
      System.out.println("\t2: additionally display stack trace.");
      System.out.println();
      System.out.println("General:");
      System.out.println();
      System.out.println("-L or --locale\t\tDisplay POSIX locale information");
      System.out.println("-l or --locale-lcs\tAs --locale but codeset ");
      System.out.println("-b or --bcp47\tDisplay locale as BCP47 tag");
      System.out.println("-c or --cwd\t\tDisplay current working directory");
      System.out.println("-m or --userhome\tDisplay user's home directory");
      System.out.println("-t or --tmpdir\t\tDisplay temporary directory");
      System.out.println("-o or --osname\t\tDisplay OS name");
      System.out.println("-r or --osversion\tDisplay OS version");
      System.out.println("-a or --osarch\t\tDisplay OS architecture");
      System.out.println("-n or --pdfnow\t\tDisplay current date-time in PDF format");
      System.out.println("-N [locale] or --numeric [locale]\tDisplay locale numeric information");
      System.out.println("-D [locale] or --locale-data [locale]\tDisplay all available locale information");

      System.out.println();
      System.out.println("File Queries:");
      System.out.println();
      System.out.println("Paths should use / for the directory divider.");
      System.out.println();
      System.out.println("-d <file> or --pdfdate <file>");
      System.out.println("  Display date stamp of <file> in PDF format");
      System.out.println();
      System.out.println("-s <file> or --filesize <file>");
      System.out.println("  Display size of <file> in bytes");
      System.out.println();
      System.out.println("-i <sep> <dir> or --list <sep> <dir>");
      System.out.println("  Display list of all files in <dir> separated by <sep>");
      System.out.println();
      System.out.println("-f <sep> <regex> <dir> or --filterlist <sep> <regex> <dir>");
      System.out.println("  Display list of files in <dir> that match <regex> separated by <sep>");
      System.out.println();
      System.out.println("-u <file> or --uri <file>");
      System.out.println("  Display the URI of <file>");
      System.out.println();
      System.out.println("-p <file> or --path <file>");
      System.out.println("  Display the canonical path of <file>");
      System.out.println();
      System.out.println("-e <file> or --dirname <file>");
      System.out.println("  Display the canonical path of the parent of <file>");
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
     * @param group Determines whether to add grouping
     * @param info Information to print
     */ 
   protected void print(boolean group, String info)
   {
      if (group)
      {
         System.out.println(String.format("{%s}", info));
      }
      else
      {
         System.out.println(info);
      }
   }

    /**
     * Process command line arguments.
     * @param args Command line arguments.
     */
   public void processArgs(String[] args)
   {
      if (args.length == 0)
      {
         System.err.println(String.format(
           "Missing argument. Try %s --help", name));
         System.exit(1);
      }

      // search for --help, --version, --debug and --nodebug before
      // processing actions.

      int actions = 0;

      for (int i = 0; i < args.length; i++)
      {
         if (args[i].equals("-h") || args[i].equals("--help"))
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
               // Negative level not permitted, so next argument
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
         else if (args[i].startsWith("-"))
         {
            actions++;
         }
      }

      boolean group = (actions > 1);

      for (int i = 0, n=args.length-1; i < args.length; i++)
      {
         if (args[i].equals("-L") || args[i].equals("--locale"))
         {
            // POSIX style locale with unconverted codeset.
            print(group, getLocale(Locale.getDefault()));
         }
         else if (args[i].equals("-l") || args[i].equals("--locale-lcs"))
         {
            // POSIX style locale with converted codeset.
            print(group, getLocale(Locale.getDefault(), true));
         }
         else if (args[i].equals("-b") || args[i].equals("--bcp47"))
         {
            // BCP47 language tag
            print(group, getLanguageTag(null));
         }
         else if (args[i].equals("-N") || args[i].equals("--numeric"))
         {
            // Get the numeric information for the default or given
            // locale (separators, currency symbol etc).

            if (i == n || args[i+1].startsWith("-"))
            {
               // Either at end of argument list or the next argument
               // is a switch so use default locale.
               print(group, getNumericalInfo(null));
            }
            else
            {
               print(group, getNumericalInfo(args[++i]));
            }
         }
         else if (args[i].equals("-D") || args[i].equals("--locale-data"))
         {
            // Get the all available locale information for the default or given
            // locale.

            if (i == n || args[i+1].startsWith("-"))
            {
               // Either at end of argument list or the next argument
               // is a switch so use default locale.
               print(group, getLocaleData(null));
            }
            else
            {
               print(group, getLocaleData(args[++i]));
            }
         }
         else if (args[i].equals("-c") || args[i].equals("--cwd"))
         {
            // current working directory
            print(group, getCwd());
         } 
         else if (args[i].equals("-m") || args[i].equals("--userhome"))
         {
            // user's home directory
            print(group, getUserHome());
         }
         else if (args[i].equals("-t") || args[i].equals("--tmpdir"))
         {
            // temporary directory
            print(group, getTmpDir());
         }
         else if (args[i].equals("-r") || args[i].equals("--osversion"))
         {
            // OS version
            print(group, getOSversion());
         } 
         else if (args[i].equals("-a") || args[i].equals("--osarch"))
         {
            // OS architecture
            print(group, getOSarch());
         }
         else if (args[i].equals("-o") || args[i].equals("--osname"))
         {
            // OS name
            print(group, getOSname());
         }
         else if (args[i].equals("-n") || args[i].equals("--pdfnow"))
         {
            // current date time in PDF format
            print(group, pdfnow());
         }
         else if (args[i].equals("-d") || args[i].equals("--pdfdate"))
         {
            // time stamp in PDF format for given file
            i++;

            if (i > n)
            {
               System.err.println(
                 String.format("filename expected after %s", args[i - 1]));
               System.exit(1);
            }

            if ("".equals(args[i]))
            {
               // empty file name (perhaps user has done something
               // like \TeXOSQuery{-d "\filename"} where \filename
               // is empty?)
               print(group, "");
            }
            else
            {
               print(group, pdfDate(fileFromTeXPath(args[i])));
            }
         }
         else if (args[i].equals("-s") || args[i].equals("--filesize"))
         {
            i++;

            if (i >= args.length)
            {
               System.err.println(
                 String.format("filename expected after %s", args[i - 1]));
               System.exit(1);
            }

            if ("".equals(args[i]))
            {
               print(group, "");
            }
            else
            {
               print(group, getFileLength(fileFromTeXPath(args[i])));
            }
         }
         else if (args[i].equals("-i") || args[i].equals("--list"))
         {
            i++;

            if (i >= args.length)
            {
               System.err.println(String.format(
                  "separator and directory name expected after %s",
                  args[i - 1]));
               System.exit(1);
            }

            i++;

            if (i >= args.length)
            {
               System.err.println(
                  String.format("directory name expected after %s %s",
                  args[i - 2], args[i - 1]));
               System.exit(1);
            }

            if ("".equals(args[i]))
            {
               print(group, "");
            }
            else
            {
               print(group, getFileList(args[i - 1],
                            new File(fromTeXPath(args[i]))));
            }
         }
         else if (args[i].equals("-f") || args[i].equals("--filterlist"))
         {
            // Filtered directory listing
            i++;

            if (i >= args.length)
            {
               System.err.println(
                  String.format(
                     "separator, regex and directory name expected after %s",
                     args[i - 1]));
               System.exit(1);
            }

            i++;

            if (i >= args.length)
            {
               System.err.println(
                  String.format("regex and directory name expected after %s %s",
                  args[i - 2], args[i - 1]));
               System.exit(1);
            }

            i++;

            if (i >= args.length)
            {
               System.err.println(
                 String.format("directory name expected after %s %s",
                 args[i - 3], args[i - 2], args[i - 1]));
               System.exit(1);
            }

            if ("".equals(args[i]))
            {
               print(group, "");
            }
            else
            {
               print(group, getFilterFileList(
                  args[i - 2], args[i - 1], new File(fromTeXPath(args[i]))));
            }
         }
         else if (args[i].equals("-u") || args[i].equals("--uri"))
         {
            // URI of file name
            i++;

            if (i >= args.length)
            {
               System.err.println(
                 String.format("filename expected after %s", args[i - 1]));
               System.exit(1);
            }

            if ("".equals(args[i]))
            {
               print(group, "");
            }
            else
            {
               print(group, fileURI(fileFromTeXPath(args[i])));
            }
         }
         else if (args[i].equals("-p") || args[i].equals("--path"))
         {
            // Canonical path
            i++;

            if (i >= args.length)
            {
               System.err.println(
                 String.format("filename expected after %s", args[i - 1]));
               System.exit(1);
            }

            if ("".equals(args[i]))
            {
               print(group, "");
            }
            else
            {
               print(group, filePath(fileFromTeXPath(args[i])));
            }
         }
         else if (args[i].equals("-e") || args[i].equals("--dirname"))
         {
            // parent directory of given file
            i++;

            if (i >= args.length)
            {
               System.err.println(
                 String.format("filename expected after %s", args[i - 1]));
               System.exit(1);
            }

            if ("".equals(args[i]))
            {
               print(group, "");
            }
            else
            {
               print(group, 
               parentPath(fileFromTeXPath(args[i])));
            }
         }
         else if (args[i].equals("-h") || args[i].equals("--help")
                || args[i].equals("-v") || args[i].equals("--version")
                || args[i].equals("--nodebug"))
         {
            // already dealt with these
         }
         else if (args[i].equals("--debug"))
         {
            // already dealt with this but need to increment loop
            // variable if there's an argument.

            if (i < n && !args[i+1].startsWith("-"))
            {
               i++;
            }
         }
         else
         {
             System.err.println(String.format("unknown option '%s'", args[i]));
             System.exit(1);
         }
      }
   }

    private String name;
    
    private static final String VERSION_NUMBER = "1.2";
    private static final String VERSION_DATE = "2016-11-06";
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
     * Debug level. (0 = no debugging, 1 or more print messages to
     * STDERR.)
     */
    private int debugLevel = 0;
}
