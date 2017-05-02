# OPAL

Opal is an extensible, bytecode processing and analysis library written in Scala 2.11.11. The latest release is *0.8.12*, the latest snapshot version is *0.9.0-SNAPSHOT*. Both versions are found on Maven central. If you want to use the snapshot version do not forget to add the respective resolver:

    resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

OPAL contains of multiple sub projects which are described in the following.

## Common
Contains general datastructure and algorithms particular useful in the context of static analyis. E.g., graph algorithms, such as
an implementation of Tarjan's algorithm for finding strongly connected components. The implementations are designed with scalability in mind and should be able to process millions of nodes.

    libraryDependencies += "de.opal-project" % "common_2.11" % "0.8.12"


## Bytecode Representation
The bytecode toolkit implements a generic infrastructure for parsing Java class files. Additionally,
it provides a default representation for Java bytecode that can be used to analyze class files. That
representation provides extensive support for pattern matching on Java bytecode to facilitate writing
basic analyses.

    libraryDependencies += "de.opal-project" % "bytecode-representation_2.11" % "0.8.12"


## Architecture Validation Framework
The architecture validation framework facilitates the development of tools for specifying and validating software architectures.

    libraryDependencies += "de.opal-project" % "architecture-validation_2.11" % "0.8.12"

## Abstract Interpretation Framework
The abstract interpretation framework is a highly-customizable framework for the lightweight abstract interpretation of the Java bytecode. The framework was designed with ease of use and customizability in mind.

    libraryDependencies += "de.opal-project" % "abstract-interpretation-framework_2.11" % "0.8.12"

Exploring the Abstract Interpretation Framework


To get a good, first idea what the abstract interpretation framework can do you can use the *BugPicker*. It enables you to perform some local abstract interpretations. To get good results it is usually necessary to load the JDK and all related libraries.
