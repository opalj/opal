package org.opalj.fpcf.properties.purity;

import org.opalj.si.FPCFAnalysis;

public @interface EP {

    Class<?> cf();

    String field() default "";

    String method() default "";

    String pk();

    String p();

    Class<? extends FPCFAnalysis>[] analyses() default {};
}
