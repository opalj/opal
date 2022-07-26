# Querying the Class Hierarchy

One of the most common tasks when implementing static analyses is to get information about the inheritance relation between different classes, to traverse all super/sub classes/interfaces or to compute the least upper bound given some classes. Support for answering such questions or for processing a project's class hierarchy is directly provided by OPAL's `org.opalj.br.ClassHierarchy`. The easiest way to get the class hierarchy is to first instantiate a project and then ask the project for the class hierarchy.

    import org.opalj.br._ ; import org.opalj.br.analyses._ ; import java.io.File
    val projectJAR = "./OPAL/bi/target/scala-2.13/resource_managed/test/method_types.jar"
    implicit val p = Project(new File(projectJAR),org.opalj.bytecode.RTJar)
    val classHierarchy = p.classHierarchy

The class hierarchy, e.g., directly makes the information about a type's super/sub types available:

    def subtypeInformation(ObjectType) : SubtypeInformation 
    def supertypeInformation(ObjectType) : SupertypeInformation 

To get, e.g., all subtypes of `java.io.Serializable`, it is sufficient to query the subtypes information:

    val subtypesOfSerializable = classHierarchy.subtypeInformation(ObjectType("java/io/Serializable"))

>Recall that in Java bytecode package names are separated using "/" and not "." as used in Java/Scala/... source code.

>OPAL's class hierarchy supports partial class hierarchies and - in case that a class hierarchy is incomplete – generally follows a best-effort approach to determine sub-/supertype relations. In particular, when `isSubtypeOf` is used, `java.lang.Object` will always be treated as the supertype and an **interface** will never be the subtype of a class type, even if the supertype hierarchy for the interface is incomplete. As a matter of fact, the standard JDK/rt.jar contains references to classes that are not delivered as part of the JDK and therefore the class hierarchy is basically always incomplete.

If you don't know whether a given type is known to the class hierarchy, you should call the `get` method to avoid potential `NoSuchElementExceptions`.

    classHierarchy.subtypeInformation.get(ObjectType("java/io/Serializable")).foreach{subtypeInformation => ... }
