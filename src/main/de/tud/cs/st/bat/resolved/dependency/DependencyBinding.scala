package de.tud.cs.st.bat.resolved.dependency

trait DependencyBinding {
  type Dependency = de.tud.cs.st.bat.resolved.Dependency
  type Dependencies = Array[Dependency]
}