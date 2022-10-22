/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.sql

import org.opalj.br.Method
import org.opalj.br.analyses.{DeclaredMethodsKey, ProjectInformationKeys, SomeProject}
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.fpcf.{PropertyBounds, PropertyStore}
import org.opalj.ifds.{IFDSAnalysisScheduler, IFDSPropertyMetaInformation}
import org.opalj.tac.cg.TypeProviderKey
import org.opalj.tac.fpcf.analyses.ifds.JavaStatement
import org.opalj.tac.fpcf.properties.{Taint, TaintFact}

object IFDSSqlAnalysisScheduler extends IFDSAnalysisScheduler[TaintFact, Method, JavaStatement] {
    override def init(p: SomeProject, ps: PropertyStore) = new SqlTaintAnalysis(p)
    override def property: IFDSPropertyMetaInformation[JavaStatement, TaintFact] = Taint
    override val uses: Set[PropertyBounds] = Set(PropertyBounds.ub(Taint))
    override def requiredProjectInformation: ProjectInformationKeys = Seq(TypeProviderKey, DeclaredMethodsKey, PropertyStoreKey)
}