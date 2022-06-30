![](https://github.com/opalj/OPAL/workflows/Build%20and%20Execute%20Tests/badge.svg)

# Overview
OPAL is an extensible library for analyzing and engineering Java bytecode.
OPAL is completely written in Scala and leverages Scala's advanced language features to provide a new and previously unseen level of flexibility and ease of use.
OPAL was designed from the ground up with *extensibility*, *adaptability* and *scalability* (memory and performance-wise) in mind.
Many parts of OPAL are either already parallelized, provide the necessary infrastructure to implement highly concurrent analyses or are at least thread-safe.

## Main Projects
OPAL consists of several projects:

* **Common** (OPAL/common): Contains general useful functions, data-structures (e.g. TrieMaps) and graph algorithms (e.g., computing strongly connected components, computing dominator information, etc.) useful when analyzing (byte) code.

* **Static Analysis Infrastructure** (OPAL/si): Contains a generic lattice-based framework for the implementation of modularized static analyses.

* **Bytecode Infrastructure** (OPAL/bi): The necessary infrastructure for parsing Java 1.0 - Java 16 bytecode.

* **Bytecode Disassembler** (OPAL/da): A Java Bytecode Disassembler that provides a one-to-one representation of the class file and which can be used to create readable HTML representations of Java class files.

* **Bytecode Creator** (OPAL/bc): Most basic infrastructure to engineer Java bytecode.

* **Bytecode Representation** (OPAL/br): OPAL's base representation of Java bytecode. Implements all functionality to do basic analyses of Java class files.

* **Abstract Interpretation Framework** (OPAL/ai): Implementation of an abstract interpretation based framework that can be used to easily implement analyses at different levels of precision.

* **Three Address Code** (OPAL/tac): Provides two 3-address code based intermediate representation. A naive one which is directly created based on the bytecode, and a higher-level SSA-like representation which directly provides a CFG as well as Def-Use information using the results of a basic abstract interpretation. 

* **Dependencies Extraction** (OPAL/de): Provides support for extracting and analyzing a project's source code dependencies. This project is the foundation for projects to, e.g., check architectures.

* **Architecture Validation** (OPAL/av): A small framework to check a project's implemented architecture against a specified one.

* **Framework** (OPAL/framework): Basically just aggregates all sub-projects to make it possible to easily get a consistent snapshot of all sub-projects. In general, it is recommended to declare a dependency on this project when you want to use OPAL.

* **Demos** (OPAL/demos): Contains working code samples that demonstrate how to use OPAL. The code in the Demo project is primarily meant as a teaching resource. To start the examples, start the `sbt` console (Scala Build Tools) and change the current project to "Demos" (`project Demos`). After that you can `run` several small demo analyses.

* **Hermes** (OPAL/TOOLS/hermes): A framework to run various code queries against sets of projects. 

* **BugPicker** (OPAL/TOOLS/bp): A tool to find control-/data-flow dependent issues in source code. The kind of issues that are identified range from useless defensive checks to bugs that lead to (unhandled) runtime exceptions.

## Developer Tools

OPAL also comes with a growing number of tools that are intended to help developers to become familiar with Java Bytecode and/or OPAL. These projects are found in the folder `DEVELOPING_OPAL/tools` and can be run using the SBT console.

## Building OPAL
The following applies to the "Master/Develop" branch.

OPAL uses SBT as its build tool and working with OPAL is particularly easy using the SBT console.
Make sure that you have Java 8 at least update 171, Scala 2.12.15 and SBT 1.6.2 installed and running and that SBT can use at least 4GB of RAM (-Xmx4G). Download a recent snapshot of OPAL or clone the repository.
Go to OPAL's root folder.

* Call `sbt cleanBuild`. This compiles all core projects (including tests), generates the project-wide ScalaDoc documentation and publishes the project to your local ivy directory.
* [Optional - but highly recommended] Edit the file `local.sbt` and specify the two system properties (`JAVA_OPTS`): `-Dorg.opalj.threads.CPUBoundTasks=8
-Dorg.opalj.threads.IOBoundTasks=24` - set the values to appropriate values for your machine (`CPUBoundTasks === "Number of real CPUs (Cores)"`, `IOBoundTasks === "Number of (hyperthreaded) cores * 1 .5"`). You can also set these properties when using sbt by typing:  
`eval sys.props("org.opalj.threads.CPUBoundTasks") = "1"`.
* Call `sbt test` to run the unit tests and to test that everything works as expected. Please note, that some tests generate some additional (colored) output. However, as long as all tests succeed without an error, everything is OK. *If `sbt test` fails, it may be due to insufficient memory. In this case it is necessary to start SBT itself with more memory.*
* Call `sbt it:test` to run the integration test suite. Executing this test suite may take very long (on a fast desktop with 32GB and 8 Cores it takes ~2h).

You are ready to go.

**Troubleshooting**

When you encounter problems in building OPAL, please consider the following options.

 - Ensure that the correct file encoding is used by your editor/sbt/... All files use UTF-8 encoding. (This is in particular relevant when you are using Windows.)
 - Increase the heap size; to build and run all tests you should give sbt at least 12GB.

## Using OPAL
To get started, go to the [project webpage](http://www.opal-project.de). Additionally, the code in the `Demos` project contains many short(er) examples that demonstrate how to solve commonly recurring tasks. Most examples can directly be executed.

## Example Usage
Start the sbt console. (In OPAL's root folder call `sbt` on the command line.)
Change the project to Demos using the command `project Demos` and type `run` to run one of the demos.

# Further Information
* [The OPAL Gitter chatroom](https://gitter.im/OPAL-Project)
* [Questions regarding how to write analyses (Stackoverflow)](http://stackoverflow.com/questions/tagged/opal-framework?sort=newest)
* [OPAL Project](http://www.opal-project.de)
