/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ba

import org.opalj.br.ClassHierarchy
import org.opalj.br.ObjectType
import org.opalj.collection.immutable.UShortPair

import scala.collection.immutable.ArraySeq

/**
 * Builder for a [[org.opalj.br.MethodTemplate]].
 *
 * @author Malte Limmeroth
 * @author Michael Eichberg
 */
class METHOD[T](
        accessModifiers: AccessModifier,
        name:            String,
        descriptor:      String,
        code:            Option[br.CodeAttributeBuilder[T]],
        attributes:      ArraySeq[br.MethodAttributeBuilder]
) {

    /**
     * Returns the build [[org.opalj.br.MethodTemplate]] and its annotations (if any).
     */
    def result(
        classFileVersion:   UShortPair,
        declaringClassType: ObjectType
    )(
        implicit
        classHierarchy: ClassHierarchy = br.ClassHierarchy.PreInitializedClassHierarchy
    ): (br.MethodTemplate, Option[T]) = {
        val descriptor = br.MethodDescriptor(this.descriptor)
        val accessFlags = accessModifiers.accessFlags

        val attributes = this.attributes.map[br.Attribute](attributeBuilder =>
            attributeBuilder(accessFlags, name, descriptor))

        if (code.isDefined) {
            val (attribute, annotations) =
                code.get(classFileVersion, declaringClassType, accessFlags, name, descriptor)
            val method = br.Method(accessFlags, name, descriptor, attributes :+ attribute)
            (method, Some(annotations))
        } else {
            val method = br.Method(accessFlags, name, descriptor, attributes)
            (method, None)
        }
    }

}

object METHOD {

    def apply[T](
        accessModifiers: AccessModifier,
        name:            String,
        descriptor:      String,
        code:            Option[br.CodeAttributeBuilder[T]],
        attributes:      ArraySeq[br.MethodAttributeBuilder]
    ): METHOD[T] = {
        new METHOD(accessModifiers, name, descriptor, code, attributes)
    }

    def apply[T](
        accessModifiers: AccessModifier,
        name:            String,
        descriptor:      String,
        code:            br.CodeAttributeBuilder[T],
        attributes:      ArraySeq[br.MethodAttributeBuilder] = ArraySeq.empty
    ): METHOD[T] = {
        new METHOD(accessModifiers, name, descriptor, Some(code), attributes)
    }

}
