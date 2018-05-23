# Overview

The folder (`org.opalj.fpcf.fixtures`) and its subfolders contain a Java project where several entities, such as methods, fields, or allocation sites, have annotations regarding their expected properties.

The expected properties are annotated using custom annotations defined in the package `org.opalj.fpcf.properties`. When the test suite is run, the project (the code in the subpackages of `org.opalj.fpcf.fixture`) is instantiated and the configured analyses are run. The project will consist of all classes below the `fixtures` package. The library classes will consist of the JDK and the classes below the `properties` package.
Afterwards, the test will check that the actual (computed) properties and the specified properties match. For that, the `PropertyMatcher` associated with the annotation is called.    
