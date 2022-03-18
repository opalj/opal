/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll.fpcf.analyses.ifds.taint

import org.opalj.br.DeclaredMethod
import org.opalj.br.analyses.{ProjectInformationKeys, SomeProject}
import org.opalj.fpcf.{PropertyBounds, PropertyStore}
import org.opalj.ifds.IFDSPropertyMetaInformation
import org.opalj.ll.LLVMProjectKey
import org.opalj.ll.fpcf.properties.NativeTaint
import org.opalj.tac.fpcf.analyses.ifds.taint._
import org.opalj.tac.fpcf.analyses.ifds.{ForwardIFDSAnalysis, IFDSAnalysisScheduler, JavaMethod, JavaStatement}
import org.opalj.tac.fpcf.properties.{TACAI, Taint}

class SimpleJavaForwardTaintProblem(p: SomeProject) extends ForwardTaintProblem(p) {
    val llvmProject = p.get(LLVMProjectKey)

    /**
     * The analysis starts with all public methods in TaintAnalysisTestClass.
     */
    override val entryPoints: Seq[(DeclaredMethod, Fact)] = (for {
        m ← p.allMethodsWithBody
        if (m.name == "main")
    } yield declaredMethods(m) -> NullFact)

    /**
     * The sanitize method is a sanitizer.
     */
    override protected def sanitizesReturnValue(callee: DeclaredMethod): Boolean =
        callee.name == "sanitize"

    /**
     * We do not sanitize parameters.
     */
    override protected def sanitizeParameters(call: JavaStatement, in: Set[Fact]): Set[Fact] = Set.empty

    /**
     * Creates a new variable fact for the callee, if the source was called.
     */
    override protected def createTaints(callee: DeclaredMethod, call: JavaStatement): Set[Fact] =
        if (callee.name == "source") Set(Variable(call.index))
        else Set.empty

    /**
     * Create a FlowFact, if sink is called with a tainted variable.
     * Note, that sink does not accept array parameters. No need to handle them.
     */
    override protected def createFlowFact(
        callee: DeclaredMethod,
        call:   JavaStatement,
        in:     Set[Fact]
    ): Option[FlowFact] =
        if (callee.name == "sink" && in.contains(Variable(-2))) Some(FlowFact(Seq(JavaMethod(call.method))))
        else None

    // Multilingual additions here

    override def outsideAnalysisContext(callee: DeclaredMethod): Option[OutsideAnalysisContextHandler] = {
        def handleNativeMethod(call: JavaStatement, successor: JavaStatement, in: Set[Fact]): Set[Fact] = {
            //val method = callee.definedMethod

            // https://docs.oracle.com/en/java/javase/13/docs/specs/jni/design.html#resolving-native-method-names
            //val nativeMethodName = "Java_"+method.classFile.fqn+"_"+method.name
            //val function = llvmProject.function(nativeMethodName)
            //val foo = propertyStore((function.get, source._2), NativeTaint)
            Set.empty[Fact]
        }

        if (callee.definedMethod.isNative) {
            Some(handleNativeMethod _)
        } else {
            super.outsideAnalysisContext(callee)
        }
    }
}

class SimpleJavaForwardTaintAnalysis(implicit val project: SomeProject)
    extends ForwardIFDSAnalysis(new SimpleJavaForwardTaintProblem(project), Taint)

object JavaForwardTaintAnalysisScheduler extends IFDSAnalysisScheduler[Fact] {
    override def init(p: SomeProject, ps: PropertyStore) = new SimpleJavaForwardTaintAnalysis()(p)
    override def property: IFDSPropertyMetaInformation[JavaStatement, Fact] = Taint
    override def requiredProjectInformation: ProjectInformationKeys = super.requiredProjectInformation ++ Seq(LLVMProjectKey)
    override val uses: Set[PropertyBounds] = Set(PropertyBounds.finalP(TACAI), PropertyBounds.ub(NativeTaint))
}
