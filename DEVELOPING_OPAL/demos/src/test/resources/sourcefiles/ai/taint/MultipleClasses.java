/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package ai.taint;

public class MultipleClasses {
	
	public Class<?> leakingMethod(String name) throws ClassNotFoundException {
		return new HiddenClass().uncheckedMethod(name);
	}

}