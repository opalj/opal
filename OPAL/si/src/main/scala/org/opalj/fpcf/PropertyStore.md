# The PropertyStore

The property store is responsible for storing properties associated with entities and for (optionally concurrently) executing a set of computations that derive properties of different kinds for some entities.
Properties computed for entities always have an underlying (semi-)lattice which puts all properties in a partial order.
 
In case that we have entities for which no analyses are scheduled, it is possible to let the store automatically fill in default values (fallback values). Fallbacks are configurable.

## Programming Model
A computation of a property must never stop and wait for the result of a dependee, instead it must collect all dependees which have a refineable property of relevance and give this information to the store when it stores (a next) intermediate result.

## Scheduling property computations
It is possible to schedule the computation of the properties for an arbitrary set of entities.

## Lazy property computations
Lazy property computations are regular analyses, however, they are only scheduled, when the property of a specific entity is requested; they avoid that properties are computed which are not required. _(In general, all base/supporting analyses should be scheduled as lazy computations.)_

## Fast-track computations
In some cases it is possible to write a very, very fast analysis (at most performing a few hundred bytecode operations) that can compute some property efficiently, without requiring significant resources upfront. 

## Dependencies are managed automatically
I.e., the analysis only states to which other entities/property kinds it has dependencies and is informed about the changes automatically. The call back function (continuation function) is guaranteed to never be executed concurrently (additionally, the store gives the guarantee that the analysis will see the last value of a entity/property kind it is depending on eventually). After processing the dependencies, the analysis states their new dependencies. The new set of dependencies can be completely different.

The property store has the guarantee that it will get a new computation result whenever it passes an updated property to a downstream analysis.

## Requirements
- all analyses know their dependencies
- the results of an analysis computed per entity (per phase) have to be monotonic (whether we go up or down the lattice is not "relevant")

## Debugging support
To facilitate the debugging of analyses, several options, which depend on the concrete property store implementation, are available.  
For example, the `PKEParallelPropertyStore` has an explicit `debugging` mode and also offeres the possibility to record all relevant events such as updates of values.
