# Collaborative Analyses
This tutorial will guide you through the implementation of a simple analysis module that integrates into OPAL's call-graph analysis framework, using OPAL's support for collaborative analyses.  
The module developed in this tutorial will compute all classes that are possibly instantiated during program execution as is necessary to perform rapid type analysis (RTA).
You should have read the tutorial on [Writing Fixed-Point Analyses](FixedPointAnalyses.html) first that introduces the basic concepts of OPAL's fixed-point analyses.

We will develop the implementation in small steps, but you can get the complete, runnable source code for this analysis [here](InstantiatedTypesAnalysis.scala).

## Defining a Lattice

As is always the case for fixed-point analyses in OPAL, we need a suitable lattice to represent our analysis' results.  
In order to represent the set of all classes potentially instantiated, we thus create a lattice of set<sup title="OPAL's UIDSet is an optimized set for several kinds of entities in OPAL, including, e.g., ObjectType which represents the type of a class.">[note]</sup> values:
```scala
sealed trait InstantiatedTypesPropertyMetaInformation extends PropertyMetaInformation {
    final type Self = InstantiatedTypes
}

case class InstantiatedTypes(classes: UIDSet[ObjectType]) extends InstantiatedTypesPropertyMetaInformation with OrderedProperty {
    override def checkIsEqualOrBetterThan(e: Entity, other: InstantiatedTypes): Unit = {
        if (!classes.subsetOf(other.classes)) {
            throw new IllegalArgumentException(s"$e: illegal refinement of $other to $this")
        }
    }

    final def key: PropertyKey[InstantiatedTypes] = InstantiatedTypes.key
}

object InstantiatedTypes extends InstantiatedTypesPropertyMetaInformation {
    final val key: PropertyKey[InstantiatedTypes] = PropertyKey.create(
        "InstantiatedTypes",
        (_: PropertyStore, reason: FallbackReason, _: Entity) => reason match {
            case PropertyIsNotDerivedByPreviouslyExecutedAnalysis => InstantiatedTypes(UIDSet.empty)
            case _ => throw new IllegalStateException(s"No analysis is scheduled for property InstantiatedTypes")
        }
    )
}
```
We use a case class here to provide a container for an arbitrary set of [`ObjectType`](/library/api/SNAPSHOT/org/opalj/br/ObjectType.html)s.  

Note that for the `key`, we didn't just provide a fallback value, but a small function that is called whenever `InstantiatedTypes` are needed, but have not been computed.  
This is to distinguish between two cases:  
Either, the analysis (which we will implement below) was executed, but there simply are no classes that have been instantiated.
In this case, we want to provide not a sound over-approximation of the program behavior, but the precise information that no class can possibly be instantiated.  
On the other hand, it may be that our analysis has not been executed.
In this case, a sound over-approximation (namely that *any* class could be instantiated) would be possible, but probably not what the user of our lattice expects.
Thus, we raise an error here to point the user to the fact that he may want to run our analysis to get the information that he requires.

## Implementing the Analysis

Now, let's implement the (simplified) analysis.  
As usual, we start by creating an analysis class with an analysis function:
```scala
class InstantiatedTypesAnalysis(val project: SomeProject) extends FPCFAnalysis {
    implicit private val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)

    def analyzeMethod(method: DeclaredMethod): PropertyComputationResult = { [...] }
}
```
We need a [`ProjectInformationKey`](/library/api/SNAPSHOT/org/opalj/br/analyses/ProjectInformationKey.html) here, namely the `DeclaredMethodsKey`, as this will later be needed implicitly to resolve calls with the call graph.  
The analysis function takes a [`DeclaredMethod`](/library/api/SNAPSHOT/org/opalj/br/DeclaredMethod.html) (a representation of a method in the context of its class) as the entity to be analyzed as we want to find all classes instantiated by methods that potentially called (i.e., that are not *dead*).  
Note that unlike most analyses, we will however *not* compute a result just for this entity, we just use it to compute its effect on the set of instantiated classes *anywhere* in the analyzed program.

We will later configure our analysis function to be called once for every method that is potentially called, but classes are instantiated only by constructors (named `<init>` in the Java Virtual Machine), thus we first check whether the analyzed method actually is a constructor:
```scala
def analyzeMethod(method: DeclaredMethod): PropertyComputationResult = {
    if (method.name != "<init>")
        return NoResult

    [...]
}
```
We use [`NoResult`](/library/api/SNAPSHOT/org/opalj/fpcf/NoResult.html) to signal that we won't compute any result for methods that are not constructors.

Next, we get the type of the class that is instantiated, which is the class that declares the analyzed constructor.
```scala
def analyzeMethod(method: DeclaredMethod): PropertyComputationResult = {
    [...]

    val instantiatedType = method.declaringClassType

    [...]
}
```

Using this, we define a local function that will produce our intended result:
```scala
def analyzeMethod(method: DeclaredMethod): PropertyComputationResult = {
    [...]

    def result(): ProperPropertyComputationResult = {
        PartialResult[SomeProject, InstantiatedTypes](
            project,
            InstantiatedTypes.key,
            (current: EOptionP[SomeProject, InstantiatedTypes]) => current match {
                case InterimUBP(ub: InstantiatedTypes) =>
                    if (ub.classes.contains(instantiatedType))
                        None
                    else
                        Some(InterimEUBP(project, InstantiatedTypes(ub.classes + instantiatedType)))

                case _: EPK[_, _] =>
                    Some(InterimEUBP(project, InstantiatedTypes(UIDSet(instantiatedType))))

                case r => throw new IllegalStateException(s"unexpected previous result $r")
            }
        )
    }

    [...]
}
```
We use a [`PartialResult`](/library/api/SNAPSHOT/org/opalj/fpcf/NoResult.html) here that allows us to contribute to a collaboratively computed property.  
The partial result takes the entity (we use `project` here, since the set of instantiated classes is global to the whole program that is analyzed) and the key of the property that we compute.  
Finally, it takes a function that will get the current value of that property and computes an update to it.  
To do so, we check whether there already is a property present and extract its upper bound.  
If that upper bound already contains our class, we return `None` to signal that no update is necessary, otherwise we create an updated result, which is an [`InterimEUBP`](/library/api/SNAPSHOT/org/opalj/fpcf/InterimEUBP.html), i.e., a not yet final result consisting of an entity (`project`) and its property, which is the old set of instantiated classes extended by the class type of the analyzed constructor.  
If, on the other hand, no property has been computed so far, the update function will be called with an [`EPK`](/library/api/SNAPSHOT/org/opalj/fpcf/EPK.html), i.e., a tuple of the entity and the key of the property.
In that case, we return property that contains just the class type of the analyzed constructor.

Next, we should check whether the analyzed constructor was explicitly called to instantiate an object of the respective class, not implicitly during instantiation of a subclass.  
For that, we need the method's callers:
```scala
def analyzeMethod(method: DeclaredMethod): PropertyComputationResult = {
    [...]

    val callersProperty = propertyStore(method, Callers.key)

    [...]
```

To check them, we define a local method and call it:
```scala
def analyzeMethod(method: DeclaredMethod): PropertyComputationResult = {
    [...]

    def checkCallers(callersProperty: EOptionP[DeclaredMethod, Callers]): PropertyComputationResult = { [...] }

    checkCallers(callersProperty)
}
```

Let's start implementing it:
```scala
def checkCallers(callersProperty: EOptionP[DeclaredMethod, Callers]): PropertyComputationResult = {
    val callers: Callers = callersProperty match {
        case FinalP(NoCallers) =>
            return NoResult

        case UBP(v) => v

        case r => throw new IllegalStateException(s"unexpected result for callers $r")
    }

    [...]
}
```
We return `NoResult` if there actually aren't any callers (`FinalP` allows to extract a final property result), otherwise we extract the upper bound from the property using `UBP`.

We next check for two special cases, namely whether the analyzed constructor is called from an unknown context (e.g., if it is an entry point to the analyzed program that may be called from outside code) or whether it is called by the Java Virtual Machine:
```scala
def checkCallers(callersProperty: EOptionP[DeclaredMethod, Callers]): PropertyComputationResult = {
    [...]

    if(callers.hasCallersWithUnknownContext || callers.hasVMLevelCallers)
        return result()

    [...]
```

Now we can iterate over all callers known so far (the second part of the tuple, the actual program counter of the call is irrelevant here).
The `callers` method implicitly requires the `DeclaredMethods`, which is why we got that at the beginning of our analysis class.
```scala
def checkCallers(callersProperty: EOptionP[DeclaredMethod, Callers]): PropertyComputationResult = {
    [...]

    for((caller, _, isDirect) <- callers.callers) {
        if (!isDirect)
            return result()

        if (caller.name != "<init>")
            return result()

        [...]
    }

    [...]
}
```
We first check whether the call is indirect, e.g., performed through reflection.
If it is, the analyzed constructor has been called explicitly, thus its class is instantiated and we can return a respective result using the `result()` method we defined above.
Second, we check whether the caller could be a subclass' constructor by checking whether it is a constructor at all.  
If it isn't, again the constructor must have been called explicitly.

If the caller is a constructor, we now check whether it belongs to a direct subclass:
```scala
for((caller, _, isDirect) <- callers.callers){
    [...]

    val callerClass = project.classFile(caller.declaringClassType)

    if(callerClass.isEmpty || callerClass.get.superclassType.isEmpty || callerClass.get.superclassType.get != instantiatedType)
        return result()

    [...]
}
```
If we don't know the class from which the analyzed constructor was called, we have to assume that it might not be a subclass and thus, as before, return a result that states that the class of the analyzed constructor was actually instantiated.  
The same is true if we know the class, but it has no superclass (this mainly concerns `java.lang.Object`), in which case it can't be a subclass, or if it has a superclass but it is not the class of the analyzed constructor.

If we didn't return a result yet, we have established that the caller is a constructor of a direct subclass.  
However, it may still have an explicit call to the analyzed constructor, thus we have to look for such call in its instructions:
```scala
for((caller, _, isDirect) <- callers.callers){
    [...]

    val body = caller.definedMethod.body.get

    [...]
}
```
To do so, we first have to get its body, i.e., a representation of the method's actual implementation.  
Note that here, we neither check whether the method actually has an actual method declaration (i.e., it is valid that we get this `definedMethod`) nor whether that declaration includes a body.  
This is because we know that the classfile is available and thus its methods are as well and also constructors are always unambiguous.  
Finally, they also can't be abstract or implemented by a native method, thus they always have a body.

Now we can look for explicit instantiations of the analyzed constructor's class:
```scala
for((caller, _, isDirect) <- callers.callers){
    [...]

    if(body.exists((_, instruction) => instruction == NEW(instantiatedType)))
        return result()
}
```
If there is a `NEW` instruction for that class, that is an explicit instantiation and we return a respective result.
Otherwise, we now know that this caller is a constructor of a direct subclass that only calls the analyzed constructor as part of its own initialization and thus, it doesn't actually instantiate the class of the analyzed constructor.

If we didn't find any caller that caused an explicit instantiation, there still might be more callers that we just don't know yet.  
Thus, we define a local continuation function to handle any updates to our callers property:
```scala
def checkCallers(callersProperty: EOptionP[DeclaredMethod, Callers]): PropertyComputationResult = {
    [...]

    def continuation(updatedValue: SomeEPS): PropertyComputationResult = {
        checkCallers(updatedValue.asInstanceOf[EOptionP[DeclaredMethod, Callers]])
    }

    [...]
}
```
It just calls `checkCallers` again with the updated value.  

Did you notice that the continuation's return type is `PropertyComputationResult` instead of `ProperPropertyComputationResult`?  
Indeed, it invokes `checkCallers` and that can return `NoResult`.  
However, this is not a problem here, because we use this continuation not for an `InterimResult`, but for an [`InterimPartialResult`](/library/api/SNAPSHOT/org/opalj/fpcf/IncrementalResult.html).  
This allows us to register dependencies and a continuation function without binding them to a specific entity/property pair.
Let's see it in use by completing the `checkCallers` function:
```scala
def checkCallers(callersProperty: EOptionP[DeclaredMethod, Callers]) = {
    [...]

    if (callersProperty.isFinal) {
        NoResult
    } else {
        InterimPartialResult(
            Nil,
            Set(callersProperty),
            continuation
        )
    }
}
```
If the callers property already was final, we have checked all possible callers and thus, there can't be any further instantiation, thus we can return `NoResult`.  
Otherwise, we need to specify a continuation function that is called when the set of dependencies (that consists only of the `callersProperty`) changes.  
The first parameter for the `InterimPartialResult` allows to give some `PartialResult`s, but we don't need any here, thus we set it to the empty list `Nil`.

This completes our analysis, now we have to provide a scheduler for it.

## Scheduling the Analysis

We use an [`FPCFTriggeredAnalysisScheduler`](/library/api/SNAPSHOT/org/opalj/br/fpcf/FPCFTriggeredAnalysisScheduler.html)<sup title="The BasicFPCFTriggeredAnalysisScheduler provides an empty implementation for some rarely used methods.">[note]</sup> that allows us to trigger our analysis whenever a method is found to be reachable in the call graph.
```scala
object InstantiatedTypesAnalysisScheduler extends BasicFPCFTriggeredAnalysisScheduler {
    override def requiredProjectInformation: ProjectInformationKeys = Seq(DeclaredMethodsKey)

    override def uses: Set[PropertyBounds] = PropertyBounds.ubs(InstantiatedTypes, Callers)

    [...]
}
```
As usual, we first have to provide the used `ProjectInformationKeys` and fixed-point properties.  
Remember we used the `DeclaredMethodsKey` in our analysis, so we have to specify it here.
Also, we use upper bounds from the `InstantiatedTypes` and `Callers` lattices.  
Note that `PropertyBounds.ubs(...)` is a shorthand for `Set(PropertyBounds.ub(...), ...)`.

Next, we specify the derived properties:
```scala
object InstantiatedTypesAnalysisScheduler extends BasicFPCFTriggeredAnalysisScheduler {

    [...]

    override def derivesEagerly: Set[PropertyBounds] = Set.empty

    override def derivesCollaboratively: Set[PropertyBounds] = PropertyBounds.ubs(InstantiatedTypes)

    [...]
}
```
We don't have any eager computations, but we contribute to the upper bound of the `InstantiatedTypes` in a collaborative way, i.e., by means of `PartialResult`s.

Finally, we specify the triggering itself:
```scala
object InstantiatedTypesAnalysisScheduler extends BasicFPCFTriggeredAnalysisScheduler {

    [...]

    override def triggeredBy: PropertyKey[Callers] = Callers.key

    override def register(project: SomeProject, propertyStore: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new InstantiatedTypesAnalysis(project)
        propertyStore.registerTriggeredComputation(triggeredBy, analysis.analyzeMethod)
        analysis
    }
}
```
We have to provide the key of the property that triggers our analysis.  
As we specify the `Callers.key` here, our analysis will be executed for every method that has *any* callers computed for it, i.e., that is reachable in the call graph.  
In the `register` method, we have to call the PropertyStore's `registerTriggeredComputation` method that takes the triggering property key and our analysis method.

## Running the Analysis

As a last step, we implement a simple runner to test our analysis.
```scala
object InstantiatedTypesRunner extends ProjectAnalysisApplication {
    override def doAnalyze(project: Project[URL], parameters: Seq[String], isInterrupted: () => Boolean): BasicReport = {
        val (propertyStore, _) = project.get(FPCFAnalysesManagerKey).runAll(
            CHACallGraphAnalysisScheduler,
            InstantiatedTypesAnalysisScheduler
        )

        val instantiatedTypes = propertyStore(project, InstantiatedTypes.key).asFinal.p.classes.size

        BasicReport(
            "Results of instantiated types analysis: \n"+
                s"Number of instantiated types: $instantiatedTypes"
        )
    }
}
```
It executes our analysis alongside a class-hierarchy analysis (CHA) call graph and prints the number of instantiated types to the console.  
Note that when querying the property store for the `InstantiatedTypes` property key, we know that it must be a final result because all analyses have been completed by that point.  
Remember to specify the program that you want to analyze with the "-cp=<some path>" parameter.

## What next?

This concludes our tutorial on the implementation a simple, yet complete collaborative fixed-point analysis in OPAL.

If you want to implement more complex analyses, we suggest you read on what else you can do with [Lattices](Lattices.html), [Schedulers](Schedulers.html) and [Results](Results.html) in OPAL.
