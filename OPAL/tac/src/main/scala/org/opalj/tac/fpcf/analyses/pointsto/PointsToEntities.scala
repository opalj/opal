/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package pointsto

import org.opalj.br.fpcf.properties.Context
import org.opalj.tac.common.DefinitionSite

/**
 * Special entities, that are only used in the points-to analysis context.
 *
 * @author Dominik Helm
 * @author Florian Kuebler
 */
case class ArrayEntity[ElementType](element: ElementType)
case class MethodExceptions(context: Context)
case class CallExceptions(defSite: DefinitionSite)