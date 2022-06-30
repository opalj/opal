/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

import net.ceedubs.ficus.Ficus._
import org.opalj.log.OPALLogger
import org.opalj.bi.reader.ClassFileReader
import org.opalj.br.reader.{ClassFileReaderConfiguration => BRClassFileReaderConfiguration}

import scala.collection.immutable.ArraySeq

/**
 *
 * @author Michael Eichberg
 */
trait ClassFileBinding extends ClassFileReader {
    this: ConstantPoolBinding with MethodsBinding with FieldsBinding with AttributeBinding =>

    /**
     * This property determines whether artificial [[SynthesizedClassFiles]] attributes
     * are kept or removed.
     *
     * @note    This setting can be set using the configuration key
     *          `ClassFileBinding.DeleteSynthesizedClassFilesAttributesConfigKey`.
     */
    val deleteSynthesizedClassFilesAttributes: Boolean = {
        import ClassFileBinding.{DeleteSynthesizedClassFilesAttributesConfigKey => Key}
        val deleteConfiguration = config.as[Option[Boolean]](Key)
        val delete: Boolean = deleteConfiguration match {
            case Some(x) => x
            case None =>
                OPALLogger.warn("project configuration", s"the configuration key $Key is not set")
                false
        }
        OPALLogger.info(
            "class file reader",
            if (delete) {
                "information about class files synthesized at parsing time is removed"
            } else {
                "information about class files synthesized at parsing time is kept"
            }
        )
        delete
    }

    type ClassFile = br.ClassFile

    //type Fields = ArraySeq[Field_Info]
    //type Methods = ArraySeq[Method_Info]

    def ClassFile(
        cp:                Constant_Pool,
        minor_version:     Int,
        major_version:     Int,
        access_flags:      Int,
        this_class_index:  Constant_Pool_Index,
        super_class_index: Constant_Pool_Index,
        interfaces:        Interfaces,
        fields:            Fields,
        methods:           Methods,
        attributes:        Attributes
    ): ClassFile = {
        br.ClassFile.reify(
            minor_version, major_version, access_flags,
            cp(this_class_index).asObjectType(cp),
            // to handle the special case that this class file represents java.lang.Object
            {
                if (super_class_index == 0)
                    None
                else
                    Some(cp(super_class_index).asObjectType(cp))
            },
            ArraySeq.from(interfaces).map(cp(_).asObjectType(cp)),
            fields,
            methods,
            attributes
        )
    }

    /**
     * Tests if the class file has a [[SynthesizedClassFiles]] attribute and – if so –
     * extracts the class file and removes the attribute.
     */
    val extractSynthesizedClassFiles: List[ClassFile] => List[ClassFile] = { classFiles =>
        var updatedClassFiles = List.empty[ClassFile]
        var classFilesToProcess = classFiles
        while (classFilesToProcess.nonEmpty) {
            val classFile = classFilesToProcess.head
            classFilesToProcess = classFilesToProcess.tail

            var hasSynthesizedClassFilesAttribute = false
            val newAttributes = classFile.attributes.filterNot { a =>
                if (a.kindId == SynthesizedClassFiles.KindId) {
                    val SynthesizedClassFiles(synthesizedClassFiles) = a
                    synthesizedClassFiles.foreach { cfAndReason =>
                        classFilesToProcess ::= cfAndReason._1
                    }
                    hasSynthesizedClassFilesAttribute = true
                    true
                } else {
                    false
                }
            }
            if (hasSynthesizedClassFilesAttribute && deleteSynthesizedClassFilesAttributes) {
                updatedClassFiles ::= classFile._UNSAFE_replaceAttributes(newAttributes)
            } else {
                updatedClassFiles ::= classFile
            }

        }
        updatedClassFiles
    }

    /**
     * Removes all [[BootstrapMethodTable]] attributes because the `invokedynamic` instructions are
     * either completely resolved by creating code that resembles the code executed by the
     * JVM or the instructions are at least enhanced and have explicit references to the
     * bootstrap methods.
     */
    val removeBootstrapMethodAttribute: List[ClassFile] => List[ClassFile] = { classFiles =>
        var updatedClassFiles = List.empty[ClassFile]
        var classFilesToProcess = classFiles
        while (classFilesToProcess.nonEmpty) {
            val classFile = classFilesToProcess.head
            classFilesToProcess = classFilesToProcess.tail

            val attributes = classFile.attributes
            if (classFile.majorVersion > 50 /* <=> does not have BootstrapMethodTable*/ &&
                attributes.nonEmpty &&
                attributes.exists(_.kindId == BootstrapMethodTable.KindId)) {
                val newAttributes = attributes.filter(_.kindId != BootstrapMethodTable.KindId)
                updatedClassFiles ::= classFile._UNSAFE_replaceAttributes(newAttributes)
            } else {
                updatedClassFiles ::= classFile
            }
        }
        updatedClassFiles
    }

    /* EXECUTED SECOND */ registerClassFilePostProcessor(removeBootstrapMethodAttribute)
    /* EXECUTED FIRST  */ registerClassFilePostProcessor(extractSynthesizedClassFiles)
}

object ClassFileBinding {

    final val DeleteSynthesizedClassFilesAttributesConfigKey = {
        BRClassFileReaderConfiguration.ConfigKeyPrefix+"deleteSynthesizedClassFilesAttributes"
    }

}
