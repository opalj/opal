/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses
package cg

import scala.language.existentials

import org.opalj.br.DeclaredMethod
import org.opalj.br.DefinedMethod
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.SomeProject
import org.opalj.collection.immutable.UIDSet
import org.opalj.fpcf.cg.properties.CallersProperty
import org.opalj.fpcf.cg.properties.InstantiatedTypes
import org.opalj.fpcf.cg.properties.LoadedClasses
import org.opalj.fpcf.cg.properties.LoadedClassesFakeProperty
import org.opalj.fpcf.cg.properties.LoadedClassesFakePropertyFinal
import org.opalj.fpcf.cg.properties.LoadedClassesFakePropertyNonFinal
import org.opalj.fpcf.cg.properties.NoCallers
import org.opalj.fpcf.cg.properties.OnlyVMLevelCallers
import org.opalj.tac.Assignment
import org.opalj.tac.ExprStmt
import org.opalj.tac.GetStatic
import org.opalj.tac.PutStatic
import org.opalj.tac.Stmt
import org.opalj.tac.fpcf.properties.TACAI

/**
 * Computes the set of classes that are being loaded by the VM during the execution of the
 * `project`.
 * Extends the call graph analysis (e.g. [[RTACallGraphAnalysis]]) to include calls to static
 * initializers from within the JVM.
 *
 * @author Florian Kuebler
 */
class LoadedClassesAnalysis(
        val project: SomeProject
) extends FPCFAnalysis {
    private val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)

    private case class LCState(
        // only present for non-final values
        var lcDependee:      Option[EOptionP[SomeProject, LoadedClasses]],
        var loadedClassesUB: Option[LoadedClasses],
        var seenClasses:     Int,

        // only present for non-final values
        var itDependee:            Option[EOptionP[SomeProject, InstantiatedTypes]],
        var instantiatedTypesUB:   Option[InstantiatedTypes],
        var seenInstantiatedTypes: Int
    )

    /**
     * For the given project, it registers to the [[LoadedClasses]] and the [[InstantiatedTypes]]
     * and ensures that:
     *     1. For each loaded class, its static initializer is called (see [[CallersProperty]])
     *     2. For each instantiated type, the type is also a loaded class
     */
    def registerToInstantiatedTypesAndLoadedClasses(project: SomeProject): PropertyComputationResult = {
        val (lcDependee, loadedClassesUB) = propertyStore(project, LoadedClasses.key) match {
            case FinalEP(_, loadedClasses)                    ⇒ None → Some(loadedClasses)
            case eps @ IntermediateESimpleP(_, loadedClasses) ⇒ Some(eps) → Some(loadedClasses)
            case epk                                          ⇒ Some(epk) → None
        }

        val (itDependee, instantiatedTypesUB) = propertyStore(project, InstantiatedTypes.key) match {
            case FinalEP(_, instantiatedTypes)                    ⇒ None → Some(instantiatedTypes)
            case eps @ IntermediateESimpleP(_, instantiatedTypes) ⇒ Some(eps) → Some(instantiatedTypes)
            case epk                                              ⇒ Some(epk) → None
        }

        implicit val state: LCState = LCState(
            lcDependee, loadedClassesUB, 0, itDependee, instantiatedTypesUB, 0
        )

        handleInstantiatedTypesAndLoadedClasses()
    }

    private[this] def handleInstantiatedTypesAndLoadedClasses()(
        implicit
        state: LCState
    ): PropertyComputationResult = {
        val loadedClassesUB = state.loadedClassesUB.map(_.classes).getOrElse(UIDSet.empty)

        val unseenLoadedClasses =
            state.loadedClassesUB.map(_.getNewClasses(state.seenClasses)).getOrElse(Iterator.empty)
        val unseenInstantiatedTypes =
            state.instantiatedTypesUB.map(_.getNewTypes(state.seenInstantiatedTypes)).getOrElse(Iterator.empty)

        state.seenClasses = state.loadedClassesUB.map(_.numElements).getOrElse(0)
        state.seenInstantiatedTypes = state.instantiatedTypesUB.map(_.numElements).getOrElse(0)

        var newLoadedClasses = UIDSet.empty[ObjectType]
        for (unseenInstantiatedType ← unseenInstantiatedTypes) {
            // todo load class if not already loaded
            if (!loadedClassesUB.contains(unseenInstantiatedType)) {
                newLoadedClasses += unseenInstantiatedType
            }
        }

        val lcResult = if (newLoadedClasses.nonEmpty) {
            Some(PartialResult[SomeProject, LoadedClasses](project, LoadedClasses.key, {
                case IntermediateESimpleP(p, ub) ⇒
                    val newUb = ub.classes ++ newLoadedClasses
                    // due to monotonicity:
                    // the size check sufficiently replaces the subset check
                    if (newUb.size > ub.classes.size)
                        Some(IntermediateESimpleP(p, ub.updated(newLoadedClasses)))
                    else
                        None

                case EPK(p, _) ⇒
                    Some(
                        IntermediateESimpleP(p, LoadedClasses.initial(newLoadedClasses))
                    )

                case r ⇒
                    throw new IllegalStateException(s"unexpected previous result $r")

            }))
        } else {
            None
        }

        var newCLInits = Set.empty[DeclaredMethod]
        for (newLoadedClass ← unseenLoadedClasses) {
            // todo create result for static initializers
            newCLInits ++= retrieveStaticInitializers(
                newLoadedClass, declaredMethods, project
            )
        }

        val callersResult = newCLInits.iterator map { clInit ⇒
            PartialResult[DeclaredMethod, CallersProperty](clInit, CallersProperty.key, {
                case IntermediateESimpleP(_, ub) if !ub.hasVMLevelCallers ⇒
                    Some(IntermediateESimpleP(clInit, ub.updatedWithVMLevelCall()))

                case _: IntermediateESimpleP[_, _] ⇒
                    None

                case _: EPK[_, _] ⇒
                    Some(IntermediateESimpleP(clInit, OnlyVMLevelCallers))

                case r ⇒
                    throw new IllegalStateException(s"unexpected previous result $r")
            })
        }

        val fakeResult = if (state.lcDependee.isDefined || state.itDependee.isDefined) {
            Some(SimplePIntermediateResult(
                project,
                LoadedClassesFakePropertyNonFinal,
                state.itDependee ++ state.lcDependee,
                continuation
            ))
        } else {
            Some(Result(project, LoadedClassesFakePropertyFinal))
        }

        Results(callersResult ++ lcResult ++ fakeResult)
    }

    private[this] def continuation(
        someEPS: SomeEPS
    )(implicit state: LCState): PropertyComputationResult = someEPS match {
        case FinalEP(_, loadedClasses: LoadedClasses) ⇒
            state.lcDependee = None
            state.loadedClassesUB = Some(loadedClasses)
            handleInstantiatedTypesAndLoadedClasses()
        case IntermediateESimpleP(_, loadedClasses: LoadedClasses) ⇒
            state.lcDependee = Some(someEPS.asInstanceOf[EPS[SomeProject, LoadedClasses]])
            state.loadedClassesUB = Some(loadedClasses)
            handleInstantiatedTypesAndLoadedClasses()
        case FinalEP(_, instantiatedTypes: InstantiatedTypes) ⇒
            state.itDependee = None
            state.instantiatedTypesUB = Some(instantiatedTypes)
            handleInstantiatedTypesAndLoadedClasses()
        case IntermediateESimpleP(_, instantiatedTypes: InstantiatedTypes) ⇒
            state.itDependee = Some(someEPS.asInstanceOf[EPS[SomeProject, InstantiatedTypes]])
            state.instantiatedTypesUB = Some(instantiatedTypes)
            handleInstantiatedTypesAndLoadedClasses()
    }

    /**
     * Retrieves the static initializer of the given type if present.
     */
    private[this] def retrieveStaticInitializers(
        declaringClassType: ObjectType, declaredMethods: DeclaredMethods, project: SomeProject
    ): Set[DefinedMethod] = {
        // todo only for interfaces with default methods
        project.classHierarchy.allSupertypes(declaringClassType, reflexive = true) flatMap { t ⇒
            project.classFile(t) flatMap { cf ⇒
                cf.staticInitializer map { clInit ⇒
                    // IMPROVE: Only return the static initializer if it is not already present
                    declaredMethods(clInit)
                }
            }
        }
    }

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
            case FinalEP(_, NoCallers) ⇒
                // nothing to do, since there is no caller
                NoResult

            case _: EPK[_, _] ⇒
                throw new IllegalStateException("unexpected state")

            case IntermediateESimpleP(_, NoCallers) ⇒
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
                if (tacaiEP.hasProperty) {
                    processMethod(declaredMethod, tacaiEP.asEPS)
                } else {
                    SimplePIntermediateResult(declaredMethod, LoadedClassesFakePropertyNonFinal, Some(tacaiEP), continuation(declaredMethod))
                }
        }
    }

    private[this] def processMethod(
        declaredMethod: DeclaredMethod, tacaiEP: EPS[Method, TACAI]
    ): PropertyComputationResult = {
        assert(tacaiEP.hasProperty)

        // the method has callers. we have to analyze it
        val newLoadedClasses =
            handleNewReachableMethod(declaredMethod, tacaiEP.ub.tac.get.stmts)

        val fakeResult =
            if (tacaiEP.isFinal)
                Result(declaredMethod, LoadedClassesFakePropertyFinal)
            else
                SimplePIntermediateResult(declaredMethod, LoadedClassesFakePropertyNonFinal, Some(tacaiEP), continuation(declaredMethod))

        if (newLoadedClasses.nonEmpty) {
            val lcResult = PartialResult[SomeProject, LoadedClasses](project, LoadedClasses.key, {
                case IntermediateESimpleP(p, ub) ⇒
                    val newUb = ub.classes ++ newLoadedClasses
                    // due to monotonicity:
                    // the size check sufficiently replaces the subset check
                    // todo use index
                    if (newUb.size > ub.classes.size)
                        Some(IntermediateESimpleP(p, ub.updated(newLoadedClasses)))
                    else
                        None

                case EPK(p, _) ⇒
                    Some(
                        IntermediateESimpleP(p, LoadedClasses.initial(newLoadedClasses))
                    )

                case r ⇒
                    throw new IllegalStateException(s"unexpected previous result $r")

            })

            Results(lcResult, fakeResult)
        } else if (tacaiEP.isRefinable) {
            fakeResult
        } else {
            NoResult
        }
    }

    private[this] def continuation(method: DeclaredMethod)(eps: SomeEPS): PropertyComputationResult = {
        eps match {
            case ESimplePS(_, _: TACAI, _) ⇒
                processMethod(method, eps.asInstanceOf[EPS[Method, TACAI]])
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
                ch.allSupertypes(methodDCT).filterNot(currentLoadedClasses.contains)
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

object EagerLoadedClassesAnalysis extends FPCFEagerAnalysisScheduler {

    override type InitializationData = LoadedClassesAnalysis

    override def start(
        project:               SomeProject,
        propertyStore:         PropertyStore,
        loadedClassesAnalysis: LoadedClassesAnalysis
    ): FPCFAnalysis = {
        propertyStore.scheduleEagerComputationForEntity(project)(
            loadedClassesAnalysis.registerToInstantiatedTypesAndLoadedClasses
        )
        loadedClassesAnalysis
    }

    override def uses: Set[PropertyKind] =
        Set(LoadedClasses, InstantiatedTypes, CallersProperty, TACAI)

    override def derives: Set[PropertyKind] =
        Set(LoadedClasses, CallersProperty, LoadedClassesFakeProperty)

    override def init(p: SomeProject, ps: PropertyStore): LoadedClassesAnalysis = {
        val analysis = new LoadedClassesAnalysis(p)
        ps.registerTriggeredComputation(CallersProperty.key, analysis.handleCaller)
        analysis
    }

    override def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    override def afterPhaseCompletion(p: SomeProject, ps: PropertyStore): Unit = {}
}
