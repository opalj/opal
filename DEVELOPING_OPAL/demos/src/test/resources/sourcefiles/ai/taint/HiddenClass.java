/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package ai.taint;

class HiddenClass {
	
	HiddenClass() {
		
	}
	
	public Class<?> uncheckedMethod(String name123) throws ClassNotFoundException {
		return Class.forName(name123);
	}
}