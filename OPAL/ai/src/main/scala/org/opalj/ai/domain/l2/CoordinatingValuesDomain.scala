/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l2

import org.opalj.br.analyses.Project

/**
 * A basic Domain that is used to identify recursive calls.
 */
class CoordinatingValuesDomain[Source](
        val project: Project[Source]
) extends ValuesCoordinatingDomain
    with SharedValuesDomain[Source]
