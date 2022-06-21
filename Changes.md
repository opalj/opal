# Changes

Scala 2.13 changes
- Replace unicode arrows (⇒,->,<-) by ascii arrows (=>,->,<-)
- Replace Traversable by scala.collection.Iterable
- Replace TraversableOnce by scala.collection.IterableOnce
- Changes to OPAL's custom data structures:
  - Replace RefIterator by scala.collection.iterator
  - Replace Chain by scala.collection.immutable.List
  - Replace RefArray and IntArray by scala.collection.immutable.ArraySeq
  - Replace RefArrayStack by scala.collection.immutable.Stack
  - Replace RefAppendChain by scala.collection.mutable.ArrayDeque
  - Replace RefArrayBuffer by scala.collection.mutable.ArrayBuffer

## 4.0.0 - Released May 7th 2021

- *we are now using Scala 2.12.13*
- `cleanBuild` now requires at least Java 14 to compile all test fixtures
- full support for Java 11 up to 16
- class files for newer Java versions can be read if they do not use features beyond Java 16
- opportunistic rewriting of dynamic constants
- parallel implementation for the PropertyStore
- FPCF analyses now use sets for their dependencies
- added preliminary framework for FPCF call graphs
  - includes CHA, RTA, XTA/MTA/FTA/CTA and points-to based call graphs
  - also includes modules for reflection, static initializers, finalizers, serialization, threads, selected native methods and to load dynamic data on reflective calls recorded by TamiFlex
  - allows resolution of calls by method signature for library analyses
- added an analysis to collect information about the usage of a class within a project
- added `LongTrieSet`, `LongLinkedTrieSet` and `LongTrieSetWithList` to optimize storage of Long values
- removed `PrecomputedPartialResult`
- fixed the semantics of virtual and interface calls w.r.t. private methods
- fixed TAC text representation where a regular return would be marked with a warning that it always throws an exception even if it doesn't
- fixed a wrong mapping of TAC indices on some dead bytecode instructions
- fixed escape analysis which could produce non-deterministic results
- fixed issues with the purity analyses
- fixed parsing of -projectConfig parameter for `AnalysisApplication`
- fixed equals implementation for `EOptionP`

## 3.0.0 - Snapshot available since June 7th, 2019

- added a preliminary IFDS framework
- the Hermes and BugPicker UI projects were deleted (JavaFX was removed from the JDK 11 which makes the overall development and deplyoment process to cost intensive)
- Hermes was promoted to a real project: TOOLS/hermes
- renamed `DefaultOneStepAnalysis` to `ProjectAnalysisApplication`; added a new subclass `MethodAnalysisApplication` to facilitate the developmen of respective analysis
- added support for analyses using the monotone framework; the monotone framework itself was added to `CFG`
- the three-address code has been moved to its own subproject (`ThreeAddressCode`) in the folder OPAL/tac
- fixed the name of the static analysis infrastructure project (the name of the project on Maven Central has changed)
- vastly improved PropertyStore with support for Transformers
- the demos subproject was moved to the OPAL-DeveloperTools subfolder
- fixed the computation of `allSubtypes` of for `java.lang.Object`
- fixed several issues related to the handling of methods with subroutines (JSR/RET)
- fixed the toString method of `StaticMethodCall`
- fixed the generation of ObjectTypes using `Type(classOf[...])`
- added a preliminary FPCF analysis which determines the type of values stored in fields
- added a preliminary FPCF analysis which determines core properties of the values returned by methods
- the FPCF framework now has proper support for analyses which refine lower bounds
- renamed `DomainValue.valueType` to `leastUpperType`
- renamed `DefaultDomainValueBinding` to `DefaultSpecialDomainValuesBinding`
- removed `...ai...TheClassHierarchy` trait - every domain has to provide a class hierarchy
- `ValueInformation` now provides a more elaborate interface and should be usable wherever `KnownTypedValue` was used before
- moved the `isValueASubtypeOf` methods to the _value framework (`org.opalj.value`)_ (i.e., the methods are moved up in the class hierarchy)
- moved the `verificationTypeInfo` methods to the _value framework_ (i.e., they are moved up in the class hierarchy)
- the domain classes (e.g., `org.opalj.ai.domain.l0.TypeLevelReferenceValue` or `...l1.ReferenceValues`) which define the framework for handling reference values now use traits instead of classes; the concrete classes are now found in the `...DefaultBinding...` classes
- `java.*.Comparable|Cloneable|Serializable` now get fixed ObjectType ids
- Java 11 files which do not use Java 11 Bytecode features can be read (Java doesn't make use of the new features so far)
- we now have a new meta-project `Framework` to facilitate reuse.

## 2.0.1 - Released Oct. 10th 2018

- fixed a bug in the identification of closed strongly connected components
- fixed a bug when computing the stackmap table when a register store instruction is found in a try block of a finally handler and therefore is considered to be throwing an exception by the VM when it tries to verify the bytecode
- fixed a bug when a simple property of an entity is queried in a later phase (after the analysis was run) and the analysis didn't compute a value

## 2.0.0 - Released Oct. 2nd 2018

- Added support for instrumenting class files
- support for Java 9
- support for Java 10
- support for Java 11
- rewriting StringConcatFactory based invokedynamics
- support for analyzing Scala 2.12.6-7 invokedynamics
- Hermes now has extended visualization capabilities to make it even easiere to comprehend the differences between projects
- the overall performance has been improved (in particular on multi-core systems with 4 or more cores)
- moved to sbt 1.2.x
- fixed issues in some tests which open a huge number of files
- fixed a rare issue in the identification of closed strongly connected components
- completely reimplemented the property store
  - added various analyses related to deriving the purity of methods, the immutabiliy of classes, escape information etc.
- very much improved OPAL's collection library w.r.t. optimized data structures for Int values

## 1.0.0 - Released Oct. 25th 2017

- *we are now using Scala 2.12.4*
- added a method to reset a Project to its initial state (all information derived by analyses is thrown away)
- added several type test/cast methods to Instruction to support cases where the type of an instruction is known from the context
- added a new framework for testing properties derived using the `PropertiesStore`
- improved the time required to create a project by ~20-30%
- improved handling of exceptions in TACAI
- improved the framework for computing control-dependency information
- improved the precision and soundness of the FieldMutabilityAnalysis
- fixed an off-by-one-error in computeMaxLocals
- fixed the recording of def-use information when the execution of a regular instruction (not `athrow`) *always* results in an exception (This also affected the generation of the 3-address code; in the 3-address code this resulted in local variable usage which had no definition site.)
- fixed the handling of exceptions by the data-flow framework if the exceptions hierarchy is not completely known
- fixed an issue related to cyclic jumps; i.e. goto instructions which form a loop (notably x: goto x;)
- fixed an infinite loop if a class file contains a "too small" unknown attribute and the reader (infinitely) waits for the next byte(s)
- updated dependencies
- removed support for setting the "isStrict" mode from AI (nowadays all VMs etc. perform all computations with asStrict semantics; we are now assuming everything is strict)

## 0.8.15 - Released Sep. 7th 2017

### General

- the call graph construction algorithms finally completely support Java 8 (e.g., default methods, static methods in interfaces, lambda expressions)
- ***Assertions are turned-off by default when you checkout the latest stable release of OPAL***; to turn them on rename `local.sbt.template` to `local.sbt`; assertions are still turned on, when you depend on a development snapshot from Maven Central
- removed the Eclipse plug-in sub-project; it wasn't maintained anymore and is now replaced by the ATOM plug-in
- removed the Viz sub-project; it wasn't maintained anymore and OPAL has gained the possibility to generate SVGs using [a JavaScript based Graphviz version](https://github.com/mdaines/viz.js) which is executed using JDK's Nashorn JavaScript engine
- fixed several minor bugs and issues when analyzing bytecode which contains compile-time dead code; the Groovy compiler frequently generated (generates?) such code
- renamed packages called "analysis" to "analyses"

### Bytecode Representation Subproject

- `Method` and `Field` now have a back-link to their respective defining class files. The previous mechanisms provided by the `Project` are no longer available.
- added explicit support for new code *entities* to better support Escape/Points-to analyses: `(Object/Array)AllocationSite` and `FormalParameter`
- `ProjectInformationKey`s can now be initialized using project specific initialization information
   (See `Project.getOrCreateProjektInformationKeyInitializationData` for further information.)
- renamed `ObjectImmutabilityAnalysi` to `ClassImmutabilityAnalysis` to reflect the names used in the forthcoming paper
- removed the legacy method call resolution methods  (those only working with pre Java 8 code) from the `Project`
- we are now *simplifying the control-flow at load-time* to ensure that the overall control-flow is more regular; i.e., that if instructions related to loops directly jump back and do not jump forward to a goto instruction which then jumps back
- fixed a bug when INVOKEDYNAMIC instructions are rewritten

### Bytecode Disassembler

- significantly overhauled the HTML structure generated by the ***OPAL Disassembler***; take a look at our new ATOM Bytecode Disassembler plug-in!

### Abstract Interpretation Framework

- significantly improved the output generated by the `TAC` tool; the CFG is also included
- Removed `FailingExpr` - to check if an expression has failed use the CFG (this was previously necessary anyway to check if a statement resulted in an exception; i.e., handling of exceptions is now more consistent.)
- renamed JumpToSubroutine in the 3-address code to JSR
- the parameters are now explicitly represented in TACAI
- fixed a bug in TACAI where the handled exceptions are swallowed
- fixed issues related to the computation of def-use information for very old (JSR/RET) bytecode
- `Stmt` and `Expr` can now be cast using the "as..." methods, if the target type is known (this provides an alternative if a pattern match is only done w.r.t. a (small) subset of the declared fields.)

### Developer Tools

- ***Hermes*** now has a headless mode (HermesCLI) (*It is not yet possible to run the optimizer, but everything else works.*)

### Common

- renamed the methods of the `PropertyStore` which used operators (e.g., '<<', '<|<', ... ) to facilitate comprehension
- clients can now control whether cycle-resolution should be performed or not
- IntSet is now called IntArraySet

## 0.8.14

- OPAL now has a very advanced 3-address code representation; go to www.opal-project.de to read about the 3-address code
- fixed all known bugs when rewriting INVOKEDYNAMIC instruction (contributed by Andreas Muttscheller)
- improved the public API of the `Abstract Interpretation Framework` to facilitate analyses using the 3-address representation
- the build script is now free of deprecation warnings (contributed by Simon Leischnig)
- added support for writing out (large) graphs as a CSV encoded adjacency matrix
- the `PropertyStoreKey` (previously `SourceElementsPropertyStoreKey`) offers the functionality to add functions which compute new entities

## 0.8.13

- we now have complete support for converting a class file using the standard bytecode representation to the native representation; the latter can then be serialized to a valid class file
- fixed a very, very rare concurrency related issue that resulted in OPAL making no progress at all due to insufficient resources (out of threads)
- fixed a null pointer exception in case of "uninitialized" `...ai.StringValues`
- we have created a new [webpage](https://www.opal-project.de) along with this release which has more documentation on how to use specific parts of OPAL

## 0.8.12

- we are now using Scala 2.11.11 (this is probably the last release using Scala 2.11)
- fixed the creation of the XHTML view (Bytecode Disassembler) for several Java 8 features

## 0.8.11

- the AI now prevents simple, unnecessary joins if a variable is known to be dead when multiple control flow paths join
- added a simple live variables analysis to `br.Code.liveVariables` which computes liveness information for a code's locals (operand stack values are not considered because standard compilers generally don't create "dead operands" and the intended usage are performance and precision improvements)
- refined the implementations of Graphs
- added efficient implementatin of Tarjan's algorithm for finding strongly connected components (the implementation can easily handle graphs with millions of nodes)
- added support for converting dot files to SVG using vis-js.com
- completely reworked `org.opalj.collection.immmutable.UIDSet`; instead of a simple binary search tree - which had significant scalability issues - we are now using a trie.
- initial release of Hermes
- removed the support for `SetProperties` from the `PropertyStore` due to conceptual mismatch
- we are now using sbt 0.13.15
- fixed ClassHierarchy.rootInterfaceTypes
- fixed Tasks.submit if all previously submitted tasks were already finished
- fixed ClassFile.isEffectiveFinal for interface types
- fixed the ids generated for CFGs

## 0.8.10

- added support for the JSON Serialization of Milliseconds, Nanoseconds and Seconds

## 0.8.9 (Initial release of OPAL on Maven Central)

- added a list-like data structure (`Chain`) which is specialized for int values to save memory
 (~ 25%) and to avoid unnecessary boxing operations
- added preliminary Java 9 support
- added the fix-point computations framework to facilitate the implementation of concurrent, fix-point based analyses

## Pre 0.8.9

Older releases can be obtained by checking out the repository and going back in the history.
