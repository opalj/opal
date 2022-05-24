/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package properties
package return_freshness
import org.opalj.br.AnnotationLike
import org.opalj.br.ObjectType
import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.properties.ReturnValueFreshness

/**
 * A property matcher that checks whether the annotated method has the specified return value
 * freshness property.
 *
 * @author Florian Kuebler
 */
class ReturnValueFreshnessMatcher(val property: ReturnValueFreshness) extends AbstractPropertyMatcher {
    /**
     * Tests if the computed property is matched by this matcher.
     *
     * @param p          The project.
     * @param as         The OPAL `ObjectType`'s of the executed analyses.
     * @param entity     The annotated entity.
     * @param a          The annotation.
     * @param properties '''All''' properties associated with the given entity.
     * @return 'None' if the property was successfully matched; 'Some(<String>)' if the
     *         property was not successfully matched; the String describes the reason
     *         why the analysis failed.
     */
    override def validateProperty(
        p: Project[_], as: Set[ObjectType], entity: scala.Any, a: AnnotationLike, properties: Iterable[Property]
    ): Option[String] = {
        if (!properties.exists {
            case `property` => true
            case _          => false
        }) {
            Some(a.elementValuePairs.head.value.asStringValue.value)
        } else {
            None
        }
    }
}

class PrimitiveReturnValueMatcher
    extends ReturnValueFreshnessMatcher(org.opalj.br.fpcf.properties.PrimitiveReturnValue)

class NoFreshReturnValueMatcher
    extends ReturnValueFreshnessMatcher(org.opalj.br.fpcf.properties.NoFreshReturnValue)

class FreshReturnValueMatcher
    extends ReturnValueFreshnessMatcher(org.opalj.br.fpcf.properties.FreshReturnValue)

class GetterMatcher
    extends ReturnValueFreshnessMatcher(org.opalj.br.fpcf.properties.Getter)
