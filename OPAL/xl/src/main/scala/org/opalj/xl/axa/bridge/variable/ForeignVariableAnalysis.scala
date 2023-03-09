/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.xl.axa.bridge.variable

import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.BasicFPCFLazyAnalysisScheduler
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.xl.axa.bridge.function.ForeignFunctionProperty

import scala.collection.immutable

class ForeignVariableAnalysis(val project: SomeProject) extends FPCFAnalysis {

    def analyzeGlobalVariable(fv: ForeignVariable): ProperPropertyComputationResult = {

        val languageName = fv.languageName
        val variableName = fv.variableName

        languageName match {
            case "Java" => Result(fv, ForeignVariableProperty(JavaVariable(variableName)))
            case "TIP"  => Result(fv, ForeignVariableProperty(TIPVariable(variableName)))
            case "L1"   => Result(fv, ForeignVariableProperty(L1Variable(variableName)))
        }
    }

    def lazilyAnalyzeForeignVariable(foreignVariable: ForeignVariable): ProperPropertyComputationResult = {
        foreignVariable match {
            case fv: ForeignVariable => analyzeGlobalVariable(fv)
            case _                   => throw new IllegalArgumentException("can only process foreignFunctions")
        }
    }

}

trait GlobalVariableAnalysisScheduler extends FPCFAnalysisScheduler {
    def derivedProperty: PropertyBounds = PropertyBounds.ub(GlobalVariableTaintLattice)

    override def requiredProjectInformation: ProjectInformationKeys = Seq.empty

    override def uses: immutable.Set[PropertyBounds] =
        immutable.Set.empty
}

object LazyForeignVariableAnalysis extends GlobalVariableAnalysisScheduler with BasicFPCFLazyAnalysisScheduler {
    override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)

    override def register(project: SomeProject, propertyStore: PropertyStore, initData: InitializationData): FPCFAnalysis = {
        val analysis = new ForeignVariableAnalysis(project)
        propertyStore.registerLazyPropertyComputation(ForeignFunctionProperty.key, analysis.lazilyAnalyzeForeignVariable)
        analysis
    }
}
