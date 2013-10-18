#Things that need to be done:

##Major tasks

* We need some support for Invokedynamic.

* Improve the possibility for exploring a project and tracing/controlling the progress of the AI. (JavaFX - WebView - ?)

* Implement a framework to analyze projects. Here, we should support something like a on-demand loading of class files to limit the amount of memory that is required.

* Implement a pluggable Escape Analysis

##On-going Tasks

* We need MORE tests.

* We need more (precise) domains.

* Develop a concept to derive and store the intermediate results of static analyses (derive constraints?)

* Develop a set of analyses that operate on top of BATAI (FindRealBugs)

* Develop an analysis to describe, derive and check method call protocols

* Develop a generalized analysis to check data-flows


##Scala - Lessons Learned (as of Scala 2.10)
- we could use better support for large-scale family polymorphism

- we could use ``dependent constructor types'' (see AIResultBuilder for why)

- we could use ``case traits'' to automatically create extractor methods based on certain fields

- dependent method types and path-dependent types ``often'' cause problems in the compiler...

- Aliasing and path-dependent types. E.g., in the following defining a value `val targetDomain = executionHandler.domain`  
would lead to a compile-time failure in the last line.
```
val executionHandler = invokeExecutionHandler(pc, definingClass, method, operands)
val parameters = operands.reverse.zipWithIndex.map { operand_index ⇒
            val (operand, index) = operand_index
            operand.adapt(executionHandler.domain, -(index + 1))
}.toArray(executionHandler.domain.DomainValueTag)
executionHandler.perform(pc, definingClass, method, parameters)
```