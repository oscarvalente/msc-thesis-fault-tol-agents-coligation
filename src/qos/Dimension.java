package qos;

import jade.util.leap.Serializable;

import java.util.ArrayList;
import java.util.Iterator;

import util.Entry;
import util.NotDowngradableException;
import util.NotUpgradableException;

public class Dimension implements IQoSIndex, IUpDowngradable, Serializable,
		Cloneable {

	private String name;
	// conjunto de N atributo-valor
	private ArrayList<Entry<Attribute, Value>> attributeValue;

	private int tmpPosAttrDowng; // para o downgrade do atributo
	private int tmpPosAttrUpg; // para o upgrade do atributo

	public Dimension(String name,
			ArrayList<Entry<Attribute, Value>> attributeValue) {
		this.name = name;
		this.attributeValue = attributeValue;
		resetPositionToDowngrade();
		resetPositionToUpgrade();
	}

	public Dimension(String name) {
		this.name = name;
		this.attributeValue = new ArrayList<Entry<Attribute, Value>>();
		resetPositionToDowngrade();
		resetPositionToUpgrade();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setAttributeValue(
			ArrayList<Entry<Attribute, Value>> attributeValue) {
		this.attributeValue = attributeValue;
	}

	public ArrayList<Entry<Attribute, Value>> getAttributeValue() {
		return this.attributeValue;
	}

	public <T> boolean add(Attribute<T> attr, Value<String, T> value) {
		try {
			Entry<Attribute, Value> e = new util.Entry<Attribute, Value>(attr,
					value);
			return attributeValue.add(e);
		} finally {
			resetPositionToDowngrade();
			resetPositionToUpgrade();
		}
	}

	public <T> Value<String, T> getValueByName(String name) {
		Iterator<Entry<Attribute, Value>> it = attributeValue.iterator();
		while (it.hasNext()) {
			Entry<Attribute, Value> attr = it.next();
			if (attr.getKey().getName().equals(name)) {
				return attr.getValue();
			}
		}
		return null;
	}

	public <T> Value<String, T> getCurrentValueByAttributeName(String name) {
		Iterator<Entry<Attribute, Value>> it = attributeValue.iterator();
		while (it.hasNext()) {
			Entry<Attribute, Value> attr = it.next();
			if (attr.getKey().getName().equals(name)) {
				return attr.getValue();
			}
		}
		return null;
	}

	public <T> Attribute<T> getAttributeByName(String name) {
		Iterator<Entry<Attribute, Value>> it = attributeValue.iterator();
		while (it.hasNext()) {
			Entry<Attribute, Value> attr = it.next();
			if (attr.getKey().getName().equals(name)) {
				return attr.getKey();
			}
		}
		return null;
	}

	public <T> double getDegreeOfAcceptability(Value<String, T> proposed,
			Value<String, T> preferred) {
		try {
			double da = 0;
			Attribute<Float> attr = getAttributeByName((String) proposed
					.getKey());
			String domain = attr.getDomain();
			if (domain.equals("continuous")) {
				da = (((Double) proposed.getValue() - (Double) preferred
						.getValue() / (attr.getMax() - attr.getMin())));
			} else { // discrete
				Attribute<T> attrDisc = getAttributeByName((String) proposed
						.getKey());
				da = (double) ((attrDisc
						.getPositionByValue(proposed.getValue()) + 1) - (attrDisc
						.getPositionByValue(preferred.getValue()) + 1))
						/ (attrDisc.getPossibleValues().size() - 1);
			}
			return da;
		} catch (NullPointerException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}

	public <T> double getDifDA(Dimension dimPref) {
		// dif entre os Props e Prefs
		double sumDif = 0;
		try {
			ArrayList<Value<String, T>> propV = this.getAllValues();
			ArrayList<Value<String, T>> prefV = dimPref.getAllValues();
			for (int i = 0; i < attributeValue.size(); i++) {
				sumDif += (double) (relativeImportance(i + 1) * Math
						.abs(getDegreeOfAcceptability(propV.get(i),
								prefV.get(i))));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sumDif;
	}

	public <T> ArrayList<Value<String, T>> getAllValues() {
		ArrayList<Value<String, T>> values = new ArrayList<Value<String, T>>();
		Iterator<Entry<Attribute, Value>> it = attributeValue.iterator();
		while (it.hasNext()) {
			Entry<Attribute, Value> attr = it.next();
			values.add(attr.getValue());
		}
		return values;
	}

	@Override
	public double relativeImportance(int k) {
		int n = attributeValue.size();
		return ((double) (n - k + 1) / n);
	}

	public Dimension createDimensionByType(String type) {
		Dimension proposedDim = new Dimension(this.name.split(" - ")[0] + type);
		Iterator<Entry<Attribute, Value>> attrIt = attributeValue.iterator();
		while (attrIt.hasNext()) {
			Entry<Attribute, Value> entry = attrIt.next();
			proposedDim.add(entry.getKey(), entry.getValue());
		}
		return proposedDim;
	}

	public float getWaste() {
		int waste = 0;
		Iterator<Entry<Attribute, Value>> it = attributeValue.iterator();
		int i = 0;
		while (it.hasNext()) {
			Entry<Attribute, Value> entry = (Entry<Attribute, Value>) it.next();
			waste += (entry.getKey().getPercentageByValue(entry.getValue()) / relativeImportance(i + 1))
					/ attributeValue.size();
			i++;
		}
		return (waste / attributeValue.size());
	}

	@Override
	public String toString() {
		String dim = "\t\t> Dimension «" + this.name + "»:\n";
		try {
			Iterator it = attributeValue.iterator();
			while (it.hasNext()) {
				Entry<Attribute, Value> entry = (Entry<Attribute, Value>) it
						.next();
				dim += "\t\t\t - " + entry.getKey().toString() + " - "
						+ entry.getValue().getValue() + "\n";
			}
			return dim;
		} catch (NullPointerException e) {

		} catch (Exception e) {
			e.printStackTrace();
		}
		return dim;
	}

	public boolean greaterOrEqual(Dimension dimension) {
		ArrayList<Entry<Attribute, Value>> attrDim = dimension
				.getAttributeValue();
		Iterator it = attributeValue.iterator();
		Iterator itDim = attrDim.iterator();
		while (it.hasNext() && itDim.hasNext()) {
			Entry<Attribute, Value> entry = (Entry<Attribute, Value>) it.next();
			Entry<Attribute, Value> entryDim = (Entry<Attribute, Value>) itDim
					.next();
			if (!entry.getKey().greaterOrEqual(entry.getValue(),
					entryDim.getValue())) {
				return false;
			}
		}
		return true;
	}

	public boolean greater(Dimension dimension) {
		ArrayList<Entry<Attribute, Value>> attrDim = dimension
				.getAttributeValue();
		Iterator it = attributeValue.iterator();
		Iterator itDim = attrDim.iterator();
		while (it.hasNext() && itDim.hasNext()) {
			Entry<Attribute, Value> entry = (Entry<Attribute, Value>) it.next();
			Entry<Attribute, Value> entryDim = (Entry<Attribute, Value>) itDim
					.next();
			if (!entry.getKey().greater(entry.getValue(), entryDim.getValue())) {
				return false;
			}
		}
		return true;
	}

	// tenta fazer downgrade a um atributo
	@Override
	public boolean downgrade() throws NotDowngradableException {
		try {
			Entry<Attribute, Value> e = attributeValue.get(tmpPosAttrDowng);
			Attribute a = e.getKey();
			Value v = new Value(a.getName());
			v.setValue(a.downgrade(e.getValue()));
			Entry<Attribute, Value> newE = new Entry<Attribute, Value>(a, v);
			attributeValue.remove(tmpPosAttrDowng);
			attributeValue.add(tmpPosAttrDowng, newE);
			return true;
		} catch (NotDowngradableException e) {
			// se entrar é porque não deu mais para fazer downgrade ao atributo
			// System.out.println(e.getMessage());
			decreasePositionToDowngrade();
		} catch (IndexOutOfBoundsException e) {
			// se entrar é porque não deu mais para fazer downgrade aos
			// atributos da dimensão
			resetPositionToDowngrade();
			throw new NotDowngradableException("Can't downgrade dimension «"
					+ this.name + "».");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean rollbackDowngrade() {
		// TODO rollback Downgrade
		return false;
	}

	@Override
	public boolean upgrade() throws NotUpgradableException {
		try {
			Entry<Attribute, Value> e = attributeValue.get(tmpPosAttrUpg);
			Attribute a = e.getKey();
			Value v = new Value(a.getName());
			v.setValue(a.upgrade(e.getValue()));
			Entry<Attribute, Value> newE = new Entry<Attribute, Value>(a, v);
			attributeValue.remove(tmpPosAttrUpg);
			attributeValue.add(tmpPosAttrUpg, newE);
			return true;
		} catch (NotUpgradableException e) {
			// se entrar é porque não deu mais para fazer upgrade ao atributo
			// System.out.println(e.getMessage());
			increasePositionToUpgrade();
		} catch (IndexOutOfBoundsException e) {
			// se entrar é porque não deu mais para fazer upgrade aos
			// atributos da dimensão
			resetPositionToUpgrade();
			throw new NotUpgradableException("Can't upgrade dimension «"
					+ this.name + "».");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean rollbackUpgrade() throws NotUpgradableException {
		try {
			Entry<Attribute, Value> e = attributeValue.get(tmpPosAttrUpg);
			Attribute a = e.getKey();
			Value v = new Value(a.getName());
			v.setValue(a.downgrade(e.getValue()));
			Entry<Attribute, Value> newE = new Entry<Attribute, Value>(a, v);
			attributeValue.remove(tmpPosAttrUpg);
			attributeValue.add(tmpPosAttrUpg, newE);
			increasePositionToUpgrade();
			return true;
		} catch (NotDowngradableException e) {
			increasePositionToUpgrade();
		} catch (IndexOutOfBoundsException e) {
			// se entrar é porque não deu mais para fazer upgrade aos
			// atributos da dimensão
			resetPositionToUpgrade();
			throw new NotUpgradableException("Can't upgrade dimension «"
					+ this.name + "».");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	// volta ao atributo menos importante (p/ o downgrade)
	@Override
	public void resetPositionToDowngrade() {
		tmpPosAttrDowng = this.attributeValue.size() - 1;
	}

	// vai apontar para um atributo mais importante (p/ o downgrade)
	@Override
	public void decreasePositionToDowngrade() {
		tmpPosAttrDowng--;
	}

	// volta ao atributo mais importante (p/ o upgrade)
	@Override
	public void resetPositionToUpgrade() {
		tmpPosAttrUpg = 0;
	}

	// vai apontar para um atributo menos importante (p/ o upgrade)
	@Override
	public void increasePositionToUpgrade() {
		tmpPosAttrUpg++;
	}

	@Override
	public void decreasePositionToUpgrade() {
		tmpPosAttrUpg--;

	}

	public boolean isFeasible(float availability) {
		for (int i = 0; i < attributeValue.size(); i++) {
			Value minValue = new Value(name);
			minValue.setValue((this.attributeValue.get(i).getKey().getMin()));
			Value affordValue = (this.attributeValue.get(i).getKey()
					.getValueByPercentage(availability));
			System.out.println("Min = " + minValue + "\t Afford = "
					+ affordValue);
			try {
				if (affordValue == null) {
					return false;
				}
				if (!this.attributeValue.get(i).getKey()
						.greaterOrEqual(affordValue, minValue)) {
					return false;
				}
			} catch (NullPointerException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return true;
	}

	public Dimension createMinimumDimension() {
		Dimension minDimension = new Dimension(this.name + " - Minimum");
		try {
			for (int i = 0; i < this.attributeValue.size(); i++) {
				Entry<Attribute, Value> e = attributeValue.get(i);
				Attribute a = e.getKey();
				Value v = new Value(a.getName());
				v.setValue(a.getMin());
				minDimension.add(a, v);
			}
			return minDimension;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public Dimension createPreferredDimension() {
		Dimension maxDimension = new Dimension(this.name + " - Preferred");
		try {
			for (int i = 0; i < this.attributeValue.size(); i++) {
				Entry<Attribute, Value> e = attributeValue.get(i);
				Attribute a = e.getKey();
				Value v = new Value(a.getName());
				v.setValue(a.getMax());
				maxDimension.add(a, v);
			}
			return maxDimension;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	// public double getLocalReward(double penalty) {
	// double localReward = 0;
	// for (int i = 0; i < attributeValue.size(); i++) {
	// localReward += attributeValue
	// .get(i)
	// .getKey()
	// .getPercentageByValue(
	// (Value) attributeValue.get(i).getValue());
	// }
	// return localReward;
	// }

	public Object clone() throws CloneNotSupportedException {
		Dimension clone = new Dimension(this.getName());
		for (int i = 0; i < attributeValue.size(); i++) {
			clone.add((Attribute) attributeValue.get(i).getKey().clone(),
					(Value) attributeValue.get(i).getValue().clone());
		}
		return clone;
	}
}
