package org.opalj.fpcf.fixtures.immutability.sandbox62;

import org.opalj.fpcf.properties.immutability.classes.DeepImmutableClass;
import org.opalj.fpcf.properties.immutability.classes.MutableClass;

@DeepImmutableClass("")
public class A {
    protected int n = 8;
}

@DeepImmutableClass("")
class B extends A {

}

/*@MutableClass("")
class C extends B {
   public void setEight(){
       super.n = 8;
   }
}*/