/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package io

import java.io.File

/**
 * Exception that is thrown if the OS cannot/does not know how/is not able to open
 * the respective file.
 *
 * @author Michael Eichberg
 */
case class OpeningFileFailedException(
        file:  File,
        cause: Throwable
) extends Exception(s"cannot open file $file", cause)
