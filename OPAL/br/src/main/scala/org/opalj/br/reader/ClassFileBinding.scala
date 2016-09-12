/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj
package br
package reader

import net.ceedubs.ficus.Ficus._

import org.opalj.bi.reader.ClassFileReader
import org.opalj.log.OPALLogger
import org.opalj.br.reader.{ClassFileReaderConfiguration ⇒ BRClassFileReaderConfiguration}

/**
 *
 * @author Michael Eichberg
 */
trait ClassFileBinding extends ClassFileReader {
    this: ConstantPoolBinding with MethodsBinding with FieldsBinding with AttributeBinding ⇒

    /**
     * This property determines whether artificial [[SynthesizedClassFiles]] attributes
     * are kept or removed.
     *
     * @note	This setting can be set using the configuration key
     * 			`ClassFileBinding.DeleteSynthesizedClassFilesAttributesConfigKey`.
     */
    val deleteSynthesizedClassFilesAttributes: Boolean = {
        import ClassFileBinding.{DeleteSynthesizedClassFilesAttributesConfigKey ⇒ Key}
        val deleteConfiguration = config.as[Option[Boolean]](Key)
        val delete: Boolean = deleteConfiguration match {
            case Some(x) ⇒ x
            case None ⇒
                OPALLogger.warn("project configuration", s"the configruation key $Key is not set")
                false
        }
        if (delete) {
            OPALLogger.info("project configuration", "information about synthesized class files is removed")
        } else {
            OPALLogger.info("project configuration", "information about synthesized class files is kept")
        }
        delete
    }

    type ClassFile = br.ClassFile

    type Fields <: IndexedSeq[Field_Info]
    type Methods <: IndexedSeq[Method_Info]

    def ClassFile(
        cp:            Constant_Pool,
        minor_version: Int, major_version: Int,
        access_flags:      Int,
        this_class_index:  Constant_Pool_Index,
        super_class_index: Constant_Pool_Index,
        interfaces:        IndexedSeq[Constant_Pool_Index],
        fields:            Fields,
        methods:           Methods,
        attributes:        Attributes
    ): ClassFile = {
        br.ClassFile(
            minor_version, major_version, access_flags,
            cp(this_class_index).asObjectType(cp),
            // to handle the special case that this class file represents java.lang.Object
            {
                if (super_class_index == 0)
                    None
                else
                    Some(cp(super_class_index).asObjectType(cp))
            },
            interfaces.map(cp(_).asObjectType(cp)),
            fields,
            methods,
            attributes
        )
    }

    /**
     * Tests if the class file has a [[SynthesizedClassFiles]] attribute and – if so –
     * extracts the class file and removes the attribute.
     */
    val extractSynthesizedClassFiles: List[ClassFile] ⇒ List[ClassFile] = { classFiles ⇒
        var updatedClassFiles = List.empty[ClassFile]
        var classFilesToProcess = classFiles
        while (classFilesToProcess.nonEmpty) {
            val classFile = classFilesToProcess.head
            classFilesToProcess = classFilesToProcess.tail

            var hasSynthesizedClassFilesAttribute = false
            val newAttributes = classFile.attributes.filterNot { a ⇒
                if (a.kindId == SynthesizedClassFiles.KindId) {
                    val SynthesizedClassFiles(synthesizedClassFiles) = a
                    synthesizedClassFiles.foreach { cfAndReason ⇒
                        classFilesToProcess = cfAndReason._1 :: classFilesToProcess
                    }
                    hasSynthesizedClassFilesAttribute = true
                    true
                } else {
                    false
                }
            }
            if (hasSynthesizedClassFilesAttribute && deleteSynthesizedClassFilesAttributes) {
                updatedClassFiles = classFile.copy(attributes = newAttributes) :: updatedClassFiles
            } else {
                updatedClassFiles = classFile :: updatedClassFiles
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
    val removeBootstrapMethodAttribute: List[ClassFile] ⇒ List[ClassFile] = { classFiles ⇒
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
                updatedClassFiles = classFile.copy(attributes = newAttributes) :: updatedClassFiles
            } else {
                updatedClassFiles = classFile :: updatedClassFiles
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
