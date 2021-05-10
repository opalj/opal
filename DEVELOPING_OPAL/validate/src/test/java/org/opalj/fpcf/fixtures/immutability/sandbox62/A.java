package org.opalj.fpcf.fixtures.immutability.sandbox62;

import org.opalj.fpcf.properties.immutability.classes.TransitivelyImmutableClass;

@TransitivelyImmutableClass("")
public class A {
    protected int n = 8;
}

@TransitivelyImmutableClass("")
class B extends A {

}

/*@MutableClass("")
class C extends B {
   public void setEight(){
       super.n = 8;
   }
}*/