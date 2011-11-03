package de.tud.cs.st.bat.dependency
import de.tud.cs.st.bat.resolved.ClassFile

trait ClassFileProcessor {
  def process(clazz: ClassFile)
}