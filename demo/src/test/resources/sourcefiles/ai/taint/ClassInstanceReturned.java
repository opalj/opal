package ai.taint;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class ClassInstanceReturned {

	public Object newInstance(String name) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		Class<?> clazz = Class.forName(name);
		Object result = clazz.newInstance();
		return result;
	}

	public Object explicitConstructor(String name) throws ClassNotFoundException, InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		Class<?> clazz = Class.forName(name);
		Constructor<?> constructor = clazz.getConstructor();
		Object result = constructor.newInstance();
		return result;
	}

	public Object allConstructors(String name) throws ClassNotFoundException, InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, SecurityException {
		Class<?> clazz = Class.forName(name);
		Object result = clazz.getConstructors()[0].newInstance();
		return result;
	}
}
