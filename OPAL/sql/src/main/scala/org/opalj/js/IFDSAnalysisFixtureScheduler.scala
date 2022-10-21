/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org
package opalj
package js

import org.opal.js.JavaScriptAwareTaintAnalysisFixture
import org.opalj.br.Method
import org.opalj.br.analyses.{DeclaredMethodsKey, ProjectInformationKeys, SomeProject}
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.fpcf.{PropertyBounds, PropertyStore}
import org.opalj.ifds.{IFDSAnalysisScheduler, IFDSPropertyMetaInformation}
import org.opalj.tac.cg.TypeProviderKey
import org.opalj.tac.fpcf.analyses.ifds.JavaStatement
import org.opalj.tac.fpcf.properties.{Taint, TaintFact}

object IFDSAnalysisJSFixtureScheduler extends IFDSAnalysisScheduler[TaintFact, Method, JavaStatement] {
    override def init(p: SomeProject, ps: PropertyStore) = new JavaScriptAwareTaintAnalysisFixture(p)
    override def property: IFDSPropertyMetaInformation[JavaStatement, TaintFact] = Taint
    override val uses: Set[PropertyBounds] = Set(PropertyBounds.ub(Taint))
    override def requiredProjectInformation: ProjectInformationKeys = Seq(TypeProviderKey, DeclaredMethodsKey, PropertyStoreKey)
}