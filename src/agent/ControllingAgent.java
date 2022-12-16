package agent;

import jade.core.Agent;
import jade.core.Location;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.ThreadedBehaviourFactory;
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
import java.util.Scanner;
import java.util.Set;

import qos.Level;
import service.Component;
import service.Service;
import util.Util;

public class ControllingAgent extends FTAgent implements Observer {

	public static final String CONFIG_FILE = "files/config.properties";
	public static final String SERVICE_FILE = "service_file";
	public static final String QOS_FILE = "qos_file";
	public static final String FREQ_HB_EXEC = "frequency_heartbeat_execution";
	public static final String TIMEOUT_HB_EXEC = "timeout_heartbeat_execution";
	public static final String DELAY_SEARCH_RECKON = "delay_search_reckon";
	public static final String RETRIES_MIG_RECKON = "retries_migration_reckon";
	public static final String TIMEOUT_NEGOT_RECKON = "timeout_negotiation_reckon";
	public static final String TIMEOUT_NEGOT_EXEC = "timeout_negotiation_execution";
	public static final String DELAY_SEARCH_OUTPUT_EXEC = "delay_search_output_execution";
	public static final String RETRIES_MIG_EXEC = "retries_migration_execution";

	public static Properties props;
	public static Service service;
	public static Level qos;

	public static Level minimumLevel;
	public static Level preferredLevel;
	public static Level maximumLevel;

	public int nReckon;
	public int nExecution;
	public Set<Component> allComponents;
	public ArrayList<Component> execComponents;

	private ArrayList<String> locationsMoved;

	private PlatformController container;

	private String delaySearch;
	private String retriesMigration;
	private String timeoutNegotReckon;

	@Override
	protected void setup() {
		super.setup();
		try {
			props = new Properties();
			props.load(new FileInputStream(CONFIG_FILE));
			service = Util.loadServiceFromXML(new File(props
					.getProperty(SERVICE_FILE)));
			qos = Util.loadLevelFromJSON(props.getProperty(QOS_FILE));
			// say(qos.toString());
			nReckon = 1;
			nExecution = 1;

			delaySearch = props.getProperty(DELAY_SEARCH_RECKON);
			retriesMigration = props.getProperty(RETRIES_MIG_RECKON);
			timeoutNegotReckon = props.getProperty(TIMEOUT_NEGOT_RECKON);

			createServiceQoS();

			// Component comp = service.getComponentByName("A");
			//
			// comp.getMinimumLevel().upgrade();
			// comp.getMinimumLevel().upgrade();
			// comp.getMinimumLevel().upgrade();
			// comp.getMinimumLevel().upgrade();
			// System.out.println("1\n\n" + comp.getMinimumLevel());
			// while (comp.getMinimumLevel().rollbackUpgrade() == false) {
			// }
			// System.out.println(comp.getMinimumLevel().rollbackUpgrade());
			// System.out.println("2\n\n" + comp.getMinimumLevel());
			// comp.getMinimumLevel().upgrade();
			// System.out.println("3\n\n" + comp.getMinimumLevel());
			// doSuspend();

			container = getContainerController();

			allComponents = (Set<Component>) service.vertexSet();
			execComponents = new ArrayList<Component>();

			// qualquer tecla pra começar
			// Scanner scan = new Scanner(System.in);
			// scan.next();

			ThreadedBehaviourFactory tbf = new ThreadedBehaviourFactory();

			OneShotBehaviour launchExecutors = new OneShotBehaviour(this) {

				@Override
				public void action() {
					try {
						for (int i = 0; i < service.vertexSet().size(); i++) {
							String compName = getNextComponent();
							String type = "";
							String frequencyHB = props
									.getProperty(FREQ_HB_EXEC);
							String timeoutHB = props
									.getProperty(TIMEOUT_HB_EXEC);
							String timeoutNegotExec = props
									.getProperty(TIMEOUT_NEGOT_EXEC);
							String delaySearchOutput = props
									.getProperty(DELAY_SEARCH_OUTPUT_EXEC);
							String retriesMigExec = props
									.getProperty(RETRIES_MIG_EXEC);
							if (!compName.equals("")) {
								// if (nExecution <= 3) { // <- LIXO: TESTES
								int input = service
										.countPredecessorComponents(compName);
								if (input > 0) {
									type = "-input";

									container.createNewAgent(
											ExecutionAgent.AGENT_TYPE
													+ nExecution,
											"agent.ExecutionAgent",
											new Object[] { compName, type,
													input, frequencyHB,
													timeoutHB,
													timeoutNegotExec,
													delaySearchOutput,
													retriesMigExec }).start();

									// say(type + " " + input); LIXO
								} else {
									container.createNewAgent(
											ExecutionAgent.AGENT_TYPE
													+ nExecution,
											"agent.ExecutionAgent",
											new Object[] { compName, "none",
													"0", frequencyHB,
													timeoutHB,
													timeoutNegotExec,
													delaySearchOutput,
													retriesMigExec }).start();
								}
								nExecution++;
								log("lauching component " + compName);

								// } // <- LIXO: TESTES
							}
						}
					} catch (StaleProxyException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (ControllerException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

			};

			addBehaviour(tbf.wrap(launchExecutors));

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
		// criar agentes Reckon se for notificado
		try {
			container.createNewAgent(
					ReckonAgent.AGENT_TYPE + nReckon,
					"agent.ReckonAgent",
					new Object[] { delaySearch, retriesMigration,
							timeoutNegotReckon }).start();
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
		Iterator it = allComponents.iterator();
		while (it.hasNext()) {
			Component c = (Component) it.next();
			if (!execComponents.contains(c)) {
				execComponents.add(c);
				return c.getName();
			}
		}
		return "";
	}

	public void createServiceQoS() {

		try {

			preferredLevel = qos.createPreferredLevel();
			minimumLevel = qos.createMinimumLevel();

			// say(minimumLevel.toString());
			// say(preferredLevel.toString());

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
