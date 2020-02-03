/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses

import org.opalj.concurrent.defaultIsInterrupted

/**
 * The ''key'' object to get global field access information.
 *
 * @example To get the index use the [[Project]]'s `get` method and pass in `this` object.
 *
 * @author Michael Eichberg
 */
object FieldAccessInformationKey extends ProjectInformationKey[FieldAccessInformation, Nothing] {

    /**
     * The [[FieldAccessInformationAnalysis]] has no special prerequisites.
     *
     * @return `Nil`.
     */
    override def requirements(project: SomeProject): Seq[ProjectInformationKey[Nothing, Nothing]] = Nil

    /**
     * Computes the field access information.
     */
    override def compute(project: SomeProject): FieldAccessInformation = {
        FieldAccessInformationAnalysis.doAnalyze(project, defaultIsInterrupted)
    }
}

