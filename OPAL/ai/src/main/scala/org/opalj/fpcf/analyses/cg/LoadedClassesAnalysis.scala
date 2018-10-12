/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses
package cg

import org.opalj.br.DeclaredMethod
import org.opalj.br.DefinedMethod
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.SomeProject
import org.opalj.collection.immutable.UIDSet
import org.opalj.fpcf.cg.properties.CallersProperty
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

    /**
     * If the method in `callersOfMethod` has no callers ([[NoCallers]]), it is not reachable, and
     * its declaring class will not be loaded (at least not via this call).
     *
     * If it is not yet known, we register a dependency to it.
     *
     * In case there are definitively some callers, we remove the potential existing dependency
     * and handle the method being newly reachable (i.e. analyse the field accesses of the method
     * and update its declaring class type as reachable)
     *
     * @return the static initializers, that are definitively not yet processed by the call graph
     *         analysis and became reachable here.
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
        val (newCLInits, newLoadedClasses) =
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
                    if (newUb.size > ub.classes.size)
                        Some(IntermediateESimpleP(p, new LoadedClasses(newUb)))
                    else
                        None

                case EPK(p, _) ⇒
                    Some(
                        IntermediateESimpleP(p, new LoadedClasses(newLoadedClasses))
                    )

                case r ⇒
                    throw new IllegalStateException(s"unexpected previous result $r")

            })

            val callersResult = newCLInits.iterator map { clInit ⇒
                PartialResult[DeclaredMethod, CallersProperty](clInit, CallersProperty.key, {
                    case IntermediateESimpleP(_, ub) if !ub.hasCallersWithUnknownContext ⇒
                        Some(IntermediateESimpleP(clInit, ub.updatedWithVMLevelCall()))

                    case _: IntermediateESimpleP[_, _] ⇒
                        None

                    case _: EPK[_, _] ⇒
                        Some(IntermediateESimpleP(clInit, OnlyVMLevelCallers))

                    case r ⇒
                        throw new IllegalStateException(s"unexpected previous result $r")
                })
            }
            Results(Iterator(lcResult) ++ callersResult ++ Iterator(fakeResult))
        } else if (tacaiEP.isRefinable) {
            fakeResult
        } else {
            NoResult
        }
    }

    def continuation(method: DeclaredMethod)(eps: SomeEPS): PropertyComputationResult = {
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
     * @return the static initializers, that became reachable and were not yet processed by the
     *         call graph analysis.
     *
     */
    def handleNewReachableMethod(
        dm: DeclaredMethod, stmts: Array[Stmt[V]]
    ): (Set[DeclaredMethod], UIDSet[ObjectType]) = {
        val method = dm.definedMethod
        val methodDCT = method.classFile.thisType
        assert(dm.declaringClassType eq methodDCT)

        var newCLInits = Set.empty[DeclaredMethod]
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
        if (isNewLoadedClass(methodDCT)) {
            // todo only for interfaces with default methods
            newLoadedClasses ++= ch.allSupertypes(methodDCT)
            LoadedClassesAnalysis.retrieveStaticInitializers(
                methodDCT, declaredMethods, project
            ).foreach(newCLInits += _)
        }

        @inline def loadClass(objectType: ObjectType): Unit = {
            LoadedClassesAnalysis.retrieveStaticInitializers(
                objectType, declaredMethods, project
            ).foreach(newCLInits += _)
            newLoadedClasses += objectType
        }

        if (method.body.isDefined) {
            for (stmt ← stmts) {
                stmt match {
                    case PutStatic(_, dc, _, _, _) if isNewLoadedClass(dc) ⇒
                        loadClass(dc)
                    case Assignment(_, _, GetStatic(_, dc, _, _)) if isNewLoadedClass(dc) ⇒
                        loadClass(dc)
                    case ExprStmt(_, GetStatic(_, dc, _, _)) if isNewLoadedClass(dc) ⇒
                        loadClass(dc)
                    case _ ⇒
                }
            }
        }

        (newCLInits, newLoadedClasses)
    }
}

object LoadedClassesAnalysis {
    /**
     * Retrieves the static initializer of the given type if present.
     */
    def retrieveStaticInitializers(
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
}

object EagerLoadedClassesAnalysis extends FPCFEagerAnalysisScheduler {

    override type InitializationData = LoadedClassesAnalysis

    override def start(
        project:               SomeProject,
        propertyStore:         PropertyStore,
        loadedClassesAnalysis: LoadedClassesAnalysis
    ): FPCFAnalysis = {
        loadedClassesAnalysis
    }

    override def uses: Set[PropertyKind] = Set(LoadedClasses, CallersProperty, TACAI)

    override def derives: Set[PropertyKind] = Set(LoadedClasses, CallersProperty, LoadedClassesFakeProperty)

    override def init(p: SomeProject, ps: PropertyStore): LoadedClassesAnalysis = {
        val analysis = new LoadedClassesAnalysis(p)
        ps.registerTriggeredComputation(CallersProperty.key, analysis.handleCaller)
        analysis
    }

    override def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    override def afterPhaseCompletion(p: SomeProject, ps: PropertyStore): Unit = {}
}
