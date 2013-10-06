package ai.taint;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

@SuppressWarnings("rawtypes")
public class ClassInstanceCastedBeforeReturned {

	public Object newInstance(String name) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		Class<?> clazz = Class.forName(name);
		List result = (List) clazz.newInstance();
		return result;
	}

	public Object explicitConstructor(String name) throws ClassNotFoundException, InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		Class<?> clazz = Class.forName(name);
		List result = (List) clazz.getConstructor().newInstance();
		return result;
	}

	public Object allConstructors(String name) throws ClassNotFoundException, InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, SecurityException {
		Class<?> clazz = Class.forName(name);
		List result = (List) clazz.getConstructors()[0].newInstance();
		return result;
	}
}
