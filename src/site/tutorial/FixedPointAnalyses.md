# Implementing Fixed-Point Analyses
This tutorial will guide you through the implementation of a simple fixed-point analysis for class immutability.  
This analysis checks whether instances of a class are mutable, non-transitively immutable (i.e., their fields are `final` but may point to mutable objects) or transitively immutable (i.e., their fields are `final` and only point to other transitively immutable classes).

We will develop the implementation in small steps, but you can get the complete, runnable source code for this analysis [here](ClassImmutabilityAnalysis.scala).

## Defining a Lattice

Fixed-point analyses in OPAL produce result values from explicitly defined [lattices](https://en.wikipedia.org/wiki/Lattice_(order)).  
The lattice for our class immutability analysis consists of several parts:

First, a trait that extends [`PropertyMetaInformation`](/library/api/SNAPSHOT/org/opalj/fpcf/PropertyMetaInformation.html):
```scala
sealed trait ClassImmutabilityPropertyMetaInformation extends PropertyMetaInformation {
    final type Self = ClassImmutability
}
```
We use this to define the type of our lattice to be `ClassImmutability`, which we implement next.

`ClassImmutability` is the main trait for our lattice (we chose it to be an [`OrderedProperty`](/library/api/SNAPSHOT/org/opalj/fpcf/OrderedProperty.html) which provides additional checks for the correctness of our analyses):
```scala
sealed trait ClassImmutability extends ClassImmutabilityPropertyMetaInformation with OrderedProperty {
    def meet(other: ClassImmutability): ClassImmutability = { [...] }

    override def checkIsEqualOrBetterThan(e: Entity, other: ClassImmutability): Unit = {
        if (meet(other) != other) {
            throw new IllegalArgumentException(s"$e: impossible refinement: $other => $this")
        }
    }

    final def key: PropertyKey[ClassImmutability] = ClassImmutability.key
}
```
We can define whatever we might need to easily handle the lattice values later on.  
Here, we define a `meet` function to compute the common lower bound of two values<sup title="In OPAL, the most optimistic value is top, the most pessimistic value is bottom.\nThus, the meet function will return a value that over-approximates its two inputs.">[note]</sup>.

The trait `OrderedProperty` requires us to implement the `checkIsEqualOrBetterThan` function to throw an exception if the `other` value is more precise.  
Here, we can use our defined `meet` function to check whether `other` is smaller easily.  
If we can't provide such check, if it would be too expensive or if we just don't care for this additional help in checking and debugging the correctness of our analyses, we could have extended [`Property`](/library/api/SNAPSHOT/org/opalj/fpcf/Property.html) instead of `OrderedProperty`.

We also have to provide a [`PropertyKey`](/library/api/SNAPSHOT/org/opalj/fpcf/PropertyKey.html) which is later used to tag and query results with.  
We will create that key below in a companion object.

We now implement the individual lattice values, called *Properties*, of which we define three simple ones here:

```scala
case object TransitivelyImmutableClass extends ClassImmutability

case object NonTransitivelyImmutableClass extends ClassImmutability

case object MutableClass extends ClassImmutability
```
This is a simple lattice consisting of singleton values, but it is possible to create more complex lattices by using (case) classes for the lattice values that can store more information, e.g., sets of values for set-based lattices.  
Check [here](/tutorial/Lattices.html) for more information on what else you can do with lattices in OPAL.

Now we can implement the `meet` function that we defined earlier:
```scala
def meet(other: ClassImmutability): ClassImmutability = {
    (this, other) match {
        case (TransitivelyImmutableClass, _)       => other
        case (_, TransitivelyImmutableClass)       => this
        case (MutableClass, _) | (_, MutableClass) => MutableClass
        case (_, _)                                => this
    }
}
```

Finally, we set up a singleton that allows us to statically refer to our lattice later on:
```scala
object ClassImmutability extends ClassImmutabilityPropertyMetaInformation {
    final val key: PropertyKey[ClassImmutability] = PropertyKey.create(
        "ClassImmutability",
        MutableClass
    )
}
```
Here, we finally create the `PropertyKey`.
It consists of a unique name of our choice and a sound fallback value to be used when someone tries to retrieve a class immutability value without previously executing the corresponding analysis.

## Implementing the Analysis

We will now implement the analysis itself.  
Remember that the goal is to check whether instance fields are `final` and whether they can only point to immutable objects.

We first create a class that extends [`FPCFAnalysis`](/library/api/SNAPSHOT/org/opalj/br/fpcf/FPCFAnalysis.html).
```scala
class ClassImmutabilityAnalysis(val project: SomeProject) extends FPCFAnalysis { [...] }
```
This requires a [`Project`](/library/api/SNAPSHOT/org/opalj/br/analyses/Project.html)<sup title="SomeProject gets rid of Project's type parameter for you.">[note]</sup> through which we can later access all information about the analyzed code.

The core of a fixed-point analysis is a single function that processes one entity and produces some form of [`PropertyComputationResult`](/library/api/SNAPSHOT/org/opalj/fpcf/PropertyComputationResult.html).
```scala
class ClassImmutabilityAnalysis(val project: SomeProject) extends FPCFAnalysis { 
    def analyzeClassImmutability(classFile: ClassFile): ProperPropertyComputationResult = { [...] }

    [...]
}
```
Method `analyzeClassImmutability` takes a [`ClassFile`](/library/api/SNAPSHOT/org/opalj/br/ClassFile.html) for the entity, as that represents a class.  
It returns a [`ProperPropertyComputationResult`](/library/api/SNAPSHOT/org/opalj/fpcf/ProperPropertyComputationResult.html) to signal it will always be able to return a result<sup title="If there are cases where no result can be computed, use PropertyComputationResult instead.">[note]</sup>.

Next, we initialize some state:
```scala
def analyzeClassImmutability(classFile: ClassFile): ProperPropertyComputationResult = {
    var immutability: ClassImmutability = TransitivelyImmutableClass
    var dependencies = Map.empty[Entity, EOptionP[Entity, Property]]

    [...]
}
```
We need to keep track of the information we already have about the immutability of our class, and as we are implementing an optimistic analysis, we initially assume it might be transitively immutable.  
Additionally, we will need to keep track of some dependencies. Dependecies are of type [`EOptionP`](/library/api/SNAPSHOT/org/opalj/fpcf/EOptionP.html), i.e., a pair of some entity with, optionally, some property that entity has.
We store them in a map with the entity as the key in order to be able to access them easily.

Now it is time to gather the information we need.  
We start by checking the immutability of the class we inherit from.  
First, we define a local function to process that information:
```scala
def analyzeClassImmutability(classFile: ClassFile): ProperPropertyComputationResult = {
    [...]

    def checkSuperclass(value: EOptionP[ClassFile, ClassImmutability]): Unit = {
        dependencies -= value.e
        if(value.hasUBP)
            immutability = immutability.meet(value.ub)
        if(value.isRefinable)
            dependencies += value.e -> value
    }

    [...]
}
```
First, if we had a dependency on this entity before, we remove it, because it is outdated now. `value.e` gives us the entity from the `EOptionP` pair.  
Next, if the value has an upper bound for the property (i.e., there is actually a value we can process), we use the `meet` function of our lattice to incorporate it into the immutability value for the analyzed class. `value.ub` gives us this upper bound from the `EOptionP` pair.  
Finally, if the value is refinable, i.e., it is not yet final, we (re-)add it to the dependencies.

Now, let's actually retrieve the superclass' immutability information:
```scala
def analyzeClassImmutability(classFile: ClassFile): ProperPropertyComputationResult = {
    [...]

    val superclassType = classFile.superclassType

    if(superclassType.isDefined && superclassType.get != ObjectType.Object) {
        [...]
    }

    [...]
}
```
First, we get the type of the superclass from our classfile.  
Note that some classes, in particular `java.lang.Object`, do not have superclasses, in which case we can't (and don't have to) check any superclass' immutability.  
We also don't have to check the immutability of `java.lang.Object` (represented by `ObjectType.Object`) as we know it is transitively immutable because it has no fields.  
Checking whether the superclass is `java.lang.Object` not only allows us to skip checking the superclass, but also allows to get useful results even when the JDK is not part of our analyzed software and thus the classfile for `java.lang.Object` is not available.

```scala
    if(superclassType.isDefined && superclassType.get != ObjectType.Object) {
        val superclass = project.classFile(superclassType.get)
        if(superclass.isEmpty)
            return Result(classFile, MutableClass)
        val superclassImmutability = propertyStore(superclass.get, ClassImmutability.key)
        checkSuperclass(superclassImmutability)
    }
```
Next, from the type, we try to retrieve the actual classfile from the analyzed project.  
It may be the case that the classfile for the superclass is not part of the analyzed project and thus, we get no result here.  
In that case, we soundly assume that it is mutable and can thus end our analysis here early, returning that our classfile represents a mutable class.  
Otherwise, we ask the [`PropertyStore`](/library/api/SNAPSHOT/org/opalj/fpcf/PropertyStore.html) for the superclass' class immutabilty, using the key we defined in our lattice.  
The property store is the central data structure that keeps track of all properties that have been computed for any entity so far.  
We didn't have to define it, because `FPCFAnalysis` provides it for us.  
Finally, we use our previously defined `checkSuperclass` function to process the value that we got from the property store.

As the immutability of our class also depends on its instance fields, we perform similar processing for them:
```scala
def analyzeClassImmutability(classFile: ClassFile): ProperPropertyComputationResult = {
    [...]

    def checkField(value: EOptionP[Field, FieldImmutability]): Unit = {
        dependencies -= value.e
        if(value.hasUBP)
            value.ub match {
                case TransitivelyImmutableField    => /* Nothing to do here */
                case NonTransitivelyImmutableField => 
                    if(immutability != MutableClass)
                        immutability = NonTransitivelyImmutableClass
                case MutableField                  => immutability = MutableClass
            }
        if(value.isRefinable)
            dependencies += value.e -> value
    }

    val instanceFields = classFile.fields.filter(!_.isStatic)
    val fieldImmutabilities = propertyStore(instanceFields, FieldImmutability.key)
    fieldImmutabilities.foreach(checkField)

    [...]
}
```
The `checkField` function is similar to `checkSuperclass`.  
We assume that a lattice `FieldImmutability` exists that is essentially identical to the one defined above for class immutability.  
Instead of using a predefined `meet` method, here we check the possible values explicitly.
We could of course have defined a function to incorporate a `FieldImmutability` into a `ClassImmutability` in the class immutability lattice to make our life easier here.

In order to get the immutability properties of the class' instance fields, we filter the classfile's fields for those that aren't static, then we use a second form of the property store's `apply` method to query several properties at once and finally pass each of the results to `checkField`.

Now we have queried all information that we need, but as you have seen, the property store may return `EOptionP` values that have no upper bound yet for the property we're interested in, or values that aren't be final yet.  
The fixed-point computation will provide these values later on in an asynchronous fashion.  
In order to be able to process them once they are available, we define another function, called the continuation:
```scala
def analyzeClassImmutability(classFile: ClassFile): ProperPropertyComputationResult = {
    [...]

    def continuation(updatedValue: SomeEPS): ProperPropertyComputationResult = { [...] }

    [...]
}
```
The continuation function gets an [`EPS`](/library/api/SNAPSHOT/org/opalj/fpcf/EPS.html)<sup title="SomeEPS gets rid of EPS's type parameters for you.">[note]</sup> which is an `EOptionP` that is guaranteed to have a value for the property.
Different to the analysis function itself, the continuation *must* return a result, thus we have to use `ProperPropertyComputationResult` and may not use the more general `PropertyComputationResult` here.
Let's not worry about `continuation`'s implementation for now, we will fill that out later.

Now, there's only one thing left to do, and that is returning the result of our analysis.  
Let's define a small function that helps us with this:
```
```scala
def analyzeClassImmutability(classFile: ClassFile): ProperPropertyComputationResult = {
    [...]

    def result(): ProperPropertyComputationResult = { 
        if(dependencies.isEmpty || immutability == MutableClass)
            Result(classFile, immutability)
        else
            InterimResult.forUB(classFile, immutability, dependencies.valuesIterator.toSet, continuation)
    }

    result()
}
```
There are two possible cases here:  
If we have no dependencies, or if we reached the lattice's bottom value `MutableClass`, we can just return the immutability that we computed.  
Otherwise, however, this immutability is not yet final and we have to create an [`InterimResult`](/library/api/SNAPSHOT/org/opalj/fpcf/InterimResult.html) instead.  
We specify that the given immutability is an optimistic upper bound by using the `forUB` factory method.  
Additionally, we provide the set of dependencies and the `continuation` function defined earlier.  
Calling the `result` function is the final thing to do in our analysis.

Now, let's get back to that `continuation` function and actually implement it:
```scala
def continuation(updatedValue: SomeEPS): ProperPropertyComputationResult = {
    updatedValue.e match {
        case _: ClassFile => checkSuperclass(updatedValue.asInstanceOf[EOptionP[ClassFile, ClassImmutability]])
        case _: Field     => checkField(updatedValue.asInstanceOf[EOptionP[Field, FieldImmutability]])
    }

    result()
}
```
It is very simple, we just use the updated value's entity to decide whether we need to process it using `checkSuperclass` or `checkField` and make sure that the types are as expected.  
Once we processed the updated value, we just call `result` to provide the result of `continuation` as well.

## Scheduling the Analysis

The analysis itself is completed now, but we still need to tell OPAL how to execute it.  
We do so using an [`FPCFAnalysisScheduler`](/library/api/SNAPSHOT/org/opalj/br/fpcf/FPCFAnalysisScheduler.html):
```scala
trait ClassImmutabilityAnalysisScheduler extends FPCFAnalysisScheduler {
    def derivedProperty: PropertyBounds = PropertyBounds.ub(ClassImmutability)

    override def requiredProjectInformation: ProjectInformationKeys = Seq.empty

    override def uses: Set[PropertyBounds] = Set(PropertyBounds.ub(ClassImmutability), PropertyBounds.ub(FieldImmutability))
}
```
With `derivedProperty`, we define that our analysis derives upper bounds (`ub`) for `ClassImmutability`, i.e., that this is an optimistic analysis of class immutability.  
While not necessary here, we do this here for convenience. We will use it shortly.  
Secondly, we have to specify what information our analysis uses.  
The analysis above uses no [`ProjectInformationKey`](/library/api/SNAPSHOT/org/opalj/br/analyses/ProjectInformationKey.html)<sup title="ProjectInformationKeys provide aggregated information about a project, such as a call graph or the set of methods that access each field.">[note]</sup>, but if it did, we had to specify them here.
However, the analysis does use values from fixed-point computations: It needs to query and process upper bounds of both `ClassImmutability` (for the superclass) and `FieldImmutability` for the instance fields.  
Specifying all of this ensures that OPAL knows that these values will have to be provided one way or the other.

Note that above we defined `ClassImmutabilityAnalysisScheduler` as a trait.  
We now extend it to implement an actual scheduler:
```scala
object EagerClassImmutabilityAnalysis extends ClassImmutabilityAnalysisScheduler with BasicFPCFEagerAnalysisScheduler {
    override def derivesEagerly: Set[PropertyBounds] = Set(derivedProperty)

    override def derivesCollaboratively: Set[PropertyBounds] = Set.empty

    override def start(project: SomeProject, propertyStore: PropertyStore, initData: InitializationData): FPCFAnalysis = {
        val analysis = new ClassImmutabilityAnalysis(project)
        propertyStore.scheduleEagerComputationsForEntities(project.allClassFiles)(analysis.analyzeClassImmutability)
        analysis
    }
}
```
This is an [`FPCFEagerAnalysisScheduler`](/library/api/SNAPSHOT/org/opalj/br/fpcf/FPCFEagerAnalysisScheduler.html)<sup title="The BasicFPCFEagerAnalysisScheduler provides an empty implementation for some rarely used methods.">[note]</sup> that allows us to run the analysis for a pre-defined number of entities.  
We first specify what our analysis computes: Using this scheduler, it eagerly computes upper bounds for `ClassImmutability`, which we provide using the shorthand defined above.  
Eager schedulers can also provide so-called collaborative computations, but that is an advanced topic discussed [here](CollaborativeAnalyses.html), so don't worry about it for now.  
There are also other types of Schedulers. You can get more information on them [here](Schedulers.html).  
Finally, we tell OPAL how to execute the analysis: We create a new analysis, then we tell the property store to eagerly call the `analyzeClassImmutability` function for all classfiles in the project and finally we return the analysis.

What if we don't want to execute our analysis for *all* classfiles?  
We can tell OPAL to only analyze those classfiles that we query from the property store.  
However, we need a small additional method in our analysis to do so:
```scala
class ClassImmutabilityAnalysis(val project: SomeProject) extends FPCFAnalysis { 
    [...]

    def lazilyAnalyzeClassImmutability(entity: Entity): ProperPropertyComputationResult = {
        entity match {
            case classfile: ClassFile => analyzeClassImmutability(classfile)
            case _ => throw new IllegalArgumentException("Class Immutability Analysis can only process classfiles!")
        }
    }
}
```
Because OPAL doesn't know in advance that we will only ever query the property store for the class immutability of classfiles, the parameter type of `analyzeClassImmutability` is too specific.  
Thus, we provide a second entry point to our analysis, `lazilyAnalyzeClassImmutability`, that takes any type of entity and, if it is a classfile, analyzes it.  
We just throw an exception if the entity is not a classfile, as we can only process classfiles.

Now we are ready to implement a [`FPCFLazyAnalysisScheduler`](/library/api/SNAPSHOT/org/opalj/br/fpcf/FPCFLazyAnalysisScheduler.html):
```scala
object LazyClassImmutabilityAnalysis extends ClassImmutabilityAnalysisScheduler with BasicFPCFLazyAnalysisScheduler {

    override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)

    override def register(project: SomeProject, propertyStore: PropertyStore, initData: InitializationData): FPCFAnalysis = {
        val analysis = new ClassImmutabilityAnalysis(project)
        propertyStore.registerLazyPropertyComputation(ClassImmutability.key, analysis.lazilyAnalyzeClassImmutability)
        analysis
    }
}
```
Similarly to the eager scheduler above, we have to specify what our analysis computes. This time, that means that it lazily computes upper bounds for `ClassImmutability`, again using our shorthand for that.  
Note that while eager schedulers can compute several kinds of properties at once (as expressed by the fact that `derivesEagerly` returns a `Set`), lazy schedulers may only compute a single kind of property (`derivesLazily` returns a `Some`, i.e., a single value).  
Again, like in the eager scheduler, we create a new analysis, but this time, we tell the property store to call `lazilyAnalyzeClassImmutability` only when a `ClassImmutability` is queried.  
As before, we finally have to return the analysis.

## Running the Analysis

Finally it is time to try our analysis.  
To do so easily, we extend [ProjectAnalysisApplication]() which provides us with an implicit `main` method that parses parameters for us, most importantly the "-cp=<some path>" parameter that lets users specify the path to a project that they want to analyze.
```scala
object ClassImmutabilityRunner extends ProjectAnalysisApplication { 
    override def doAnalyze(project: Project[URL], parameters: Seq[String], isInterrupted: () => Boolean): BasicReport = { [...] }
}
```

There is only on method that we need to implement, and that is `doAnalyze`:
```scala
override def doAnalyze(project: Project[URL], parameters: Seq[String], isInterrupted: () => Boolean): BasicReport = {
    val (propertyStore, _) = project.get(FPCFAnalysesManagerKey).runAll(
        EagerClassImmutabilityAnalysis,
        LazyFieldImmutabilityAnalysis
    )

    [...]
}
```
We use the [`FPCFAnalysisManagerKey`](/library/api/SNAPSHOT/org/opalj/br/fpcf/FPCFAnalysesManagerKey$.html) to get an [`FPCFAnalysisManager`](/library/api/SNAPSHOT/org/opalj/br/fpcf/FPCFAnalysesManager.html) that will run our analyses.  
We just pass all analyses that we want to execute to the `runAll` method.  
Note that we assume that a `LazyFieldImmutabilityAnalysis` has been implemented as well.  
As long as that doesn't exist, you can remove that line and OPAL will use the fallback value of the `FieldImmutability` lattice whenever a field immutability is queried.

The `runAll` method returns the property store that we can then use to query the results of our analyses:
```scala
override def doAnalyze(project: Project[URL], parameters: Seq[String], isInterrupted: () => Boolean): BasicReport = {
    [...]

    val transitivelyImmutableClasses    = propertyStore.finalEntities(TransitivelyImmutableClass).size
    val nonTransitivelyImmutableClasses = propertyStore.finalEntities(NonTransitivelyImmutableClass).size
    val mutableClasses                  = propertyStore.finalEntities(MutableClass).size

    [...]
}
```
The [`PropertyStore`](/library/api/SNAPSHOT/org/opalj/fpcf/PropertyStore.html) provides several methods to inspect the results.  
Here we use `finalEntities` to get all entities that have the given final property, then we count those.

Finally, the `doAnalyze` method requires us to return a [`BasicReport`](/library/api/SNAPSHOT/org/opalj/br/analyses/BasicReport.html), which is a simple way to return some string that will ultimate be printed to the console:
```scala
override def doAnalyze(project: Project[URL], parameters: Seq[String], isInterrupted: () => Boolean): BasicReport = {
    [...]

    BasicReport(
        "Results of class immutability analysis: \n" +
        s"Transitively Immutable classes:     $transitivelyImmutableClasses \n" +
        s"Non-Transitively Immutable classes: $nonTransitivelyImmutableClasses \n" +
        s"Mutable classes:                    $mutableClasses"
    )
}
```

## What next?

This concludes our tutorial on the implementation of a simple, yet complete fixed-point analysis in OPAL.

If you want to implement more complex analyses, we suggest you read on what else you can do with [Lattices](Lattices.html), [Schedulers](Schedulers.html) and [Results](Results.html) in OPAL.  
You can also read about [Collaborative Analyses](CollaborativeAnalyses.html) used, e.g., to build call graphs.
