#Reflection API Usage

This analysis derives which methods/functionality of Java's Reflection API is used how often.

## Core Reflection API

The following features are targeted by the analysis:

- Usage of `Class.forName`
- (reflective) creation of new Instances
- (reflective) field write
- (reflective) field read
- setting the accessibility of `Field`, `Method`, `Constructor`,
    or other `AccessibleObject` objects in general
- (reflective) method invocations

## Reflection with MethodHandles

Factory methods provided by `java.lang.invoke.MethodHandles.Lookup Lookup` can be used to convert
any class member represented by a Core Reflection API object to a behaviorally equivalent
`MethodHandle`, therefore, `MethodHandle` objects are relevant when assessing reflection usage of
a project.

- usage of `MethodHandles.Lookup`
- `MethodHandle` invocations over `MethodHandle.invoke`, `MethodHandle.invokeExact`, and `MethodHandle.invokeWithArguments`
- creation of `java.lang.reflect.Proxy` to customize the dispatch method invocations 