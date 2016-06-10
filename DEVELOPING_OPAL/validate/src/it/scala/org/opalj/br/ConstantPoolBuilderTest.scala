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

import scala.language.implicitConversions

import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import java.net.URL
import org.opalj.bytecode.RTJar
import org.opalj.br.analyses.Project
import org.opalj.br.cp.ConstantPoolBuilder
import org.opalj.da.ClassFileReader
import org.opalj.da.{ClassFile ⇒ DAClassFile}
import org.opalj.da.{Constant_Pool_Entry ⇒ DAConstant_Pool_Entry}
import org.opalj.br.cp.{Constant_Pool_Entry ⇒ BRConstant_Pool_Entry}
import org.scalatest.Matchers
import org.opalj.bi.reader.Constant_PoolAbstractions

/**
 * Tests that every entry in the rebuild constant pool can also be found in the original
 * constant pool.
 *
 * @author Andre Pacak
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class ConstantPoolBuilderTest extends FunSuite with Matchers {

    import ConstantPoolBuilderTest.brProject
    import ConstantPoolBuilderTest.daClassFiles

    def testConstantPoolCreation(daClassFile: DAClassFile, source: URL): Unit = {
        val daCP = daClassFile.constant_pool
        val fqn = daClassFile.fqn
        val classType = ObjectType(fqn.replace('.', '/'))
        val brClassFile = brProject.classFile(classType).get
        val brCP = ConstantPoolBuilder(brClassFile)
        assert((daCP(0) eq null) || daCP(0).isInstanceOf[Constant_PoolAbstractions#DeferredActionsStore])
        assert((brCP(0) eq null) || brCP(0).isInstanceOf[Constant_PoolAbstractions#DeferredActionsStore])

        val daCPEntries = daCP.view.tail.filter(_ != null).groupBy { _.getClass.getSimpleName }.map(e ⇒ (e._1, e._2.size))
        val brCPEntries = brCP.view.tail.filter(_ != null).groupBy { _.getClass.getSimpleName }.map(e ⇒ (e._1, e._2.size))
        if (!brCPEntries.keySet.subsetOf(daCPEntries.keySet)) {
            fail(s"$fqn: unexpected constant pool entry types: "+(brCPEntries.keySet -- daCPEntries.keySet))
        }

        daCPEntries foreach { daCPEntry ⇒
            val (daCPEntryName, daCPEntryCount) = daCPEntry
            assert(
                !brCPEntries.contains(daCPEntryName) || daCPEntryCount >= brCPEntries(daCPEntryName),
                s"; $fqn:the new constant pool contains more entries of type "+daCPEntryName+"\n"+
                    brCP.filter(cpe ⇒ cpe != null && cpe.getClass.getSimpleName == daCPEntryName).mkString("\n")
            )
        }

        val compressedBRCP = brCP.filter(_ != null)
        if (compressedBRCP.toSet.size != compressedBRCP.length) {
            fail(compressedBRCP.mkString(s"$fqn: the constant pool contains duplicates:\n\t", "\n\t", "\n"))
        }

        // test that the structure is correct (in particular that the first entry is empty and
        // that constant_long and constant_double entries require two indexes)
        assert(brCP(0) == null)
        for (index ← 1 until brCP.length) {
            if (brCP(index) == null)
                assert(brCP(index - 1).isInstanceOf[br.cp.CONSTANT_Long_info] || brCP(index - 1).isInstanceOf[br.cp.CONSTANT_Double_info])

            if (brCP(index).isInstanceOf[br.cp.CONSTANT_Long_info] || brCP(index).isInstanceOf[br.cp.CONSTANT_Double_info])
                assert(
                    index + 1 == brCP.length /*the constant long | double entry is the last entry*/ ||
                        brCP(index + 1) == null,
                    s"$fqn: the constant pool entry ${brCP(index)} does not use two slots"
                )
            else
                assert(index + 1 == brCP.length || brCP(index + 1) != null)
        }

        implicit def idxToDACPE(idx: da.Constant_Pool_Index): DAConstant_Pool_Entry = daCP(idx)
        implicit def idxToBRCPE(idx: br.cp.Constant_Pool_Index): BRConstant_Pool_Entry = brCP(idx)

        // Tests if two entries are equal.
        def isEqual(
            daCPE: DAConstant_Pool_Entry,
            brCPE: BRConstant_Pool_Entry
        ): Boolean = daCPE match {
            case e: da.CONSTANT_Integer_info ⇒
                brCPE match {
                    case br.cp.CONSTANT_Integer_info(value) ⇒ e.value == value.value
                    case _                                  ⇒ false
                }
            case e: da.CONSTANT_Float_info ⇒
                brCPE match {
                    case br.cp.CONSTANT_Float_info(value) ⇒
                        (e.value.isNaN && value.value.isNaN) || e.value == value.value
                    case _ ⇒ false
                }
            case e: da.CONSTANT_Long_info ⇒
                brCPE match {
                    case br.cp.CONSTANT_Long_info(value) ⇒ e.value == value.value
                    case _                               ⇒ false
                }
            case e: da.CONSTANT_Double_info ⇒ brCPE match {
                case br.cp.CONSTANT_Double_info(value) ⇒
                    (e.value.isNaN && value.value.isNaN) || e.value == value.value
                case _ ⇒ false
            }
            case e: da.CONSTANT_Utf8_info ⇒
                brCPE match {
                    case br.cp.CONSTANT_Utf8_info(value) ⇒ e.value == value
                    case _                               ⇒ false
                }

            case e: da.CONSTANT_String_info ⇒ brCPE match {
                case br.cp.CONSTANT_String_info(string_index) ⇒ isEqual(e.string_index, string_index)
                case _                                        ⇒ false
            }
            case e: da.CONSTANT_Class_info ⇒ brCPE match {
                case br.cp.CONSTANT_Class_info(name_index) ⇒ isEqual(e.name_index, name_index)
                case _                                     ⇒ false
            }
            case e: da.CONSTANT_NameAndType_info ⇒ brCPE match {
                case br.cp.CONSTANT_NameAndType_info(name_index, descriptor_index) ⇒
                    isEqual(e.name_index, name_index) && isEqual(e.descriptor_index, descriptor_index)
                case _ ⇒ false
            }
            case e: da.CONSTANT_MethodType_info ⇒ brCPE match {
                case br.cp.CONSTANT_MethodType_info(descriptorIndex) ⇒
                    isEqual(e.descriptor_index, descriptorIndex)
                case _ ⇒ false
            }
            case e: da.CONSTANT_MethodHandle_info ⇒ brCPE match {
                case br.cp.CONSTANT_MethodHandle_info(refKind, refIndex) ⇒
                    e.reference_kind == refKind &&
                        isEqual(e.reference_index, refIndex)
                case _ ⇒ false
            }
            case e: da.CONSTANT_Fieldref_info ⇒ brCPE match {
                case br.cp.CONSTANT_Fieldref_info(class_index, name_and_type_index) ⇒
                    isEqual(e.class_index, class_index) &&
                        isEqual(e.name_and_type_index, name_and_type_index)
                case _ ⇒ false
            }
            case e: da.CONSTANT_Methodref_info ⇒ brCPE match {
                case br.cp.CONSTANT_Methodref_info(class_index, name_and_type_index) ⇒
                    isEqual(e.class_index, class_index) &&
                        isEqual(e.name_and_type_index, name_and_type_index)
                case _ ⇒ false
            }
            case e: da.CONSTANT_InterfaceMethodref_info ⇒ brCPE match {
                case br.cp.CONSTANT_InterfaceMethodref_info(class_index, name_and_type_index) ⇒
                    isEqual(e.class_index, class_index) &&
                        isEqual(e.name_and_type_index, name_and_type_index)
                case _ ⇒ false
            }
            case e: da.CONSTANT_InvokeDynamic_info ⇒ brCPE match {
                case br.cp.CONSTANT_InvokeDynamic_info(bootstrap_index, name_and_type_index) ⇒
                    isEqual(e.name_and_type_index, name_and_type_index)
                case _ ⇒ false
            }

            case _ ⇒ false

        }

        brCP.foreach { brCPE ⇒
            assert(
                brCPE == null || daCP.exists(daCPE ⇒ isEqual(daCPE, brCPE)),
                s"$fqn: the bytecode entry $brCPE has no equivalent; "+
                    brCP.zipWithIndex.map(_.swap).mkString("computed constant pool:\n", "\n", "\n") +
                    daCP.zipWithIndex.map(_.swap).mkString("original constant pool:\n", "\n", "\n")
            )
        }

    }

    test("that the constant pool does not contain unexpected entries") {
        daClassFiles.par.foreach {
            case (cf, source) ⇒
                try {
                    testConstantPoolCreation(cf, source)
                } catch {
                    case t: Throwable ⇒
                        println(t.getMessage)
                        t.printStackTrace()
                        throw t
                }

        }
    }
}

private object ConstantPoolBuilderTest {

    val daClassFiles = ClassFileReader.ClassFiles(RTJar)

    val brProject = Project(RTJar)
}
