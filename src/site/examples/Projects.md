# Loading Java Projects
OPAL has decent support for representing and analyzing entire Java projects (e.g., command-line applications, domain-specific applications, or libraries).

> The following examples, expect that you have checked out OPAL, and that you started `sbt` in OPAL's main folder. After that, you have changed to the project `project OPAL-DeveloperTools` and started the `console`.

Reading in a project is generally straight forward using [`org.oalj.br.analyses.Project`](http://www.opal-project.de/library/api/SNAPSHOT/#org.opalj.br.analyses.Project$)'s factory methods.

    import org.opalj.br.analyses.Project

    val projectJAR = "OPAL/bi/target/scala-2.13/resource_managed/test/method_types.jar"
    implicit val p = Project(
        new java.io.File(projectJAR), // path to the JAR files/directories containing the project
        org.opalj.bytecode.RTJar // predefined path(s) to the used libraries
    )

After loading the project it is possible to query the project. E.g., to get the project's class hierarchy, to get information about overridden methods, or to get the set of all actual **functional interfaces**. In the latter case it is sufficient to call the respective method:

    val fi = p.functionalInterfaces
    fi.map(_.toJava).toList.sorted.foreach(println)

Additional [functionality](http://www.opal-project.de/library/api/SNAPSHOT/#org.opalj.br.analyses.Project) exists, e.g., for querying the potential call targets for method calls, to get the set of all methods that can be called on a given instance of a class (this includes the set of inherited, visible methods).
