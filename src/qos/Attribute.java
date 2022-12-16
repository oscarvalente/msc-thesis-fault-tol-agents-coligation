package qos;

import jade.util.leap.Serializable;

import java.util.ArrayList;
import java.util.Iterator;

import util.NotDowngradableException;
import util.NotUpgradableException;

public class Attribute<T> implements Serializable, Cloneable {

	private String name;
	private ArrayList<T> possibleValues;
	private String domain;
	private int posCurrentValue;

	public Attribute(String name, String type) {
		this.name = name;
		this.possibleValues = new ArrayList<T>();
		this.domain = type;
	}

	public Attribute(String name, ArrayList<T> possibleValues, String type) {
		this.name = name;
		this.possibleValues = possibleValues;
		this.domain = type;
	}

	public ArrayList<T> getPossibleValues() {
		return possibleValues;
	}

	public void setPossibleValues(ArrayList<T> possibleValues) {
		this.possibleValues = possibleValues;
	}

	public boolean addPossibleValue(T val) {
		return this.possibleValues.add(val);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public T getMin() {
		T min = (T) possibleValues.get(possibleValues.size() - 1);
		if (domain.equals("continuous")) {
			for (int i = 1; i < possibleValues.size(); i++) {
				if ((Float) min > (Float) possibleValues.get(i)) {
					min = (T) possibleValues.get(i);
				}
			}
		}
		return (T) min;
	}

	public T getMax() {
		T max = (T) possibleValues.get(0);
		if (domain.equals("continuous")) {
			for (int i = 1; i < possibleValues.size(); i++) {
				if ((Float) max < (Float) possibleValues.get(i)) {
					max = (T) possibleValues.get(i);
				}
			}
		}
		return (T) max;
	}

	public int getPositionByValue(T value) {
		if (domain.equals("discrete")) {
			for (int i = 0; i < possibleValues.size(); i++) {
				if (possibleValues.get(i).equals(value)) {
					return i;
				}
			}
		}
		System.out.println("ERRO");
		return -1;
	}

	public Value<String, T> getValueByPercentage(float availability) {
		Iterator<T> it = (Iterator<T>) possibleValues.iterator();
		float percVal = (float) 100 / possibleValues.size();
		float perc = 100;
		while (it.hasNext()) {
			T tmpV = (T) it.next();
			if (perc <= availability) {
				Value<String, T> v = new Value<String, T>(this.getName(), tmpV);
				return v;
			} else {
				perc -= percVal;
			}
		}
		return null;
	}

	public float getPercentageByValue(Value<String, T> v) {
		try {

			Iterator<T> it = (Iterator<T>) possibleValues.iterator();
			float percVal = (float) 100 / possibleValues.size();
			float perc = 100;
			while (it.hasNext()) {
				T tmpV = (T) it.next();
				if (tmpV.equals(v.getValue())) {
					return perc;
				} else {
					perc -= percVal;
				}
			}
		} catch (NullPointerException e) {
			return 0;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}

	public boolean greaterOrEqual(Value<String, T> v1, Value<String, T> v2) {
		if (domain.equals("continuous")) {
			if ((Float) v1.getValue() >= (Float) v2.getValue()) {
				return true;
			}
		} else { // discrete
			int posV1 = -1;
			int posV2 = -1;
			for (int i = 0; i < possibleValues.size(); i++) {
				if ((possibleValues.get(i)).equals(v1.getValue())) {
					posV1 = i;
				}
				if ((possibleValues.get(i)).equals(v2.getValue())) {
					posV2 = i;
				}
				if (posV1 > -1 && posV2 > -1) {
					if (posV1 <= posV2) {
						return true;
					} else {
						return false;
					}
				}
			}
		}
		return false;
	}

	public boolean greater(Value<String, T> v1, Value<String, T> v2) {
		if (domain.equals("continuous")) {
			if ((Float) v1.getValue() > (Float) v2.getValue()) {
				return true;
			}
		} else { // discrete
			int posV1 = -1;
			int posV2 = -1;
			for (int i = 0; i < possibleValues.size(); i++) {
				if ((possibleValues.get(i)).equals(v1.getValue())) {
					posV1 = i;
				}
				if ((possibleValues.get(i)).equals(v2.getValue())) {
					posV2 = i;
				}
				if (posV1 > -1 && posV2 > -1) {
					if (posV1 < posV2) {
						return true;
					} else {
						return false;
					}
				}
			}
		}
		return false;
	}

	@Override
	public String toString() {
		return this.name;
	}

	public T downgrade(Value<String, T> v) throws NotDowngradableException {
		try {
			return (T) this.possibleValues.get(this.getPositionByValue((T) v
					.getValue()) + 1);
		} catch (IndexOutOfBoundsException e) {
			throw new NotDowngradableException("Can't downgrade attribute «"
					+ this.name + "».");
		}
	}

	public T upgrade(Value<String, T> v) throws NotUpgradableException {
		try {
			return (T) this.possibleValues.get(this.getPositionByValue((T) v
					.getValue()) - 1);
		} catch (IndexOutOfBoundsException e) {
			throw new NotUpgradableException("Can't upgrade attribute "
					+ this.name + ".");
		}
	}

	public Object clone() throws CloneNotSupportedException {
		Attribute<T> clone = new Attribute<>(this.getName(), this.getDomain());
		clone.posCurrentValue = this.posCurrentValue;
		for (int i = 0; i < possibleValues.size(); i++) {
			clone.addPossibleValue(possibleValues.get(i));
		}
		return clone;
	}
}
