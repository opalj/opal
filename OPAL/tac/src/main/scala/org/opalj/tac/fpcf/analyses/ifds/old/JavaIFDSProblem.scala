/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ifds.old

import org.opalj.br.analyses.{DeclaredMethods, DeclaredMethodsKey, SomeProject}
import org.opalj.br.cfg.CFGNode
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.br.{DeclaredMethod, ObjectType}
import org.opalj.fpcf._
import org.opalj.ifds.old.IFDSProblem
import org.opalj.ifds.{AbstractIFDSFact, IFDSPropertyMetaInformation}
import org.opalj.tac.cg.TypeProviderKey
import org.opalj.tac.fpcf.analyses.cg.TypeProvider
import org.opalj.tac.fpcf.analyses.ifds.JavaIFDSProblem.V
import org.opalj.tac.fpcf.properties.cg.Callers
import org.opalj.tac.{Assignment, Call, ExprStmt, Stmt}

abstract class JavaIFDSProblem[Fact <: AbstractIFDSFact](project: SomeProject) extends IFDSProblem[Fact, DeclaredMethod, DeclaredMethodJavaStatement, CFGNode](
    new ForwardICFG[Fact]()(project.get(PropertyStoreKey), project.get(TypeProviderKey), project.get(DeclaredMethodsKey))
) {
    /**
     * All declared methods in the project.
     */
    implicit final protected val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)

    final implicit val propertyStore: PropertyStore = project.get(PropertyStoreKey)
    implicit final protected val typeProvider: TypeProvider = project.get(TypeProviderKey)

    override def outsideAnalysisContext(callee: DeclaredMethod): Option[(DeclaredMethodJavaStatement, DeclaredMethodJavaStatement, Set[Fact]) => Set[Fact]] = callee.definedMethod.body.isDefined match {
        case true  => None
        case false => Some((_call: DeclaredMethodJavaStatement, _successor: DeclaredMethodJavaStatement, in: Set[Fact]) => in)
    }

    /**
     * Returns all methods, that can be called from outside the library.
     * The call graph must be computed, before this method may be invoked.
     *
     * @return All methods, that can be called from outside the library.
     */
    protected def methodsCallableFromOutside: Set[DeclaredMethod] =
        declaredMethods.declaredMethods.filter(canBeCalledFromOutside).toSet

    /**
     * Checks, if some `method` can be called from outside the library.
     * The call graph must be computed, before this method may be invoked.
     *
     * @param method The method, which may be callable from outside.
     * @return True, if `method` can be called from outside the library.
     */
    protected def canBeCalledFromOutside(method: DeclaredMethod): Boolean = {
        val FinalEP(_, callers) = propertyStore(method, Callers.key)
        callers.hasCallersWithUnknownContext
    }

    /**
     * Gets the call object for a statement that contains a call.
     *
     * @param call The call statement.
     * @return The call object for `call`.
     */
    protected def asCall(call: Stmt[V]): Call[V] = call.astID match {
        case Assignment.ASTID => call.asAssignment.expr.asFunctionCall
        case ExprStmt.ASTID   => call.asExprStmt.expr.asFunctionCall
        case _                => call.asMethodCall
    }

    override def specialCase(source: (DeclaredMethod, Fact), propertyKey: IFDSPropertyMetaInformation[DeclaredMethodJavaStatement, Fact]): Option[ProperPropertyComputationResult] = {
        val declaredMethod = source._1
        val method = declaredMethod.definedMethod
        val declaringClass: ObjectType = method.classFile.thisType
        /*
        * If this is not the method's declaration, but a non-overwritten method in a subtype, do
        * not re-analyze the code.
        */
        if (declaringClass ne declaredMethod.declaringClassType) Some(baseMethodResult(source, propertyKey))
        super.specialCase(source, propertyKey)
    }

    /**
     * This method will be called if a non-overwritten declared method in a sub type shall be
     * analyzed. Analyzes the defined method of the supertype instead.
     *
     * @param source A pair consisting of the declared method of the subtype and an input fact.
     * @return The result of the analysis of the defined method of the supertype.
     */
    private def baseMethodResult(source: (DeclaredMethod, Fact), propertyKey: IFDSPropertyMetaInformation[DeclaredMethodJavaStatement, Fact]): ProperPropertyComputationResult = {

        def c(eps: SomeEOptionP): ProperPropertyComputationResult = eps match {
            case FinalP(p) => Result(source, p)

            case ep @ InterimUBP(ub: Property) =>
                InterimResult.forUB(source, ub, Set(ep), c)

            case epk =>
                InterimResult.forUB(source, propertyKey.create(Map.empty), Set(epk), c)
        }
        c(propertyStore((declaredMethods(source._1.definedMethod), source._2), propertyKey.key))
    }
}

abstract class JavaBackwardIFDSProblem[IFDSFact <: AbstractIFDSFact, UnbalancedIFDSFact <: IFDSFact with UnbalancedReturnFact[IFDSFact]](project: SomeProject) extends JavaIFDSProblem[IFDSFact](project) with BackwardIFDSProblem[IFDSFact, UnbalancedIFDSFact, DeclaredMethod, DeclaredMethodJavaStatement] {
    /**
     * Checks for the analyzed entity, if an unbalanced return should be performed.
     *
     * @param source The analyzed entity.
     * @return False, if no unbalanced return should be performed.
     */
    def shouldPerformUnbalancedReturn(source: (DeclaredMethod, IFDSFact)): Boolean =
        source._2.isInstanceOf[UnbalancedReturnFact[IFDSFact @unchecked]] || entryPoints.contains(source)
}
