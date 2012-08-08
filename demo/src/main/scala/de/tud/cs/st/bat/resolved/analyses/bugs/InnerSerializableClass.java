package de.tud.cs.st.bat.resolved.analyses.bugs;

/**
 * Democode for the issue: "A non-seriablizable class has a serializable inner class". This situation is
 * problematic, because the serialization of the inner class would require – due to the link to its outer
 * class – always the serialization of the outer class which will, however, fail.
 * 
 * @author Michael Eichberg
 */
public class InnerSerializableClass {

	class InnerClass implements java.io.Serializable {

		static final long serialVersionUID = 1l;

		public String toString() {
			return "InnerSerializableClass.InnerClass";
		}
	}

}
 