/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.escape.virtual_calls;

import org.opalj.fpcf.fixtures.escape.Circle;

public interface Interface {
     Circle copyCircle(Circle aCircle);

     Circle cyclicFunction(Circle aCircle, int count);

}
