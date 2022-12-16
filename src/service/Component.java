package service;

import java.io.Serializable;

import qos.Level;
import util.NotDowngradableException;
import util.NotUpgradableException;

public class Component implements jade.util.leap.Serializable, Cloneable {

	private String name;

	private Level minimumLevel;
	private Level currentLevel;
	private Level preferredLevel;
	private Serializable executable;

	public Component() {
	}

	public Component(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Level getMinimumLevel() {
		return this.minimumLevel;
	}

	public void setMinimumLevel(Level minimumLevel) {
		this.minimumLevel = minimumLevel;
	}

	public Level getCurrentLevel() {
		return this.currentLevel;
	}

	public void setCurrentLevel(Level currentLevel) {
		this.currentLevel = currentLevel;
	}

	public Level getPreferredLevel() {
		return this.preferredLevel;
	}

	public void setPreferredLevel(Level preferredLevel) {
		this.preferredLevel = preferredLevel;
	}

	public Serializable getExecutable() {
		return executable;
	}

	public void setExecutable(Serializable executable) {
		this.executable = executable;
	}

	public boolean isFeasibile(float availability) {
		/*
		 * maxNivel - 100 | minimumLevel - minAvailability minAvailability <=
		 * availabity ? true : false
		 */
		// forma mais simples
		// Value minValue = (minimumLevel.getDimension(0)
		// .getValueByName("Char Gen p/ sec"));
		// Value affordValue = minimumLevel.getDimension(0)
		// .getAttributeByName("Char Gen p/ sec")
		// .getValueByPercentage(availability);
		// if (affordValue == null) {
		// return false;
		// }
		// return (minimumLevel.getDimension(0).getAttributeByName(
		// "Char Gen p/ sec").greaterOrEqual(affordValue, minValue));
		if (availability - minimumLevel.getWaste() > 0) {
			return true;
		} else {
			return false;
		}
	}

	public boolean downgradeLevel() throws NotDowngradableException {
		try {
			return this.currentLevel.downgrade();
		} catch (NotDowngradableException e) {
			throw new NotDowngradableException(e.getMessage() + " of "
					+ this.name);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean upgradeLevel() throws NotUpgradableException {
		try {
			return this.currentLevel.upgrade();
		} catch (NotUpgradableException e) {
			throw new NotUpgradableException(e.getMessage() + " of "
					+ this.name);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean rollbackUpgrade() throws NotUpgradableException {
		try {
			return this.currentLevel.rollbackUpgrade();
		} catch (NotUpgradableException e) {
			throw new NotUpgradableException(e.getMessage() + " of "
					+ this.name);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public void resetUpDownPointers() {
		this.currentLevel.resetUpDownPointers();
	}

	@Override
	public String toString() {
		return this.name;
	}

	public double getDistanceToPreferred() {
		return this.currentLevel.distanceTo(preferredLevel);
		// double localReward = 0;
		// for (int i = 0; i < this.currentLevel.getAllDimensions().size(); i++)
		// {
		// localReward += this.currentLevel.getAllDimensions().get(i)
		// .getLocalReward(penalty);
		// }
		// return localReward;
	}

	public Object clone() throws CloneNotSupportedException {
		Component clone = new Component(this.name);
		try {
			try {
				clone.setCurrentLevel((Level) this.getCurrentLevel().clone());
			} catch (NullPointerException e) {
			}
			try {
				clone.setMinimumLevel((Level) this.getMinimumLevel().clone());
			} catch (NullPointerException e) {
			}
			try {
				clone.setPreferredLevel((Level) this.getPreferredLevel()
						.clone());
			} catch (NullPointerException e) {
			}
			try {
				clone.setExecutable(this.getExecutable());
			} catch (NullPointerException e) {
			}
		} catch (NullPointerException e) {
		} catch (Exception e) {
			e.printStackTrace();
		}
		return clone;
	}
}
