package org.opalj.fpcf.fixtures.immutability.classes.generic;

import org.opalj.fpcf.properties.field_immutability.DependentImmutableFieldAnnotation;

public class Nested{
}

class Simple<T>{
    class Inner{
        @DependentImmutableFieldAnnotation(value= "", genericString = "T")
        private T t;
        public Inner(T t){
            this.t = t;
        }
    }
}

class Complex<T>{
    class Inner {
     @DependentImmutableFieldAnnotation(value="", genericString = "T")
        private Generic_class1<T,T,T,T,T> gc1;
     public Inner(Generic_class1<T,T, T, T, T> gc1){
         this.gc1 = gc1;
     }


    }
}





