/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses
package cg

import org.opalj.br.DeclaredMethod
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.analyses.SomeProject
import org.opalj.collection.immutable.UIDSet
import org.opalj.fpcf.cg.properties.CallersProperty
import org.opalj.fpcf.cg.properties.LoadedClasses
import org.opalj.fpcf.cg.properties.NoCallers
import org.opalj.tac.Assignment
import org.opalj.tac.ExprStmt
import org.opalj.tac.GetStatic
import org.opalj.tac.PutStatic
import org.opalj.tac.Stmt
import org.opalj.tac.fpcf.properties.TACAI

/**
 * For a reachable methods (see [[CallersProperty]]) this class computes the classes that are
 * being loaded (e.g. due to static field accesses).
 *
 * @author Florian Kuebler
 */
class LoadedClassesAnalysis(
        val project: SomeProject
) extends FPCFAnalysis {
    /**
     * If the method in `callersOfMethod` has no callers ([[NoCallers]]), it is not reachable, and
     * its declaring class will not be loaded (at least not via this call).
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
        val callersOfMethod = propertyStore(declaredMethod, CallersProperty.key)
        callersOfMethod match {
            case FinalP(NoCallers) ⇒
                // nothing to do, since there is no caller
                NoResult

            case _: EPK[_, _] ⇒
                throw new IllegalStateException("unexpected state")

            case InterimUBP(NoCallers) ⇒
                // we can not create a dependency here, so the analysis is not allowed to create
                // such a result
                throw new IllegalStateException("illegal immediate result for callers")

            case _: EPS[_, _] ⇒
                if (!declaredMethod.hasSingleDefinedMethod)
                    return NoResult;

                val method = declaredMethod.definedMethod

                if (declaredMethod.declaringClassType ne method.classFile.thisType)
                    return NoResult;

                if (method.body.isEmpty)
                    return NoResult; // we don't analyze native methods

                val tacaiEP = propertyStore(method, TACAI.key)
                if (tacaiEP.hasUBP && tacaiEP.ub.tac.isDefined) {
                    processMethod(declaredMethod, tacaiEP.asEPS)
                } else {
                    InterimPartialResult(
                        Nil,
                        Some(tacaiEP),
                        continuationForTAC(declaredMethod)
                    )
                }
        }
    }
    private[this] def continuationForTAC(
        method: DeclaredMethod
    )(eps: SomeEPS): PropertyComputationResult = {
        eps match {
            case UBP(tac: TACAI) if tac.tac.isDefined ⇒
                processMethod(method, eps.asInstanceOf[EPS[Method, TACAI]])
            case _ ⇒
                InterimPartialResult(
                    Nil,
                    Some(eps),
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
            handleNewReachableMethod(declaredMethod, tacaiEP.ub.tac.get.stmts)

        def update(
            eop: EOptionP[_, LoadedClasses]
        ): Option[EPS[SomeProject, LoadedClasses]] = eop match {
            case InterimUBP(ub) ⇒
                val newUb = ub.classes ++ newLoadedClasses
                // due to monotonicity:
                // the size check sufficiently replaces the subset check
                // todo use index
                if (newUb.size > ub.classes.size)
                    Some(InterimEUBP(project, ub.updated(newLoadedClasses)))
                else
                    None

            case _: EPK[_, _] ⇒
                Some(
                    InterimEUBP(project, LoadedClasses.initial(newLoadedClasses))
                )

            case r ⇒
                throw new IllegalStateException(s"unexpected previous result $r")
        }

        if (tacaiEP.isRefinable) {
            InterimPartialResult(
                if (newLoadedClasses.nonEmpty)
                    Some(PartialResult(propertyStore, LoadedClasses.key, update))
                else
                    None,
                Some(tacaiEP),
                continuationForTAC(declaredMethod)
            )
        } else if (newLoadedClasses.nonEmpty) {
            PartialResult[SomeProject, LoadedClasses](project, LoadedClasses.key, update)
        } else {
            NoResult
        }
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
        dm: DeclaredMethod, stmts: Array[Stmt[V]]
    ): UIDSet[ObjectType] = {
        val method = dm.definedMethod
        val methodDCT = method.classFile.thisType
        assert(dm.declaringClassType eq methodDCT)

        var newLoadedClasses = UIDSet.empty[ObjectType]

        val currentLoadedClassesEPS: EOptionP[SomeProject, LoadedClasses] =
            propertyStore(project, LoadedClasses.key)

        val currentLoadedClasses = currentLoadedClassesEPS match {
            case _: EPK[_, _] ⇒ UIDSet.empty[ObjectType]
            case p: EPS[_, _] ⇒ p.ub.classes
        }

        @inline def isNewLoadedClass(dc: ObjectType): Boolean = {
            !currentLoadedClasses.contains(dc) && !newLoadedClasses.contains(dc)
        }

        // whenever a method is called the first time, its declaring class gets loaded
        // todo what about resolution A <- B <- C: C::foo() and foo is def. in A.
        if (isNewLoadedClass(methodDCT)) {
            // todo only for interfaces with default methods
            newLoadedClasses ++=
                ch.allSupertypes(methodDCT, true).filterNot(currentLoadedClasses.contains)
        }

        if (method.body.isDefined) {
            for (stmt ← stmts) {
                stmt match {
                    // todo is dc sufficient enough
                    case PutStatic(_, dc, _, _, _) if isNewLoadedClass(dc) ⇒
                        newLoadedClasses += dc
                    case Assignment(_, _, GetStatic(_, dc, _, _)) if isNewLoadedClass(dc) ⇒
                        newLoadedClasses += dc
                    case ExprStmt(_, GetStatic(_, dc, _, _)) if isNewLoadedClass(dc) ⇒
                        newLoadedClasses += dc
                    case _ ⇒
                }
            }
        }

        newLoadedClasses
    }
}

object TriggeredLoadedClassesAnalysis extends BasicFPCFTriggeredAnalysisScheduler {

    override def register(
        p: SomeProject, ps: PropertyStore, unused: Null
    ): FPCFAnalysis = {
        val analysis = new LoadedClassesAnalysis(p)
        ps.registerTriggeredComputation(CallersProperty.key, analysis.handleCaller)
        analysis
    }

    override def uses: Set[PropertyBounds] =
        Set(
            PropertyBounds.ub(LoadedClasses),
            PropertyBounds.ub(CallersProperty),
            PropertyBounds.ub(TACAI)
        )

    override def derivesEagerly: Set[PropertyBounds] = Set.empty

    override def derivesCollaboratively: Set[PropertyBounds] =
        Set(PropertyBounds.ub(LoadedClasses))

}
