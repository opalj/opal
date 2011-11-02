package de.tud.cs.st.bat.dependency

trait DepGraphBuilder {
  def getID(identifier: String): Int
  def addEdge(src: Int, trgt: Int, eType: EdgeType)

  def addEdge(src: Integer, trgt: Integer, eType: EdgeType) {
    if (src != null && trgt != null) {
      addEdge(src.intValue(), trgt.intValue(), eType)
    }
  }
}