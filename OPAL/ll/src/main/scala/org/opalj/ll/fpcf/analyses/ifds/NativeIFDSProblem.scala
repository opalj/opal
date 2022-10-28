/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll.fpcf.analyses.ifds

import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.fpcf.{EOptionP, FinalEP, InterimEUBP, Property, PropertyKey, PropertyStore}
import org.opalj.fpcf.ifds.Dependees.Getter
import org.opalj.fpcf.ifds.{AbstractIFDSFact, IFDSProblem, IFDSProperty}
import org.opalj.ll.LLVMProjectKey
import org.opalj.si.PropertyStoreKey
import org.opalj.tac.fpcf.analyses.ifds.JavaStatement

abstract class NativeIFDSProblem[Fact <: AbstractIFDSFact, JavaFact <: AbstractIFDSFact](project: SomeProject) extends IFDSProblem[Fact, NativeFunction, LLVMStatement](new NativeForwardICFG(project)) {
    final implicit val propertyStore: PropertyStore = project.get(PropertyStoreKey)
    val llvmProject = project.get(LLVMProjectKey)
    val javaPropertyKey: PropertyKey[Property]

    override def outsideAnalysisContext(callee: NativeFunction): Option[(LLVMStatement, LLVMStatement, Fact, Getter) => Set[Fact]] = callee match {
        case LLVMFunction(function) =>
            function.basicBlockCount match {
                case 0 => Some((_: LLVMStatement, _: LLVMStatement, in: Fact, _: Getter) => Set(in))
                case _ => None
            }
        case JNIMethod(method) => Some(handleJavaMethod(method))
    }

    private def handleJavaMethod(callee: Method)(call: LLVMStatement, successor: LLVMStatement, in: Fact, dependeesGetter: Getter): Set[Fact] = {
        var result = Set.empty[Fact]
        val entryFacts = javaCallFlow(call, callee, in)
        for (entryFact <- entryFacts) { // ifds line 14
            val e = (callee, entryFact)
            val exitFacts: Map[JavaStatement, Set[JavaFact]] =
                dependeesGetter(e, javaPropertyKey).asInstanceOf[EOptionP[(JavaStatement, JavaFact), IFDSProperty[JavaStatement, JavaFact]]] match {
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
                result ++= javaReturnFlow(exitStatement, exitStatementFact, call, in, successor)
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
     * @param source The entity, which is analyzed.
     * @return The facts, which hold after the execution of `statement` under the assumption that
     *         the facts in `in` held before `statement` and `statement` calls `callee`.
     */
    protected def javaCallFlow(
        call:   LLVMStatement,
        callee: Method,
        in:     Fact
    ): Set[JavaFact]

    protected def javaReturnFlow(
        exit:      JavaStatement,
        in:        JavaFact,
        call:      LLVMStatement,
        callFact:  Fact,
        successor: LLVMStatement
    ): Set[Fact]
}
