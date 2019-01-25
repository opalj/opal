/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.br.fpcf.properties

import org.opalj.br.fpcf.properties.string_definition.StringTreeElement

package object properties {

    /**
     * `StringTree` is used to build trees that represent how a particular string looks and / or how
     * it can looks like from a pattern point of view (thus be approximated).
     */
    type StringTree = StringTreeElement

}
