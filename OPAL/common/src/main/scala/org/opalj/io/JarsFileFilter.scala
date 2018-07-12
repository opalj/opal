/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package io

import java.io.File
import java.io.FileFilter

object JARsFileFilter extends FileFilter {

    def accept(path: File): Boolean = {
        path.isFile && path.getName.endsWith(".jar") && path.canRead
    }

}
