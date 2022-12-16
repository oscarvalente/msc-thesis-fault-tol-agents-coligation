package qos;

import jade.util.leap.Serializable;

import java.util.ArrayList;

import util.NotDowngradableException;
import util.NotUpgradableException;
import util.Util;

public class Level implements IQoSIndex, IUpDowngradable, Serializable,
		Cloneable {

	private String name;
	private ArrayList<Dimension> dimensions;

	private int tmpPosDimDowng; // para o downgrade da dimensão
	private int tmpPosDimUpg; // para o upgrade da dimensão

	public Level(String name) {
		this.name = name;
		dimensions = new ArrayList<Dimension>();
		resetPositionToDowngrade();
		resetPositionToUpgrade();
	}

	public Level(String name, ArrayList<Dimension> dimensions) {
		this.name = name;
		this.dimensions = dimensions;
		resetPositionToDowngrade();
		resetPositionToUpgrade();
	}

	public Level(Level l) {
		this(l.name, l.dimensions);
	}

	public boolean addDimension(Dimension dim) {
		try {
			return (dimensions.add(dim) ? true : false);
		} finally {
			resetPositionToDowngrade();
			resetPositionToUpgrade();
		}
	}

	public Dimension getDimension(int index) {
		return dimensions.get(index);
	}

	public ArrayList<Dimension> getAllDimensions() {
		return this.dimensions;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public double distanceTo(Level preferred) {
		double sumDistance = 0;
		try {
			for (int i = 0; i < dimensions.size(); i++) {
				sumDistance += (double) (relativeImportance(i + 1) * dimensions
						.get(i).getDifDA(preferred.getDimension(i)));

			}
		} catch (NullPointerException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sumDistance;
	}

	@Override
	public double relativeImportance(int k) {
		int n = dimensions.size();
		return ((double) (n - k + 1) / n);
	}

	public Level createLevelByType(String type) {
		Level proposedLevel = new Level(this.name.split(" - ")[0] + type);
		for (int i = 0; i < dimensions.size(); i++) {
			proposedLevel.addDimension(dimensions.get(i).createDimensionByType(
					type));
		}
		return proposedLevel;
	}

	public Level createMinimumLevel() {
		Level minLevel = new Level(this.name + " - Minimum");
		try {
			for (int i = 0; i < this.dimensions.size(); i++) {
				minLevel.addDimension(dimensions.get(i)
						.createMinimumDimension());
			}
			return minLevel;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public Level createPreferredLevel() {
		Level maxLevel = new Level(this.name + " - Preferred");
		try {
			for (int i = 0; i < this.dimensions.size(); i++) {
				maxLevel.addDimension(dimensions.get(i)
						.createPreferredDimension());
			}
			return maxLevel;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public float getWaste() {
		int waste = 0;
		for (int i = 0; i < dimensions.size(); i++) {
			waste += (this.getDimension(i).getWaste() / relativeImportance(i + 1))
					/ dimensions.size();
		}
		return (waste / dimensions.size());
	}

	@Override
	public String toString() {
		String level = "\n\tLevel «" + this.name + "»:\n";
		try {
			for (int d = 0; d < dimensions.size(); d++) {
				level += dimensions.get(d).toString() + "\n";
			}
			return level;
		} catch (NullPointerException e) {

		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	public boolean greaterOrEqual(Level level) {
		ArrayList<Dimension> dim = level.getAllDimensions();
		for (int i = 0; i < dimensions.size(); i++) {
			if (!dimensions.get(i).greaterOrEqual(dim.get(i))) {
				return false;
			}
		}
		return true;
	}

	public boolean greater(Level level) {
		ArrayList<Dimension> dim = level.getAllDimensions();
		for (int i = 0; i < dimensions.size(); i++) {
			if (!dimensions.get(i).greater(dim.get(i))) {
				return false;
			}
		}
		return true;
	}

	public boolean downgrade() throws NotDowngradableException {
		try {
			return dimensions.get(tmpPosDimDowng).downgrade();
		} catch (NotDowngradableException e) {
			decreasePositionToDowngrade();
		} catch (IndexOutOfBoundsException e) {
			resetPositionToDowngrade();
			throw new NotDowngradableException("Can't downgrade level «"
					+ this.name + "»");
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
			return dimensions.get(tmpPosDimUpg).upgrade();
		} catch (NotUpgradableException e) {
			// System.out.println(e.getMessage());
			increasePositionToUpgrade();
		} catch (IndexOutOfBoundsException e) {
			resetPositionToUpgrade();
			throw new NotUpgradableException("Can't upgrade level «"
					+ this.name + "»");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean rollbackUpgrade() throws NotUpgradableException {
		try {
			return dimensions.get(tmpPosDimUpg).rollbackUpgrade();
		} catch (NotUpgradableException e) {
			increasePositionToUpgrade();
		} catch (IndexOutOfBoundsException e) {
			resetPositionToUpgrade();
			throw new NotUpgradableException("Can't upgrade level «"
					+ this.name + "»");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	// volta à dimensão menos importante (p/ o downgrade)
	@Override
	public void resetPositionToDowngrade() {
		tmpPosDimDowng = this.dimensions.size() - 1;
	}

	// vai apontar para uma dimensão mais importante (p/ o downgrade)
	@Override
	public void decreasePositionToDowngrade() {
		tmpPosDimDowng--;
	}

	@Override
	public void resetPositionToUpgrade() {
		tmpPosDimUpg = 0;
	}

	@Override
	public void increasePositionToUpgrade() {
		tmpPosDimUpg++;
	}

	@Override
	public void decreasePositionToUpgrade() {
		tmpPosDimUpg--;
	}

	public void resetUpDownPointers() {
		resetPositionToUpgrade();
		resetPositionToDowngrade();
		for (int i = 0; i < dimensions.size(); i++) {
			dimensions.get(i).resetPositionToUpgrade();
			dimensions.get(i).resetPositionToDowngrade();
		}
	}

	public Object clone() throws CloneNotSupportedException {
		return new Level(getName(),
				(ArrayList<Dimension>) Util.cloneDimensionList(dimensions));
	}

}
