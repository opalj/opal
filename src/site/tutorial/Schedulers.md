# Schedulers

Schedulers are objects that tell OPAL how to use your analysis to compute some desired results.
There are four types of schedulers in OPAL that provide four different modes of scheduling:  
[eager](#eager-scheduling), [lazy](#lazy-scheduling), [transformers](#transformers) and [triggered](#triggered-scheduling) schedulers.  

Before discussing each of them individually, let's have a look at what they have in common.

## Scheduler Basics

All schedulers have to provide information on what data your analysis might use:
```scala
override def requiredProjectInformation: ProjectInformationKeys = Seq(FieldAccessInformationKey)
override def uses: Set[PropertyBounds] = Set(PropertyBounds.ub(FieldImmutability), PropertyBounds.ub(ClassImmutability))
}
```
This consists of two parts:

First, `requiredProjectInformation` gives the [`ProjectInformationKey`s](/library/api/SNAPSHOT/org/opalj/br/analyses/ProjectInformationKey.html) that your analysis uses.  
ProjectInformationKeys provide aggregated information about a project, such as a call graph or the set of methods that access each field.  
Here, we specified the [`FieldAccessInformationKey`](/library/api/SNAPSHOT/org/opalj/br/analyses/FieldAccessInformationKey$.html) that you can use with [`Project.get()`](/library/api/SNAPSHOT/org/opalj/br/analyses/Project.html#get[T%3C:AnyRef](pik:org.opalj.br.analyses.ProjectInformationKey[T,_]):T) to get [`FieldAccessInformation`](/library/api/SNAPSHOT/org/opalj/br/analyses/FieldAccessInformation.html), i.e., information about where each field is read or written.

Second, `uses` gives the results of fixed-point analyses that your analysis requires.  
Here, our analysis uses upper bounds for both `FieldImmutability` and `ClassImmutability`.  
You can specify other bounds using other methods from [`PropertyBounds`](/library/api/SNAPSHOT/org/opalj/fpcf/PropertyBounds$.html).  
Remember to always include the type(s) of results produced by your own analysis as well if you use them!

In general, every scheduler also has to provide information on what type(s) of results your analysis produces, but this is specified differently for different schedulers. 
Let's take a look at the individual scheduler types now.

## Eager Scheduling

The [`FPCFEagerAnalysisScheduler`](/library/api/SNAPSHOT/org/opalj/br/fpcf/FPCFEagerAnalysisScheduler.html) is for simple analyses that compute some properties for a number of entities that you know in advance.  
For example, you could compute the immutability for all fields of all classes in your analyzed program like this:
```scala
object EagerFieldImmutabilityAnalysis extends BasicFPCFEagerAnalysisScheduler {
	[...]

    override def derivesEagerly: Set[PropertyBounds] = Set(PropertyBounds.ub(FieldImmutability))

    override def derivesCollaboratively: Set[PropertyBounds] = Set.empty

    override def start(project: SomeProject, propertyStore: PropertyStore, initData: InitializationData): FPCFAnalysis = {
        val analysis = new FieldImmutabilityAnalysis(project)
        propertyStore.scheduleEagerComputationsForEntities(project.allFields)(analysis.analyzeFieldImmutability)
        analysis
    }
}
```
We specify the types of the produced results with `derivesEagerly`.  
Note that you can specify more than just one type of result if your analysis computes several properties at once.
[Collaborative Analyses](/tutorial/CollaborativeAnalyses.html) use the `derivesCollaboratively` instead and you can combine both if necessary.  

The eager scheduler's entry point is the `start` method.  
You are given the [`Project`](/library/api/SNAPSHOT/org/opalj/br/analyses/Project.html), i.e., your analyzed program and the [`PropertyStore`](/library/api/SNAPSHOT/org/opalj/fpcf/PropertyStore.html) that will execute your analyses.  
You can also get some initialization data if you need it, see [Advanced Scheduling](#advanced-scheduling) below for more information.

After creating your analysis, you use `scheduleEagerComputation` to schedule it.  
The first argument gives the entities for which you want to compute properties. Your analysis function will be called once for each entity.  
The second argument is the analysis function. It must take a single argument of the type of your entities and produce a [`PropertyComputationResult`](/library/api/SNAPSHOT/org/opalj/fpcf/PropertyComputationResult.html).
In the end, you return your analysis object.

## Lazy Scheduling

The [`FPCFLazyAnalysisScheduler`](/library/api/SNAPSHOT/org/opalj/br/fpcf/FPCFEagerAnalysisScheduler.html) lets you compute properties only for entities that need them, i.e., properties that are queried by other analyses<sup title="You can also manually tell the PropertyStore to compute the property for some entity using the force method.">[note]</sup>.
Let's again implement a scheduler for a field immutability analysis:
```scala
object LazyFieldImmutabilityAnalysis extends BasicFPCFLazyAnalysisScheduler {

	[...]

    override def derivesLazily: Some[PropertyBounds] = Some(PropertyBounds.ub(FieldImmutability))

    override def register(project: SomeProject, propertyStore: PropertyStore, initData: InitializationData): FPCFAnalysis = {
        val analysis = new ClassImmutabilityAnalysis(project)
        propertyStore.registerLazyPropertyComputation(FieldImmutability.key, analysis.lazilyAnalyzeFieldImmutability)
        analysis
    }
}
```
A lazy scheduler is very similar to an eager one, but it can only compute a single type of property that we specify with `derivesLazily`.  
The entry point for the lazy scheduler is called `register` and it provides you with the same objects as the eager scheduler's `start` method did.  
However, this time, we call `registerLazyPropertyComputation`.
That method takes the key that identifies the property we want to compute.
It also takes an analysis function, but this time, it must take a single argument that could be *any* type of entity. You can throw an exception if it doesn't match your expected type of entity, though.
As before, the analysis function must return a PropertyComputationResult.

## Transformers

The [`FPCFTransformerScheduler`](/library/api/SNAPSHOT/org/opalj/br/fpcf/FPCFTransformerScheduler.html) is a special kind of lazy scheduler.  
As with a lazy scheduler, properties are only computed for entities that are queried.
However, in contrast to the lazy scheduler, the analysis function is only called once some other property for the same entity has a final result.

As an example, see the following scheduler:
```scala
object TACAITransformer extends BasicFPCFTransformerScheduler {

	[...]

	override def uses: Set[PropertyBounds] = Set(PropertyBounds.finalP(BaseAIResult))

    override def derivesLazily: Some[PropertyBounds] = Some(PropertyBounds.finalP(TACAI))

    override def register(project: SomeProject, propertyStore: PropertyStore, initData: InitializationData): FPCFAnalysis = {
        val analysis = new TACAITransformer
        propertyStore.registerTransformer(BaseAIResult.key, TACAI.key)(analysis.analyze)
        analysis
    }
}
```
It computes the [`TACAI`](/library/api/SNAPSHOT/org/opalj/tac/fpcf/properties/TACAI.html), a three address code intermediate representation of your methods based on the result of an abstract interpretation.  
As that abstract interpretation has to be performed first, there is no need to call the analysis function before this has completed, thus a transformer is more convenient and more performant here than a lazy scheduler.

As you can see, it is almost identical to he lazy scheduler, but it uses `registerTransformer` to register the analysis function.  
In addition to the key of the property that our analysis computes (`TACAI.key` here), we also have to specify the key of the property that the analysis waits for (`BaseAIResult.key` in our example).  
Note that it is not necessary to explicitly query for this required property: when the property computed by the transformer is queried, that automatically triggers the computation of its required property as well.  
The analysis function here takes two arguments: the entity (as with lazy schedulers, this could be on any type) and the required property (a `BaseAIResult` here).

## Triggered Scheduling

The [`FPCFTriggeredAnalysisScheduler`](/library/api/SNAPSHOT/org/opalj/br/fpcf/FPCFTriggeredAnalysisScheduler.html) lets you start a computation for an entity only once you know that this entity does have some other property.  
Different to a transformer, that other property does not have to have a final result yet, though.  
Let's see an example of how to use this for a call graph module:
```scala
object MyCallGraphModule extends BasicFPCFTriggeredAnalysisScheduler {

	[...]

	override def derivesCollaboratively: Set[PropertyBounds] = Set(PropertyBounds.ub(Callees), PropertyBounds.ub(Callers))

    override def derivesEagerly: Set[PropertyBounds] = Set.empty

    override def triggeredBy: PropertyKind = Callers

    override def start(project: SomeProject, propertyStore: PropertyStore, initData: InitializationData): FPCFAnalysis = {
        val analysis = new MyCallGraphModule(project)
        propertyStore.registerTriggeredComputation(Callers.key, analysis.analyze)
        analysis
    }
}
```
Like for an eager scheduler, we can give several properties that we compute with one analysis.
Here, we use `derivesCollaboratively` to specify that our analysis contributes to both the `Callees` and `Callers` properties.

Additionally, we have to specify what kind of property will trigger our analysis with `triggeredBy`.
Here, it is the `Callers` property.

We use `registerTriggeredComputation` to register our analysis function with the property store, giving the key for the triggering property.
Like with an eager scheduler, the analysis function take a single argument of the type of entity that we want to process and returns a PropertyComputationResult.

Now, what exactly does this scheduler do?  
It ensures that the analysis function is only called for entities (methods in our example) if they have the given property (callers).  
Thus, we will only analyze methods that are actually called somewhere and not waste our time analyzing methods that are dead (not called anywhere).

Note that it is thus necessary to make sure that the property we depend upon is actually computed at least for some entities.  
In the call graph, this means to give the program's entry points (e.g., the `main` method) an (initial) callers property.
This can, e.g., be done by an eager analysis or manually, e.g. during initialization of some analysis (see [Advanced Scheduling](#advanced-scheduling) below).

## Advanced Scheduling

The class FPCFAnalysisScheduler provides a few more methods that you can implement if you need them.  
They implement a kind of life cycle of a scheduler.

```scala
type InitializationData
def init(project: SomeProject, propertyStore: PropertyStore): InitializationData
```
The `init` method is called immediately when your analysis is chosen to be executed.  
You can use it to compute any kind of data that you may need later on, it is provided when your scheduling method (`start` or `register`, depending on the type of scheduler) is called.
You can also use it to put some known data into the PropertyStore early on, e.g., for a base case of your analysis.

```scala
def beforeSchedule(project: SomeProject, propertyStore: PropertyStore): Unit
```
The `beforeSchedule` method is called shortly before your analysis is scheduled
It is called for all analyses that are executed at the same time before any of these analyses is actually scheduled.
Thus, you can use it to do anything that another analysis might need for its own scheduling.

Next up, the scheduling method (`start` or `register`, depending on the type of scheduler) is called for your analyses.

Once this has happened, `afterPhaseScheduling` for your analyses before they are finally executed.
You can use it if another analysis might have done something in its scheduling method that you need to react upon.
```scala
def afterPhaseScheduling(propertyStore: PropertyStore, analysis: A): Unit
```

```scala
def afterPhaseCompletion(project: SomeProject, propertyStore: PropertyStore, analysis: A): Unit
```
Once your analysis have completed execution and the fixed point is reached, `afterPhaseCompletion` is the last life-cycle method called.
You can use it to clean up after your analysis if necessary.

## What next?

This concludes our overview of different analysis scheduling options in OPAL.  
If you haven't done so ye, we suggest you read on what option you have for [Lattices](Lattices.html) and [Results](Results.html) in OPAL.
