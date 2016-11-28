package com.dickimawbooks.texosquery;

import java.io.Serializable;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;

/**
 * Class representing an action to be performed by the application.
 * @since 1.2
 */ 
public abstract class QueryAction implements Serializable
{
   public QueryAction()
   {
   }

   public QueryAction(String longForm, QueryActionType type,
      String description)
   {
      this(longForm, null, 0, 0, "", type, description);
   }

   public QueryAction(String longForm, QueryActionType type,
      String description, int minCompat)
   {
      this(longForm, null, 0, 0, "", type, description, minCompat);
   }

   public QueryAction(String longForm, String shortForm, 
     QueryActionType type, String description)
   {
      this(longForm, shortForm, 0, 0, "", type, description);
   }

   public QueryAction(String longForm, String shortForm, 
     QueryActionType type, String description, int minCompat)
   {
      this(longForm, shortForm, 0, 0, "", type, description, minCompat);
   }

   public QueryAction(String longForm, String shortForm, String syntax, 
      QueryActionType type, String description)
   {
      this(longForm, shortForm, 0, 0, syntax, type, description);
   }

   public QueryAction(String longForm, String shortForm,
     int numOptional, int numRequired, String syntax, 
     QueryActionType type, String description)
   {
      this(longForm, shortForm, numOptional, numRequired, 
           syntax, type, description, 0);
   }

   public QueryAction(String longForm, String shortForm,
     int numOptional, int numRequired, String syntax, 
     QueryActionType type, String description, int minCompat)
   {
      if (longForm != null)
      {
         longName = "--"+longForm;
      }

      if (shortForm != null)
      {
         shortName = "-"+shortForm;
      }

      optional = numOptional;
      required = numRequired;

      this.syntax = syntax;
      this.type = type;
      this.description = description;
      this.minCompatibility = minCompat;
   }

   public boolean isAction(String name)
   {
      return (name.equals(longName) || name.equals(shortName));
   }

   public int parseArgs(String[] args, int index)
   throws IllegalArgumentException
   {
      invokedName = args[index++];

      optionalProvided = 0;

      if (optional > 0)
      {
         optionalArgs = new String[optional];
      }

      for (int i = 0; i < optional; i++)
      {
         if (index >= args.length || args[index].startsWith("-"))
         {
            // no optional arguments, skip
            break;
         }

         optionalArgs[optionalProvided++] = args[index++];
      }

      if (required > 0)
      {
         requiredArgs = new String[required];
      }

      for (int i = 0; i < required; i++)
      {
         if (index >= args.length)
         {
            throw new IllegalArgumentException(String.format(
              "Invalid syntax for action '%s'.%nExpected: %s",
              invokedName, getUsage(invokedName)));
         }

         requiredArgs[i] = args[index++];
      }

      return index;
   }

   public int providedOptionCount()
   {
      return optionalProvided;
   }

   public String getOptionalArgument(int index)
   {
      return index < optionalArgs.length ? optionalArgs[index] : null;
   }

   public String getRequiredArgument(int index)
   {
      return requiredArgs[index];
   }

   public String getInvokedName()
   {
      return invokedName;
   }

   public QueryActionType getType()
   {
      return type;
   }

   public String getUsage(String name)
   {
      return String.format("%s%s",
        name, syntax == null || "".equals(syntax) ? "" : " "+syntax);
   }

   public String help()
   {
      String usage;

      if (shortName == null)
      {
         usage = getUsage(longName);
      }
      else
      {
         usage = String.format("%s or %s", getUsage(shortName),
           getUsage(longName));
      }

      int n = usage.length();

      // This could do with a bit of neatening. Some of the
      // descriptions need line wrapping.
      return String.format("%s%n\t%s.%n", usage, description);
   }

   public String doAction(int compatible) throws IllegalArgumentException
   {
      if (compatible < minCompatibility)
      {
         throw new IllegalArgumentException(String.format(
         "'%s' option not available in compatibility mode %d",
         invokedName, compatible));
      }

      return action();
   }

   protected abstract String action();

   /**
    * Make a copy of this object.
    */ 
   public final QueryAction copy()
   {
      try
      {
         ByteArrayOutputStream byteStr = new ByteArrayOutputStream();

         new ObjectOutputStream(byteStr).writeObject(this);

         QueryAction action = (QueryAction) new ObjectInputStream(
           new ByteArrayInputStream(byteStr.toByteArray())).readObject();
         action.same(this);
     
         return action;
      }
      catch (Exception e)
      {
         throw new AssertionError(e);
      }
   }

   private void same(QueryAction action)
   {
      longName = action.longName;
      shortName = action.shortName;
      invokedName = action.invokedName;
      syntax = action.syntax;
      optional = action.optional;
      optionalProvided = action.optionalProvided;
      required = action.required;
      type = action.type;
      description = action.description;
      minCompatibility = action.minCompatibility;

      if (action.optionalArgs != null)
      {
         optionalArgs = new String[action.optionalArgs.length];

         for (int i = 0; i < action.optionalArgs.length; i++)
         {
            optionalArgs[i] = action.optionalArgs[i];
         }
      }

      if (action.requiredArgs != null)
      {
         requiredArgs = new String[action.requiredArgs.length];

         for (int i = 0; i < action.requiredArgs.length; i++)
         {
            requiredArgs[i] = action.requiredArgs[i];
         }
      }
   }

   private String longName=null;
   private String shortName=null;
   private String invokedName=null;
   private String syntax="";
   private int optional=0;
   private int optionalProvided=0;
   private int required=0;
   private String[] requiredArgs=null;
   private String[] optionalArgs=null;
   private QueryActionType type;
   private String description;
   private int minCompatibility=0;
}
