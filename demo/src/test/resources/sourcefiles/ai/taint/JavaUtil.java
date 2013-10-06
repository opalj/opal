package ai.taint;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class JavaUtil {

	public Class<?> hashMap(String name) throws ClassNotFoundException {
		HashMap<String, String> nameMap = new HashMap<>();
		nameMap.put("name", name);
		Class<?> c = Class.forName(nameMap.get("name"));

		HashMap<String, Class<?>> classMap = new HashMap<>();
		classMap.put("class", c);
		return classMap.get("class");
	}

	public Class<?> customMap(String name, Map<String, String> nameMap, Map<String, Class<?>> classMap) throws ClassNotFoundException {
		nameMap.put("name", name);
		Class<?> c = Class.forName(nameMap.get("name"));

		classMap.put("class", c);
		return classMap.get("class");
	}

	public static class CustomMapA<K, V> implements Map<K, V> {

		@Override
		public int size() {
			return 0;
		}

		@Override
		public boolean isEmpty() {
			return false;
		}

		@Override
		public boolean containsKey(Object key) {
			return false;
		}

		@Override
		public boolean containsValue(Object value) {
			return false;
		}

		@Override
		public V get(Object key) {
			return null;
		}

		@Override
		public V put(K key, V value) {
			return null;
		}

		@Override
		public V remove(Object key) {
			return null;
		}

		@Override
		public void putAll(Map<? extends K, ? extends V> m) {

		}

		@Override
		public void clear() {

		}

		@Override
		public Set<K> keySet() {
			return null;
		}

		@Override
		public Collection<V> values() {
			return null;
		}

		@Override
		public Set<java.util.Map.Entry<K, V>> entrySet() {
			return null;
		}

	}

	public static class CustomMapB<K, V> implements Map<K, V> {

		@Override
		public int size() {
			return 0;
		}

		@Override
		public boolean isEmpty() {
			return false;
		}

		@Override
		public boolean containsKey(Object key) {
			return false;
		}

		@Override
		public boolean containsValue(Object value) {
			return false;
		}

		@Override
		public V get(Object key) {
			return null;
		}

		@Override
		public V put(K key, V value) {
			return null;
		}

		@Override
		public V remove(Object key) {
			return null;
		}

		@Override
		public void putAll(Map<? extends K, ? extends V> m) {

		}

		@Override
		public void clear() {

		}

		@Override
		public Set<K> keySet() {
			return null;
		}

		@Override
		public Collection<V> values() {
			return null;
		}

		@Override
		public Set<java.util.Map.Entry<K, V>> entrySet() {
			return null;
		}

	}
}
