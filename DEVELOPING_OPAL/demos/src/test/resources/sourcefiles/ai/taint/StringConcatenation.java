/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package ai.taint;

public class StringConcatenation {

	public Class<?> nameInput(String name) throws ClassNotFoundException {
		name += "0";
		return Class.forName(name);
	}

}
