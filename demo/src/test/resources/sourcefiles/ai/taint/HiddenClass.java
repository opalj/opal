package ai.taint;


class HiddenClass {
	
	HiddenClass() {
		
	}
	
	public Class<?> uncheckedMethod(String name123) throws ClassNotFoundException {
		return Class.forName(name123);
	}
}