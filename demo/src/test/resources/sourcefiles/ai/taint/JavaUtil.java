/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
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
