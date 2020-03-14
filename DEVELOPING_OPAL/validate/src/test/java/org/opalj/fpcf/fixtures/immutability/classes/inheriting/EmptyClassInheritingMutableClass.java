package org.opalj.fpcf.fixtures.immutability.classes.inheriting;

import org.opalj.fpcf.properties.class_immutability.MutableClassAnnotation;
import org.opalj.fpcf.properties.type_mutability.MutableType;

@MutableType("Because of mutable class")
@MutableClassAnnotation("Because of extending mutable class")
public final class EmptyClassInheritingMutableClass extends MutableClass {

}
