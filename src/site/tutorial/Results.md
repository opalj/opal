# Results

OPAL provides several options for your analyses to return their results.  
You should have read the tutorial on [Writing Fixed-Point Analyses](FixedPointAnalyses.html) first that introduces the basic concepts.

In OPAL, types of results are represented by the subtypes of [`PropertyComputationResult`](/library/api/SNAPSHOT/org/opalj/fpcf/PropertyComputationResult.html).  
Let's see what is possible using them.

## NoResult

[`NoResult`](/library/api/SNAPSHOT/org/opalj/fpcf/NoResult.html) is the simplest one.  
As the name suggests, it is used when your analysis cannot compute anything.  
Note that in contrast to all other result types, `NoResult` can only be returned by the analysis function, not by a continuation (it is not a [`ProperPropertyComputationResult`](/library/api/SNAPSHOT/org/opalj/fpcf/ProperPropertyComputationResult.html)).
This is because a continuation continues the computation of some result, thus it is only executed when there is something to be computed.

## Result

[`Result`](/library/api/SNAPSHOT/org/opalj/fpcf/Result.html) is one of the most important types of results.  
It represents a single, final result of your analysis.  
It consists of the entity for which some property is valid and that respective property:
```scala
Result(classFile, ImmutableClass)
```

## InterimResult

[`InterimResult`](/library/api/SNAPSHOT/org/opalj/fpcf/InterimResult.html) is the second important type of result.  
Like a `Result`, it represents a single result of your analysis, but one that is not yet final because it may depend on some other properties that were not yet available.  
It consists of the entity for which you are computing a property, the bound(s) to that property that you already know, the open dependencies and the continuation function to be called when there is an update to one of the dependencies.  
There are multiple ways to construct an `InterimResult`, the three most important ones shown here:
```scala
InterimResult.forUB(classFile, ImmutableClass, dependencies, continuation)
InterimResult.forLB(classFile, MutableClass, dependencies, continuation)
InterimResult(classFile, MutableClass, ImmutableClass, dependencies, continuation)
```
`InterimResult.forUB()` is used to provide an upper bound for a property, i.e., an optimistic assumption of what the final result could be.  
`InterimResult.forLB()` is its counterpart for a lower bound, i.e., a pessimistic assumption.  
Finally, if you can provide both a lower and an upper bound, `InterimResult` has an `apply` method that takes both bounds.

## MultiResult

[`MultiResult`](/library/api/SNAPSHOT/org/opalj/fpcf/MultiResult.html), like `Result` represents final results of your analysis, but it allows you to return several at once.  
You construct it from a number of [`FinalEP`](/library/api/SNAPSHOT/org/opalj/fpcf/FinalEP.html) objects, each of which is one property for one entity.  
Make sure that they are for different entities or different kinds of properties though!
```scala
MultResult(classHierarchy.allSubclassTypes(classType).map(FinalEP(_, MutableClass)))
```

## Results
[`Results`](/library/api/SNAPSHOT/org/opalj/fpcf/Results.html) allows your analysis to return several results of any kind at once, as long as they are for different entities or different kinds of properties.  
```scala
Results(Result(classFile, MutableClass), InterimResult.forUB(field, NonTransitivelyImmutableField, fieldDependencies, fieldContinuation))
```

## IncrementalResult

[`IncrementalResult`](/library/api/SNAPSHOT/org/opalj/fpcf/IncrementalResult.html) allows your analysis to guide the computation order.  
Use it with an eager analysis that you don't start for all entities, but only for some kind of base case (e.g., for `Java.lang.Object`) to schedule the analysis of more complex cases only once the respective base case has been computed.  
`IncrementalResult` takes one result on any kind and an iterator of pairs of an analysis function and the entity this function should be applied to:
```scala
IncrementalResult(Result(classFile, immutability), classHierarchy.allSubclassTypes(classType).map((analyzeClassImmutability, _)))
```

## PartialResult

[`PartialResult`](/library/api/SNAPSHOT/org/opalj/fpcf/IncrementalResult.html) allows several analyses to collaboratively compute a single property (see the tutorial on [Collaborative Analyses](CollaborativeAnalyses.html)).
A `PartialResult` is different from the other result types in that it does not take a property value, but, in addition to analyzed entity, the kind of property (identified by the [`PropertyKey`](/library/api/SNAPSHOT/org/opalj/fpcf/PropertyKey.html)) and a function that is applied to whatever value that entity/property kind pair currently has:
```scala
PartialResult[DeclaredMethod, Callers](method, Callers.key, {
    case InterimUBP(ub: Callers) =>
        if (!ub.hasVMLevelCallers)
            Some(InterimEUBP(method, ub.updatedWithVMLevelCall()))
        else None
    case _: EPK[_, _] =>
        Some(InterimEUBP(method, OnlyVMLevelCallers))
    case r =>
        throw new IllegalStateException(s"unexpected previous result $r")
})
```
It has to return a `Some` of some new value for the property if you want to update the value or it can return `None` if no update is necessary.  
As you can see, while it is only some partial result, it does not have any dependencies, thus, no further computation will be scheduled once the partial result has been applied.  
If you need to schedule further updates, use an `InterimPartialResult`:

## InterimPartialResult

[`InterimPartialResult`](/library/api/SNAPSHOT/org/opalj/fpcf/IncrementalResult.html) finally allows you to give several `PartialResult`s (or even none) and give a set of dependencies and a continuation function to be called when these dependencies are updated:
```scala
InterimPartialResult(None, dependencies, continuation)
```
In this example, we don't provide any `PartialResult`s (`None`), but still ensure that `continuation` is called whenever one of the dependencies changes.

## What next?

This concludes our overview of different types of results in OPAL.  
If you haven't done so ye, we suggest you read on what option you have for [Lattices](Lattices.html) and [Schedulers](Schedulers.html) in OPAL.
You can also read about [Collaborative Analyses](CollaborativeAnalyses.html) for more detail on `PartialResult` and `InterimPartialResult`.
