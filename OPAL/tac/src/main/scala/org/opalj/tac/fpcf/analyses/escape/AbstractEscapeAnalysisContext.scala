/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package escape

import org.opalj.fpcf.Entity
import org.opalj.fpcf.PropertyStore
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.VirtualFormalParameters
import org.opalj.br.fpcf.properties.Context

/**
 * Provides the basic information corresponding to an entity to determine its escape information.
 * Furthermore, it has helper functions to check whether the entity might be used in expressions.
 *
 * @see [[AbstractEscapeAnalysis]]
 *
 * @author Florian Kuebler
 */
trait AbstractEscapeAnalysisContext {

    val entity: (Context, Entity)
    val targetMethod: Method

    def targetMethodDeclaringClassType: ObjectType = targetMethod.classFile.thisType
}

trait PropertyStoreContainer {
    val propertyStore: PropertyStore
}

trait DeclaredMethodsContainer {
    val declaredMethods: DeclaredMethods
}

trait VirtualFormalParametersContainer {
    val virtualFormalParameters: VirtualFormalParameters
}

trait IsMethodOverridableContainer {
    val isMethodOverridable: Method => Answer
}
