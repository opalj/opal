/* BSD 2-Clause License - see OPAL/LICENSE for details. */
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
 * @author Florian Kübler
 * @author Michael Eichberg
 */
class L1FieldMutabilityAnalysis private[analyses] (val project: SomeProject) extends FPCFAnalysis {

    final val tacai = project.get(DefaultTACAIKey)
    final val typeExtensibility = project.get(TypeExtensibilityKey)
    final val closedPackages = project.get(ClosedPackagesKey)
    final val fieldAccessInformation = project.get(FieldAccessInformationKey)

    def doDetermineFieldMutability(entity: Entity): PropertyComputationResult = {
        entity match {
            case field: Field ⇒ determineFieldMutability(field)
            case _ ⇒
                val m = entity.getClass.getName+"is not an org.opalj.br.Field"
                throw new IllegalArgumentException(m)
        }
    }

    /**
     * Analyzes the mutability of private non-final fields.
     *
     * This analysis is only ''soundy'' if the class file does not contain native methods.
     * If the analysis is schedulued using its companion object all class files with
     * native methods are filtered.
     */
    private[analyses] def determineFieldMutability(field: Field): PropertyComputationResult = {
        if (field.isFinal) {
            return Result(field, DeclaredFinalField)
        }

        val thisType = field.classFile.thisType

        if (field.isPublic) {
            return Result(field, NonFinalFieldByLackOfInformation);
        }

        var classesHavingAccess: Iterator[ClassFile] = Iterator(field.classFile)

        if (field.isProtected || field.isPackagePrivate) {
            if (!closedPackages.isClosed(thisType.packageName)) {
                return Result(field, NonFinalFieldByLackOfInformation)
            };
            classesHavingAccess ++= project.classesPerPackage(thisType.packageName).iterator
        }

        if (field.isProtected) {
            if (typeExtensibility(thisType).isYesOrUnknown) {
                return Result(field, NonFinalFieldByLackOfInformation);
            }
            val subTypes = classHierarchy.allSubclassTypes(thisType, reflexive = false)
            classesHavingAccess ++= subTypes.map(project.classFile(_).get)
        }

        if (classesHavingAccess.exists(_.methods.exists(_.isNative))) {
            return Result(field, NonFinalFieldByLackOfInformation);
        }

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
            val taCode = tacai(method)
            val stmts = taCode.stmts
            val index = taCode.pcToIndex(pc)
            if (index >= 0) {
                val stmtCandidate = stmts(index)
                if (stmtCandidate.pc == pc) {
                    stmtCandidate match {
                        case _: PutStatic[_] ⇒
                            if (!method.isStaticInitializer) {
                                return Result(field, NonFinalFieldByAnalysis)
                            };
                        case stmt: PutField[DUVar[_]] ⇒
                            val objRef = stmt.objRef
                            if (!method.isConstructor ||
                                objRef.asVar.definedBy != SelfReferenceParameter) {
                                // note that here we assume real three address code (flat hierarchy)

                                // for instance fields it is okay if they are written in the constructor
                                // (w.r.t. the currently initialized object!)

                                // If the field that is written is not the one referred to by the
                                // self reference, it is not effectively final.
                                return Result(field, NonFinalFieldByAnalysis)

                            }
                        case _ ⇒ throw new RuntimeException("unexpected field access")
                    }
                } else {
                    // nothing to do as the put field is dead
                }
            }
        }

        Result(field, EffectivelyFinalField)
    }
}

sealed trait L1FieldMutabilityAnalysisScheduler extends ComputationSpecification {

    final override def uses: Set[PropertyKind] = Set.empty

    final override def derives: Set[PropertyKind] = Set(FieldMutability)

    final override type InitializationData = Null
    final def init(p: SomeProject, ps: PropertyStore): Null = null

    def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    def afterPhaseCompletion(p: SomeProject, ps: PropertyStore): Unit = {}

}

/**
 * Executor for the field mutability analysis.
 */
object EagerL1FieldMutabilityAnalysis
    extends L1FieldMutabilityAnalysisScheduler
    with FPCFEagerAnalysisScheduler {

    final override def start(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new L1FieldMutabilityAnalysis(p)
        val fields = p.allFields
        ps.scheduleEagerComputationsForEntities(fields)(analysis.determineFieldMutability)
        analysis
    }
}

/**
 * Executor for the lazy field mutability analysis.
 */
object LazyL1FieldMutabilityAnalysis
    extends L1FieldMutabilityAnalysisScheduler
    with FPCFLazyAnalysisScheduler {

    final override def startLazily(
        p:      SomeProject,
        ps:     PropertyStore,
        unused: Null
    ): FPCFAnalysis = {
        val analysis = new L1FieldMutabilityAnalysis(p)
        ps.registerLazyPropertyComputation(
            FieldMutability.key, analysis.determineFieldMutability
        )
        analysis
    }
}
