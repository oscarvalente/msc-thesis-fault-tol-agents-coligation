package qos;

import jade.util.leap.Serializable;

public class Proposal implements Serializable {

	private double globalReward;
	private double localReward;
	private double pcpmReward;
	private Level level;

	public Proposal(double globalReward, double localReward, double pcpmReward,
			Level level) {
		this.globalReward = globalReward;
		this.localReward = localReward;
		this.pcpmReward = pcpmReward;
		this.level = level;
	}

	public Proposal() {
		this.globalReward = 0;
		this.localReward = 0;
		this.pcpmReward = 0;
	}

	public double getGlobalReward() {
		return this.globalReward;
	}

	public void setGlobalReward(double globalReward) {
		this.globalReward = globalReward;
	}

	public double getLocalReward() {
		return this.localReward;
	}

	public void setLocalReward(double localReward) {
		this.localReward = localReward;
	}

	public double getPCPMReward() {
		return this.pcpmReward;
	}

	public void setPCPMReward(double pcpmReward) {
		this.pcpmReward = pcpmReward;
	}

	public Level getLevel() {
		return this.level;
	}

	public void setLevel(Level level) {
		this.level = level;
	}

	@Override
	public String toString() {
		String prop = "Proposal:" + "\n -> Local reward is "
				+ this.getLocalReward() + "\n -> Global reward is "
				+ this.getGlobalReward() + "\n -> PCPM reward is "
				+ this.getPCPMReward();
		if (this.level != null) {
			prop += "\nLevel is " + this.getLevel().toString();
		} else {
			prop += "\nLevel is null";
		}
		return prop;
	}
}
