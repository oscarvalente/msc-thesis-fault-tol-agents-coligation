package qos;

import util.NotDowngradableException;
import util.NotUpgradableException;

public interface IUpDowngradable {

	public boolean downgrade() throws NotDowngradableException;

	public boolean upgrade() throws NotUpgradableException;

	public boolean rollbackUpgrade() throws NotUpgradableException;

	public boolean rollbackDowngrade();

}
