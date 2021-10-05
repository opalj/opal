# Lattices

In OPAL, fixed-point analyses need to represent their results using values from an explicitly defined lattices.  
This article will discuss a few possibilities when defining them.  
You should have read the tutorial on [Writing Fixed-Point Analyses](FixedPointAnalyses.html) first that introduces the basic concepts.

There are two basic kinds of lattices that are typically used in OPAL: [singleton-based lattices](#singleton-based-lattices) and [set-based lattices](#set-based-lattices).
It is also possible to [combine them](#combined-lattices).  
Before discussing each of them individually, let's have a look at what they have in common.

## Lattices Basics

A lattice in OPAL consists of three parts.  
The first one is the [`PropertyMetaInformation`](/library/api/SNAPSHOT/org/opalj/fpcf/PropertyMetaInformation.html):
```scala
sealed trait ClassImmutabilityPropertyMetaInformation extends PropertyMetaInformation {
    final type Self = ClassImmutability
}
```
We use it to provide the type of the property.

Second, the property companion object provides the [PropertyKey]():
```scala
object ClassImmutability extends ClassImmutabilityPropertyMetaInformation {
    final val key: PropertyKey[ClassImmutability] = PropertyKey.create(
        "ClassImmutability",
        MutableClass
    )
}
```
We discuss more on the `PropertyKey` [below](#property-keys).  
A second use of the companion object would be to provide factory methods to create your lattice values conveniently.

Finally, the actual properties.  
If you have more than one class, you should provide a trait through which the `PropertyKey` can be accessed:
```scala
sealed trait ClassImmutability extends ClassImmutabilityPropertyMetaInformation {
    final def key: PropertyKey[ClassImmutability] = ClassImmutability.key    
}
```
Implement your lattice using classes that implement this trait.  
If you only need a single class for your lattice, you can of course define the `key` method there and omit defining a trait.

You can also use this trait to declare methods that you need for lattice, like `meet` or `join` methods or methods to extract data from your lattice values conveniently.

## Singleton-based Lattices

Singleton-based lattices are very common in OPAL.  
They consist of a number of case objects each of which represents a singleton value of the lattice:
```scala
case object TransitivelyImmutableClass extends ClassImmutability

case object NonTransitivelyImmutableClass extends ClassImmutability

case object MutableClass extends ClassImmutability
```
Of course, you may need to implement any methods defined in your trait above.

## Set-based Lattices

The second common type of lattices in OPAL is set-based lattices.
They typically consist of one (or sometimes several) case classes that hold the set of values:
```scala
case class InstantiatedTypes(classes: UIDSet[ObjectType]) extends InstantiatedTypesPropertyMetaInformation {
    final def key: PropertyKey[InstantiatedTypes] = InstantiatedTypes.key
}
```
Note that here we extended the `PropertyMetaInformation` directly without an additional trait as we need only one class.

## Combined Lattices

You can combine both concepts in a single lattice if you need to.  
For example, you could extend the above `ClassImmutability` lattice with this set-based case class:
```scala
case class DependentlyImmutableClass(dependentTypeParameters: Set[String])
```

Or you could provide a singleton object as a convenient extension for the above `InstantiatedTypes` lattice:
```scala
object NoInstantiatedTypes extends InstantiatedTypes(UIDSet.empty)
```

However, always take care that what you implement really has the semantics of a lattice (i.e., the values form a partial order and there exist a unique least upper bound and unique greatest lower bound for every two values).

## Additional Options for Lattices

There are two additional classes that you might want to consider when implementing your lattice.

The first one is [`OrderedProperty`](/library/api/SNAPSHOT/org/opalj/fpcf/OrderedProperty.html).  
If your lattice values extend this trait, you get additional checks in the PropertyStore whether your analyses refine their results monotonically with respect to your lattice's partial order.  
`OrderedProperty` requires that you implement a method `checkIsEqualOrBetterThan(e: Entity, other: Self)` that should throw an exception if `other` is greater than the current value with respect to the lattice's partial order.  
If you defined a `meet` method, you can simply implement it like this:
```scala
override def checkIsEqualOrBetterThan(e: Entity, other: ClassImmutability): Unit = {
    if (meet(other) != other) {
        throw new IllegalArgumentException(s"$e: impossible refinement: $other => $this")
    }
}
```
However, it may be better to provide optimized implementations for your individual lattice values if the `meet` operation is not trivial.

The second option to consider is [`AggregatedProperty`](/library/api/SNAPSHOT/org/opalj/br/fpcf/properties/AggregatedProperty.html).  
Some properties really represent an aggregation of another property, e.g., `ClassImmutability` aggregates the `FieldImmutability` of a class' instance fields.  
In such cases, one often needs to convert between corresponding values of the two lattices.  
Also, the partial order and thus `meet` operator are equivalent and need to be defined only once.

In order to use `AggregatedProperty`, the base lattice has to extend `IndividualProperty[BaseLattice, AggregateLattice]`, implement the `meet` method as well as an `aggregatedProperty` method that maps each value to its corresponding value of the aggregated lattice.  
The aggregate lattice then has to extend `AggregatedProperty[BaseLattice, AggregateLattice]`.  
It has to implement the `individualProperty` method that maps each value to the corresponding value of the base lattice.  
The `meet` method on the aggregate lattice is provided for you based on the `meet` operator of the base lattice.

## Property Keys

The [`PropertyKey`](/library/api/SNAPSHOT/org/opalj/fpcf/PropertyKey.html) makes your lattice known to OPAL and gives it a unique identifier.  
You create it using one of two methods:
```scala
final val classImmutabilityKey: PropertyKey[ClassImmutability] = PropertyKey.create(
    "ClassImmutability",
    MutableClass
)

final val instantiatedTypesKey: PropertyKey[InstantiatedTypes] = PropertyKey.create(
    "InstantiatedTypes",
    (_: PropertyStore, reason: FallbackReason, _: Entity) => reason match {
        case PropertyIsNotDerivedByPreviouslyExecutedAnalysis => InstantiatedTypes(UIDSet.empty)
        case _                                                => throw new IllegalStateException(s"No analysis is scheduled for property InstantiatedTypes")
    }
)
```
For both, you provide a unique name for your lattice and a fallback if a value from your lattice is queried but not computed.

The difference is in the fallback: You can either provide a single lattice value to use in all cases.  
It should be your lattice's bottom element in order to be sound.

Or you can provide a function to compute the fallback value on demand.  
This has two benefits: First, you can use some information about your entity to compute a sound but more precise fallback value.  
Additionally, as shown above, this function gets a [`FallbackReason`](/library/api/SNAPSHOT/org/opalj/fpcf/FallbackReason.html).  
This tells you whether no analysis was executed to compute any values of your lattice at all (`PropertyIsNotComputedByAnyAnalysis`), in which case you should provide a sound over-approximation as before.  
Or it tells you that an analysis was executed, but it did not provide a result for the respective entity (`PropertyIsNotDerivedByPreviouslyExecutedAnalysis`), in which case you may be able to provide a precise result, because you know that, e.g., the entity belongs to dead code.