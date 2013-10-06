package ai.taint;

import static sun.reflect.misc.ReflectUtil.checkPackageAccess;

public class PermissionCheckNotOnCallstack {

	public Class<?> foo(String className) throws ClassNotFoundException {
		String name = className;
		bar(name);
		return Class.forName(name);
	}

	private void bar(String name) {
		checkPackageAccess(name);
	}
}
