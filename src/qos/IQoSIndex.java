package qos;

public interface IQoSIndex {

	public double relativeImportance(int k);

	public void resetPositionToDowngrade();

	public void decreasePositionToDowngrade();

	public void resetPositionToUpgrade();

	public void increasePositionToUpgrade();

	public void decreasePositionToUpgrade();

}
