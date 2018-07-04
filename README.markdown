# Overview
OPAL is an extensible library for analyzing and engineering Java bytecode. OPAL is completely written in Scala and leverages Scala's
advanced language features to provide a new and previously unseen level of flexibility and ease of use.
OPAL was designed from the ground up with *extensibility*, *adaptability* and *scalability* (memory and performance-wise) in mind. Many parts of OPAL are either already parallelized, provide the necessary infrastructure to implement highly concurrent analyses or are at least thread-safe.

## Main Projects
OPAL consists of several projects which are found in the folder OPAL:

* **Common**(OPAL/common): Contains general useful functions, data-structures (e.g. TrieMaps) and graph algorithms (e.g., computing strongly connected components, computing dominator information etc.) useful when analyzing (byte) code.

* **Static Analysis Infrastructure**(OPAL/si): Contains a generic lattices based framework for the implementation of modularized static analyses.

* **Bytecode Infrastructure**(OPAL/bi): The necessary infrastructure for parsing Java 1.0 - Java 9 bytecode.

* **Bytecode Disassembler**(OPAL/da): A Java Bytecode Disassembler that provides a one-to-one representation of the class file and which can be used to create beautiful HTML representations of Java class files.

* **Bytecode Creator**(OPAL/bc): Most basic infrastructure to engineer Java bytecode.

* **Bytecode Representation**(OPAL/br): OPAL's base representation of Java bytecode. Implements all functionality to do basic analyses on top of Java class files.

* **Abstract Interpretation Framework**(OPAL/ai): Implementation of an abstract interpretation based framework that can be used to easily implement analyses at different levels of precision. Additionally, a three-address based representation (called TAC) is provided that uses the results of a basic abstract interpretation.

* **Dependencies Extraction**(OPAL/de): Provides support for extracting and analyzing a project's source code dependencies. This project is the foundation for projects to, e.g., check architectures.

* **Architecture Validation**(OPAL/av): A small framework to check a project's implemented architecture against a specified one.

* **Demos**(OPAL/demos): Contains working code samples that demonstrate how to use OPAL. The code in the Demo project is primarily meant as a teaching resource. To start the examples, start the `sbt` console (Scala Build Tools) and change the current project to "Demos" (`project Demos`). After that you can `run` several small demo analyses.

* **BugPicker**(OPAL/TOOLS/bp): A tool to find control-/data-flow dependent issues in source code. The kind of issues that are identified range from useless defensive checks to bugs that lead to (unhandled) runtime exceptions.

## Developer Tools

OPAL also comes with a growing number of tools that are intended to help developers to become familiar with Java Bytecode and/or OPAL. These projects are found in the folder `DEVELOPING_OPAL/tools` and can be run using the SBT console.

## Building OPAL
The following applies to the "Master/Develop" branch.

OPAL uses SBT as its build tool and working with OPAL is particularly easy using the SBT console.
Make sure that you have Java 8 at least update 171, Scala 2.12.6 and SBT 1.1.6 installed and running and that SBT can use at least 3GB of RAM (-Xmx3G). Download a recent snapshot of OPAL or clone the repository.
Go to OPAL's root folder.

* Call `sbt cleanBuild`. This compiles all core projects (including tests), generates the project-wide ScalaDoc documentation and publishes the project to your local ivy directory.
* Go to the `TOOLS/bp` folder and call `sbt compile` to compile the BugPicker UI. You can run the BugPicker using `sbt run`.
* [Optional - but highly recommended] Edit the file `local.sbt` and specify the two system properties (`JAVA_OPTS`): `-Dorg.opalj.threads.CPUBoundTasks=8
-Dorg.opalj.threads.IOBoundTasks=24` - set the values to appropriate values for your machine (CPUBoundTasks === "Number of real CPUs (Cores)", IOBoundTasks === "Number of (hyperthreaded) cores * 1 .5"). You can also set these properties when using sbt by typing: `eval sys.props("org.opalj.threads.CPUBoundTasks") = "1"`.
* Call `sbt test` to run the unit tests and to test that everything works as expected. Please note, that some tests generate some additional (colored) output. However, as long as all tests succeed without an error, everything is OK. *If `sbt test` fails it may be due to insufficient memory. In this case it is necessary to start SBT itself with more memory.*
* Call `sbt it:test` to run the integration test suite. Executing this test suite may take longer (on a fast desktop with 32GB and 8 Cores it taks ~30min).

You are ready to go.

**Toubleshooting**

When you encounter problems to build OPAL, please consider the following options.

 - Windows users have to adapt the __global__ sbt options such that it does work with UTF-8. To achieve this you have to add the JVM parameter `-Dfile.encoding=UTF8` to the sbt's _sbtopts_ and _sbtconfig.txt_ file that is located in the sbt installation directory.
 - The OPAL developer tools subproject depends on JavaFX and therefore, if you want to build everything, the JavaFX libraries need to be on the class path. This is always the case when you use the Oracle JDK. If you want to use the OpenJDK you have to configure this manually!
 - To increase the heap size on Windows (using sbt 0.13.15) it is necessary to set the `-mem` option in the the sbtopts file in, e.g., `C:/Program Files (x86)/sbt/conf`. For example, `-mem 3072` is generally sufficient for executing `sbt test`. For running the integration test suite (`sbt it:test`) 6GB are recommended. If you prefer not to set the memory limit globally, you can create a file `.jvmopts` in OPAL's root directory containing a respective JVM parameter, e.g., "-Xmx3072M". (Note, that the default sbt executable doesn't support the `-mem` option directly.)

## Using OPAL
To get started, go to the webpage of the project [The OPAL Project](http://www.opal-project.de) and go to *Articles and Tutorials*. Additionally, the code in the `Demos` project contain a very large number of short(er) examples that demonstrate how to solve commonly recurring tasks and most examples can directly be executed.

## Example Usage
Start the sbt console. (In OPAL's root folder call `sbt` on the command line.)
Change the project to Demos using the command `project Demos` and type `run` to run one of the demos.

# Further Information
* [The OPAL Gitter chatroom](https://gitter.im/OPAL-Project)
* [Questions regarding how to write analyses (Stackoverflow)](http://stackoverflow.com/questions/tagged/opal-framework?sort=newest)
* [OPAL Project](http://www.opal-project.de)
