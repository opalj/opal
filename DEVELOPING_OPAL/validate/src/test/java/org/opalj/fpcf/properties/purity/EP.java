/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.purity;

import org.opalj.br.fpcf.FPCFAnalysis;

public @interface EP {

    Class<?> cf();

    String field() default "";

    String method() default "";

    String pk();

    String p();

    Class<? extends FPCFAnalysis>[] analyses() default {};
}
