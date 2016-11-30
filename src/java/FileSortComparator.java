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

import java.util.Comparator;
import java.io.File;

public class FileSortComparator implements Comparator<String>
{
   public FileSortComparator(File baseDir, FileSortType sortType)
   {
      this.baseDir = baseDir;
      this.sortType = sortType;
   }

   public int compare(String name1, String name2)
   {
      int idx=-1;
      int result=0;
      long date1=0L;
      long date2=0L;
      long size1=0L;
      long size2=0L;
      String ext1="";
      String ext2="";
      File file1;
      File file2;

      switch (sortType)
      {
         case FILE_SORT_NAME_ASCENDING:
           return name1.compareTo(name2);
         case FILE_SORT_NAME_DESCENDING:
           return name2.compareTo(name1);
         case FILE_SORT_NAME_NOCASE_ASCENDING:
           return name1.compareToIgnoreCase(name2);
         case FILE_SORT_NAME_NOCASE_DESCENDING:
           return name2.compareToIgnoreCase(name1);
         case FILE_SORT_EXT_ASCENDING:

           idx = name1.lastIndexOf(".");

           ext1 = idx > -1 ? name1.substring(idx+1) : "";

           idx = name2.lastIndexOf(".");

           ext2 = idx > -1 ? name2.substring(idx+1) : "";

           result = ext1.compareTo(ext2);

           return result == 0 ? name1.compareTo(name2) : result;

         case FILE_SORT_EXT_DESCENDING:

           idx = name1.lastIndexOf(".");

           ext1 = idx > -1 ? name1.substring(idx+1) : "";

           idx = name2.lastIndexOf(".");

           ext2 = idx > -1 ? name2.substring(idx+1) : "";

           result =  ext2.compareTo(ext1);

           return result == 0 ? name2.compareTo(name1) : result;

         case FILE_SORT_DATE_ASCENDING:

           file1 = new File(baseDir, name1);
           file2 = new File(baseDir, name2);

           try
           {
              date1 = file1.lastModified();
              date2 = file2.lastModified();
           }
           catch (Exception e)
           {// file missing or no read access or for some other
            // reason the last modified date can't be obtained.
           }

           return date1 == date2 ? 0 : (date1 < date2 ? -1 : 0);

         case FILE_SORT_DATE_DESCENDING:

           file1 = new File(baseDir, name1);
           file2 = new File(baseDir, name2);

           try
           {
              date1 = file1.lastModified();
              date2 = file2.lastModified();
           }
           catch (Exception e)
           {// file missing or no read access or for some other
            // reason the last modified date can't be obtained.
           }

           return date1 == date2 ? 0 : (date1 > date2 ? -1 : 0);

         case FILE_SORT_SIZE_ASCENDING:

           file1 = new File(baseDir, name1);
           file2 = new File(baseDir, name2);

           try
           {
              size1 = file1.length();
              size2 = file2.length();
           }
           catch (Exception e)
           {// file missing or no read access or for some other
            // reason the file size can't be obtained.
           }

           return size1 == size2 ? 0 : (size1 < size2 ? -1 : 0);

         case FILE_SORT_SIZE_DESCENDING:

           file1 = new File(baseDir, name1);
           file2 = new File(baseDir, name2);

           try
           {
              size1 = file1.length();
              size2 = file2.length();
           }
           catch (Exception e)
           {// file missing or no read access or for some other
            // reason the file size can't be obtained.
           }

           return size1 == size2 ? 0 : (size1 > size2 ? -1 : 0);
      }

      return 0;
   }

   public static String[] getFileSortOptions()
   {
      return new String[]{"date-ascending", "date-descending",
        "size-ascending", "size-descending",
        "name-ascending", "name-descending",
        "iname-ascending", "iname-descending",
        "ext-ascending", "ext-descending"};
   }

   public static FileSortType getFileSortType(String type)
   {
      if (type == null || "default".equals(type))
      {
         return FileSortType.FILE_SORT_DEFAULT;
      }

      if (type.equals("date") || type.equals("date-asc")
          || type.equals("date-ascending"))
      {
         return FileSortType.FILE_SORT_DATE_ASCENDING;
      }

      if (type.equals("date-des") || type.equals("date-descending"))
      {
         return FileSortType.FILE_SORT_DATE_DESCENDING;
      }

      if (type.equals("size") || type.equals("size-asc")
          || type.equals("size-ascending"))
      {
         return FileSortType.FILE_SORT_SIZE_ASCENDING;
      }

      if (type.equals("size-des") || type.equals("size-descending"))
      {
         return FileSortType.FILE_SORT_SIZE_DESCENDING;
      }

      if (type.equals("name") || type.equals("name-asc")
          || type.equals("name-ascending"))
      {
         return FileSortType.FILE_SORT_NAME_ASCENDING;
      }

      if (type.equals("name-des") || type.equals("name-descending"))
      {
         return FileSortType.FILE_SORT_NAME_DESCENDING;
      }

      if (type.equals("iname") || type.equals("iname-asc")
          || type.equals("iname-ascending"))
      {
         return FileSortType.FILE_SORT_NAME_NOCASE_ASCENDING;
      }

      if (type.equals("iname-des") || type.equals("iname-descending"))
      {
         return FileSortType.FILE_SORT_NAME_NOCASE_DESCENDING;
      }

      if (type.equals("ext") || type.equals("ext-asc")
          || type.equals("ext-ascending"))
      {
         return FileSortType.FILE_SORT_EXT_ASCENDING;
      }

      if (type.equals("ext-des") || type.equals("ext-descending"))
      {
         return FileSortType.FILE_SORT_EXT_DESCENDING;
      }

      throw new IllegalArgumentException( "Invalid sort type: "+type);
   }

   private File baseDir;
   private FileSortType sortType;
}
