package jdkBugsTest;

import java.util.concurrent.ConcurrentHashMap;

public class JdkBugsFalsePositives {

	// ================================================================================================
	// com/sun/org/apache/xml/internal/utils/UnImplNode
	// com/sun/org/apache/xml/internal/dtm/ref/dom2dtm/DOM2DTMdefaultNamespaceDeclarationNode
	// com/sun/org/apache/xerces/internal/dom/NodeImpl
	// com/sun/org/apache/xml/internal/dtm/ref/DTMNodeProxy
	// ================================================================================================

	public Object getFeature(String feature, String version) {
		// we don't have any alternate node, either this node does the job
		// or we don't have anything that does
		return isSupported(feature, version) ? this : null;
	}

	public boolean isSupported(String feature, String version) {
		return false;
	}

	// ================================================================================================
	// java/util/Objects
	// ================================================================================================

	public static <T> T requireNonNull(T obj, String message) {
		if (obj == null)
			throw new NullPointerException(message);
		return obj;
	}

	// ================================================================================================
	// java/lang/ClassLoader
	// ================================================================================================

	protected Object getClassLoadingLock(String className) {
		Object lock = this;
		if (parallelLockMap != null) {
			Object newLock = new Object();
			lock = parallelLockMap.putIfAbsent(className, newLock);
			if (lock == null) {
				lock = newLock;
			}
		}
		return lock;
	}

	private final ConcurrentHashMap<String, Object> parallelLockMap = new ConcurrentHashMap<>();;

}
