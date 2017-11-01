package org.opalj.fpcf.fixtures.escape.virtual_calls;

import org.opalj.fpcf.fixtures.escape.Circle;
import org.opalj.fpcf.properties.escape.MaybeNoEscape;

public interface Interface {
     Circle copyCircle(@MaybeNoEscape("the method has no body") Circle aCircle);

     Circle cyclicFunction(@MaybeNoEscape("the method has no body") Circle aCircle, int count);

}
