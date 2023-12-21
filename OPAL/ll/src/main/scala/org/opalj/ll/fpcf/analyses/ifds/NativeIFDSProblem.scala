/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ll
package fpcf
package analyses
package ifds

import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.InterimEUBP
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyStore
import org.opalj.ifds.AbstractIFDSFact
import org.opalj.ifds.Callable
import org.opalj.ifds.Dependees.Getter
import org.opalj.ifds.IFDSFact
import org.opalj.ifds.IFDSProblem
import org.opalj.ifds.IFDSProperty
import org.opalj.ll.LLVMProjectKey
import org.opalj.tac.DUVar
import org.opalj.tac.LazyDetachedTACAIKey
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.TACode
import org.opalj.tac.fpcf.analyses.ifds.JavaICFG
import org.opalj.tac.fpcf.analyses.ifds.JavaStatement
import org.opalj.value.ValueInformation

abstract class NativeForwardIFDSProblem[Fact <: AbstractIFDSFact, JavaFact <: AbstractIFDSFact](
        project: SomeProject
) extends NativeIFDSProblem[Fact, JavaFact](project, new NativeForwardICFG(project))

abstract class NativeBackwardIFDSProblem[Fact <: AbstractIFDSFact, JavaFact <: AbstractIFDSFact](
        project: SomeProject
) extends NativeIFDSProblem[Fact, JavaFact](project, new NativeBackwardICFG(project))

/**
 * Superclass for all IFDS Problems that analyze native code
 * @tparam Fact The type of flow facts, which are tracked by the concrete analysis.
 * @tparam JavaFact the type of facts generated in the java parts of the program
 *
 * @author Marc Clement
 */
abstract class NativeIFDSProblem[Fact <: AbstractIFDSFact, JavaFact <: AbstractIFDSFact](
        project:           SomeProject,
        override val icfg: NativeICFG
) extends IFDSProblem[Fact, NativeFunction, LLVMStatement](icfg) {
    implicit final val propertyStore: PropertyStore = project.get(PropertyStoreKey)
    val llvmProject: LLVMProject = project.get(LLVMProjectKey)
    val javaPropertyKey: PropertyKey[Property]
    val tacai: Method => TACode[TACMethodParameter, DUVar[ValueInformation]] =
        project.get(LazyDetachedTACAIKey)
    val javaICFG: JavaICFG

    override def createCallable(callable: NativeFunction): Callable = callable

    override def outsideAnalysisContextCall(
        callee: NativeFunction
    ): Option[(LLVMStatement, Option[LLVMStatement], Fact, Seq[Callable], Getter) => Set[Fact]] =
        callee match {
            case LLVMFunction(function) =>
                function.basicBlockCount match {
                    case 0 =>
                        Some(
                            (_: LLVMStatement, _: Option[LLVMStatement], in: Fact, _: Seq[Callable], _: Getter) =>
                                Set(in)
                        )
                    case _ => None
                }
            case JNIMethod(method) => Some(handleJavaMethod(method))
        }

    private def handleJavaMethod(callee: Method)(
        call:            LLVMStatement,
        successor:       Option[LLVMStatement],
        in:              Fact,
        unbCallChain:    Seq[Callable],
        dependeesGetter: Getter
    ): Set[Fact] = {
        var result = Set.empty[Fact]
        val entryFacts = javaICFG
            .startStatements(callee)
            .flatMap(javaCallFlow(_, call, callee, in))
            .map(new IFDSFact(_))
        for (entryFact <- entryFacts) { // ifds line 14
            val e = (callee, entryFact)
            val exitFacts: Map[JavaStatement, Set[JavaFact]] =
                dependeesGetter(e, javaPropertyKey)
                    .asInstanceOf[EOptionP[(JavaStatement, JavaFact), IFDSProperty[JavaStatement, JavaFact]]] match {
                        // cast masks type erasure warning/error, match does not ...
                        case ep: FinalEP[_, IFDSProperty[JavaStatement, JavaFact]] =>
                            ep.p.flows
                        case ep: InterimEUBP[_, IFDSProperty[JavaStatement, JavaFact]] =>
                            ep.ub.flows
                        case _ =>
                            Map.empty
                    }
            for {
                (exitStatement, exitStatementFacts) <- exitFacts // ifds line 15.2
                exitStatementFact <- exitStatementFacts // ifds line 15.3
            } {
                result ++= javaReturnFlow(
                    exitStatement,
                    exitStatementFact,
                    call,
                    in,
                    unbCallChain,
                    successor
                )
            }
        }
        result
    }

    /**
     * Computes the data flow for a call to start edge.
     *
     * @param call The analyzed call statement.
     * @param callee The called method, for which the data flow shall be computed.
     * @param in The fact which holds before the execution of the `call`.
     * @return The facts which hold after the execution of `statement` under the assumption that
     *         the facts in `in` held before `statement` and `statement` calls `callee`.
     */
    protected def javaCallFlow(
        start:  JavaStatement,
        call:   LLVMStatement,
        callee: Method,
        in:     Fact
    ): Set[JavaFact]

    protected def javaReturnFlow(
        exit:         JavaStatement,
        in:           JavaFact,
        call:         LLVMStatement,
        callFact:     Fact,
        unbCallChain: Seq[Callable],
        successor:    Option[LLVMStatement]
    ): Set[Fact]
}
