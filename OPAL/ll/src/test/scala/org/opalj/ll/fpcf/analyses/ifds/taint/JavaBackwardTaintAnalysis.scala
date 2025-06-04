/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ll
package fpcf
package analyses
package ifds
package taint

import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.FPCFAnalysis
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.UBP
import org.opalj.ifds.Dependees.Getter
import org.opalj.ifds.Callable
import org.opalj.ifds.IFDSAnalysis
import org.opalj.ifds.IFDSAnalysisScheduler
import org.opalj.ifds.IFDSFact
import org.opalj.ifds.IFDSProperty
import org.opalj.ifds.IFDSPropertyMetaInformation
import org.opalj.ll.LLVMProjectKey
import org.opalj.ll.fpcf.analyses.ifds.JNICallUtil
import org.opalj.ll.fpcf.analyses.ifds.JNIMethod
import org.opalj.ll.fpcf.analyses.ifds.LLVMFunction
import org.opalj.ll.fpcf.analyses.ifds.LLVMStatement
import org.opalj.ll.fpcf.analyses.ifds.NativeBackwardICFG
import org.opalj.ll.llvm.value.Call
import org.opalj.ll.llvm.value.Ret
import org.opalj.ll.llvm.value.Value
import org.opalj.tac.fpcf.analyses.ifds.taint.ArrayElement
import org.opalj.tac.fpcf.analyses.ifds.taint.FlowFact
import org.opalj.tac.fpcf.analyses.ifds.taint.InstanceField
import org.opalj.tac.fpcf.analyses.ifds.taint.JavaBackwardTaintProblem
import org.opalj.tac.fpcf.analyses.ifds.taint.StaticField
import org.opalj.tac.fpcf.analyses.ifds.taint.TaintFact
import org.opalj.tac.fpcf.analyses.ifds.taint.TaintNullFact
import org.opalj.tac.fpcf.analyses.ifds.taint.Variable
import org.opalj.tac.fpcf.analyses.ifds.JavaIFDSProblem
import org.opalj.tac.fpcf.analyses.ifds.JavaMethod
import org.opalj.tac.fpcf.analyses.ifds.JavaStatement
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.tac.fpcf.properties.Taint

/**
 * Implementation of a backward taint analysis for Java code.
 * This is a mostly a demo, especially since the source/sink/sanitize methods are not configurable.
 *
 * @author Nicolas Gross
 */
class SimpleJavaBackwardTaintProblem(p: SomeProject) extends JavaBackwardTaintProblem(p) {
    val llvmProject: LLVMProject = p.get(LLVMProjectKey)
    private val nativeICFG = new NativeBackwardICFG(p)

    /**
     * The analysis starts with the sink function.
     */
    override val entryPoints: Seq[(Method, IFDSFact[TaintFact, JavaStatement])] =
        p.allProjectClassFiles.flatMap {
            case cf if cf.thisType == ObjectType("org/opalj/fpcf/fixtures/taint_xlang/TaintTest") =>
                cf.methods.collect {
                    case m if m.name == "sink" =>
                        m -> new IFDSFact(
                            Variable(JavaIFDSProblem.remapParamAndVariableIndex(0, isStaticMethod = true))
                        )
                }
            case _ => None
        }
    override protected def sanitizesReturnValue(callee: Method): Boolean = callee.name == "sanitize"

    override protected def sanitizesParameter(call: JavaStatement, in: TaintFact): Boolean = false

    /**
     * Create a flow fact, if a source method is called and the returned value is tainted.
     * This is done in callToReturnFlow, because it may be the case that the callee never
     * terminates.
     * In this case, callFlow would never be called and no FlowFact would be created.
     */
    override protected def createFlowFactAtCall(
        call:         JavaStatement,
        in:           TaintFact,
        unbCallChain: Seq[Callable]
    ): Option[FlowFact] = {
        if ((in match {
            case Variable(index) => index == call.index
            case _               => false
        }) && icfg.getCalleesIfCallStatement(call).get.exists(_.name == "source")) {
            val currentMethod = call.callable
            // Avoid infinite loops.
            if (unbCallChain.contains(JavaMethod(currentMethod))) None
            else Some(FlowFact(unbCallChain.prepended(JavaMethod(currentMethod))))
        } else None
    }

    /**
     * When a callee calls the source, we create a FlowFact with the caller's call chain.
     */
    override protected def applyFlowFactFromCallee(
        calleeFact:   FlowFact,
        caller:       Method,
        in:           TaintFact,
        unbCallChain: Seq[Callable]
    ): Option[FlowFact] =
        Some(FlowFact(unbCallChain.prepended(JavaMethod(caller))))

    /**
     * This analysis does not create FlowFacts at the beginning of a method.
     * Instead, FlowFacts are created, when the return value of source is tainted.
     */
    override def createFlowFactAtExit(
        callee:       Method,
        in:           TaintFact,
        unbCallChain: Seq[Callable]
    ): Option[FlowFact] = None

    //

    /**
     * Checks, if a callee is outside this analysis' context.
     * By default, native methods are not inside the analysis context.
     * For callees outside this analysis' context the returned handler is called
     * to compute the summary edge for the call instead of analyzing the callee.
     *
     * @param callee The method called by `call`.
     * @return The handler function. It receives
     *         the statement which invoked the call,
     *         the successor statement, which will be executed after the call and
     *         the set of input facts which hold before the `call`.
     *         It returns facts, which hold after the call, excluding the call to return flow.
     */
    override def outsideAnalysisContextCall(
        callee: Method
    ): Option[OutsideAnalysisContextCallHandler] = {
        def handleNativeMethod(
            call:            JavaStatement,
            successor:       Option[JavaStatement],
            in:              TaintFact,
            unbCallChain:    Seq[Callable],
            dependeesGetter: Getter
        ): Set[TaintFact] = {
            val nativeFunctionName = JNICallUtil.resolveNativeFunctionName(callee)
            val function = LLVMFunction(llvmProject.function(nativeFunctionName).get)
            var result = Set.empty[TaintFact]
            val entryFacts = nativeICFG
                .startStatements(function)
                .flatMap(nativeCallFlow(_, call, callee, in))
                .map(new IFDSFact(_))
            for (entryFact <- entryFacts) { // ifds line 14
                val e = (function, entryFact)
                val exitFacts: Map[LLVMStatement, Set[NativeTaintFact]] =
                    dependeesGetter(e, NativeTaint.key)
                        .asInstanceOf[EOptionP[(LLVMStatement, IFDSFact[NativeTaintFact, LLVMStatement]), IFDSProperty[LLVMStatement, NativeTaintFact]]] match { // this cast is necessary
                            case UBP(prop) => prop.flows
                            case _         => Map.empty
                        }
                for {
                    (_, exitStatementFacts) <- exitFacts // ifds line 15.2
                    exitStatementFact <- exitStatementFacts // ifds line 15.3
                } {
                    result ++= nativeReturnFlow(exitStatementFact, call, function, callee, unbCallChain)
                }
            }
            result
        }

        if (callee.isNative) Some(handleNativeMethod _)
        else super.outsideAnalysisContextCall(callee)
    }

    override def outsideAnalysisContextUnbReturn(
        callee: Method
    ): Option[OutsideAnalysisContextUnbReturnHandler] = {
        def handleNativeUnbalancedReturn(
            callee:          Method,
            in:              TaintFact,
            callChain:       Seq[Callable],
            dependeesGetter: Getter
        ): Unit = {
            // find calls of java method in native code
            val nativeCalls = nativeICFG.getCallers(JNIMethod(callee))
            if (nativeCalls.isEmpty) return // no native callers

            for (callStmt <- nativeCalls) {
                val unbalancedReturnFacts = nativeUnbalancedReturnFlow(callee, callStmt, in)
                    .map(new IFDSFact(_, true, Some(callStmt), Some(callChain.prepended(JavaMethod(callee)))))

                // Add the caller with the unbalanced return facts as a dependency to start its analysis
                for (unbRetFact <- unbalancedReturnFacts) {
                    val newEntity = (callStmt.callable, unbRetFact)
                    dependeesGetter(newEntity, NativeTaint.key)
                }
            }
        }
        Some(handleNativeUnbalancedReturn _)
    }

    private def nativeUnbalancedReturnFlow(
        callee: Method,
        call:   LLVMStatement,
        in:     TaintFact
    ): Set[NativeTaintFact] = {
        if (sanitizesReturnValue(callee)) return Set.empty
        val callInstr = call.instruction.asInstanceOf[Call]

        // JNI call args if static: JNIEnv, class, method, arg 0, arg 1, ...
        // JNI call args if non-static: JNIEnv, this, method, arg 0, arg 1, ...
        def taintActualIfFormal(index: Int): Set[NativeTaintFact] = {
            if (index > -1) Set.empty // tac parameter indices are < 0, index is no argument

            val javaParamIndex = JavaIFDSProblem.remapParamAndVariableIndex(index, callee.isStatic)
            val nativeParamIndex = JNICallUtil.javaParamIndexToNative(javaParamIndex, callee.isStatic)
            Set(NativeVariable(callInstr.argument(nativeParamIndex).get))
        }

        in match {
            // Taint actual parameter if formal parameter is tainted
            case Variable(index)                   => taintActualIfFormal(index)
            case ArrayElement(index, _)            => taintActualIfFormal(index)
            case InstanceField(index, _, _)        => taintActualIfFormal(index)
            // also propagate tainted static fields
            case StaticField(classType, fieldName) => Set(JavaStaticField(classType, fieldName))
            case TaintNullFact                     => Set(NativeTaintNullFact)
            // Track the call chain to the sink back
            case FlowFact(flow) if !flow.contains(call.function) =>
                Set(NativeFlowFact(call.function +: flow))
            case _ => Set.empty
        }
    }

    private def nativeCallFlow(
        start:      LLVMStatement,
        call:       JavaStatement,
        javaCallee: Method,
        in:         TaintFact
    ): Set[NativeTaintFact] = {
        val flow = scala.collection.mutable.Set.empty[NativeTaintFact]

        // taint return value in callee, if tainted in caller
        start.instruction match {
            case ret: Ret if ret.value.isDefined =>
                in match {
                    case Variable(index) if index == call.index => flow += NativeVariable(ret.value.get)
                    case ArrayElement(index, _) if index == call.index =>
                        flow += NativeVariable(ret.value.get)
                    case InstanceField(index, _, _) if index == call.index =>
                        flow += NativeVariable(ret.value.get)
                    case _ =>
                }
            case _ =>
        }

        // check for tainted 'this' and pass-by-reference parameters
        val thisOffset = if (javaCallee.isStatic) 0 else 1
        val callObject = JavaIFDSProblem.asCall(call.stmt)
        callObject.allParams.iterator.zipWithIndex
            .filter(
                pair =>
                    (pair._2 == 0 && !javaCallee.isStatic) || // this
                        callObject.descriptor.parameterTypes(pair._2 - thisOffset).isReferenceType
            ) // pass-by-reference parameters
            .foreach { pair =>
                val param = pair._1.asVar
                val paramIndex = pair._2
                in match {
                    case Variable(index) if param.definedBy.contains(index) =>
                        flow += NativeVariable(start.callable.function.argument(paramIndex + 1)) // +1 offset JNIEnv
                    case ArrayElement(index, _) if param.definedBy.contains(index) =>
                        flow += NativeVariable(start.callable.function.argument(paramIndex + 1)) // +1 offset JNIEnv
                    case InstanceField(index, _, _) if param.definedBy.contains(index) =>
                        flow += NativeVariable(start.callable.function.argument(paramIndex + 1)) // +1 offset JNIEnv
                    case _ =>
                }
            }

        // keep tainted static fields and null fact
        in match {
            case StaticField(classType, fieldName) => flow += JavaStaticField(classType, fieldName)
            case TaintNullFact                     => flow += NativeTaintNullFact
            case _                                 =>
        }
        flow.toSet
    }

    private def nativeReturnFlow(
        in:               NativeTaintFact,
        call:             JavaStatement,
        callee:           LLVMFunction,
        javaNativeCallee: Method,
        unbCallChain:     Seq[Callable]
    ): Set[TaintFact] = {
        if (sanitizesReturnValue(javaNativeCallee)) return Set.empty
        val callStatement = JavaIFDSProblem.asCall(call.stmt)

        def taintActualIfFormal(in: Value): Set[TaintFact] = {
            callee.function.arguments.find(_.address == in.address) match {
                case Some(arg) =>
                    val javaParamIndex =
                        JNICallUtil.nativeParamIndexToJava(arg.index, javaNativeCallee.isStatic)
                    if (javaParamIndex < 0) Set.empty
                    else callStatement.allParams(javaParamIndex).asVar.definedBy.map(Variable)
                case _ => Set.empty
            }
        }

        in match {
            // Taint actual parameter if formal parameter is tainted
            case NativeVariable(value)                 => taintActualIfFormal(value)
            case NativeArrayElement(base, _)           => taintActualIfFormal(base)
            // keep static field taints
            case JavaStaticField(classType, fieldName) => Set(StaticField(classType, fieldName))
            // propagate flow facts
            case NativeFlowFact(flow)                  => Set(FlowFact(unbCallChain.prepended(JavaMethod(call.method))))
            case NativeTaintNullFact                   => Set(TaintNullFact)
            case _                                     => Set.empty
        }
    }
}

class SimpleJavaBackwardTaintAnalysis(project: SomeProject)
    extends IFDSAnalysis(project, new SimpleJavaBackwardTaintProblem(project), Taint)

object JavaBackwardTaintAnalysisScheduler
    extends IFDSAnalysisScheduler[TaintFact, Method, JavaStatement] {
    override def init(p: SomeProject, ps: PropertyStore) = new SimpleJavaBackwardTaintAnalysis(p)
    override def property: IFDSPropertyMetaInformation[JavaStatement, TaintFact] = Taint
    override def requiredProjectInformation: ProjectInformationKeys = Seq(LLVMProjectKey)
    override val uses: Set[PropertyBounds] =
        Set(PropertyBounds.finalP(TACAI), PropertyBounds.ub(NativeTaint))
}
