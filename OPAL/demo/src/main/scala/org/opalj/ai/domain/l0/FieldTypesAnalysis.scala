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
package ai
package domain
package l0

import java.net.URL
import org.opalj.br.analyses.{ OneStepAnalysis, AnalysisExecutor, BasicReport, Project }
import org.opalj.br.{ ClassFile, Method, Field, Code }
import org.opalj.br.{ ObjectType, FieldType }
import org.opalj.ai.Domain
import org.opalj.ai.Computation
import org.opalj.ai.ComputationWithSideEffectOnly
import org.opalj.ai.domain
import org.opalj.collection.immutable.UIDSet
import org.opalj.br.UpperTypeBound

/**
 * This analysis performs a simple abstract interpretation of all methods of a class
 * to identify fields that are always initialized with null or which are always assigned
 * an object that is a subtype of the field's declared type.
 *
 * @note
 * WE IGNORE THOSE FIELDS WHICH SEEMS TO BE ALWAYS NULL, BUT FOR WHICH
 * WE CAN EITHER FIND A "SETTER METHOD" OR "A CONSTANT WITH THE NAME OF
 * THE VARIABLE". FURTHERMORE, IF THE CLASS DEFINES NO PUBLIC OR PROTECTED
 * CONSTRUCTOR THE FIELDS ARE ALSO IGNORED.
 *
 * THESE FIELDS ARE OFTEN INITIALZED - AT RUNTIME - BY SOME CODE OUTSIDE
 * THE SCOPE OF "PURE" JAVA BASED ANALYSeS.
 *
 * E.G., WE IGNORE THE FOLLOWING FIELDS FROM JAVA 8:
 *  - [BY NAME] java.util.concurrent.FutureTask{ runner:null // Originaltype: java.lang.Thread }
 *  - [BY NAME] java.nio.channels.SelectionKey{ attachment:null // Originaltype: java.lang.Object }
 *  - [BY SETTER] java.lang.System{ err:null // Originaltype: java.io.PrintStream }
 *  - [BY SETTER] java.lang.System{ in:null // Originaltype: java.io.InputStream }
 *  - [BY SETTER] java.lang.System{ out:null // Originaltype: java.io.PrintStream }
 *  - [BY CONSTRUCTOR] java.net.InterfaceAddress{ address:null // Originaltype: java.net.InetAddress }
 *
 * '''[UPDATE BY NATIVE CODE...] sun.nio.ch.sctp.ResultContainer{ value:null // Originaltype: java.lang.Object }'''
 *
 * THE FOLLOWING FIELDS ARE "REALLY" NULL in JAVA 8 (1.8.0 - 1.8.0_25):
 *  - [OK] java.util.TimeZone{ NO_TIMEZONE:null // Originaltype: java.util.TimeZone }
 *  - [OK - DEPRECATED] java.security.SecureRandom{ digest:null // Originaltype: java.security.MessageDigest }
 *
 * The reason/purpose is not 100% clear:
 *  - [OK] javax.swing.JList$AccessibleJList$AccessibleJListChild{ accessibleContext:null // Originaltype: javax.accessibility.AccessibleContext }
 *  - [OK] javax.swing.JList$AccessibleJList$AccessibleJListChild{ component:null // Originaltype: java.awt.Component }
 *  - [OK] com.sun.corba.se.impl.io.IIOPInputStream{ abortIOException:null // Originaltype: java.io.IOException }
 *  - [OK] com.sun.corba.se.impl.orb.ORBImpl{ codeBaseIOR:null // Originaltype: com.sun.corba.se.spi.ior.IOR }
 *  - [OK - ACCIDENTIALLY CREATED?] com.sun.org.apache.xpath.internal.jaxp.XPathImpl{ d:null // Originaltype: org.w3c.dom.Document }
 *  - [OK - LEGACY CODE?] javax.swing.JPopupMenu{ margin:null // Originaltype: java.awt.Insets }
 *  - [OK - LEGACY CODE?] sun.audio.AudioDevice{ mixer:null // Originaltype: javax.sound.sampled.Mixer }
 *  - [OK - RESERVED FOR FUTURE USAGE] com.sun.corba.se.impl.corba.ServerRequestImpl{ _ctx:null // Originaltype: org.omg.CORBA.Context }
 *  - [OK - LEGACY CODE?] com.sun.java.swing.plaf.motif.MotifPopupMenuUI{ border:null // Originaltype: javax.swing.border.Border }
 *  - [OK - LEGACY CODE?] com.sun.media.sound.SoftSynthesizer{ testline:null // Originaltype: javax.sound.sampled.SourceDataLine }
 *  - [OK - LEGACY CODE?] com.sun.org.apache.xerces.internal.impl.xs.XMLSchemaLoader{ fSymbolTable:null // Originaltype: com.sun.org.apache.xerces.internal.util.SymbolTable }
 *  - [OK - LEGACY CODE] com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl{ _piParams:null // Originaltype: java.util.Hashtable }
 *  - [OK - RSERVED FOR FUTURE USAGE?] com.sun.org.apache.xml.internal.dtm.ref.DTMDefaultBase{ m_namespaceLists:null // Originaltype: java.util.Vector }
 *  - [OK - LEGACY CODE?] sun.awt.motif.MFontConfiguration{ fontConfig:null // Originaltype: sun.awt.FontConfiguration }
 *  - [OK - LEGACY CODE?] sun.print.PSStreamPrintJob{ reader:null // Originaltype: java.io.Reader }
 *
 * @author Michael Eichberg
 */
object FieldTypesAnalysis extends AnalysisExecutor {

    import scala.collection.mutable.{ Map ⇒ MutableMap }

    class FieldTypesAnalysisDomain(
        override val project: Project[java.net.URL],
        val classFile: ClassFile)
            extends Domain
            with domain.TheProject[java.net.URL]
            with domain.ProjectBasedClassHierarchy
            with domain.TheClassFile
            with domain.TheCode
            with domain.DefaultDomainValueBinding
            with domain.ThrowAllPotentialExceptionsConfiguration
            with domain.l0.DefaultTypeLevelIntegerValues
            with domain.l0.DefaultTypeLevelLongValues
            with domain.l0.DefaultTypeLevelFloatValues
            with domain.l0.DefaultTypeLevelDoubleValues
            with domain.l0.DefaultPrimitiveValuesConversions
            with domain.l0.TypeLevelFieldAccessInstructions
            with domain.l0.TypeLevelInvokeInstructions
            with domain.l0.DefaultReferenceValuesBinding
            with domain.DefaultHandlingOfMethodResults
            with domain.IgnoreSynchronization {

        val thisClassType: ObjectType = classFile.thisType

        private[this] var currentCode: Code = null

        /**
         * Sets the method that is currently analyzed. This method '''must not be called'''
         * during the abstract interpretation of a method. It is allowed to be called
         * before this domain is used for the first time and immediately after the
         * abstract interpretation of the method has completed/before the next interpreation
         * starts.
         */
        def setMethodContext(method: Method): Unit = {
            currentCode = method.body.get
        }

        def code: Code = currentCode

        // Map of fieldNames (that are relevant) and the (refined) type information
        private[this] val fieldInformation: MutableMap[String /*FieldName*/ , Option[DomainValue]] = {
            val relevantFields: Iterable[String] =
                for {
                    field ← classFile.fields
                    if field.fieldType.isObjectType
                    fieldType = field.fieldType.asObjectType

                    // test that there is some potential for specialization
                    if !project.classFile(fieldType).map(_.isFinal).getOrElse(false)
                    if classHierarchy.hasSubtypes(fieldType).isYes

                    // test that the initialization can be made by the declaring class only:
                    if field.isFinal || field.isPrivate
                } yield { field.name }
            MutableMap.empty ++ relevantFields.map(_ -> None)
        }

        def hasCandidateFields: Boolean = fieldInformation.nonEmpty

        def candidateFields: Iterable[String] = fieldInformation.keys

        def fieldsWithRefinedTypes: Seq[((ClassFile, Field), UpperTypeBound)] = {
            val refinedFields =
                for {
                    field ← classFile.fields
                    Some(ReferenceValue(fieldValue)) ← fieldInformation.get(field.name)
                    upperTypeBound = fieldValue.upperTypeBound
                    if (upperTypeBound.size != 1) || (upperTypeBound.first ne field.fieldType)
                } yield {
                    ((classFile, field), upperTypeBound)
                }

            refinedFields
        }

        private def updateFieldInformation(
            value: DomainValue,
            declaringClassType: ObjectType,
            name: String): Unit = {
            if ((declaringClassType eq thisClassType) &&
                fieldInformation.contains(name)) {
                fieldInformation(name) match {
                    case Some(previousValue) ⇒
                        if (previousValue ne value) {
                            previousValue.join(Int.MinValue, value) match {
                                case SomeUpdate(newValue) ⇒
                                    fieldInformation.update(name, Some(newValue))
                                case NoUpdate ⇒ /*nothing to do*/
                            }
                        }
                    case None ⇒
                        fieldInformation.update(name, Some(value))
                }
            }
        }

        override def putfield(
            pc: PC,
            objectref: DomainValue,
            value: DomainValue,
            declaringClassType: ObjectType,
            name: String,
            fieldType: FieldType): Computation[Nothing, ExceptionValue] = {

            updateFieldInformation(value, declaringClassType, name)

            super.putfield(pc, objectref, value, declaringClassType, name, fieldType)
        }

        override def putstatic(
            pc: PC,
            value: DomainValue,
            declaringClassType: ObjectType,
            name: String,
            fieldType: FieldType): Computation[Nothing, Nothing] = {

            updateFieldInformation(value, declaringClassType, name)

            super.putstatic(pc, value, declaringClassType, name, fieldType)
        }

    }

    val analysis = new OneStepAnalysis[URL, BasicReport] {

        override def title: String =
            "Tries to derive more precise information about the fields of a class."

        override def description: String =
            "Identifies fields of a class where we can – statically – derive more precise type/value information."

        override def doAnalyze(
            theProject: Project[URL],
            parameters: Seq[String],
            isInterrupted: () ⇒ Boolean) = {
            import org.opalj.util.PerformanceEvaluation.{ time, ns2sec }

            val refinedFieldTypes = time {
                val refinedFieldTypes = for {
                    classFile ← theProject.classFiles.par
                    // this analysis does not support parallelization at a more 
                    // fined-grained level, because we reuse the same domain instance
                    // to perform an abstract interpretation of all methods of the 
                    // same class file
                    domain = new FieldTypesAnalysisDomain(theProject, classFile)
                    if domain.hasCandidateFields
                } yield {
                    // println(classFile.thisType.toJava+" => "+domain.candidateFields.mkString(", "))
                    classFile.methods.foreach { method ⇒
                        if (method.body.isDefined) {
                            domain.setMethodContext(method)
                            BaseAI(classFile, method, domain)
                        }
                    }
                    domain.fieldsWithRefinedTypes
                }
                refinedFieldTypes.flatten
            } { t ⇒ println(f"Analysis time: ${ns2sec(t)}%2.2f seconds.") }

            BasicReport(
                refinedFieldTypes.seq.map { info ⇒
                    val ((classFile, field), upperTypeBound) = info
                    classFile.thisType.toJava+"{ "+
                        field.name+":"+
                        {
                            if (upperTypeBound.isEmpty)
                                "null"
                            else
                                upperTypeBound.map { _.toJava }.mkString(" with ")
                        }+
                        " // Originaltype: "+field.fieldType.toJava+" }"
                }.mkString("\n")+
                    "\n"+
                    "Number of refined field types: "+refinedFieldTypes.size+"\n"
            )
        }
    }
}

