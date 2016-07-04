package com.dickimawbooks.texosquery;

import java.util.Locale;
import java.util.Calendar;
import java.io.File;

public class TeXOSQuery
{
   public static String getLocale(Locale locale)
   {
      String id = "";

      if (locale != null)
      {
         String lang = locale.getLanguage();

         if (lang != null)
         {
            id = lang;
         }

         String country = locale.getCountry();
         
         if (country != null && !country.isEmpty())
         {
            if (id.isEmpty())
            {
               id = country;
            }
            else
            {
               id = id+"-"+country;
            }
         }

         String codeset = System.getProperty("file.encoding");

         if (codeset != null && !codeset.isEmpty())
         {
            id = id+"."+codeset;
         }

         String script = locale.getScript();

         if (script != null && !script.isEmpty())
         {
            id = id+"@"+script;
         }

      }

      return id;
   }

   public static String getOSname()
   {
      try
      {
         return System.getProperty("os.name", "");
      }
      catch (SecurityException e)
      {
         return "";
      }
   }

   public static String getOSarch()
   {
      try
      {
         return System.getProperty("os.arch", "");
      }
      catch (SecurityException e)
      {
         return "";
      }
   }

   public static String getOSversion()
   {
      try
      {
         return System.getProperty("os.version", "");
      }
      catch (SecurityException e)
      {
         return "";
      }
   }

   public static String getUserHome()
   {
      try
      {
         return System.getProperty("user.home", "");
      }
      catch (SecurityException e)
      {
         return "";
      }
   }

   /*
   * Since this is designed to work within TeX, backslashes in paths
   * need to be replaced with forward slashes.
   */

   public static String toTeXPath(String filename)
   {
      if (File.separatorChar == '\\')
      {
         return filename.replaceAll("\\\\", "/");
      }

      return filename;
   }

   public static String fromTeXPath(String filename)
   {
      if (File.separatorChar != '/')
      {
         return filename.replaceAll("/", File.separator);
      }

      return filename;
   }

   public static String getCwd()
   {
      try
      {
         return toTeXPath(System.getProperty("user.dir", ""));
      }
      catch (SecurityException e)
      {
         return "";
      }
   }

   public static String getTmpDir()
   {
      try
      {
         return toTeXPath(System.getProperty("java.io.tmpdir", ""));
      }
      catch (SecurityException e)
      {
         return "";
      }
   }

   public static String pdfnow()
   {
      Calendar cal = Calendar.getInstance();

      return pdfDate(cal);
   }

   public static String pdfDate(Calendar cal)
   {
      String tz = String.format("%1$tz", cal);

      return String.format("D:%1$tY%1$tm%1td%1$tH%1$tM%1$tS%2$s'%3$s'", cal,
         tz.substring(0,3), tz.substring(3)); 
   }

   public static String pdfDate(File file)
   {
      try
      {
         long millisecs = file.lastModified();

         if (millisecs > 0L)
         {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(millisecs);

            return pdfDate(cal);
         }
      }
      catch (SecurityException e)
      {
      }

      return "";
   }

   public static String getFileLength(File file)
   {
      try
      {
         long len = file.length();

         if (len > 0L)
         {
            return String.format("%d", len);
         }
      }
      catch (SecurityException e)
      {
      }

      return "";
   }

   public static void syntax()
   {
      System.out.println("Useage: texosquery [option]...");

      System.out.println();
      System.out.println("Cross-platform OS query application");
      System.out.println("for use with TeX's shell escape.");
      System.out.println("Paths should use / for the directory divider.");
      System.out.println("A blank line is printed if the requested");
      System.out.println("information is unavailable.");

      System.out.println();
      System.out.println("-l or --locale\tDisplay locale information");
      System.out.println("-c or --cwd\tDisplay current working directory");
      System.out.println("-n or --pdfnow\tDisplay current date-time in PDF format");
      System.out.println("-d <file> or --pdfdate <file>\tDisplay date stamp of <file> in PDF format");
      System.out.println("-s <file> or --filesize <file>\tDisplay size of <file> in bytes");
      System.out.println("-h or --help\tDisplay this help message and exit");
      System.out.println("-v or --version\tDisplay version information and exit");

   }

   public static void version()
   {
      System.out.println(String.format("texosquery %s %s", versionNum,
       versionDate));
      System.out.println("Copyright 2016 Nicola Talbot");
      System.out.println("License LPPL 1.3+ (http://ctan.org/license/lppl1.3)");
   }

   public static void main(String[] args)
   {
      if (args.length == 0)
      {
         System.err.println("Missing argument. Try texosquery --help");
         System.exit(1);
      }

      for (int i = 0; i < args.length; i++)
      {
         if (args[i].equals("-l") || args[i].equals("--locale"))
         {
            System.out.println(getLocale(Locale.getDefault()));
         }
         else if (args[i].equals("-c") || args[i].equals("--cwd"))
         {
            System.out.println(getCwd());
         }
         else if (args[i].equals("-n") || args[i].equals("--pdfnow"))
         {
            System.out.println(pdfnow());
         }
         else if (args[i].equals("-d") || args[i].equals("--pdfdate"))
         {
            i++;

            if (i >= args.length)
            {
               System.err.println(
                 String.format("filename expected after %s", args[i-1]));
               System.exit(1);
            }

            System.out.println(pdfDate(new File(fromTeXPath(args[i]))));
         }
         else if (args[i].equals("-s") || args[i].equals("--filesize"))
         {
            i++;

            if (i >= args.length)
            {
               System.err.println(
                 String.format("filename expected after %s", args[i-1]));
               System.exit(1);
            }

            System.out.println(getFileLength(new File(fromTeXPath(args[i]))));
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
         else
         {
            System.err.println(String.format("unknown option '%s'", args[i]));
            System.exit(1);
         }
      }
   }

   public static final String versionNum = "1.0";
   public static final String versionDate = "2016-07-02";
}
