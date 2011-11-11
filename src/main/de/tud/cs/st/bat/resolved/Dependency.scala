package de.tud.cs.st.bat.resolved

import DependencyType._

sealed trait Dependency {
  //  def targetType: Option[Type] = None
  //  def fieldName: Option[String] = None
  //  def methodName: Option[String] = None
  //  def methodDescriptor: Option[MethodDescriptor] = None
  def dependencyType: DependencyType
}

class ToTypeDependency private (
  val targetType: Type,
  val dependencyType: DependencyType) extends Dependency {
}

object ToTypeDependency {
  def apply(targetType: Type, dependencyType: DependencyType) = {
    new ToTypeDependency(targetType, dependencyType)
  }
}

class ToFieldDependency private (
  val targetType: Type,
  val fieldName: String,
  val dependencyType: DependencyType) extends Dependency {
}

object ToFieldDependency {
  def apply(targetType: Type, fieldName: String, dependencyType: DependencyType) {
    new ToFieldDependency(targetType, fieldName, dependencyType)
  }
}

class ToMethodDependency private (
  val targetType: Option[Type],
  val methodName: String,
  val methodDescriptor: MethodDescriptor,
  val dependencyType: DependencyType) extends Dependency {
}

object ToMethodDependency {
  def apply(targetType: Type, methodName: String, methodDescriptor: MethodDescriptor, dependencyType: DependencyType) {
    new ToMethodDependency(Some(targetType), methodName, methodDescriptor, dependencyType)
  }

  def apply(methodName: String, methodDescriptor: MethodDescriptor, dependencyType: DependencyType) {
    new ToMethodDependency(None, methodName, methodDescriptor, dependencyType)
  }
}