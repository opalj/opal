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

import org.opalj.bytecode.RTJar
import org.opalj.br.analyses.Project
import org.opalj.br.cp.ConstantPoolBuilder
import org.junit.runner.RunWith
import scala.collection.mutable.ArrayBuffer
import org.scalatest.junit.JUnitRunner
import org.opalj.da.ClassFileReader
import org.scalatest.FunSuite
import java.net.URL
import org.scalatest.ParallelTestExecution
import scala.language.implicitConversions

/**
 * Tests that the rebuild constant pool at least contains all referenced
 * constant pool entries of the original classfile.
 *
 * @author Andre Pacak
 */
@RunWith(classOf[JUnitRunner])
class ConstantPoolBuilderTest extends FunSuite with ParallelTestExecution {

    def isConstantPoolEqual(daClassFile: da.ClassFile, source: URL): Unit = {
        import ConstantPoolBuilderTest._
        val brClassFile =
            brProject.classFilesWithSources.find {
                case (cf, s) ⇒ s == source
            }.get._1
        val rebuildCP = ConstantPoolBuilder(brClassFile)
        var originalCP = daClassFile.constant_pool

        implicit def indexToDaCPE(
            index: da.Constant_Pool_Index): da.Constant_Pool_Entry = originalCP(index)
        implicit def indexToBrCPE(
            index: br.cp.Constant_Pool_Index): br.cp.Constant_Pool_Entry =
            rebuildCP(index)

        def isEqual(
            original: da.Constant_Pool_Entry,
            rebuild: br.cp.Constant_Pool_Entry): Boolean = original match {
            case e: da.CONSTANT_Integer_info ⇒ rebuild match {
                case br.cp.CONSTANT_Integer_info(value) ⇒
                    e.value == value.value
                case _ ⇒ false
            }
            case e: da.CONSTANT_Float_info ⇒ rebuild match {
                case br.cp.CONSTANT_Float_info(value) ⇒
                    (e.value.isNaN && value.value.isNaN) ||
                        e.value == value.value
                case _ ⇒ false
            }
            case e: da.CONSTANT_Long_info ⇒ rebuild match {
                case br.cp.CONSTANT_Long_info(value) ⇒
                    e.value == value.value
                case _ ⇒ false
            }
            case e: da.CONSTANT_Double_info ⇒ rebuild match {
                case br.cp.CONSTANT_Double_info(value) ⇒
                    (e.value.isNaN && value.value.isNaN) ||
                        e.value == value.value
                case _ ⇒ false
            }
            case e: da.CONSTANT_Utf8_info ⇒ rebuild match {
                case br.cp.CONSTANT_Utf8_info(value) ⇒
                    e.value == value
                case _ ⇒ false
            }
            case e: da.CONSTANT_String_info ⇒ rebuild match {
                case br.cp.CONSTANT_String_info(string_index) ⇒
                    isEqual(e.string_index, string_index)
                case _ ⇒ false
            }
            case e: da.CONSTANT_Class_info ⇒ rebuild match {
                case br.cp.CONSTANT_Class_info(name_index) ⇒
                    isEqual(e.name_index, name_index)
                case _ ⇒ false
            }
            case e: da.CONSTANT_NameAndType_info ⇒ rebuild match {
                case br.cp.CONSTANT_NameAndType_info(name_index, type_index) ⇒
                    isEqual(e.name_index, name_index) &&
                        isEqual(e.descriptor_index, type_index)
                case _ ⇒ false
            }
            case e: da.CONSTANT_MethodType_info ⇒ rebuild match {
                case br.cp.CONSTANT_MethodType_info(descriptorIndex) ⇒
                    isEqual(e.descriptor_index, descriptorIndex)
                case _ ⇒ false
            }
            case e: da.CONSTANT_MethodHandle_info ⇒ rebuild match {
                case br.cp.CONSTANT_MethodHandle_info(refKind, refIndex) ⇒
                    e.reference_kind == refKind &&
                        isEqual(e.reference_index, refIndex)
                case _ ⇒ false
            }
            case e: da.CONSTANT_Fieldref_info ⇒ rebuild match {
                case br.cp.CONSTANT_Fieldref_info(class_index, name_and_type_index) ⇒
                    isEqual(e.class_index, class_index) &&
                        isEqual(e.name_and_type_index, name_and_type_index)
                case _ ⇒ false
            }
            case e: da.CONSTANT_Methodref_info ⇒ rebuild match {
                case br.cp.CONSTANT_Methodref_info(class_index, name_and_type_index) ⇒
                    isEqual(e.class_index, class_index) &&
                        isEqual(e.name_and_type_index, name_and_type_index)
                case _ ⇒ false
            }
            case e: da.CONSTANT_InterfaceMethodref_info ⇒ rebuild match {
                case br.cp.CONSTANT_InterfaceMethodref_info(class_index, name_and_type_index) ⇒
                    isEqual(e.class_index, class_index) &&
                        isEqual(e.name_and_type_index, name_and_type_index)
                case _ ⇒ false
            }
            case e: da.CONSTANT_InvokeDynamic_info ⇒ rebuild match {
                case br.cp.CONSTANT_InvokeDynamic_info(bootstrap_index, name_and_type_index) ⇒
                    isEqual(e.name_and_type_index, name_and_type_index)
                case _ ⇒ false
            }
            case _ ⇒ false

        }

        def collectMissingEntries(
            original: Array[da.Constant_Pool_Entry],
            rebuild: Array[br.cp.Constant_Pool_Entry]): Seq[da.Constant_Pool_Entry] =
            original.filter {
                case ArrayBuffer() ⇒ false
                case null          ⇒ false
                case oEntry @ _ ⇒
                    !rebuild.exists { rEntry ⇒ isEqual(oEntry, rEntry) }
            }

        val missingEntries = collectMissingEntries(originalCP, rebuildCP)

        if (missingEntries.nonEmpty) {
            originalCP = daClassFile.referencedConstantPoolEntries
            val newMissingEntries =
                collectMissingEntries(originalCP, rebuildCP)
            assert(newMissingEntries.isEmpty)
        }
    }

    import ConstantPoolBuilderTest._

    test("should build up the constant pool correctly") {
        daClassFiles.par.foreach {
            case (cf, source) ⇒
                isConstantPoolEqual(cf, source)
        }
    }
}
private object ConstantPoolBuilderTest {
    val daClassFiles = ClassFileReader.ClassFiles(RTJar)
    val brProject = Project(RTJar)
}
