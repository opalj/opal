package de.tud.cs.st.bat.resolved

sealed trait DependencyTypeOLD {
  def id: Int
  def descr: String
}

// edge types used for classes/interfaces and methods
case object ANNOTATION_TYPE_OLD extends DependencyTypeOLD { val id = 10; val descr = "annotated with" }
case object PARAMETER_ANNOTATION_TYPE_OLD extends DependencyTypeOLD { val id = 11; val descr = "parameter annotated with" }

// edge types used for classes/interfaces 
case object EXTENDS_OLD extends DependencyTypeOLD { val id = 110; val descr = "extends" }
case object IMPLEMENTS_OLD extends DependencyTypeOLD { val id = 120; val descr = "implements" }

// edge types used for methods and fields
case object IS_DEFINED_IN_OLD extends DependencyTypeOLD { val id = 210; val descr = "is defined in" }

// edge types used for methods
case object RETURN_TYPE_OLD extends DependencyTypeOLD { val id = 310; val descr = "return type" }
case object PARAMETER_TYPE_OLD extends DependencyTypeOLD { val id = 320; val descr = "parameter type" }
case object THROWS_EXCEPTION_TYPE_OLD extends DependencyTypeOLD { val id = 330; val descr = "throws" }
case object CATCHED_EXCEPTION_TYPE_OLD extends DependencyTypeOLD { val id = 331; val descr = "catches" }
case object FIELD_READ_OLD extends DependencyTypeOLD { val id = 340; val descr = "field read" }
case object FIELD_WRITE_OLD extends DependencyTypeOLD { val id = 350; val descr = "field write" }
case object USED_TYPE_OLD extends DependencyTypeOLD { val id = 360; val descr = "used type" }
case object USED_ENUM_VALUE_OLD extends DependencyTypeOLD { val id = 361; val descr = "used enum value" }
case object LOCAL_VARIABLE_TYPE_OLD extends DependencyTypeOLD { val id = 361; val descr = "local variable type" }
case object METHOD_CALL_OLD extends DependencyTypeOLD { val id = 370; val descr = "method call" }

// edge types used for fields
case object FIELD_TYPE_OLD extends DependencyTypeOLD { val id = 410; val descr = "field type" }
case object CONSTANT_VALUE_TYPE_OLD extends DependencyTypeOLD { val id = 411; val descr = "constant value type" }

case class CustomTypeOLD(id: Int, descr: String) extends DependencyTypeOLD