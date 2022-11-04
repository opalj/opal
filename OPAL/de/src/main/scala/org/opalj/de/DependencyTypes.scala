/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package de

/**
 * Enumeration of all kinds of dependencies that are extracted by the
 * [[DependencyExtractor]].
 *
 * @author Thomas Schlosser
 * @author Michael Eichberg
 * @author Marco Torsello
 */
object DependencyTypes extends Enumeration(0 /* <= value of first enumeration value*/ ) {

    val EXTENDS = Value("type declaration EXTENDS class type")
    val IMPLEMENTS = Value("type declaration IMPLEMENTS interface type")

    val OUTER_CLASS = Value("class type declaration IS OUTER CLASS of method or type")
    val INNER_CLASS = Value("class type declaration IS INNER CLASS of method or type")

    val INSTANCE_MEMBER = Value("field or method IS INSTANCE MEMBER of class type")
    val CLASS_MEMBER = Value("field/method IS CLASS MEMBER of class/interface/annotation/enum")
    val ENCLOSED = Value("class type declaration IS ENCLOSED by method or type")

    // field definition related dependency types
    val FIELD_TYPE = Value("the FIELD has TYPE")
    val CONSTANT_VALUE = Value("the field is initialized with a CONSTANT VALUE with TYPE")

    // method definition related dependency types
    val PARAMETER_TYPE = Value("the method defines a PARAMETER with TYPE")
    val RETURN_TYPE = Value("the method's RETURN TYPE")
    val THROWN_EXCEPTION = Value("the method may THROW an exception of type")

    // code related dependency types
    val CATCHES = Value("catches")

    val LOCAL_VARIABLE_TYPE = Value("the method has a LOCAL VARIABLE with TYPE")

    val TYPECAST = Value("performs a reference based TYPE CAST")
    val TYPECHECK = Value("performs a TYPE CHECK using \"instanceOf\"")

    val CREATES_ARRAY = Value("CREATES a new ARRAY of type")
    val CREATES = Value("CREATES a new instance of class")

    val READS_FIELD = Value("the method READS the value stored in the FIELD")
    val WRITES_FIELD = Value("the method WRITES the value stored in the FIELD")
    val DECLARING_CLASS_OF_ACCESSED_FIELD = Value("the method ACCESSES a FIELD that is DECLARED by CLASS")
    val TYPE_OF_ACCESSED_FIELD = Value("the method ACCESSES a FIELD with TYPE")
    val LOADS_CONSTANT = Value("the method DEFINES the constant")

    val CALLS_METHOD = Value("the method CALLS the METHOD")
    val DECLARING_CLASS_OF_CALLED_METHOD = Value("the CALLED METHOD is DECLARED by TYPE")
    val PARAMETER_TYPE_OF_CALLED_METHOD = Value("the method CALLS a METHOD that has a PARAMETER with TYPE")
    val RETURN_TYPE_OF_CALLED_METHOD = Value("the method CALLS a METHOD that RETURNS a value with TYPE")

    // annotation related dependency types
    //val ANNOTATED_WITH = Value("the class, field, method, type parameter, local variable, ... is ANNOTATED WITH")
    val ANNOTATED_WITH = Value("the element is ANNOTATED WITH")
    val PARAMETER_ANNOTATED_WITH = Value("the method's parameter is ANNOTATED WITH")

    // element value related dependency type
    val ANNOTATION_DEFAULT_VALUE_TYPE = Value("the TYPE of the ANNOTATIONS DEFAULT VALUE")
    val ANNOTATION_ELEMENT_TYPE = Value("the TYPE of the ANNOTATION's ELEMENT value")
    val USES_ENUM_VALUE = Value("the annotation element's value is the ENUM VALUE")

    // signature/type parameter related dependency types
    val TYPE_IN_TYPE_PARAMETERS = Value("the TYPE is used in the declaration of a TYPE PARAMETER (signature)")

    def bitMask(v: Value): Long = 1L << v.id

    def toSet(set: DependencyTypesBitSet): scala.collection.Set[DependencyType] = {
        val max = maxId
        var i = 0
        val dependencies = new scala.collection.mutable.HashSet[DependencyType]
        while (i <= max) {
            if (((set >> i) & 1) == 1)
                dependencies += DependencyTypes(i)
            i += 1
        }
        dependencies
    }

    def toUsageDescription(dependencyType: DependencyType): String = {
        dependencyType match {
            case EXTENDS                           => "extend class type"
            case IMPLEMENTS                        => "implement interface type"
            case OUTER_CLASS                       => "be outer class"
            case INNER_CLASS                       => "be inner class"
            case INSTANCE_MEMBER                   => "be instance member"
            case CLASS_MEMBER                      => "be class member"
            case ENCLOSED                          => "be enclosed"
            case FIELD_TYPE                        => "be of type"
            case CONSTANT_VALUE                    => "be initialized with constant value"
            case PARAMETER_TYPE                    => "have parameter"
            case RETURN_TYPE                       => "return"
            case THROWN_EXCEPTION                  => "throw exception"
            case CATCHES                           => "catch exception"
            case LOCAL_VARIABLE_TYPE               => "have local variable"
            case TYPECAST                          => "perform type cast"
            case TYPECHECK                         => "perform type check"
            case CREATES_ARRAY                     => "create array"
            case CREATES                           => "create instance"
            case READS_FIELD                       => "read field"
            case WRITES_FIELD                      => "write field"
            case DECLARING_CLASS_OF_ACCESSED_FIELD => "access field declared by"
            case TYPE_OF_ACCESSED_FIELD            => "access field"
            case LOADS_CONSTANT                    => "load constant"
            case CALLS_METHOD                      => "call method"
            case DECLARING_CLASS_OF_CALLED_METHOD  => "call method declared by"
            case PARAMETER_TYPE_OF_CALLED_METHOD   => "call method with parameter"
            case RETURN_TYPE_OF_CALLED_METHOD      => "call method with return type"
            case ANNOTATED_WITH                    => "be annotated with"
            case PARAMETER_ANNOTATED_WITH          => "have parameter annotated with"
            case ANNOTATION_DEFAULT_VALUE_TYPE     => "have annotation default value type"
            case ANNOTATION_ELEMENT_TYPE           => "have annotation element value type"
            case USES_ENUM_VALUE                   => "have enum value as annotation element value"
            case TYPE_IN_TYPE_PARAMETERS           => "be used in the declaration of a signature"

            case _ =>
                throw new UnknownError(s"unknown dependency type: $dependencyType")
        }
    }
}
