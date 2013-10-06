package ai.taint;

import static sun.reflect.misc.ReflectUtil.checkPackageAccess;

public class PermissionCheckNotOnAllPaths {

	public Class<?> foo(String className) throws ClassNotFoundException {
		if (className.length() > 1) {
			String name = className;
			bar(name);
		}
		return Class.forName(className);
	}

	private void bar(String name) {
		checkPackageAccess(name);
	}
}
