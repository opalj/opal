/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.cifi_benchmark.common;

import org.opalj.fpcf.properties.immutability.classes.MutableClass;
import org.opalj.fpcf.properties.immutability.types.MutableType;

@MutableType("The class has mutable fields")
@MutableClass("The class has mutable fields")
public class ClassWithMutableField {
     public int n = 8;
}