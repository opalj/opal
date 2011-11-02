package de.tud.cs.st.bat.dependency

sealed trait EdgeType {
  def id: Int
  def descr: String
}

// edge types used for classes/interfaces and methods
case object ANNOTATION_TYPE extends EdgeType { val id = 10; val descr = "annotated with" }

// edge types used for classes/interfaces 
case object EXTENDS extends EdgeType { val id = 110; val descr = "extends" }
case object IMPLEMENTS extends EdgeType { val id = 120; val descr = "implements" }

// edge types used for methods and fields
case object IS_DEFINED_IN extends EdgeType { val id = 210; val descr = "is defined in" }

// edge types used for methods
case object RETURN_TYPE extends EdgeType { val id = 310; val descr = "return type" }
case object PARAMETER_TYPE extends EdgeType { val id = 320; val descr = "parameter type" }
case object EXCEPTION_TYPE extends EdgeType { val id = 330; val descr = "exception type" }
case object FIELD_READ extends EdgeType { val id = 340; val descr = "field read" }
case object FIELD_WRITE extends EdgeType { val id = 350; val descr = "field write" }
case object USED_TYPE extends EdgeType { val id = 360; val descr = "used type" }
case object METHOD_CALL extends EdgeType { val id = 370; val descr = "method call" }

// edge types used for fields
case object FIELD_TYPE extends EdgeType { val id = 410; val descr = "field type" }

case class CustomType(id: Int, descr: String) extends EdgeType