/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bugpicker
package core

import org.opalj.util.Nanoseconds
import org.opalj.br.ObjectType
import org.opalj.br.analyses.AnalysisException
import org.opalj.br.BooleanType
import org.opalj.br.MethodDescriptor
import org.opalj.br.IntegerType
import org.opalj.issues.Issue

/**
 * Common constants and helper methods used by the BugPicker's analyses.
 *
 * @author Michael Eichberg
 */
package object analyses {

    type BugPickerResults = (Nanoseconds, Iterable[Issue], Iterable[AnalysisException])

    final val AssertionError = ObjectType("java/lang/AssertionError")

    final val ObjectEqualsMethodDescriptor = MethodDescriptor(ObjectType.Object, BooleanType)

    final val ObjectHashCodeMethodDescriptor = MethodDescriptor.withNoArgs(IntegerType)
}
