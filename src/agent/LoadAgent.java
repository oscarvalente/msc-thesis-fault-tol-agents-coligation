package agent;

import jade.core.Agent;
import jade.core.Location;
import jade.core.behaviours.OneShotBehaviour;
import jade.wrapper.ControllerException;
import jade.wrapper.PlatformController;
import jade.wrapper.StaleProxyException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;
import java.util.Properties;
import java.util.Set;

import qos.Attribute;
import qos.Dimension;
import qos.Level;
import qos.Value;
import service.Component;
import service.Service;
import util.Util;

public class LoadAgent extends FTAgent implements Observer {

	public static final String CONFIG_FILE = "files/config.properties";
	public static final String SERVICE_FILE = "service_file";
	public static Properties props;
	public static Service service;

	public static Level minimumLevel;
	public static Level preferredLevel;
	public static Level maximumLevel;

	public int nReckon;
	public int nExecution;
	public Set<Component> allComponents;
	public ArrayList<Component> execComponents;

	@Override
	protected void setup() {
		super.setup();
		try {
			props = new Properties();
			props.load(new FileInputStream(CONFIG_FILE));
			service = Util.loadServiceFromXML(new File(props
					.getProperty(SERVICE_FILE)));
			nReckon = 1;
			nExecution = 1;

			createServiceQoS();

			allComponents = (Set<Component>) service.vertexSet();
			execComponents = new ArrayList<Component>();

			addBehaviour(new OneShotBehaviour(this) {

				@Override
				public void action() {
					ContainerListener c = new ContainerListener(myAgent);
					c.listen();
				}
			});

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void update(Observable obj, Object arg) {
		// criar agentes se for notificado
		PlatformController container = getContainerController();
		try {
			String compName = getNextComponent();
			if (!compName.equals("")) {
				container.createNewAgent(
						ExecutionAgent.AGENT_TYPE + nExecution,
						"agent.ExecutionAgent", new Object[] { compName })
						.start();
				nExecution++;
				say(compName);
			}
			container.createNewAgent(ReckonAgent.AGENT_TYPE + nReckon,
					"agent.ReckonAgent", null).start();
			nReckon++;
		} catch (StaleProxyException e) {
			e.printStackTrace();
		} catch (ControllerException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String getNextComponent() {
		ArrayList<Component> a = new ArrayList<Component>();
		Iterator it = allComponents.iterator();

		while (it.hasNext()) {
			Component c = (Component) it.next();
			if (execComponents.isEmpty()) {
				execComponents.add(c);
				say("vai component " + c.getName());
				return c.getName();
			} else {
				Iterator it2 = execComponents.iterator();
				while (it2.hasNext()) {
					Component c2 = (Component) it2.next();
					if (!c2.getName().equals(c.getName())) {
						say("vai component " + c.getName());
						execComponents.add(c);
						return c.getName();
					}
				}
			}
		}
		return "";
	}

	public static void createServiceQoS() {

		// video
		// Dimension videoQoS = new Dimension("Video Quality");
		// videoQoS.put("Compression Index", Integer.parseInt("20"));
		// videoQoS.put("Color Depth", Integer.parseInt("32"));
		// videoQoS.put("Frame Rate", Integer.parseInt("48"));

		// audio
		// Dimension audioQoS = new Dimension("Audio Quality");
		// audioQoS.put("Sampling Rate", Integer.parseInt("44"));
		// audioQoS.put("Sample Bits", Integer.parseInt("16"));

		// dimensions.add(videoQoS);
		// dimensions.add(audioQoS);
		try {
			ArrayList<Dimension> minimumDim = new ArrayList<Dimension>();
			ArrayList<Dimension> preferredDim = new ArrayList<Dimension>();

			ArrayList<Float> possibleValues = new ArrayList<Float>();
			possibleValues.add((float) 256);
			possibleValues.add((float) 128);
			possibleValues.add((float) 64);
			possibleValues.add((float) 32);
			possibleValues.add((float) 16);
			possibleValues.add((float) 8);
			Attribute<Float> attribute = new Attribute<Float>(
					"Char Gen p/ sec", possibleValues, "discrete");

			Dimension dimMin = new Dimension("Char Qos - Minimum");

			dimMin.add(attribute, new Value<String, Float>("Char Gen p/ sec",
					(float) 32));
			minimumDim.add(dimMin);

			Dimension dimPref = new Dimension("Char Qos - Preferred");
			dimPref.add(attribute, new Value<String, Float>("Char Gen p/ sec",
					(float) 128));
			preferredDim.add(dimPref);

			minimumLevel = new Level("Minimum", minimumDim);
			preferredLevel = new Level("Preferred", preferredDim);

			service.setAllMinimumLevel(minimumLevel);
			service.setAllPreferredLevel(preferredLevel);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	class ContainerListener extends Observable {
		ArrayList<Location> listenedLocations;
		Agent myAgent;

		public ContainerListener(Agent a) {
			this.myAgent = a;
			listenedLocations = new ArrayList<Location>();
			this.addObserver((Observer) a);
		}

		public void listen() {
			try {
				while (myAgent.getAgentState().getValue() == jade.wrapper.AgentState.cAGENT_STATE_ACTIVE) {
					ArrayList<Location> locations = getAMSLocations();
					Iterator it = (Iterator) locations.iterator();
					while (it.hasNext()) {
						Location loc = (Location) it.next();
						if (!listenedLocations.contains(loc)) {
							setChanged();
							notifyObservers();
							listenedLocations.add(loc);
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
