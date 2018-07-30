/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package dependencies;

import java.lang.annotation.ElementType;

/**
 * This class was used to create a class file with some well defined properties. The
 * created class is subsequently used by several tests.
 * 
 * NOTE<br />
 * This class is only meant to be (automatically) compiled by OPAL's build script.
 * 
 * @author Thomas Schlosser
 */
public @interface TestAnnotation {
	
    public abstract String stringValue() default "default";

    public abstract Class<?> classValue() default String.class;

    public abstract ElementType enumValue() default ElementType.TYPE;

    public abstract SuppressWarnings annotationValue() default @SuppressWarnings("default");

    public abstract Class<?>[] arrayClassValue() default { String.class, Integer.class };
	
}
