/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg

import org.opalj.collection.immutable.UIDSet
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPK
import org.opalj.fpcf.EPS
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.InterimEP
import org.opalj.fpcf.InterimEUBP
import org.opalj.fpcf.InterimPartialResult
import org.opalj.fpcf.InterimUBP
import org.opalj.fpcf.NoResult
import org.opalj.fpcf.PartialResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.UBP
import org.opalj.br.DeclaredMethod
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.BasicFPCFTriggeredAnalysisScheduler
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.tac.fpcf.properties.cg.Callers
import org.opalj.tac.fpcf.properties.cg.LoadedClasses
import org.opalj.tac.fpcf.properties.cg.NoCallers
import org.opalj.tac.fpcf.properties.TACAI

/**
 * For a reachable methods (see [[Callers]]) this class computes the
 * classes that are being loaded (e.g. due to static field accesses).
 *
 * @author Florian Kuebler
 */
class LoadedClassesAnalysis(
        val project: SomeProject
) extends FPCFAnalysis {
    /**
     * If the method in `callersOfMethod` has no callers
     * ([[NoCallers]]), it is not reachable, and its declaring class
     * will not be loaded (at least not via this call).
     *
     * If it is not yet known, we register a dependency to it.
     *
     * In case there are definitively some callers, we remove the potential existing dependency
     * and handle the method as being newly reachable (i.e. analyse the field accesses of the method
     * and update its declaring class type as reachable)
     * Here we add ne classes as being loaded.
     *
     */
    def handleCaller(
        declaredMethod: DeclaredMethod
    ): PropertyComputationResult = {
        val callersOfMethod = propertyStore(declaredMethod, Callers.key)
        callersOfMethod match {
            case FinalP(NoCallers) =>
                // nothing to do, since there is no caller
                NoResult

            case _: EPK[_, _] =>
                throw new IllegalStateException("unexpected state")

            case InterimUBP(NoCallers) =>
                // we can not create a dependency here, so the analysis is not allowed to create
                // such a result
                throw new IllegalStateException("illegal immediate result for callers")

            case _: EPS[_, _] =>
                if (!declaredMethod.hasSingleDefinedMethod)
                    return NoResult;

                val method = declaredMethod.definedMethod
                val declClassType = declaredMethod.declaringClassType

                if (declClassType ne method.classFile.thisType)
                    return NoResult;

                val currentLoadedClasses = getCurrentLoadedClasses()
                if (method.body.isEmpty) {
                    if (!currentLoadedClasses.contains(declClassType)) {
                        // todo only for interfaces with default methods
                        val newLoadedClasses =
                            getSuperclassesNotYetLoaded(declClassType, currentLoadedClasses)

                        PartialResult[SomeProject, LoadedClasses](
                            project, LoadedClasses.key, update(newLoadedClasses)
                        )
                    } else {
                        NoResult
                    }
                } else {
                    val tacaiEP = propertyStore(method, TACAI.key)
                    if (tacaiEP.hasUBP && tacaiEP.ub.tac.isDefined) {
                        processMethod(declaredMethod, tacaiEP.asEPS)
                    } else {
                        InterimPartialResult(
                            Nil,
                            Set(tacaiEP),
                            continuationForTAC(declaredMethod)
                        )
                    }
                }
        }
    }

    private[this] def continuationForTAC(
        method: DeclaredMethod
    )(eps: SomeEPS): PropertyComputationResult = {
        eps match {
            case UBP(tac: TACAI) if tac.tac.isDefined =>
                processMethod(method, eps.asInstanceOf[EPS[Method, TACAI]])
            case _ =>
                InterimPartialResult(
                    Nil,
                    Set(eps),
                    continuationForTAC(method)
                )
        }
    }

    private[this] def processMethod(
        declaredMethod: DeclaredMethod, tacaiEP: EPS[Method, TACAI]
    ): PropertyComputationResult = {
        assert(tacaiEP.hasUBP && tacaiEP.ub.tac.isDefined)

        // the method has callers. we have to analyze it
        val newLoadedClasses =
            handleNewReachableMethod(declaredMethod.declaringClassType, tacaiEP.ub.tac.get.stmts)

        if (tacaiEP.isRefinable) {
            InterimPartialResult(
                if (newLoadedClasses.nonEmpty)
                    Some(PartialResult(propertyStore, LoadedClasses.key, update(newLoadedClasses)))
                else
                    None,
                Set(tacaiEP),
                continuationForTAC(declaredMethod)
            )
        } else if (newLoadedClasses.nonEmpty) {
            PartialResult[SomeProject, LoadedClasses](
                project, LoadedClasses.key, update(newLoadedClasses)
            )
        } else {
            NoResult
        }
    }

    private[this] def update(
        newLoadedClasses: UIDSet[ObjectType]
    )(
        eop: EOptionP[_, LoadedClasses]
    ): Option[InterimEP[SomeProject, LoadedClasses]] = eop match {
        case InterimUBP(ub: LoadedClasses) =>
            val newUb = ub.classes ++ newLoadedClasses
            // due to monotonicity:
            // the size check sufficiently replaces the subset check
            if (newUb.size > ub.classes.size)
                Some(InterimEUBP(project, ub.updated(newLoadedClasses)))
            else
                None

        case _: EPK[_, _] =>
            Some(
                InterimEUBP(project, LoadedClasses(newLoadedClasses))
            )

        case r =>
            throw new IllegalStateException(s"unexpected previous result $r")
    }

    /**
     * For a reachable method, its declaring class will be loaded by the VM (if not done already).
     * In order to ensure this, the `state` will be updated.
     *
     * Furthermore, the method may access static fields, which again may lead to class loading.
     *
     * @return The set of classes being loaded.
     *
     */
    def handleNewReachableMethod(
        declClassType: ObjectType, stmts: Array[Stmt[V]]
    ): UIDSet[ObjectType] = {
        var newLoadedClasses = UIDSet.empty[ObjectType]
        val currentLoadedClasses = getCurrentLoadedClasses()

        @inline def isNewLoadedClass(dc: ObjectType): Boolean = {
            !currentLoadedClasses.contains(dc) && !newLoadedClasses.contains(dc)
        }

        // whenever a method is called the first time, its declaring class gets loaded
        //TODO what about resolution A <- B <- C: C::foo() and foo is def. in A.
        if (isNewLoadedClass(declClassType)) {
            //TODO only for interfaces with default methods
            newLoadedClasses ++= getSuperclassesNotYetLoaded(declClassType, currentLoadedClasses)
        }

        for (stmt <- stmts) {
            stmt match {
                //TODO is dc sufficient?
                case PutStatic(_, dc, _, _, _) if isNewLoadedClass(dc) =>
                    newLoadedClasses += dc
                case Assignment(_, _, GetStatic(_, dc, _, _)) if isNewLoadedClass(dc) =>
                    newLoadedClasses += dc
                case ExprStmt(_, GetStatic(_, dc, _, _)) if isNewLoadedClass(dc) =>
                    newLoadedClasses += dc
                case _ =>
            }
        }

        newLoadedClasses
    }

    private[this] def getSuperclassesNotYetLoaded(
        declClassType: ObjectType, currentLoadedClasses: UIDSet[ObjectType]
    ): UIDSet[ObjectType] = {
        ch.allSupertypes(declClassType, reflexive = true).filterNot(currentLoadedClasses.contains)
    }

    private[this] def getCurrentLoadedClasses(): UIDSet[ObjectType] = {
        val currentLoadedClassesEPS: EOptionP[SomeProject, LoadedClasses] =
            propertyStore(project, LoadedClasses.key)

        currentLoadedClassesEPS match {
            case _: EPK[_, _] => UIDSet.empty
            case p: EPS[_, _] => p.ub.classes
        }
    }
}

object LoadedClassesAnalysisScheduler extends BasicFPCFTriggeredAnalysisScheduler {

    override def requiredProjectInformation: ProjectInformationKeys = Seq.empty

    override def uses: Set[PropertyBounds] = PropertyBounds.ubs(
        LoadedClasses,
        Callers,
        TACAI
    )

    override def triggeredBy: PropertyKey[Callers] = Callers.key

    override def derivesEagerly: Set[PropertyBounds] = Set.empty

    override def derivesCollaboratively: Set[PropertyBounds] = PropertyBounds.ubs(LoadedClasses)

    override def register(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new LoadedClassesAnalysis(p)
        ps.registerTriggeredComputation(triggeredBy, analysis.handleCaller)
        analysis
    }

}
