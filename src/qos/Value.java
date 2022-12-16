package qos;

import util.Entry;
import jade.util.leap.Serializable;

public class Value<String, T> extends Entry<String, T> implements Serializable,
		Cloneable {

	public Value(String name) {
		super(name);
	}

	public Value(String name, T value) {
		super(name, value);
	}

	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

}
