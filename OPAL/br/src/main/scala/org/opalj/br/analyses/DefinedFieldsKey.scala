/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses

object DefinedFieldsKey extends ProjectInformationKey[DefinedFields, Nothing] {

    override def requirements(project: SomeProject): ProjectInformationKeys = Seq.empty

    override def compute(p: SomeProject): DefinedFields = new DefinedFields()
}
