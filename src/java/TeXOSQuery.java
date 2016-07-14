package com.dickimawbooks.texosquery;

import java.io.BufferedReader;
import java.util.Locale;
import java.util.Calendar;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Main class.
 * @author Nicola Talbot
 * @version 1.1
 * @since 1.0
 */
public class TeXOSQuery {

    private static final String VERSION_NUMBER = "1.1";
    private static final String VERSION_DATE = "2016-07-14";
    private static final char BACKSLASH = '\\';
    private static final char FORWARDSLASH = '/';
    private static final long ZERO = 0L;
    
    /**
     * Escapes hash from input string.
     * @param string Input string.
     * @return String with hash escaped.
     */
    private static String escapeHash(String string) {
        return string.replaceAll("#", "\\\\#");
    }

    /**
     * Gets a string representation of the provided locale.
     * @param locale The provided locale.
     * @return String representation.
     */
    private static String getLocale(Locale locale) {
        return getLocale(locale, false);
    }

    /**
     * Gets a string representation of the provided locale, converting the code
     * set if possible.
     * @param locale The provided locale.
     * @param convertCodeset Boolean value to convert the code set.
     * @return String representation.
     */
    private static String getLocale(Locale locale, boolean convertCodeset) {
        
        String identifier = "";

        if (locale != null) {
            
            String language = locale.getLanguage();

            if (language != null) {
                identifier = language;
            }

            String country = locale.getCountry();

            if ((country != null) &&
                    (!"".equals(country))) {
                
                if ("".equals(identifier)) {
                    identifier = country;
                } else {
                    identifier = identifier.concat("-").concat(country);
                }
                
            }

            String codeset = System.getProperty("file.encoding", "UTF-8");

            if ((codeset != null) &&
                    (!"".equals(codeset))) {
                
                if (convertCodeset) {
                    codeset = codeset.toLowerCase().replaceAll("-", "");
                }

                identifier = identifier.concat(".").concat(codeset);
            }

            String script = locale.getScript();

            if ((script != null) &&
                    (!"".equals(script))) {
                identifier = identifier.concat("@").concat(escapeHash(script));
            }

        }

        return identifier;
    }

    /**
     * Gets the OS name.
     * @return The OS name as string.
     */
    private static String getOSname() {
        return getSystemProperty("os.name");
    }

    /**
     * Gets the OS architecture.
     * @return The OS architecture as string.
     */
    private static String getOSarch() {
        return getSystemProperty("os.arch");
    }

    /**
     * Gets the OS version.
     * @return The OS version as string.
     */
    private static String getOSversion() {
        return getSystemProperty("os.version");
    }

    /**
     * Gets the user home.
     * @return The user home as string.
     */
    private static String getUserHome() {
        return getSystemProperty("user.home");
    }

    /*
   * 
     */
    /**
     * Converts the filename string to TeX path. Since this is designed to work
     * within TeX, backslashes in paths need to be replaced with forward
     * slashes.
     * @param filename The filename string.
     * @return TeX path.
     */
    private static String toTeXPath(String filename) {
        
        String path = "";
        
        if (filename != null) {
            if (File.separatorChar == BACKSLASH) {
                filename = filename.replaceAll("\\\\", "/");
            }
            path = escapeHash(filename);
        }
        
        return path;
    }

    /**
     * Converts the TeX path back to the original representation.
     * @param filename The filename string.
     * @return The original representation.
     */
    private static String fromTeXPath(String filename) {
        
        if (File.separatorChar != FORWARDSLASH) {
            filename = filename.replaceAll("/", File.separator);
        }

        return filename;
    }

    /**
     * Gets a file representation from a filename string.
     * @param filename Filename string.
     * @return File representation 
     */
    private static File fileFromTeXPath(String filename) {
        
        filename = fromTeXPath(filename);
        File file = new File(filename);

        if (!file.exists() && file.getParent() == null) {

            try {
                Process process = new ProcessBuilder(
                        "kpsewhich",
                        filename
                ).start();

                if (process.waitFor() == 0) {
                
                    InputStream stream = process.getInputStream();
                    
                    if (stream != null) {
                        
                        BufferedReader reader = new BufferedReader(
                                new InputStreamReader(stream)
                        );

                        String line = reader.readLine();
                        reader.close();

                        if ((line != null) && (!"".equals(line))) {
                            file = new File(fromTeXPath(line));
                        }
                    }
                }
            } catch (IOException exception1) {
                // quack quack
            } catch (InterruptedException exception2) {
                // quack quack
            }
        }

        return file;
    }

    /**
     * Gets the current working directory.
     * @return The current working directory.
     */
    private static String getCwd() {
        return getSystemProperty("user.dir");
    }

    /**
     * Gets the temporary directory.
     * @return Temporary directory.
     */
    private static String getTmpDir() {
        return getSystemProperty("java.io.tmpdir");
    }

    /**
     * Gets the current date.
     * @return The current date.
     */
    private static String pdfnow() {
        return pdfDate(Calendar.getInstance());
    }

    /**
     * Gets the date in an specific format.
     * @param calendar A calendar object.
     * @return Date in an specific format.
     */
    private static String pdfDate(Calendar calendar) {
        String tz = String.format("%1$tz", calendar);
        return String.format(
                "D:%1$tY%1$tm%1td%1$tH%1$tM%1$tS%2$s'%3$s'",
                calendar,
                tz.substring(0, 3),
                tz.substring(3)
        );
    }

    /**
     * Gets the date from a file.
     * @param file File.
     * @return The date in an specific format.
     */
    private static String pdfDate(File file) {
        
        String date = "";
        
        try {
            long millisecs = file.lastModified();
            
            if (millisecs > ZERO) {
                
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(millisecs);
                date = pdfDate(calendar);
                
            }
        } catch (SecurityException exception) {
            // quack quack
        }

        return date;
    }

    /**
     * Gets the file length.
     * @param file The file.
     * @return The length as a string.
     */
    private static String getFileLength(File file) {
        
        String output = "";
        
        try {
            long length = file.length();

            if (length > ZERO) {
                output = String.format("%d", length);
            }
        } catch (SecurityException exception) {
            // quack quack
        }

        return output;
    }

    /**
     * Gets the list of files from a directory.
     * @param separator Separator.
     * @param directory Directory.
     * @return List as a string.
     */
    private static String getFileList(String separator, File directory) {
        
        StringBuilder builder = new StringBuilder();
        
        if (directory.isDirectory()) {
        
            try {
                
                String[] list = directory.list();
                if (list != null) {
                    
                    for (int i = 0; i < list.length; i++) {
                        
                        if (i > 0) {
                            builder.append(separator);
                        }
                        
                        builder.append(escapeHash(list[i]));
                        
                    }
                }
                
            } catch (SecurityException exception) {
                // quack quack
            }
        }

        return builder.toString();
    }

    /**
     * Gets a filtered list of files from directory.
     * @param separator Separator.
     * @param regex Regular expression.
     * @param directory Directory.
     * @return Filtered list as string.
     */
    private static String getFilterFileList(String separator,
            final String regex, File directory) {
        
        StringBuilder builder = new StringBuilder();
        
        if (directory.isDirectory()) {
      
            if ((regex == null) || ("".equals(regex))) {
                builder.append(getFileList(separator, directory));
            }
            else {        
                try {
                    String[] list = directory.list(new FilenameFilter() {
                        @Override
                        public boolean accept(File dir, String name) {
                            return name.matches(regex);
                        }
                    });

                    if (list != null) {
                        
                        for (int i = 0; i < list.length; i++) {
                        
                            if (i > 0) {
                                builder.append(separator);
                            }
                            
                            builder.append(escapeHash(list[i]));
                        }
                        
                    }
                } catch (SecurityException exception) {
                    // quack quack
                }
            }
        }

        return builder.toString();
    }

    /**
     * Gets the file URI.
     * @param file The file.
     * @return The URI.
     */
    private static String fileURI(File file) {
        
        String uri = "";
        
        if (file.exists()) {
            try {
                uri = file.toURI().toString();
            } catch (SecurityException exception) {
                // quack quack
            }
        }

        return uri;
    }

    /**
     * Gets the file path.
     * @param file The file.
     * @return The path.
     */
    private static String filePath(File file) {
        
        String path = "";
        
        if (file.exists()) {
            try {
                path = toTeXPath(file.getCanonicalPath());
            } catch (SecurityException exception1) {
                // quack quack
            } catch (IOException exception2) {
                // quack quack
            }
        }

        return path;
    }

    /**
     * Gets the path for the file's parent.
     * @param file The file.
     * @return The path.
     */
    private static String parentPath(File file) {
        
        String path = "";
        
        if (file.exists()) {
            try {
                path = toTeXPath(file.getCanonicalFile().getParent());
            } catch (SecurityException exception1) {
                // quack quack
            } catch (IOException exception2) {
                // quack quack
            }
        }

        return path;
    }

    /**
     * Prints the syntax usage.
     */
    private static void syntax() {
        System.out.println("Usage: texosquery <option>...");

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
        System.out.println("-L or --locale\t\tDisplay locale information");
        System.out.println("-l or --locale-lcs\tAs --locale but codeset ");
        System.out.println("\t\t\tin lowercase with hyphens stripped");
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
        System.out.println();
        System.out.println("-p <file> or --path <file>");
        System.out.println("  Display the canonical path of <file>");
        System.out.println();
        System.out.println("-e <file> or --dirname <file>");
        System.out.println("  Display the parent of <file>");
    }

    /**
     * Prints the version.
     */
    private static void version() {
        System.out.println(String.format("texosquery %s %s", VERSION_NUMBER,
                VERSION_DATE));
        System.out.println("Copyright 2016 Nicola Talbot");
        System.out.println("License LPPL 1.3+ (http://ctan.org/license/lppl1.3)");
    }

    /**
     * Prints the information with optional grouping.
     * @param group Determines whether to add grouping
     * @param info Information to print
     */ 
   private static void print(boolean group, String info)
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
     * Main method.
     * @param args Command line arguments.
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Missing argument. Try texosquery --help");
            System.exit(1);
        }

        boolean group = false;

        for (int i = 0, n=args.length-1; i < args.length; i++) {
            if (args[i].equals("-L") || args[i].equals("--locale")) {
                if (i < n) group = true;

                print(group, getLocale(Locale.getDefault()));
            } else if (args[i].equals("-l") || args[i].equals("--locale-lcs")) {
                if (i < n) group = true;

                print(group, getLocale(Locale.getDefault(), true));
            } else if (args[i].equals("-c") || args[i].equals("--cwd")) {
                if (i < n) group = true;

                print(group, getCwd());
            } else if (args[i].equals("-m") || args[i].equals("--userhome")) {
                if (i < n) group = true;

                print(group, getUserHome());
            } else if (args[i].equals("-t") || args[i].equals("--tmpdir")) {
                if (i < n) group = true;

                print(group, getTmpDir());
            } else if (args[i].equals("-r") || args[i].equals("--osversion")) {
                if (i < n) group = true;

                print(group, getOSversion());
            } else if (args[i].equals("-a") || args[i].equals("--osarch")) {
                if (i < n) group = true;

                print(group, getOSarch());
            } else if (args[i].equals("-o") || args[i].equals("--osname")) {
                if (i < n) group = true;

                print(group, getOSname());
            } else if (args[i].equals("-n") || args[i].equals("--pdfnow")) {
                if (i < n) group = true;

                print(group, pdfnow());
            } else if (args[i].equals("-d") || args[i].equals("--pdfdate")) {
                i++;

                if (i >= args.length) {
                    System.err.println(
                            String.format("filename expected after %s", args[i - 1]));
                    System.exit(1);
                }

                if (i < n) group = true;

                if ("".equals(args[i])) {
                    System.out.println();
                } else {
                    System.out.println(pdfDate(fileFromTeXPath(args[i])));
                }
            } else if (args[i].equals("-s") || args[i].equals("--filesize")) {
                i++;

                if (i >= args.length) {
                    System.err.println(
                            String.format("filename expected after %s", args[i - 1]));
                    System.exit(1);
                }

                if (i < n) group = true;

                if ("".equals(args[i])) {
                    print(group, "");
                } else {
                    print(group, getFileLength(fileFromTeXPath(args[i])));
                }
            } else if (args[i].equals("-i") || args[i].equals("--list")) {
                i++;

                if (i >= args.length) {
                    System.err.println(
                            String.format("separator and directory name expected after %s",
                                    args[i - 1]));
                    System.exit(1);
                }

                i++;

                if (i >= args.length) {
                    System.err.println(
                            String.format("directory name expected after %s %s",
                                    args[i - 2], args[i - 1]));
                    System.exit(1);
                }

                if (i < n) group = true;

                if ("".equals(args[i])) {
                    print(group, "");
                } else {
                    print(group, getFileList(args[i - 1],
                            new File(fromTeXPath(args[i]))));
                }
            } else if (args[i].equals("-f") || args[i].equals("--filterlist")) {
                i++;

                if (i >= args.length) {
                    System.err.println(
                            String.format(
                                    "separator, regex and directory name expected after %s",
                                    args[i - 1]));
                    System.exit(1);
                }

                i++;

                if (i >= args.length) {
                    System.err.println(
                            String.format("regex and directory name expected after %s %s",
                                    args[i - 2], args[i - 1]));
                    System.exit(1);
                }

                i++;

                if (i >= args.length) {
                    System.err.println(
                            String.format("directory name expected after %s %s",
                                    args[i - 3], args[i - 2], args[i - 1]));
                    System.exit(1);
                }

                if (i < n) group = true;

                if ("".equals(args[i])) {
                    print(group, "");
                } else {
                    print(group, getFilterFileList(
                            args[i - 2], args[i - 1], new File(fromTeXPath(args[i]))));
                }
            } else if (args[i].equals("-u") || args[i].equals("--uri")) {
                i++;

                if (i >= args.length) {
                    System.err.println(
                            String.format("filename expected after %s", args[i - 1]));
                    System.exit(1);
                }

                if (i < n) group = true;

                if ("".equals(args[i])) {
                    print(group, "");
                } else {
                    print(group, fileURI(fileFromTeXPath(args[i])));
                }
            }
            else if (args[i].equals("-p") || args[i].equals("--path"))
            {
                i++;

                if (i >= args.length) {
                    System.err.println(
                            String.format("filename expected after %s", args[i - 1]));
                    System.exit(1);
                }

                if (i < n) group = true;

                if ("".equals(args[i])) {
                    print(group, "");
                } else {
                    print(group, filePath(fileFromTeXPath(args[i])));
                }
            }
            else if (args[i].equals("-e") || args[i].equals("--dirname"))
            {
                i++;

                if (i >= args.length) {
                    System.err.println(
                            String.format("filename expected after %s", args[i - 1]));
                    System.exit(1);
                }

                if (i < n) group = true;

                if ("".equals(args[i])) {
                    print(group, "");
                } else {
                    print(group, 
                     parentPath(fileFromTeXPath(args[i])));
                }
            }
            else if (args[i].equals("-h") || args[i].equals("--help")) {
                syntax();
                System.exit(0);
            } else if (args[i].equals("-v") || args[i].equals("--version")) {
                version();
                System.exit(0);
            } else {
                System.err.println(String.format("unknown option '%s'", args[i]));
                System.exit(1);
            }
        }
    }
    
    /**
     * Gets the system property.
     * @param key Key.
     * @return Value;
     */
    private static String getSystemProperty(String key) {
        
        String value = "";
        
        try {
            value = System.getProperty(key, "");
        }
        catch (SecurityException exception) {
            // quack quack
        }
        
        return value;
        
    }

}
