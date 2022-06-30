# Hermes - Building and Evaluating Test Corpora

## Overview
Hermes enables you to evaluate a given set of Java (bytecode) projects to comprehend their basic properties and to select those projects that have interesting, distinguishing factors when evaluating and testing your static analysis.

For example, when you want to test that your analysis is able to handle all types of bytecode instructions, then you should take a close look at the results of the respective query (`BytecodeInstructions`) which reports the usage of Java bytecode instructions for a given project. Additionally, Hermes can automatically select projects for you such that at all instructions are guaranteed to be in the final corpus and that the overall code base of the selected projects is minimal w.r.t. the overall number of methods. This minimal corpus can be computed once all queries are evaluated. To start the computation go to `File` &rarr; `Compute Projects for Corpus`.

![Hermes - Overview](images/Hermes.png)

Hermes can be run in [headless mode](http://www.opal-project.de/library/api/SNAPSHOT/org/opalj/hermes/HermesCLI$.html) to just evaluate a set of projects and get back a CSV file with all results or you can start the [UI](http://www.opal-project.de/library/api/SNAPSHOT/org/opalj/hermes/Hermes$.html) which enables more advanced exploration of projects.

## Developing Queries
A query is basically a mapping between some feature and those elements of a project that implement/provide/have the respective feature. For example, a feature could be the kind of type (*interface*, *class*, *enum*, *annotation*) which is defined by a specific class file. The query would then analyze all class files of the project and assign each class file to its respective category. In the context of Hermes, we would consider the query as simultaneously deriving multiple features.

All queries in Hermes have to inherit from `org.opalj.hermes.FeatureQuery` and have to implement the two methods: `featureIDs` and `apply` as seen in the example blow.

    package org.opalj
    package hermes
    package queries

    import org.opalj.br.analyses.Project

    object MyQuery extends FeatureQuery {

        override val featureIDs: List[String] = { ??? }

        override def apply[S](
            projectConfiguration: ProjectConfiguration,
            project:              Project[S],
            rawClassFiles:        Traversable[(da.ClassFile, S)]
        ): TraversableOnce[Feature[S]] = {
            ???
    }   }

Next, we will discuss a complete query which finds *native* methods.

    package org.opalj
    package hermes
    package queries

    import org.opalj.br.analyses.Project

    object NativeMethods extends FeatureQuery {

        // The list returns the unique names of the derived features - the names will be
        // used in Hermes' UI and in the CVS export to name the columns. It is therefore
        // recommended to use short, but descriptive names.
        // Additionally, it is recommend to capitalize the name as used in titles.
        //
        // The names of the features returned here have to equal the names used by
        // the query itself (the `apply` function below)!
        override val featureIDs: List[String] = List("Native Methods")

        override def apply[S](
            projectConfiguration: ProjectConfiguration,
            project:              Project[S],
            rawClassFiles:        Traversable[(da.ClassFile, S)]
        ): TraversableOnce[Feature[S]] = {

            // To store the location information; i.e., to store the native methods,
            // we create a new empty LocationsContainer.
            // Using a LocationsContainer for storing locations is highly recommended,
            // because it automatically takes care of limiting the overall number of
            // locations to a pre-configured value. I.e., we don't have to worry about
            // creating too many locations and filling up the memory.
            val nativeMethods = new LocationsContainer[S]

            for {
                // Let's iterate over all class files belonging to the project.
                (classFile, source) <- project.projectClassFilesWithSources

                // It is highly recommended to regularly check if the query should be aborted;
                // if so, the reported (intermediate/partial) results will always be thrown away.
                if !isInterrupted()

                // Locations are immutable and hierarchically organized and therefore it
                // is generally meaningful to always create instances of location information,
                // which may be shared, as soon as possible.
                classLocation = ClassFileLocation(source, classFile)
                m <- classFile.methods
                if m.isNative // basically "the query"
            } {
                // The current method is native and is added to the set of native methods..
                nativeMethods += MethodLocation(classLocation, m)
            }

            // Finally, we create the feature using the same id as
            // returned by `featureIDs` and the list of native methods.
            Feature[S](featureIDs.head, nativeMethods)
        }
    }

In some cases it might be interesting to also derive general project-wide statistic on the fly. In this case, the results should be stored in the project configuration's `statistics` object. E.g., if you would have computed the average size of the inheritance tree on the fly, you would then store the value in the project's statistics as shown below.

    projectConfiguration.addStatistic("⟨SizeOfInheritanceTree⟩",averageSizeOfInheritanceTree)

Here, the string `"⟨SizeOfInheritanceTree⟩"` uses the mathematical notation "⟨⟩" to denote the average.

After implementing the query, it is highly recommended to document the query using Markdown. The documentation should describe the derived feature and should also give at least one example in which context the query is useful. E.g., the above query would help to select trivial programs for which a pure Java based analysis would be sufficient. Hermes will use the simple name of the class which implements the query, here *NativeMethods*, to determine the name of the markdown file: *Simple_Name_Of_Class.markdown*. The file will be loaded using the class' `getResource` method. Hence, the markdown file has to be stored along with the class. If you want to document your code in a different way; e.g., using HTML, go to the documentation of [org.opalj.hermes.FeatureQuery](http://www.opal-project.de/library/api/SNAPSHOT/#org.opalj.hermes.FeatureQuery)

## Activating Queries

After the development of the query, it is necessary to register it, to make it possible for Hermes to execute it. For that, it is either necessary to add the query to the `application.conf` file, which is part of Hermes, or to create your own config file and add it over there. In both cases the config key has to be:

    org.opalj.hermes.queries.registered = [
        {query = org.opalj.hermes.queries.NativeMethods, activate = true }
    ]

The big advantage of registering all queries in the same place is that it is easily possible to order the queries. The recommended ordering is: **descending execution time**. This will reduce the overall runtime. To get an idea of the execution time, just run Hermes and open the window *Feature Execution Times*.

## Starting Hermes

Hermes can be be started by specifying the json file which contains the configured projects that should be analysed. If you have checked out OPAL/Hermes from BitBucket you can use the `sbt` console to start Hermes (the main class is: `org.opalj.hermes.Hermes`). For example, to get an overview of the properties of some of the test projects which are part of OPAL, you can use the preconfigured project configuration:

    project OPAL-DeveloperTools
    ~runMain org.opalj.hermes.Hermes src/main/resources/hermes-test-fixtures.json

If you want to analyze your own library, just use the above file as a template for your project configuration.

## Contributing to Hermes

### New Queries

If you want to contribute a new query to Hermes, create a pull request that consists of (1) the query - which has to have the package name `org.opalj.hermes.queries` - (2) the user-level documentation, and (3) the updated `application.conf`. The files should be stored in/are found in:

 - The documentation (Markdown file):  **Developing_OPAL/tools/src/main/resources/org/opalj/hermes/queries/&lt;QueryName&gt;.markdown**
 - The query (Scala file): **Developing_OPAL/tools/src/main/scala/org/opalj/hermes/queries/&lt;QueryName&gt;.scala**
 - Application.conf: **DEVELOPING_OPAL/tools/src/main/resources/application.conf**
