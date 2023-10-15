package org.opalj.fpcf.properties.alias;

import java.lang.annotation.*;

import static java.lang.annotation.ElementType.*;

/**
 * Annotation to specify that the annotated element is part of one or multiple alias relations.
 * The concrete alias information are specified by the {@link MayAlias}, {@link NoAlias} and {@link MustAlias} arrays
 * <p>
 * An alias relation is identified by the id of the {@link MayAlias}, {@link NoAlias} or {@link MustAlias} annotation.
 * IDs have to be unique within the project to allow for a correct association of the alias relations.
 * For any ID, there have to be exactly two {@link MayAlias}, {@link NoAlias} or {@link MustAlias} annotations
 * with that ID.
 * <p>
 * The {@link MayAlias}, {@link NoAlias} and {@link MustAlias} annotations can be used multiple times within the arrays
 * to specify that the element is part of multiple alias relations.
 *
 * @see MayAlias
 * @see NoAlias
 * @see MustAlias
 */
@Target({ TYPE_USE, PARAMETER, METHOD })
@Retention(RetentionPolicy.CLASS)
@Documented
public @interface Alias {

    /**
     * The {@link MayAlias} relations that the annotated element is part of.
     * @return The {@link MayAlias} relations that the annotated element is part of.
     */
    MayAlias[] mayAlias() default {};

    /**
     * The {@link NoAlias} relations that the annotated element is part of.
     * @return The {@link NoAlias} relations that the annotated element is part of.
     */
    NoAlias[] noAlias() default {};

    /**
     * The {@link MustAlias} relations that the annotated element is part of.
     * @return The {@link MustAlias} relations that the annotated element is part of.
     */
    MustAlias[] mustAlias() default {};
}
