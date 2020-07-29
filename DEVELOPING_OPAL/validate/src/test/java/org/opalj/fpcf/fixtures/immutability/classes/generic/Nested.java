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
        private GenericClass<T> gc;
     public Inner(GenericClass<T> gc){
         this.gc = gc;
     }
    }
}

class GenericClass<T> {
    @DependentImmutableFieldAnnotation(value = "", genericString = "T")
    private T t;
    public GenericClass(T t){
        this.t = t;
    }
}






