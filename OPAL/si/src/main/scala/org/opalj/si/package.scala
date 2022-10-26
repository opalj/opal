/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj

package object si {
    type ProjectInformationKeys[-P <: MetaProject] = Seq[ProjectInformationKey[_ <: MetaProject, _ <: AnyRef, _ <: AnyRef]]
}
