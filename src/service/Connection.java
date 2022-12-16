package service;

import org.jgrapht.graph.DefaultWeightedEdge;

public class Connection extends DefaultWeightedEdge {

	private int deadline;

	public Connection() {
		super();
	}

	public long getDeadline() {
		return deadline;
	}

	public void setDeadline(int deadline) {
		this.deadline = deadline;
	}

	@Override
	public String toString() {
		return (((Component) this.getSource()).getName() + " -> " + ((Component) this
				.getTarget()).getName());
	}
}