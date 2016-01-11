# Overview
OPAL is an extensible library for analyzing Java bytecode. OPAL is completely written in Scala and leverages Scala's 
advanced language features to provide a new and previously unseen level of flexibility and ease of use. 
OPAL was designed from the ground up with *extensibility*, *adaptability* and *scalability* (memory and performance-wise) in mind. 

# Project Structure
OPAL consists of several projects which are found in the folder OPAL:

* **Common**(OPAL/common): Contains common helper classes such as generic data structures and graph algorithms.

* **Bytecode Infrastructure**(OPAL/bi): The necessary infrastructure for parsing Java bytecode.  

* **Bytecode Disassembler**(OPAL/da): A Java Bytecode Disassembler that creates a beautiful HTML representatoin of Java class files. An Eclipse plug-in is found in (OPAL/TOOLS/ep).

* **Bytecode Representation**(OPAL/br): OPAL's base representation of Java bytecode. Implements all functionality to do basic analyses on top of Java class files.  

* **Abstract Interpretation Framework**(OPAL/ai): Implementation of an abstract interpretation based framework that can be used to easily implement analyses at very different levels of precision. 

* **Dependencies Extraction**(OPAL/de): Provides support for extracting and analyzing a project's source code dependencies. This project is the foundation for projects to, e.g., check architectures.

* **Architecture Valiation**(OPAL/av): A small framework to check a project's implemented architecture against a specified one.

* **Demos**(OPAL/demos): Contains working code samples that demonstrate how to use OPAL. The code in the Demo project is primarily meant as a teaching resource. To start the examples, start the `sbt` console (Scala Build Tools) and change the current project to "Demos" (`project Demos`). After that you can `run` several small demo analyses.

* **BugPicker**(OPAL/TOOLS/bp): A tool to find data-flow dependent issues in source code. The kind of issues that are identified range from useless defensive checks to bugs that lead to (unhandled) runtime exceptions.

# Building OPAL #

The following applies to the "Master" branch.

OPAL uses SBT as its build tool and working with OPAL is particularly easy using the SBT console.
Make sure that you have Java 8, Scala 2.11.7 and SBT 0.13.x installed and running. Download a recent snapshot of OPAL or clone the repository.
Go to OPAL's root folder. 

* Call `sbt clean clean-files clean-cache clean-local eclipse copyResources it:compile test:compile unidoc publishLocal copyToEclipsePlugin`. This compiles all core projects (including tests), generates the project-wide ScalaDoc documentation and publishes the project to your local ivy directory.
* Go to the `TOOLS/bp` folder and call `sbt compile`to compile the BugPicker. You can run the BugPicker using `sbt run`.
* [Optional - but highly recommended] Edit the file `local.sbt` and specify the two system properties (`JAVA_OPTS`): `-Dorg.opalj.threads.CPUBoundTasks=8
-Dorg.opalj.threads.IOBoundTasks=24` - set the values to appropriate values for your machine (CPUBoundTasks === "Number of real CPUs (Cores)", IOBoundTasks === "Number of (hyperthreaded) cores * 1 .5")
* Call `sbt test` to run the unit tests and to test that everything works as expected. Please note, that some tests generate some additional (colored) output. However, as long as all tests succeed without an error, everything is OK. *If `sbt test` fails it may be due to insufficient memory. In this case it is necessary to edit your `.sbtconfig` file and to specify that you want to use more memory (`-Xmx3072M`).*
* Call `sbt it:test` to run the integration test suite. Executing this test suite will take several minutes (your .sbtconfig file needs to be changed accordingly).
* If you want to contribute to OPAL and want to develop your analyses using Eclipse, call `sbt eclipse` in the main folder and/or in the `TOOLS/bp` folder to create Eclipse projects. Afterwards, you can directly import the projects into Eclipse.

You are ready to go.

# Using OPAL #
To get started go to the webpage of the project [The OPAL Project](www.opal-project.de) and go to *Articles and Tutorials*. Additionally, the code in the `Demos` project contain a very large number of short(er) examples that demonstrate how to solve commonly recurring tasks and most examples can directly be executed.

# Example Usage #

Start the sbt console. (In OPAL's root folder call `sbt` on the command line.)
Change the project to OPAL-DeveloperTools using the command `project OPAL-DeveloperTools`.
To get the call graph of some class call run and specify (a) a jar file and (b) the name of some class. E.g., use the following command (`run ExtVTA /Library/Java/JavaVirtualMachines/jdk1.8.0_40.jdk/Contents/Home/jre/lib java/util/ArrayList`) and then select `CallGraphVisualization`. Afterwards the call graph related to that class is calculated and opened. (Graphviz needs to be installed first, since the visualization of the call graph is done using it.)

# Contributing to OPAL #
Everybody is welcome to contribute to OPAL and to submit pull requests. However, a pull request is only taken into consideration if:

* ___the pull request consists of only **one commit** and this commit **implements a single feature**___

Additionally, the pull request has to meet the following conditions:

* the copyright information (BSD License) was added to the file
* author information was added where appropriate
* all existing unit and integration tests succeed
* the code is formatted using the same settings and style as the rest of the code (use the "Scalariform settings" as a basis)
* the code is reasonably documented
* the code conventions w.r.t. naming and formatting are followed (Note, that some formatting conventions used by OPAL are not enforced by scalariform. In particular, **a line should not have more than 100 chars** (unless Scalariform always reformats the code such that the line has more than 100 chars which is, e.g., often the case for type declarations))
* sufficient tests are included (use Scalatest for the development and use scoverage for checking the coverage; the tests should check all features and should have a coverage that is close to 100%)

A recommended read (to spead up the process of getting your Pull Request pulled):
 [The Twitter Scala Style Guide](http://twitter.github.io/effectivescala/)

* Our Jenkins can be found [here](http://opal.st.informatik.tu-darmstadt.de:8080)

# Further Information #

* [OPAL Project](http://www.opal-project.de)