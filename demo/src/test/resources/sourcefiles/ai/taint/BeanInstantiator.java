package ai.taint;

import javax.management.ReflectionException;
import javax.management.RuntimeOperationsException;

import com.sun.jmx.mbeanserver.MBeanInstantiator;

@SuppressWarnings("restriction")
public class BeanInstantiator {
	
    public Class<?> findClass(String className, ClassLoader loader)
            throws ReflectionException {

            return loadClass(className,loader);
    }
	
	static Class<?> loadClass(String className, ClassLoader loader)
            throws ReflectionException {

            Class<?> theClass;
            if (className == null) {
                throw new RuntimeOperationsException(new
                    IllegalArgumentException("The class name cannot be null"),
                                  "Exception occurred during object instantiation");
            }
            try {
                if (loader == null)
                    loader = MBeanInstantiator.class.getClassLoader();
                if (loader != null) {
                    theClass = Class.forName(className, false, loader);
                } else {
                    theClass = Class.forName(className);
                }
            } catch (ClassNotFoundException e) {
                throw new ReflectionException(e,
                "The MBean class could not be loaded");
            }
            return theClass;
        }

}
