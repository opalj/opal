/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses

object DeclaredFieldsKey extends ProjectInformationKey[DeclaredFields, Nothing] {

    override def requirements(project: SomeProject): ProjectInformationKeys = Seq.empty

    override def compute(p: SomeProject): DeclaredFields = new DeclaredFields(p)
}
