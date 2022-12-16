package qos;

public interface IDowngradable {

	public void resetPositionToDowngrade();

	public void decreasePositionToDowngrade();

	public void resetPositionToUpgrade();

	public void increasePositionToUpgrade();

	public void decreasePositionToUpgrade();

}
