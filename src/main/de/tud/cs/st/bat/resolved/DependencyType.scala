package de.tud.cs.st.bat.resolved

object DependencyType extends Enumeration {
  type DependencyType = Value

  // class/method/field related dependency types
  val EXTENDS = Value("extends")
  val IMPLEMENTS = Value("implements")
  val IS_DEFINED_IN = Value("is defined in")

  // annotation related dependency types
  val ANNOTATION_TYPE = Value("annotated with")
  val PARAMETER_ANNOTATION_TYPE = Value("parameter annotated with")
  val USED_DEFAULT_CLASS_VALUE_TYPE = Value("used default class value type")
  val USED_DEFAULT_ENUM_VALUE_TYPE = Value("used default enum value type")
  val USED_DEFAULT_ANNOTATION_VALUE_TYPE = Value("used default annotation value type")

  // method related dependency types
  val RETURN_TYPE = Value("return type")
  val PARAMETER_TYPE = Value("parameter type")
  val THROWS_EXCEPTION_TYPE = Value("throws")
  val CATCHED_EXCEPTION_TYPE = Value("catches")
  val FIELD_READ = Value("field read")
  val FIELD_WRITE = Value("field write")
  val USED_TYPE = Value("used type")
  val USED_ENUM_VALUE = Value("used enum value")
  val LOCAL_VARIABLE_TYPE = Value("local variable type")
  val METHOD_CALL = Value("method call")

  // field related dependency types
  val FIELD_TYPE = Value("field type")
  val CONSTANT_VALUE_TYPE = Value("constant value type")

  val UNDEFINED = Value("undefined dependency")

}
