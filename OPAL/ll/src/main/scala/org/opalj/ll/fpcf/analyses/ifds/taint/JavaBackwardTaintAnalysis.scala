/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll.fpcf.analyses.ifds.taint

import org.opalj.br.Method
import org.opalj.br.analyses.{ProjectInformationKeys, SomeProject}
import org.opalj.fpcf.{EOptionP, FinalEP, InterimEUBP, PropertyBounds, PropertyStore}
import org.opalj.ifds.Dependees.Getter
import org.opalj.ifds.{IFDSAnalysis, IFDSAnalysisScheduler, IFDSFact, IFDSProperty, IFDSPropertyMetaInformation}
import org.opalj.ll.LLVMProjectKey
import org.opalj.ll.fpcf.analyses.ifds.{JNICallUtil, LLVMFunction, LLVMStatement, NativeBackwardICFG, NativeFunction}
import org.opalj.ll.fpcf.properties.NativeTaint
import org.opalj.ll.llvm.value.{Ret, Value}
import org.opalj.tac.fpcf.analyses.ifds.{JavaIFDSProblem, JavaMethod, JavaStatement}
import org.opalj.tac.fpcf.analyses.ifds.taint.{ArrayElement, FlowFact, InstanceField, JavaBackwardTaintProblem, StaticField, TaintFact, TaintNullFact, Variable}
import org.opalj.tac.fpcf.properties.{TACAI, Taint}

class SimpleJavaBackwardTaintProblem(p: SomeProject) extends JavaBackwardTaintProblem(p) {
    val llvmProject = p.get(LLVMProjectKey)
    val nativeICFG = new NativeBackwardICFG(p)

    /**
     * The analysis starts with the sink function.
     */
    override val entryPoints: Seq[(Method, IFDSFact[TaintFact, Method, JavaStatement])] =
        p.allProjectClassFiles.filter(classFile =>
            classFile.thisType.fqn == "TaintTest")
            .flatMap(_.methods)
            .filter(_.name == "sink")
            .map(method => method -> new IFDSFact(
                Variable(JavaIFDSProblem.switchParamAndVariableIndex(0, isStaticMethod = true))
            ))

    override protected def sanitizesReturnValue(callee: Method): Boolean = callee.name == "sanitize"

    override protected def sanitizesParameter(call: JavaStatement, in: TaintFact): Boolean = false

    /**
     * Create a flow fact, if a source method is called and the returned value is tainted.
     * This is done in callToReturnFlow, because it may be the case that the callee never
     * terminates.
     * In this case, callFlow would never be called and no FlowFact would be created.
     */
    override protected def createFlowFactAtCall(call: JavaStatement, in: TaintFact,
                                                unbCallChain: Seq[Method]): Option[FlowFact] = {
        if ((in match {
            case Variable(index) => index == call.index
            case _               => false
        }) && icfg.getCalleesIfCallStatement(call).get.exists(_.name == "source")) {
            val currentMethod = call.callable
            // Avoid infinite loops.
            if (unbCallChain.contains(currentMethod)) None
            else Some(FlowFact(unbCallChain.prepended(currentMethod).map(JavaMethod)))
        } else None
    }

    /**
     * When a callee calls the source, we create a FlowFact with the caller's call chain.
     */
    override protected def applyFlowFactFromCallee(calleeFact: FlowFact, caller: Method, in: TaintFact,
                                                   unbCallChain: Seq[Method]): Option[FlowFact] =
        Some(FlowFact(unbCallChain.prepended(caller).map(JavaMethod)))

    /**
     * This analysis does not create FlowFacts at the beginning of a method.
     * Instead, FlowFacts are created, when the return value of source is tainted.
     */
    override def createFlowFactAtExit(callee: Method, in: TaintFact,
                                      unbCallChain: Seq[Method]): Option[FlowFact] = None

    // Multilingual additions here
    override def outsideAnalysisContext(callee: Method): Option[OutsideAnalysisContextHandler] = {
        def handleNativeMethod(call: JavaStatement, successor: Option[JavaStatement],
                               in: TaintFact, dependeesGetter: Getter): Set[TaintFact] = {
            val nativeFunctionName = JNICallUtil.resolveNativeMethodName(callee)
            val function = LLVMFunction(llvmProject.function(nativeFunctionName).get)
            var result = Set.empty[TaintFact]
            val entryFacts = nativeICFG.startStatements(function)
                .flatMap(nativeCallFlow(_, call, callee, in))
                .map(new IFDSFact(_))
            for (entryFact <- entryFacts) { // ifds line 14
                val e = (function, entryFact)
                val exitFacts: Map[LLVMStatement, Set[NativeTaintFact]] =
                    dependeesGetter(e, NativeTaint.key).asInstanceOf[EOptionP[(LLVMStatement, IFDSFact[NativeTaintFact, NativeFunction, LLVMStatement]), IFDSProperty[LLVMStatement, NativeTaintFact]]] match {
                        case ep: FinalEP[_, IFDSProperty[LLVMStatement, NativeTaintFact]] =>
                            ep.p.flows
                        case ep: InterimEUBP[_, IFDSProperty[LLVMStatement, NativeTaintFact]] =>
                            ep.ub.flows
                        case _ =>
                            Map.empty
                    }
                for {
                    (exitStatement, exitStatementFacts) <- exitFacts // ifds line 15.2
                    exitStatementFact <- exitStatementFacts // ifds line 15.3
                } {
                    result ++= nativeReturnFlow(exitStatement, exitStatementFact, call, function, callee)
                }
            }
            result
        }

        if (callee.isNative) Some(handleNativeMethod _)
        else super.outsideAnalysisContext(callee)
    }

    private def nativeCallFlow(start: LLVMStatement, call: JavaStatement, javaCallee: Method,
                               in: TaintFact): Set[NativeTaintFact] = {
        val flow = collection.mutable.Set.empty[NativeTaintFact]

        // taint return value in callee, if tainted in caller
        start.instruction match {
            case ret: Ret if ret.value.isDefined => in match {
                case Variable(index) if index == call.index => flow += NativeVariable(ret.value.get)
                case ArrayElement(index, _) if index == call.index => flow += NativeVariable(ret.value.get)
                case InstanceField(index, _, _) if index == call.index => flow += NativeVariable(ret.value.get)
            }
        }

        // check for tainted 'this' and pass-by-reference parameters
        val thisOffset = if (javaCallee.isStatic) 0 else 1
        val callObject = JavaIFDSProblem.asCall(call.stmt)
        callObject.allParams.iterator.zipWithIndex
            .filter(pair => (pair._2 == 0 && !javaCallee.isStatic) || // this
                callObject.descriptor.parameterTypes(pair._2 - thisOffset).isReferenceType) // pass-by-reference parameters
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
                }
            }

        // keep tainted static fields and null fact
        in match {
            case StaticField(classType, fieldName) => flow += JavaStaticField(classType, fieldName)
            case TaintNullFact => flow += NativeTaintNullFact
        }
        flow.toSet
    }

    private def nativeReturnFlow(exit: LLVMStatement, in: NativeTaintFact, call: JavaStatement, callee: LLVMFunction,
                                 nativeCallee: Method): Set[TaintFact] = {
        if (sanitizesReturnValue(nativeCallee)) return Set.empty

        val callStatement = JavaIFDSProblem.asCall(call.stmt)
        callStatement.allParams

        def taintActualIfFormal(in: Value): Set[TaintFact] = {
            callee.function.arguments.find(_.address == in.address) match {
                case Some(arg) => callStatement.params(arg.index).asVar.definedBy.map(Variable)
                case None => Set.empty
            }
        }

        in match {
            // Taint actual parameter if formal parameter is tainted
            case NativeVariable(value) => taintActualIfFormal(value)
            case NativeArrayElement(base, _) => taintActualIfFormal(base)
            // keep static field taints
            case JavaStaticField(classType, fieldName) => Set(StaticField(classType, fieldName))
            // propagate flow facts
            case NativeFlowFact(flow) if !flow.contains(JavaMethod(call.method)) =>
                Set(FlowFact(JavaMethod(call.method) +: flow))
            case NativeTaintNullFact => Set(TaintNullFact)
            case _ => Set.empty
        }
    }

    // TODO xlang unbalanced returns
}

class SimpleJavaBackwardTaintAnalysis(project: SomeProject)
    extends IFDSAnalysis()(project, new SimpleJavaBackwardTaintProblem(project), Taint)

object JavaBackwardTaintAnalysisScheduler extends IFDSAnalysisScheduler[TaintFact, Method, JavaStatement] {
    override def init(p: SomeProject, ps: PropertyStore) = new SimpleJavaBackwardTaintAnalysis(p)
    override def property: IFDSPropertyMetaInformation[JavaStatement, TaintFact] = Taint
    override def requiredProjectInformation: ProjectInformationKeys = Seq(LLVMProjectKey)
    override val uses: Set[PropertyBounds] = Set(PropertyBounds.finalP(TACAI), PropertyBounds.ub(NativeTaint))
}
