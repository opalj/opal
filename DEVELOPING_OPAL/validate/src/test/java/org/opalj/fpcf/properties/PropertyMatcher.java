/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties;

import scala.Option;
import scala.collection.Iterable;
import scala.collection.immutable.Set;

import org.opalj.fpcf.Property;
import org.opalj.br.AnnotationLike;
import org.opalj.br.ObjectType;
import org.opalj.br.analyses.Project;

/**
 * Defines a class that, when given a specific entity and its properties, validates if the
 * property is as expected. The class implicitly defines the expectation.
 * <p>
 * Concrete implementations should inherit from <code>AbstractPropertyMatcher</code>.
 * </p>
 *
 * @author Michael Eichberg
 */
public interface PropertyMatcher {

    /**
     * Called by the framework to test if executing the matcher - given the set of
     * actual analyses that are executed - is meaningful.
     *
     * This test is used to filter the creation of useless test cases.
     *
     * @param p The project.
     * @param as The OPAL `ObjectType`'s of the executed analyses.
     */
    default boolean isRelevant(
            Project<?> p, Set<ObjectType> as,
            Object entity, AnnotationLike a) {
        return true;
    }

    /**
     * Tests if the computed property is matched by this matcher.
     *
     * @param p The project.
     * @param as The OPAL `ObjectType`'s of the executed analyses.
     * @param entity The annotated entity.
     * @param a The annotation.
     * @param properties '''All''' properties associated with the given entity.
     *
     * @return 'None' if the property was successfully matched; 'Some(<String>)' if the
     *          property was not successfully matched; the String describes the reason
     *          why the analysis failed.
     */
    Option<String> validateProperty(
            Project<?> p, Set<ObjectType> as,
            Object entity, AnnotationLike a, Iterable<Property> properties);

}
