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

* **Abstract Interpretation Framework**(OPAL/ai): Implementation of an abstract interpretation based framework that can be used to easily implement analyses at different levels of precision. Additionally, a three-address representation is provided that uses the results of a basic abstract interpretation.

* **Dependencies Extraction**(OPAL/de): Provides support for extracting and analyzing a project's source code dependencies. This project is the foundation for projects to, e.g., check architectures.

* **Architecture Validation**(OPAL/av): A small framework to check a project's implemented architecture against a specified one.

* **Demos**(OPAL/demos): Contains working code samples that demonstrates how to use OPAL. The code in the Demo project is primarily meant as a teaching resource. To start the examples, start the `sbt` console (Scala Build Tools) and change the current project to "Demos" (`project Demos`). After that you can `run` several small demo analyses.

* **BugPicker**(OPAL/TOOLS/bp): A tool to find data-flow dependent issues in source code. The kind of issues that are identified range from useless defensive checks to bugs that lead to (unhandled) runtime exceptions.

# Building OPAL
The following applies to the "Master" branch.

OPAL uses SBT as its build tool and working with OPAL is particularly easy using the SBT console.
Make sure that you have Java 8, Scala 2.11.11 and SBT 0.13.15 installed and running and that SBT can use at least 3GB of ram (-Xmx3G). Download a recent snapshot of OPAL or clone the repository.
Go to OPAL's root folder.

* Call `sbt cleanBuild`. This compiles all core projects (including tests), generates the project-wide ScalaDoc documentation and publishes the project to your local ivy directory.
* Go to the `TOOLS/bp` folder and call `sbt compile` to compile the BugPicker. You can run the BugPicker using `sbt run`.
* [Optional - but highly recommended] Edit the file `local.sbt` and specify the two system properties (`JAVA_OPTS`): `-Dorg.opalj.threads.CPUBoundTasks=8
-Dorg.opalj.threads.IOBoundTasks=24` - set the values to appropriate values for your machine (CPUBoundTasks === "Number of real CPUs (Cores)", IOBoundTasks === "Number of (hyperthreaded) cores * 1 .5"). You can also set these properties when using sbt by typing: `eval sys.props("org.opalj.threads.CPUBoundTasks") = "1"`.
* Call `sbt test` to run the unit tests and to test that everything works as expected. Please note, that some tests generate some additional (colored) output. However, as long as all tests succeed without an error, everything is OK. *If `sbt test` fails it may be due to insufficient memory. In this case it is necessary to start SBT itself with more memory.*
* Call `sbt it:test` to run the integration test suite. Executing this test suite will take several minutes (your .sbtconfig file needs to be changed accordingly).

You are ready to go.

**Toubleshooting**

When you encounter problems to build OPAL, please consider the following options.

 - Windows users have to adapt the __global__ sbt options such that it does work with UTF-8. To achieve this you have to add the JVM parameter `-Dfile.encoding=UTF8` to the sbt's _sbtopts_ and _sbtconfig.txt_ file that is located in the sbt installation directory.
 - The OPAL developer tools subproject depends on JavaFX for and therefore, if you want to build everything, the JavaFX libraries need to be on the class path. This is always the case when you use the Oracle JDK. If you want to use the OpenJDK you have to configure this manually!
 - For increasing sbt heap size on Windows, the default sbt executable doesn't support the -mem option directly (using sbt 0.13.15). It is accessible though via the sbtopts file in e. g. "C:/Program Files (x86)/sbt/conf". For example, "-mem 3072" normally works for executing "sbt test". For running the integration test suite with "sbt it:test", you want to set the limit to 6 gigabytes. If you prefer not to set the memory limit globally, you can create a file ".jvmopts" in opal's root directory containing e. g. "-Xmx3072M".

# Using OPAL
To get started, go to the webpage of the project [The OPAL Project](http://www.opal-project.de) and go to *Articles and Tutorials*. Additionally, the code in the `Demos` project contain a very large number of short(er) examples that demonstrate how to solve commonly recurring tasks and most examples can directly be executed.

# Example Usage
Start the sbt console. (In OPAL's root folder call `sbt` on the command line.)
Change the project to Demors using the command `project Demos` and type `run` to run one of the demos.

# Further Information
* [The OPAL Gitter chatroom](https://gitter.im/OPAL-Project)
* [Questions regarding how to write analyses (Stackoverflow)](http://stackoverflow.com/questions/tagged/opal-framework?sort=newest)
* [OPAL Project](http://www.opal-project.de)
