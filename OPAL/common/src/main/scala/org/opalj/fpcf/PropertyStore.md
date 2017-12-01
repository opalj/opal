# The PropertyStore

The property store is responsible for (optionally concurrently) executing a set of computations that derive properties of different kinds for some entities.
Properties computed for entities always have to have (at least conceptually) an underlying lattice which puts all properties in a partial order. Furthermore, properties can be final or refineable. Additionally, it is possible to state that - though the property is conceptually refineable – it is actually final in the context of the running analysis. I.e., an analysis depending on a specific property may be informed that the result (whose extension has not changed) has become final.

In case that we have entities for which no analyses are scheduled, it is possible to let the store automatically fill in default values (fallback).

Additionally, if cycles between computations are detected the store offers appropriate means to resolve them.

Fallbacks are configurable and can be postponed.

## Programming Model
A computation of a property must never stop and wait for the result of a dependee, instead it must collect all dependees which have refineable property of relevance and give this information to the store when it stores (a next) intermediate result.

## Scheduling property computations
It is possible to schedule the computation of the properties for an arbitrary set of entities.

## Direct property computations
Direct property computations are fast and quick analyses that are triggered whenever a specific property is required; they are generally done in the context of the first thread that queries the property. _(There primary purpose is to avoid that the property stores does unnecessary work.)_

## Lazy property computations
 Lazy property computations are regular analyses, however, they are only scheduled, when the property of a specific entity is requested; they avoid that properties are computed which are not required. _(In general, all base analyses should be scheduled as lazy computations.)_

## Phased computations
In some cases it is possible to first run a very shallow, simple analysis and only if this one is finished, i.e., we have reached quiesence, a more precise one is scheduled which picks-up the results of the previous one to further refine the results. Though the results of an analysis w.r.t. a specific property

## Dependencies are managed automatically
I.e., the analysis only states to which other entities/property kinds it has dependencies and is informed about the changes automatically. The call back function (continuation function) is guaranteed to never be executed concurrently (additionally, the store gives the guarantee that the analysis will see the last value of a entity/property kind it is depending on eventually). After processing the dependencies, the analysis states their new dependencies. The new set of dependencies can be completely different.

The property store has the guarantee that it will get a new computation result whenever it passes an updated property to a downstream analysis.

(Goodies:
    - support debugging
        - support visualization
        - support querying
    - pool/bulk notifications, i.e., wait as long as possible before rescheduling the computation for a specific property
    - set the value for a specific cell
)

## Requirements
- all analyses know their dependencies
- the results of an analysis computed per entity (per phase) have to be monotonic (whether we go up or down the lattice is not "relevant")
