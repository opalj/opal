# Overview
OPAL is an extensible library for analyzing Java bytecode. OPAL is completely written in Scala and leverages Scala's 
advanced language features to provide a new and previously unseen level of flexibility and ease of use. 
OPAL was designed from the ground up with *extensibility*, *adaptability* and *scalability* (memory and performance-wise) in mind. 

# Project Structure
OPAL consists of several projects which are found in the folder OPAL:

* **Common**(OPAL/common): Contains common helper classes useful when analyzing (byte) code such as generic data structures and graph algorithms (e.g., to compute the DominatorTree).

* **Bytecode Infrastructure**(OPAL/bi): The necessary infrastructure for parsing Java 1.0 - Java 8 bytecode.  

* **Bytecode Disassembler**(OPAL/da): A Java Bytecode Disassembler that provides a one-to-one representation of the class file and which can be used to create a beautiful HTML representation of Java class files. An Eclipse plug-in that uses the Bytecode Disassembler is found in (OPAL/TOOLS/ep).

* **Bytecode Creator**(OPAL/bc): Infrastructure that facilitates the engineering of bytecode.

* **Bytecode Representation**(OPAL/br): OPAL's base representation of Java bytecode. Implements all functionality to do basic analyses on top of Java class files.  

* **Abstract Interpretation Framework**(OPAL/ai): Implementation of an abstract interpretation based framework that can be used to easily implement analyses at very different levels of precision. Additionally a three-address representation is provided that uses the results of a basic abstract interpretation.

* **Dependencies Extraction**(OPAL/de): Provides support for extracting and analyzing a project's source code dependencies. This project is the foundation for projects to, e.g., check architectures.

* **Architecture Validation**(OPAL/av): A small framework to check a project's implemented architecture against a specified one.

* **Demos**(OPAL/demos): Contains working code samples that demonstrate how to use OPAL. The code in the Demo project is primarily meant as a teaching resource. To start the examples, start the `sbt` console (Scala Build Tools) and change the current project to "Demos" (`project Demos`). After that you can `run` several small demo analyses.

* **BugPicker**(OPAL/TOOLS/bp): A tool to find data-flow dependent issues in source code. The kind of issues that are identified range from useless defensive checks to bugs that lead to (unhandled) runtime exceptions.

# Building OPAL #

The following applies to the "Master" branch.

OPAL uses SBT as its build tool and working with OPAL is particularly easy using the SBT console.
Make sure that you have Java 8, Scala 2.11.8 and SBT 0.13.x installed and running. Download a recent snapshot of OPAL or clone the repository.
Go to OPAL's root folder. 

* Call `sbt clean cleanFiles cleanCache cleanLocal eclipse copyResources it:compile test:compile unidoc publishLocal`. This compiles all core projects (including tests), generates the project-wide ScalaDoc documentation and publishes the project to your local ivy directory.
* Go to the `TOOLS/bp` folder and call `sbt compile` to compile the BugPicker. You can run the BugPicker using `sbt run`.
* [Optional - but highly recommended] Edit the file `local.sbt` and specify the two system properties (`JAVA_OPTS`): `-Dorg.opalj.threads.CPUBoundTasks=8
-Dorg.opalj.threads.IOBoundTasks=24` - set the values to appropriate values for your machine (CPUBoundTasks === "Number of real CPUs (Cores)", IOBoundTasks === "Number of (hyperthreaded) cores * 1 .5"). You can also set these properties when using sbt by typing: `eval sys.props("org.opalj.threads.CPUBoundTasks") = "1"`.
* Call `sbt test` to run the unit tests and to test that everything works as expected. Please note, that some tests generate some additional (colored) output. However, as long as all tests succeed without an error, everything is OK. *If `sbt test` fails it may be due to insufficient memory. In this case it is necessary to edit your `.sbtconfig` file and to specify that you want to use more memory (`-Xmx3072M`).*
* Call `sbt it:test` to run the integration test suite. Executing this test suite will take several minutes (your .sbtconfig file needs to be changed accordingly).
* If you want to contribute to OPAL and want to develop your analyses using Eclipse, call `sbt eclipse` in the main folder and/or in the `TOOLS/bp` folder to create Eclipse projects. Afterwards, you can directly import the projects into Eclipse.

You are ready to go.

# Using OPAL #
To get started, go to the webpage of the project [The OPAL Project](htt://www.opal-project.de) and go to *Articles and Tutorials*. Additionally, the code in the `Demos` project contain a very large number of short(er) examples that demonstrate how to solve commonly recurring tasks and most examples can directly be executed.

# Example Usage #

Start the sbt console. (In OPAL's root folder call `sbt` on the command line.)
Change the project to Demors using the command `project Demos` and type `run` to run one of the demos.

# Contributing to OPAL #
Everybody is welcome to contribute to OPAL and to submit pull requests. However, a pull request is only taken into consideration if:

* ___the pull request consists of only **one commit** and this commit **implements a single feature**___

Additionally, the pull request has to meet the following conditions:

* the copyright information (BSD License) was added to the file
* author information was added where appropriate
* all existing unit and integration tests were successfully executed
* the code is formatted using the same settings and style as the rest of the code (use the "Scalariform settings" as a basis)
* the code is reasonably documented
* the code conventions w.r.t. naming and formatting are followed (Note, that some formatting conventions used by OPAL are not enforced by scalariform. In particular, **a line should not have more than 100 chars** (unless Scalariform always reformats the code such that the line has more than 100 chars which is, e.g., often the case for type declarations))
* sufficient tests are included (use `Scalatest` for the development and use `scoverage` for checking the coverage; the tests should check all features and should have a coverage that is close to 100%)

A recommended read (to speed up the process of getting your pull request pulled):
 [The Twitter Scala Style Guide](http://twitter.github.io/effectivescala/)

* OPAL is build using Shippable.

# Further Information #
* [The OPAL Gitter chatroom](https://gitter.im/OPAL-Project)
* [Questions regarding how to write analyses (Stackoverflow)](http://stackoverflow.com/questions/tagged/opal-framework?sort=newest)
* [OPAL Project](http://www.opal-project.de)