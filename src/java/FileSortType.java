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

/**
 * Indicates the ordering of files in listings.
 * @since 1.2
 */
public enum FileSortType
{
   FILE_SORT_DEFAULT ("default"), 
   FILE_SORT_DATE_ASCENDING ("date-ascending", "date", "date-asc"), 
   FILE_SORT_DATE_DESCENDING ("date-descending", "date-des"),
   FILE_SORT_SIZE_ASCENDING ("size-ascending", "size", "size-asc"), 
   FILE_SORT_SIZE_DESCENDING ("size-descending", "size-des"),
   FILE_SORT_NAME_ASCENDING ("name-ascending", "name", "name-asc"), 
   FILE_SORT_NAME_DESCENDING ("name-descending", "name-des"),
   FILE_SORT_NAME_NOCASE_ASCENDING ("iname-ascending", "iname", "iname-asc"), 
   FILE_SORT_NAME_NOCASE_DESCENDING ("iname-descending", "iname-des"), 
   FILE_SORT_EXT_ASCENDING ("ext-ascending", "ext", "ext-asc"), 
   FILE_SORT_EXT_DESCENDING ("ext-descending", "ext-des");

   private final String name, altName1, altName2;

   FileSortType(String name)
   {
      this(name, null, null);
   }

   FileSortType(String name, String altName)
   {
      this(name, altName, null);
   }

   FileSortType(String name, String altName1, String altName2)
   {
      this.name = name;
      this.altName1 = altName1;
      this.altName2 = altName2;
   }

   /**
    * Returns available option names. 
    */ 
   private String options()
   {
      if (altName1 == null && altName2 == null)
      {
         return name;
      }

      if (altName2 == null)
      {
         return String.format("%s (or %s)", name, altName1);
      }

      return String.format("%s (or %s or %s)", name, altName1, altName2);
   }

   /**
    * Checks if the given type matches this option.
    * @return true if type matches this option
    */ 
   private boolean isOption(String type)
   {
      return type.equals(name) || type.equals(altName1) 
        || type.equals(altName2);
   }

   /**
    * Returns a list of available sort options used in the command
    * line invocation.
    * @return list of options for syntax information.
    */ 
   public static String getFileSortOptions()
   {
      StringBuilder builder = new StringBuilder();

      for (FileSortType t : FileSortType.values())
      {
         if (builder.length() > 0)
         {
            builder.append(", ");
         }

         builder.append(t.options());
      }

      return builder.toString();
   }

   /**
    * Gets the sort type from the given string.
    * @param type the sort type as provided by the command line
    * invocation 
    * @return a FileSortType object that indicates the sort type
    */ 
   public static FileSortType getFileSortType(String type)
   {
      if (type == null)
      {
         return FileSortType.FILE_SORT_DEFAULT;
      }

      for (FileSortType t : FileSortType.values())
      {
         if (t.isOption(type))
         {
            return t;
         }
      }

      throw new IllegalArgumentException( "Invalid sort type: "+type);
   }

   public String toString()
   {
      return name;
   }
}

