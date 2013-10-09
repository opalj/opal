#Things that need to be done:

##Major tasks

* We need some support for Invokedynamic.

* Improve the possibility for exploring a project and tracing/controlling the progress of the AI. (JavaFX - WebView - ?)

* Implement a framework to analyze projects. Here, we should support something like a on-demand loading of class files to limit the amount of memory that is required.

* Implement a pluggable Escape Analysis

* Implement a Class Analysis (i.e., an analysis that - for a given program point - which type a value might have.)

##On-going Tasks

* We need MORE tests.

* We need more (precise) domains.

* Develop a concept to derive and store the intermediate results of static analyses (derive constraints?)

* Develop a set of analyses that operate on top of BATAI

* Develop an analysis to describe, derive and check method call protocols


##Scala - Lessons Learned
- we could use better support for large-scale family polymorphism
- we could use ``constructor-dependent types'' (see AIResultBuilder for why)
- we could use ``case traits'' to automatically create extractor methods based on certain fields
- method dependent types and path-dependent types ``often'' cause problems