# Analyzing Java Projects
OPAL has decent support for representing and analyzing entire Java projects (e.g., command-line applications, domain-specific applications, or libraries).

Reading in a project is generally straight forward using `org.oalj.br.analyses.Project`'s factory methods.

    import org.opalj.br.analyses.Project

    val projectJAR = "OPAL/bi/target/scala-2.11/resource_managed/test/jvm_features-1.8-g-parameters-genericsignature.jar"
    implicit val p = Project(
        new java.io.File(projectJAR), // path to the JAR files/directories containing the project
        org.opalj.bytecode.RTJar // predefined path(s) to the used libraries
    )

After the project is loaded it is now possible to query the project. E.g.,  to get the project's class hierarchy, to get information about overridden methods, or to get the set of '''functional interfaces'''. In the latter case it is sufficient to call the respective method:

    val fi = p.functionalInterfaces
    fi.map(_.toJava).toList.sorted.foreach(println)

Additional functionality exists, e.g., for querying the potential call targets for method calls, to get the set of all methods that can be called on a given instance of a class (this includes the set of inherited, visible methods).
