/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package ai.taint;

public class NotReturnedValue {

	public void instantiate(String name) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		Class<?> clazz = Class.forName(name);
		Object newInstance = clazz.newInstance();
		newInstance.toString();
	}
}
