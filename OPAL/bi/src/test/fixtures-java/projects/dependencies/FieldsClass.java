/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package dependencies;

/**
 * This class was used to create a class file with some well defined properties. The
 * created class is subsequently used by several tests.
 * 
 * NOTE<br />
 * This class is only meant to be (automatically) compiled by OPAL's build script.
 * 
 * @author Thomas Schlosser
 */
public class FieldsClass {
	
	public final static String CONSTANT = "constant";
	private Integer i;

	@Deprecated
	protected int deprecatedField;

	private Integer readField() {
		return i;
	}

	private void writeField(Integer j) {
		i = j;
	}

	public Integer readWrite(Integer j) {
		Integer result = readField();
		writeField(j);
		return result;
	}
}
