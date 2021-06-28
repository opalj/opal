## Using The Latest Release

The latest release is always found on [Maven Central](https://search.maven.org/#search%7Cga%7C1%7Cde.opal-project) and can therefore be added to your project as standard library dependency.

## Using The Latest Development Snapshot

If you want to use the snapshot version do not forget to add the respective resolver:

    resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

## Using OPAL to Develop Analyses

If you want to use OPAL for the development of static analyses, you can either use the latest release found on Maven Central or just checkout and build the current development snapshot of OPAL found on GitHub. In general, using the development snapshot; i.e., the `develop` branch of OPAL is very safe and gives you access to the latest features, improvements and bug fixes.

Go to [GitHub](https://github.com/opalj/OPAL) to checkout OPAL and to read how to compile and deploy it.

## Exemplary Uses

OPAL comes with a large number of code [snippets](https://bitbucket.org/snippets/delors/) and [small (i.e., one-class) analyses](https://github.com/opalj/OPAL/tree/develop/DEVELOPING_OPAL/demos/src/main/scala/org/opalj) to demonstrate various features.

## Sub Projects

OPAL consists of multiple sub projects and tools which are described in the following.

### Framework
Combines all of OPAL's subproject for ease of use.

    libraryDependencies += "de.opal-project" % "framework_2.12" % "4.0.0"

### Common
Contains general data structures and algorithms particular useful in the context of static analysis. E.g., graph algorithms, such as
an implementation of Tarjan's algorithm for finding strongly connected components. The implementations are designed with scalability in mind and should be able to process millions of nodes.

    libraryDependencies += "de.opal-project" % "common_2.12" % "4.0.0"

### Static Analysis Framework
The static analysis framework is a generally useful framework for developing static analyses. The framework has wide ranging support for very different types of static analyses and automatically parallels their execution. The framework only depends on `Common` and can be flexibly combined with other static analyses frameworks (e.g., BCEL, SOOT, Wala, ASM,... ) 

    libraryDependencies += "de.opal-project" % "static-analysis-infrastructure_2.12" % "4.0.0"

### Bytecode Representation
The bytecode toolkit implements a generic infrastructure for parsing Java class files. Additionally,
it provides a default representation for Java bytecode that can be used to analyze class files. That
representation provides extensive support for pattern matching on Java bytecode to facilitate writing
basic analyses.

    libraryDependencies += "de.opal-project" % "bytecode-representation_2.12" % "4.0.0"

### Abstract Interpretation Framework
The abstract interpretation framework is a highly-customizable framework for the lightweight abstract interpretation of the Java bytecode. The framework was designed with ease of use and customizability in mind.

    libraryDependencies += "de.opal-project" % "abstract-interpretation-framework_2.12" % "4.0.0"

### Three-Address Code
The three-address-code toolkit provides a more human readable representation of the bytecode that includes additional information derived by the abstract interpretation framework.

    libraryDependencies += "de.opal-project" % "three-address-code_2.12" % "4.0.0"

### Architecture Validation Framework
The architecture validation framework facilitates the development of tools for specifying and validating software architectures.

    libraryDependencies += "de.opal-project" % "architecture-validation_2.12" % "4.0.0"

[comment]: # "Exploring the Abstract Interpretation Framework"

[comment]: # "To get a good, first idea what the abstract interpretation framework can do, you can use the *BugPicker*. It enables you to perform some local abstract interpretations. To get good results it is usually necessary to load the JDK and all related libraries."
