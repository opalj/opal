/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package properties

import org.opalj.br.ObjectType
import org.opalj.br.ElementValue
import org.opalj.br.ElementValuePair
import org.opalj.br.ElementValuePairs
import org.opalj.br.analyses.SomeProject

/**
 * @inheritedDoc
 *
 * Defines commonly useful helper methods.
 *
 * @author Michael Eichberg
 */
abstract class AbstractPropertyMatcher extends org.opalj.fpcf.properties.PropertyMatcher {

    /**
     * Gets the value of the named element (`elementName`) of an annotation's element value pairs.
     * If the element values pairs do not contain the named element, the default values will
     * be looked up. This requires that the class file defining the annotation is part of the
     * analyzed code base.
     *
     * @param p The current project.
     * @param annotationType The type of the annotation.
     * @param evps The element value pairs of a concrete annotation of the given type.
     *          (Recall that elements with default values need not be specified and are also
     *          not stored; they need to be looked up in the defining class file if required.)
     * @param elementName The name of the element-value-pair.
     */
    def getValue(
        p:              SomeProject,
        annotationType: ObjectType,
        evps:           ElementValuePairs,
        elementName:    String
    ): ElementValue = {
        evps.collectFirst {
            case ElementValuePair(`elementName`, value) => value
        }.orElse {
            // get default value ...
            p.classFile(annotationType).get.findMethod(elementName).head.annotationDefault
        }.get
    }

}
