package com.dickimawbooks.texosquery;

import java.util.Locale;
import java.util.Calendar;
import java.io.*;

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

   public static File fileFromTeXPath(String filename)
   {
      filename = fromTeXPath(filename);

      File file = new File(filename);

      if (!file.exists() && file.getParent() == null)
      {
         // use kpsewhich to find it

         try
         {
            Process p = new ProcessBuilder("kpsewhich", filename).start();

            if (p.waitFor() == 0)
            {
               InputStream is = p.getInputStream();

               if (is != null)
               {
                  BufferedReader in = new BufferedReader(
                     new InputStreamReader(is));

                  String line = in.readLine();

                  in.close();

                  if (line != null && !line.isEmpty())
                  {
                     file = new File(fromTeXPath(line));
                  }
               }
            }
         }
         catch (IOException e)
         {
         }
         catch (InterruptedException e)
         {
         }
      }

      return file;
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

   public static String getFileList(String sep, File dir)
   {
      if (!dir.isDirectory())
      {
         return "";
      }

      StringBuilder builder = new StringBuilder();

      try
      {
         String[] list = dir.list();

         if (list == null)
         {
            return "";
         }

         for (int i = 0; i < list.length; i++)
         {
            if (i > 0)
            {
               builder.append(sep);
            }

            // no need to worry about directory divider
            // File.list() just returns the name not the path
            builder.append(list[i]);
         }
      }
      catch (SecurityException e)
      {
      }

      return builder.toString();
   }

   public static String getFilterFileList(String sep, 
     final String regex, File dir)
   {
      if (!dir.isDirectory())
      {
         return "";
      }

      if (regex == null || regex.isEmpty())
      {
         return getFileList(sep, dir);
      }

      StringBuilder builder = new StringBuilder();

      try
      {
         String[] list = dir.list(new FilenameFilter()
         {
            public boolean accept(File dir, String name)
            {
               return name.matches(regex);
            }
         });

         if (list == null)
         {
            return "";
         }

         for (int i = 0; i < list.length; i++)
         {
            if (i > 0)
            {
               builder.append(sep);
            }

            // no need to worry about directory divider
            // File.list() just returns the name not the path
            builder.append(list[i]);
         }
      }
      catch (SecurityException e)
      {
      }

      return builder.toString();
   }

   public static String fileURI(File file)
   {
      if (!file.exists())
      {
         return "";
      }

      try
      {
         return file.toURI().toString();
      }
      catch (SecurityException e)
      {
      }

      return "";
   }

   public static void syntax()
   {
      System.out.println("Useage: texosquery <option>...");

      System.out.println();
      System.out.println("Cross-platform OS query application");
      System.out.println("for use with TeX's shell escape.");
      System.out.println();
      System.out.println("Each query displays the result in a single line.");
      System.out.println("A blank line is printed if the requested");
      System.out.println("information is unavailable.");

      System.out.println();
      System.out.println("-h or --help\tDisplay this help message and exit");
      System.out.println("-v or --version\tDisplay version information and exit");
      System.out.println();
      System.out.println("General:");
      System.out.println();
      System.out.println("-l or --locale\t\tDisplay locale information");
      System.out.println("-c or --cwd\t\tDisplay current working directory");
      System.out.println("-m or --userhome\tDisplay user's home directory");
      System.out.println("-t or --tmpdir\t\tDisplay temporary directory");
      System.out.println("-o or --osname\t\tDisplay OS name");
      System.out.println("-r or --osversion\tDisplay OS version");
      System.out.println("-a or --osarch\t\tDisplay OS architecture");
      System.out.println("-n or --pdfnow\t\tDisplay current date-time in PDF format");

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
         else if (args[i].equals("-m") || args[i].equals("--userhome"))
         {
            System.out.println(getUserHome());
         }
         else if (args[i].equals("-t") || args[i].equals("--tmpdir"))
         {
            System.out.println(getTmpDir());
         }
         else if (args[i].equals("-r") || args[i].equals("--osversion"))
         {
            System.out.println(getOSversion());
         }
         else if (args[i].equals("-a") || args[i].equals("--osarch"))
         {
            System.out.println(getOSarch());
         }
         else if (args[i].equals("-o") || args[i].equals("--osname"))
         {
            System.out.println(getOSname());
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

            if (args[i].isEmpty())
            {
               System.out.println();
            }
            else
            {
               System.out.println(pdfDate(fileFromTeXPath(args[i])));
            }
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

            if (args[i].isEmpty())
            {
               System.out.println();
            }
            else
            {
               System.out.println(getFileLength(fileFromTeXPath(args[i])));
            }
         }
         else if (args[i].equals("-i") || args[i].equals("--list"))
         {
            i++;

            if (i >= args.length)
            {
               System.err.println(
                 String.format("separator and directory name expected after %s",
                   args[i-1]));
               System.exit(1);
            }

            i++;

            if (i >= args.length)
            {
               System.err.println(
                 String.format("directory name expected after %s %s",
                   args[i-2], args[i-1]));
               System.exit(1);
            }

            if (args[i].isEmpty())
            {
               System.out.println();
            }
            else
            {
               System.out.println(getFileList(args[i-1],
                  new File(fromTeXPath(args[i]))));
            }
         }
         else if (args[i].equals("-f") || args[i].equals("--filterlist"))
         {
            i++;

            if (i >= args.length)
            {
               System.err.println(
                 String.format(
                  "separator, regex and directory name expected after %s",
                  args[i-1]));
               System.exit(1);
            }

            i++;

            if (i >= args.length)
            {
               System.err.println(
                 String.format("regex and directory name expected after %s %s",
                   args[i-2], args[i-1]));
               System.exit(1);
            }

            i++;

            if (i >= args.length)
            {
               System.err.println(
                 String.format("directory name expected after %s %s",
                   args[i-3], args[i-2], args[i-1]));
               System.exit(1);
            }

            if (args[i].isEmpty())
            {
               System.out.println();
            }
            else
            {
               System.out.println(getFilterFileList(
                args[i-2], args[i-1], new File(fromTeXPath(args[i]))));
            }
         }
         else if (args[i].equals("-u") || args[i].equals("--uri"))
         {
            i++;

            if (i >= args.length)
            {
               System.err.println(
                 String.format("filename expected after %s", args[i-1]));
               System.exit(1);
            }

            if (args[i].isEmpty())
            {
               System.out.println();
            }
            else
            {
               System.out.println(fileURI(fileFromTeXPath(args[i])));
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
         else
         {
            System.err.println(String.format("unknown option '%s'", args[i]));
            System.exit(1);
         }
      }
   }

   public static final String versionNum = "1.0";
   public static final String versionDate = "2016-07-05";
}
