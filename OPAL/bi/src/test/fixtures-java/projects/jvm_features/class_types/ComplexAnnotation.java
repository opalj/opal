/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package class_types;

/**
 * Defines an annotation which basically covers all features an annotation can have.
 *
 * NOTE<br />
 * This class is only meant to be (automatically) compiled by OPAL's build script.
 *
 * @author Michael Eichberg
 */
public @interface ComplexAnnotation {

    boolean booleanValue() default true;

    byte byteValue() default 1;
    char charValue() default 1;
    short shortValue() default 1;
    int intValue() default 1;
    long longValue() default 1;
    float floatValue() default 1.0f;
    double doubleValue() default 1.0f;

    String stringValue() default "Some String";

    String[] stringValues() default { "String 1", "String2" };

    SomeEnumeration enumValue() default SomeEnumeration.Value1;

    SuppressWarnings annotationValue() default @SuppressWarnings("all");

}
