package de.tud.cs.st.bat.resolved

object DependencyType extends Enumeration {
  type DependencyType = Value

  // class/method/field related dependency types
  val EXTENDS = Value("extends")
  val IMPLEMENTS = Value("implements")
  val IS_DEFINED_IN = Value("is defined in")

  // annotation related dependency types
  val ANNOTATED_WITH = Value("annotated with")
  val PARAMETER_ANNOTATED_WITH = Value("parameter annotated with")
  val USES_DEFAULT_CLASS_VALUE_TYPE = Value("uses default class value type")
  val USES_DEFAULT_ENUM_VALUE_TYPE = Value("uses default enum value type")
  val USES_DEFAULT_ANNOTATION_VALUE_TYPE = Value("uses default annotation value type")

  // method related dependency types
  val RETURNS = Value("returns")
  val HAS_PARAMETER_OF_TYPE = Value("has parameter of type")
  val THROWS = Value("throws")
  val CATCHES = Value("catches")
  val USES_ENUM_VALUE = Value("uses enum value")
  val HAS_LOCAL_VARIABLE_OF_TYPE = Value("has local variable of type")

  // field related dependency types
  val IS_OF_TYPE = Value("is of type")
  val USES_CONSTANT_VALUE_OF_TYPE = Value("uses constant value of type")

  // instruction related dependency types
  val CREATES_ARRAY_OF_TYPE = Value("creates array of type")
  val CASTS_INTO = Value("casts into")
  val CHECKS_INSTANCEOF = Value("checks instanceOf")
  val CREATES = Value("creates")
  val USES_FIELD_DECLARING_TYPE = Value("uses field declaring type")
  val READS_FIELD = Value("reads field")
  val WRITES_FIELD = Value("writes field")
  val USES_FIELD_READ_TYPE = Value("uses field read type")
  val USES_FIELD_WRITE_TYPE = Value("uses field write type")
  val USES_PARAMETER_TYPE = Value("uses parameter type")
  val USES_RETURN_TYPE = Value("uses return type")
  val USES_METHOD_DECLARING_TYPE = Value("uses method declaring type")
  val CALLS_METHOD = Value("calls method")
  val CALLS_INTERFACE_METHOD = Value("calls interface method")
  val USES_TYPE = Value("uses type") // default uses dependency

  val UNDEFINED = Value("undefined dependency")

}
