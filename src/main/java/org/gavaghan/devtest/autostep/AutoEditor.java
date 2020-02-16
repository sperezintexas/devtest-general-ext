package org.gavaghan.devtest.autostep;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JTextField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.itko.lisa.editor.CustomEditor;

/**
 * 
 * @author <a href="mailto:mike@gavaghan.org">Mike Gavaghan</a>
 */
public abstract class AutoEditor<T extends AutoStep> extends CustomEditor
{
   /** Logger. */
   static private final Logger LOG = LoggerFactory.getLogger(AutoStep.class);

   /** Map of property names to their UI components. */
   private final Map<String, JComponent> mPropComponents = new HashMap<String, JComponent>();

   /** Map of property names to their UI components. */
   private final Map<String, Property> mPropByName = new HashMap<String, Property>();

   /** Our factory for creating instances of the type parameter. */
   private final TypeParameterFactory<T> mStepFactory;

   /** Our step prototype. */
   private AutoStep mPrototype;

   /** Our concrete type. */
   private final Class<?> mSubClass;

   /** Our step key */
   private final String mStepKey;

   /**
    * Convenience method for getting AutoStep resources.
    * 
    * @param key
    * @param args
    * @return
    */
   private String getString(String key, Object... args)
   {
      return AutoStepUtils.getString(AutoStep.class, key, args);
   }

   /**
    * Find all of the properties from the step's @Property annotations and select
    * UI Components
    */
   private void reflectStepProperties()
   {
      Properties props = mPrototype.getClass().getAnnotation(Properties.class);

      if (props != null)
      {
         for (Property prop : props.value())
         {
            // save the property
            mPropByName.put(prop.value(), prop);

            // create the component for this property
            JComponent comp = createComponent(prop);
            mPropComponents.put(prop.value(), comp);

            /*
            String propName = prop.value();
            Class<?> propType = prop.type();
            String descr = prop.description();
            
            // check if value should be boxed
            Class<?> box = sBoxedTypes.get(propType);
            
            if (box != null)
            {
               LOG.warn(getString("Boxing", propName, box.getSimpleName()));
            
               propType = box;
            }
            
            // look for duplicate name in descriptions
            if (mPropDescr.containsKey(propName))
            {
               throw new RuntimeException(getString("DupeProperty", propName, mSubClass.getName()));
            }
            
            // add description
            if (descr.length() == 0) descr = propName; // default
            if (prop.localized()) descr = AutoStepUtils.getString(mSubClass, descr);
            mPropDescr.put(propName, descr);
            if (LOG.isDebugEnabled()) LOG.debug("Property added.  " + propName + ": " + descr);
            
            // add default values
            mPropValues.put(propName, getDefault(propType));
            
            // add type
            mPropTypes.put(propName, propType);
            */
         }
      }
      else
      {
         LOG.warn(getString("LogNoProperties", mSubClass.getName()));
      }
   }

   /**
    * Start reflecting on the concrete type
    * 
    * @param stepType the AutoStep we edit
    */
   protected AutoEditor(Class<T> stepType)
   {
      try
      {
         // build a type factory so we can get a prototype instance of the step
         mStepFactory = new TypeParameterFactory<T>(stepType);
         mPrototype = mStepFactory.get();

         mStepKey = mPrototype.getStepKey();

         // get concrete type
         mSubClass = getClass();
         if (LOG.isDebugEnabled()) LOG.debug("Constructing AutoEditor of type: " + mSubClass.getName());

         LOG.debug("About to reflecy step properties");
         reflectStepProperties();
      }
      catch (RuntimeException exc)
      {
         String text = getString("FailedToInstantiate", getClass().getName());
         LOG.error(text, exc);
         throw new RuntimeException(text, exc);
      }
   }

   /**
    * Create the UI component for the Property.
    * 
    * FIXME support default values
    * 
    * @param property
    * @return
    */
   protected JComponent createComponent(Property property)
   {
      Class<?> propType = property.type();
      JComponent comp;

      if (String.class.equals(propType))
      {
         // FIXME support password fields
         // FIXME support text areas
         comp = new JTextField("");
      }
      else if (Integer.class.equals(propType))
      {
         comp = new JTextField("");
      }
      else if (Boolean.class.equals(propType))
      {
         // FIXME select description from step
         // FIXME allow string alternative
         comp = new JCheckBox("");
      }
      else
      {
         throw new IllegalArgumentException(getString("TypeNotSupported", propType, mSubClass.getName()));
      }

      return comp;
   }

   /*
    * (non-Javadoc)
    * @see com.itko.lisa.editor.CustomEditor#save()
    */
   @Override
   public void save()
   {
      AutoController controller = (AutoController) getController();
      controller.getTestCaseInfo().getTestExec().saveNodeResponse(controller.getName(), controller.getRet());
      AutoStep step = (AutoStep) controller.getAttribute(getStepKey());

      for (String propName : mPropByName.keySet())
      {
         JComponent comp = ((JTextField) getComponent(propName));
         
         // FIXME - this could actually be a JComponent
         Object value = ((JTextField) comp).getText();
         
         step.setProperty(propName, value);
      }
   }
   
   /**
    * Get the JComponent for the property name.
    * 
    * @param propName property name
    * @return the JComponent
    */
   protected JComponent getComponent(String propName)
   {
      Objects.requireNonNull(propName);

      JComponent comp = mPropComponents.get(propName);

      if (comp == null)
      {
         if (comp == null) throw new RuntimeException(getString("NoSuchProperty", propName, mSubClass.getName()));
      }

      return comp;
   }
   
   /**
    * Determine if a parameter is valid
    * @param text
    * @param klass
    * @return
    */
   protected boolean isValid(Property prop, String text)
   {
      try
      {
         AutoStepUtils.parseString(text, prop.type());
         return true;
      }
      catch(IllegalArgumentException exc)
      {
         return false;
      }
   }

   /**
    * Get the context key.
    * 
    * @return the context key
    */
   public String getStepKey()
   {
      return mStepKey;
   }

   /*
    * (non-Javadoc)
    * @see com.itko.lisa.editor.CustomEditor#isEditorValid()
    */
   @Override
   public String isEditorValid()
   {
      for (String propName : mPropByName.keySet())
      {
         // FIXME handle mandatories

         Property prop = mPropByName.get(propName);
         JComponent comp = getComponent(propName);

         // if it's a text field
         if (comp instanceof JTextField)
         {
            String text = ((JTextField) comp).getText().trim();
            
            if (text.length() == 0)
            {
               return "Please specify a '" + mPrototype.getDescription(propName) + "'";
            }
            else if (!isValid(prop, text))
            {
               return "Please specify a valid value for '" + mPrototype.getDescription(propName) + "'";
            }
         }
      }
      
      return null;
   }
}
