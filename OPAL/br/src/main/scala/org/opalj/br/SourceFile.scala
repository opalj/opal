/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

/**
 * The source file attribute is an optional attribute in the attributes
 * table of [[org.opalj.br.ClassFile]] objects.
 *
 * @param sourceFile The name of the source file from which this class file was compiled;
 *          it will not contain any path information.
 *
 * @author Michael Eichberg
 */
case class SourceFile(sourceFile: String) extends Attribute {

    override def kindId: Int = SourceFile.KindId

    override def similar(other: Attribute, config: SimilarityTestConfiguration): Boolean = this == other

}
object SourceFile {

    final val KindId = 17

}
