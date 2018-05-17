/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
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
package fpcf
package analyses

import org.opalj.br.ClassFile
import org.opalj.br.Field
import org.opalj.br.analyses.FieldAccessInformationKey
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.cg.ClosedPackagesKey
import org.opalj.br.analyses.cg.TypeExtensibilityKey
import org.opalj.fpcf.properties.DeclaredFinalField
import org.opalj.fpcf.properties.EffectivelyFinalField
import org.opalj.fpcf.properties.FieldMutability
import org.opalj.fpcf.properties.NonFinalFieldByAnalysis
import org.opalj.fpcf.properties.NonFinalFieldByLackOfInformation
import org.opalj.tac.DUVar
import org.opalj.tac.DefaultTACAIKey
import org.opalj.tac.PutField
import org.opalj.tac.PutStatic
import org.opalj.tac.SelfReferenceParameter

/**
 * Simple analysis that checks if a private (static or instance) field is always initialized at
 * most once or if a field is or can be mutated after (lazy) initialization.
 *
 * @note Requires that the 3-address code's expressions are not deeply nested.
 *
 * @author Dominik Helm
 * @author Florian Kuebler
 * @author Michael Eichberg
 */
class L1FieldMutabilityAnalysis private[analyses] (val project: SomeProject) extends FPCFAnalysis {

    final val tacai = project.get(DefaultTACAIKey)
    final val typeExtensibility = project.get(TypeExtensibilityKey)
    final val closedPackagesKey = project.get(ClosedPackagesKey)
    final val fieldAccessInformation = project.get(FieldAccessInformationKey)

    def doDetermineFieldMutability(entity: Entity): PropertyComputationResult = entity match {
        case field: Field ⇒ determineFieldMutability(field)
        case _ ⇒
            val m = entity.getClass.getSimpleName+"is not an org.opalj.br.Field"
            throw new IllegalArgumentException(m)
    }

    /**
     * Analyzes the mutability of private non-final fields.
     *
     * This analysis is only ''soundy'' if the class file does not contain native methods.
     * If the analysis is schedulued using its companion object all class files with
     * native methods are filtered.
     */
    private[analyses] def determineFieldMutability(field: Field): PropertyComputationResult = {
        if (field.isFinal)
            return Result(field, DeclaredFinalField)

        val thisType = field.classFile.thisType

        if (field.isPublic)
            return Result(field, NonFinalFieldByLackOfInformation)

        var classesHavingAccess: Set[ClassFile] = Set(field.classFile)

        if (field.isProtected || field.isPackagePrivate) {
            if (!closedPackagesKey.isClosed(thisType.packageName))
                return Result(field, NonFinalFieldByLackOfInformation)
            classesHavingAccess ++= project.allClassFiles.filter {
                _.thisType.packageName == thisType.packageName
            }
        }

        if (field.isProtected) {
            if (typeExtensibility(thisType).isYesOrUnknown) {
                return Result(field, NonFinalFieldByLackOfInformation)
            }
            val subTypes = classHierarchy.allSubclassTypes(thisType, reflexive = false)
            classesHavingAccess ++= subTypes.map(project.classFile(_).get)
        }

        if (classesHavingAccess.flatMap(_.methods).exists(_.isNative))
            return Result(field, NonFinalFieldByLackOfInformation)

        // We now (compared to the simple one) have to analyze the static initializer as
        // the static initializer can be used to initialize a private field of an instance
        // of the class after the reference to the class and (an indirect) reference to the
        // field has become available. Consider the following example:
        // class X implements Y{
        //
        //     private Object o;
        //
        //     public Object getO() { return o; }
        //
        //     private static X instance;
        //     static {
        //         instance = new X();
        //         Z.register(instance);
        //         // when we reach this point o is now (via getO) publically accessible and
        //         // X is properly initialized!
        //         o = new Object(); // o is mutated...
        //     }
        // }

        for {
            (method, pcs) ← fieldAccessInformation.writeAccesses(field)
            pc ← pcs
        } {
            val stmts = tacai(method).stmts
            stmts.find(_.pc == pc) match {
                case None ⇒ // nothing to do as the put field is dead
                case Some(_: PutStatic[_]) ⇒
                    if (!method.isStaticInitializer)
                        return Result(field, NonFinalFieldByAnalysis)
                case Some(stmt: PutField[DUVar[_]]) ⇒
                    val objRef = stmt.objRef
                    if (!method.isConstructor || objRef.asVar.definedBy != SelfReferenceParameter) {
                        // note that here we assume real three address code (flat hierarchy)

                        // for instance fields it is okay if they are written in the constructor
                        // (w.r.t. the currently initialized object!)

                        // If the field that is written is not the one referred to by the
                        // self reference, it is not effectively final.
                        return Result(field, NonFinalFieldByAnalysis)

                    }
                case _ ⇒ throw new RuntimeException("unexpected field access")
            }
        }
        Result(field, EffectivelyFinalField)
    }
}

trait L1FieldMutabilityAnalysisScheduler extends ComputationSpecification {
    override def uses: Set[PropertyKind] = Set.empty

    override def derives: Set[PropertyKind] = Set(FieldMutability)
}

/**
 * Executor for the field mutability analysis.
 */
object EagerL1FieldMutabilityAnalysis extends L1FieldMutabilityAnalysisScheduler with FPCFEagerAnalysisScheduler {

    def start(project: SomeProject, propertyStore: PropertyStore): FPCFAnalysis = {
        val analysis = new L1FieldMutabilityAnalysis(project)

        val fields = project.allFields

        propertyStore.scheduleEagerComputationsForEntities(fields)(analysis.determineFieldMutability)
        analysis
    }
}

/**
 * Executor for the lazy field mutability analysis.
 */
object LazyL1FieldMutabilityAnalysis extends L1FieldMutabilityAnalysisScheduler with FPCFLazyAnalysisScheduler {

    def startLazily(project: SomeProject, propertyStore: PropertyStore): FPCFAnalysis = {
        val analysis = new L1FieldMutabilityAnalysis(project)
        propertyStore.registerLazyPropertyComputation(
            FieldMutability.key, analysis.determineFieldMutability
        )
        analysis
    }
}
