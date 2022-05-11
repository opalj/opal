/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

/**
 * This attribute stores references to [[ClassFile]] objects that have been generated
 * while parsing the annotated ClassFile.
 *
 * For example, to represent proxy types that have been created
 * by Java8 lambda or method reference expressions.
 *
 * This attribute may only be present while the class file is processed/read
 * and will be removed from the attributes table before any analysis sees the
 * "final" class file.
 *
 * This attribute may occur multiple times in the attributes table of a class file structure.
 *
 * @param   classFiles A sequence consisting of class file objects and "reasons" why the
 *          respective class file was created.
 *
 * @author  Arne Lottmann
 * @author  Michael Eichberg
 */
case class SynthesizedClassFiles(
        classFiles: List[( /*generated*/ ClassFile, /*reason*/ Option[AnyRef])]
) extends Attribute {

    final override val kindId = SynthesizedClassFiles.KindId

    // TODO needs to be reconsidered when we serialize this attribute!
    override def similar(other: Attribute, config: SimilarityTestConfiguration): Boolean = this == other

    override def toString: String = {
        classFiles.map { cfAndReason =>
            val (cf, reason) = cfAndReason
            cf.thisType.toJava + (reason.map(r => s"/*$r*/").getOrElse(""))
        }.mkString("SynthesizedClassFiles(", ", ", ")")
    }
}

object SynthesizedClassFiles {

    final val KindId = 1002

    final val Name = "org.opalj.br.SynthesizedClassFiles"
}
