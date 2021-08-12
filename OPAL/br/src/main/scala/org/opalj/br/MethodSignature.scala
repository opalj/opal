/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

/**
 * Represents a method signature which consists of the name and descriptor of a method;
 * the signatures of all methods of a class file have to be different.
 *
 * @author Michael Eichberg
 */
final case class MethodSignature(
        name:       String,
        descriptor: MethodDescriptor
) {

    def toJava: String = descriptor.toJava(name)

    override def equals(other: Any): Boolean = {
        other match {
            case that: MethodSignature =>
                this.descriptor == that.descriptor && this.name == that.name
            case _ =>
                false
        }
    }
    override val hashCode: Int = name.hashCode * 13 + descriptor.hashCode
}
