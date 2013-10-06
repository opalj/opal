package ai.taint;


import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import sun.reflect.misc.ReflectUtil;

public class DoPrivileged {

	public Class<?> callable1(final String className) {
		return AccessController.doPrivileged(new PrivilegedAction<Class<?>>() {
			@Override
			public Class<?> run() {
				try {
					return foo(className);
				} catch (ClassNotFoundException e) {
					throw new RuntimeException(e);
				}
			}
		});
	}

	public Class<?> callable2(final String className) throws PrivilegedActionException {
		return AccessController.doPrivileged(new PrivilegedExceptionAction<Class<?>>() {
			@Override
			public Class<?> run() throws Exception {
				return foo(className);
			}
		});
	}

	private Class<?> foo(String className) throws ClassNotFoundException {
		ReflectUtil.checkPackageAccess(className);
		return Class.forName(className);
	}
}
