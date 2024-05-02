/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.alias.line;

import org.opalj.br.fpcf.FPCFAnalysis;
import org.opalj.fpcf.properties.PropertyValidator;
import org.opalj.fpcf.properties.alias.NoAliasMatcher;
import org.opalj.tac.fpcf.analyses.alias.IntraProceduralAliasAnalysis;
import org.opalj.tac.fpcf.analyses.alias.pointsto.AllocationSitePointsToBasedAliasAnalysis;
import org.opalj.tac.fpcf.analyses.alias.pointsto.TypePointsToBasedAliasAnalysis;

import java.lang.annotation.Documented;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;

/**
 * Annotation used to describe a no alias between two referenced alias elements.
 * <p>
 * This annotation allows one or two of the elements two be code elements that cannot be annotated (e.g. uVar)
 * <p>
 * The first element is always the element described by the information given in the following parameters:
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
 * first but using the values starting with "second*". Otherwise, the second element is resolved the same way as a
 * normal {@link org.opalj.fpcf.properties.alias.NoAlias} annotation would resolve its element.
 */
@PropertyValidator(key = "AliasProperty", validator = NoAliasMatcher.class)
@Repeatable(NoAliasLines.class)
@Documented
@Target({TYPE_USE, PARAMETER, METHOD})
@Retention(RetentionPolicy.CLASS)
public @interface NoAliasLine {

    /**
     * A short reasoning why this relation is a NoAlias relation.
     */
    String reason();

    // information about first line

    int lineNumber();

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
    Class<? extends FPCFAnalysis>[] analyses() default {
            AllocationSitePointsToBasedAliasAnalysis.class,
            TypePointsToBasedAliasAnalysis.class,
            IntraProceduralAliasAnalysis.class
    };
}
