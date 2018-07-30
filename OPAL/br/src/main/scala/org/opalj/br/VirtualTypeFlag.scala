/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

/**
 * This class file level attribute identifies [[ClassFile]] objects that have
 * no direct representation in the bytecode of a project.
 *
 * Instead, the class file annotated with this attribute was generated to represent
 * a class file object that is either explicitly generated at runtime and then used
 * by the program or is conceptually generated at runtime by the JavaVM, but not exposed
 * to the program.
 * An example of the later case are the call site objects that are generated
 * for `invokedynamic` instructions.
 *
 * However, such classes are generally required to facilitate subsequent analyses.
 *
 * @author Arne Lottmann
 */
case object VirtualTypeFlag extends Attribute {

    final val Name = "org.opalj.br.VirtualTypeFlag"

    final val KindId = 1001

    override def kindId = KindId

    override def similar(other: Attribute, config: SimilarityTestConfiguration): Boolean = {
        this == other
    }

}
