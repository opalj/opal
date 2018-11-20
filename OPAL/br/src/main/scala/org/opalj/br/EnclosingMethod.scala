/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

/**
 * The optional enclosing method attribute of a class.
 *
 * @param     name The name of the enclosing method.
 *            The name is optional, but if defined, the descriptor also has to be defined.
 * @param     descriptor The method descriptor of the enclosing method.
 *            The descriptor is optional, but if defined, the name also has to be defined.
 *
 * @author Michael Eichberg
 */
case class EnclosingMethod(
        clazz:      ObjectType,
        name:       Option[String],
        descriptor: Option[MethodDescriptor]
) extends Attribute {

    assert(name.isDefined == descriptor.isDefined)

    override def kindId: Int = EnclosingMethod.KindId

    override def similar(other: Attribute, config: SimilarityTestConfiguration): Boolean = this == other

}
object EnclosingMethod {

    final val KindId = 10

}
