package util;

import jade.util.leap.Serializable;

public class Entry<K, V> implements java.util.Map.Entry<K, V>, Serializable {

	protected K key;
	protected V value;

	public Entry(K key) {
		this.key = key;
	}

	public Entry(K key, V value) {
		this.key = key;
		this.value = value;
	}

	public K getKey() {
		return key;
	}

	public V getValue() {
		return value;
	}

	public V setValue(V value) {
		final V oldValue = this.value;
		this.value = value;
		return oldValue;
	}

}
