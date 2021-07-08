package org.opalj.fpcf.fixtures.benchmark.commons;

import org.opalj.fpcf.properties.immutability.classes.TransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.types.MutableType;

/**
 * This class is used instead of the class java.lang.Object.
 * to omit some analysis' special handling of java.lang.Object
 */
@MutableType("The class is extensible")
@TransitivelyImmutableClass("The class has no field and thus no state")
public class CustomObject {}
