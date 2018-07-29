/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package annotations.property;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * An entity along with a specific property value it should have.
 *
 * @author Dominik Helm
 */
@Retention(RUNTIME)
public @interface EP {

    /**
     * The entity that should have a specific property value.
     *
     * Supported entities are:
     * <ul>
     *  <li>Classes are specified using the fully qualified name, e.g.
     *      <code>java/lang/Math</code></li>
     *  <li>Fields are specified by &lt;className&gt;.&lt;fieldName&gt;, e.g.
     *      <code>java/lang/Long.value</code></li>
     *  <li>Methods are specified by &lt;className&gt;.&lt;methodName&gt;&lt;methodDescriptor&gt;,
     *      e.g. <code>java/lang/Math.abs(I)I</code>
     * </ul>
     */
    String e();

    /**
     * The name of the property key of the required property.
     *
     * By convention, it is the simple name of the property class, e.g. <code>pk="Purity"</code>.
     * The actual mapping to the property key is up to the test, though.
     */
    String pk() default "";

    /**
     * The expected entity's property value; should be left empty if the entity should not have
     * the respective property.
     *
     * <i>Implementation note: This can't be a specific enum as it must be able to hold any
     * property.</i>
     */
    String p() default "";
}
