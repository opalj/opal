# Overview
Hermes is a small, extensible project that helps to assess the quality of a given corpus of applications. It extracts a wide range of properties from the given projects to make it possible to asses which language features and API features are used by the respective project(s). This information can then be used to design small, efficient test fixtures/corpuses.


## Categories of Queries
Currently, we implement analyses across the following categories:

 - *Completeness - static* - Features of Java BytecodeM; e.g., "Number of classes with(out) debug information.", "Occurrences of specific types of constants.", "Number of invoke dynamic statements?", ...
 - *Completeness - dynamic* - do we have an effective virtual method call, is field resolution is required, ...
 - *Corner Cases* - (e.g., a method without a return statement, a switch with just a default case, ...)
 - *Landmark APIs* - Java Reflection; finalize, Serialization
 - *Weird Code* - e.g., methods which will never return, switches with only default targets,...

## Extending Hermes
See Hermes' built-in documentation or go to [Hermes.md](https://github.com/stg-tud/opal/blob/master/src/site/Hermes.md)
