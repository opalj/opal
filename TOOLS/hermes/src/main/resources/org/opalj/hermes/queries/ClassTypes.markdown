# Class Types
Extracts the information about the type of the specified class.

## (concrete) classes
Definition of regular non-abstract class.

## abstract classes
Definition of an abstract class.

## annotations
Definition of an annotation.

## enumerations
Definition of an enumeration.

## marker interfaces
A marker interface is an empty interface. For example, `java.io.Serializable` or `java.lang.Cloneable` are marker interfaces.

## functional interfaces  
*(Also called: single abstract method (SAM) interfaces)*

> [Java 8 Lang. Spec.] A functional interface is an interface that has just one abstract method (aside from the methods of Object), and thus represents a single function contract. This "single" method may take the form of multiple abstract methods with override- equivalent signatures inherited from superinterfaces; in this case, the inherited methods logically represent a single method.

Such interfaces are particularly used in combination with lambda expressions.

[Functional Interfaces Demo Code](https://github.com/stg-tud/opal/blob/develop/OPAL/bi/src/test/fixtures-java/projects/jvm_features/class_types/SAMInterface.java)


## Interface with default methods (*Java >8*)
The interface defines a concrete instance method (a so-called default method.)

## Interface which defines a static method (*Java >8*)
An interface which defines a static method.


 > The static initializer is not considered in this case.
 > The latter is always generated when a complex constant field is defined; e.g.,
 >
 >      public interface X {
 >         public static final String s = new String(new byte[]{25,26,27})
 >      }

## module (Java >9)
The class file defines a Java 9 module.
