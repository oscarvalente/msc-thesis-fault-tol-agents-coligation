import jade.Boot;

import java.net.InetAddress;

public class Main {
	public static void main(String[] args) {
		try {
			String[] param = new String[5];
			param[0] = "-gui";
			param[1] = "-agents";
			param[2] = "controller:agent.ControllingAgent()";
			param[3] = "-local-host";
			String hostName = InetAddress.getLocalHost().getCanonicalHostName();
			param[4] = InetAddress.getAllByName(hostName)[2].getHostAddress();
			// [1] VMware | [2] LAN

			// System.out.println(param[4]);

			Boot.main(param);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
