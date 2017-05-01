# Querying the Class Hierarchy

One of the most common tasks when implementing static analyses is to get information about the inheritance relation between different classes, to traverse all super/sub classes/interfaces or to compute the least upper bound given some classes. Support for answering such questions or for processing a project's class hierarchy is directly provided by OPAL's `org.opalj.br.ClassHierarchy`. The easyiest way to get the class hierarchy is to first instantiate a project and then ask the project for the class hierarchy.

    import import org.opalj.br._ ; import org.opalj.br.analyses._ ; import java.io.File
    val projectJAR = "./OPAL/bi/target/scala-2.11/resource_managed/test/jvm_features-1.8-g-parameters-genericsignature.jar"
    implicit val p = Project(new File(projectJAR),org.opalj.bytecode.RTJar)
    val classHierarchy = p.classHierarchy

The class hierarchy, e.g., directly makes the information about a type's super/sub types available:

    val subtypes : Map[ObjectType,SubtypeInformation] = classHierarchy.subtypes
    val supertypes : Map[ObjectType,SupertypeInformation] = classHierarchy.supertypes

Now, to get all subtypes of, e.g., `java.io.Serializable`, it is sufficient to query the subtypes information:

    val subtypeInformation = subtypes(ObjectType("java/io/Serializable"))

If you don't know whether a given type is known to the class hierarchy, you should call the `get` method to avoid potential `NoSuchElementExceptions`.

    subtypes.get(ObjectType("java/io/Serializable")).foreach{subtypeInformation => ... }
