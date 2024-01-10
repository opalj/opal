package org.opalj.fpcf.properties.alias;

import org.opalj.br.fpcf.FPCFAnalysis;
import org.opalj.fpcf.properties.PropertyValidator;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to specify that this element is part of the MustAlias relation with the given ID.
 *
 * @see Alias
 */
@PropertyValidator(key = "AliasProperty", validator = MustAliasMatcher.class)
@Documented
@Target({})
@Retention(RetentionPolicy.CLASS)
public @interface MustAlias {

    /**
     * A short reasoning why this relation is a NoAlias relation.
     */
    String reason() default "No reason Provided";

    /**
     * The id of this NoAlias relation.
     * It is used to associate this element with the other element that is part of this relation.
     * @return The id of this NoAlias relation.
     */
    String id();

    /**
     * All analyses that should be able to correctly detect this relation.
     * @return All analyses that should be able to correctly detect this relation.
     */
    Class<? extends FPCFAnalysis>[] analyses() default {
            //TODO add default analyses
    };

    /**
     * Indicates whether this element is part of a NoAlias relation with null.
     * @return Whether this element is part of a NoAlias relation with null.
     */
    boolean aliasWithNull() default false;
}