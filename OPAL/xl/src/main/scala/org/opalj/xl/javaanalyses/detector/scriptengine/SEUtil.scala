/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl
package javaanalyses
package detector
package scriptengine

import org.opalj.xl.translator.JavaJavaScriptTranslator

import org.opalj.br.fpcf.properties.pointsto.PointsToSetLike

class SEUtil[PointsToSet >: Null <: PointsToSetLike[_, _, PointsToSet], ContextType] {
    def java2js = JavaJavaScriptTranslator.Java2JavaScript[PointsToSet, ContextType]
    def js2java = JavaJavaScriptTranslator.JavaScript2Java[PointsToSet, ContextType]

}
