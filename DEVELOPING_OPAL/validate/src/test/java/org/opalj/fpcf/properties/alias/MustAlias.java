/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.alias;

import org.opalj.br.fpcf.FPCFAnalysis;
import org.opalj.fpcf.properties.PropertyValidator;

import java.lang.annotation.Documented;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;

/**
 * Annotation used to describe a must alias relation between two referenced alias elements.
 * <p>
 * This annotation allows one or two of the elements two be code elements that cannot be annotated (e.g. uVar)
 * <p>
 * If the id value is set to a non-default value, the first element is the annotated construct. The other element is
 * then resolved by searching for an annotation with the same id in the same class.
 * <p>
 * Otherwise, if id is not set, the first element is always the element described by the information given in the following parameters:
 * <ul>
 *     <li>
 *         lineNumber: The line in which the element is used.
 *     </li>
 *     <li>
 *         methodName: The name of the method in which the element is used. If no method name is provided, the method
 *         this annotation is attached to is used.
 *     </li>
 *     <li>
 *         parameterIndex: If the element is used as a parameter in a method call, this value is the index the element has in the parameter list.
 *         -1 is the this Parameter. Other parameter indices start wit 0 and increase by 1.
 *     </li>
 *     <li>
 *         fieldName: If the element is a fieldReference, this value is the name of the field.
 *     </li>
 *     <li>
 *         fieldClass: If the element is a fieldReference, this value is the class in which the referenced field is defined.
 *     </li>
 * </ul>
 *
 * <p>
 * If secondLineNumber set to a non-default value, the second element is resolved the same way as the
 * first but using the values starting with "second*". Otherwise, the second element is the annotated construct.
 */
@PropertyValidator(key = "AliasProperty", validator = MustAliasMatcher.class)
@Repeatable(MustAliases.class)
@Documented
@Target({TYPE_USE, PARAMETER, METHOD})
@Retention(RetentionPolicy.CLASS)
public @interface MustAlias {

    /**
     * A short reasoning why this relation is a MustAlias relation.
     */
    String reason();

    /**
     * The id of this MayAlias relation.
     * It is used to associate this element with the other element that is part of this relation.
     * <p>
     * The id of the relation must be unique within the class.
     * <p>
     * It is only used when both elements can be annotated. In that case, the remaining values will be ignored.
     * If it is not set, the remaining values are used to resolve the elements.
     */
    int id() default -1;

    // information about first line

    int lineNumber() default -1;

    String methodName() default "";

    int parameterIndex() default -1;

    String fieldName() default "";

    Class<?> fieldClass() default Object.class;

    String callerContext() default "";

    // information about second line

    int secondLineNumber() default -1;

    int secondParameterIndex() default -1;

    String secondMethodName() default "";

    String secondFieldName() default "";

    Class<?> secondFieldClass() default Object.class;

    String secondCallerContext() default "";

    //other information

    /**
     * true, iff the second element is the {@code this} parameter of the annotated method.
     */
    boolean thisParameter() default false;

    /**
     * All analyses that should be able to correctly detect this relation.
     */
    Class<? extends FPCFAnalysis>[] analyses() default {};
}
