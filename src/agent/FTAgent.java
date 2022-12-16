package agent;

import jade.content.ContentElement;
import jade.content.lang.Codec.CodecException;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.OntologyException;
import jade.content.onto.UngroundedException;
import jade.content.onto.basic.Action;
import jade.content.onto.basic.Result;
import jade.core.AID;
import jade.core.Agent;
import jade.core.Location;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.AMSService;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.AMSAgentDescription;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.JADEAgentManagement.QueryAgentsOnLocation;
import jade.domain.JADEAgentManagement.QueryPlatformLocationsAction;
import jade.domain.JADEAgentManagement.WhereIsAgentAction;
import jade.domain.mobility.MobilityOntology;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.util.Logger;
import jade.util.leap.Iterator;
import jade.util.leap.List;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.SimpleFormatter;

public abstract class FTAgent extends Agent {

	public static final String CONFIG_FILE = "files/config.properties";
	public static final String LOG_FILE = "log_file";

	public static Properties props;
	private Logger log;

	private String logPath;

	protected void initLog() {
		try {
			File logFile = new File(logPath);

			logFile = new File(logFile.getAbsolutePath());

			if (!logFile.exists()) {
				if (logFile.getParentFile().mkdir()) {
					if (logFile.createNewFile()) {
						this.log("log file created successfully");
					} else {
						System.err.println("Cannot create log file.");
					}
				} else {
					System.err.println("Cannot create log directory.");
				}
			}
			FileHandler fh = new FileHandler(logFile.getAbsolutePath());
			SimpleFormatter formatter = new SimpleFormatter();
			fh.setFormatter(formatter);
			log.addHandler(fh);

		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("IOException");
			e.printStackTrace();
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}

	@Override
	protected void setup() {
		super.setup();
		try {
			props = new Properties();
			props.load(new FileInputStream(CONFIG_FILE));

			log = Logger.getJADELogger("FTLogger");
			log.setLevel(Level.ALL);

			logPath = props.getProperty(LOG_FILE);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		initLog();
	}

	@Override
	protected void afterMove() {
		super.afterMove();
		initLog();
	}

	protected ArrayList<AID> getAMSAgentsByLocation(Location loc, String aType,
			String aNotType) {
		try {
			ArrayList<AID> aidList = new ArrayList<AID>();
			ACLMessage query = createFIPA_ACLMessage(getAMS(),
					ACLMessage.REQUEST,
					FIPANames.InteractionProtocol.FIPA_REQUEST, loc);
			send(query);
			MessageTemplate mt = MessageTemplate.MatchSender(getAMS());
			ACLMessage response = blockingReceive(mt);
			Result results = (Result) getContentManager().extractContent(
					response);
			List residents = results.getItems();
			for (Iterator it = residents.iterator(); it.hasNext();) {
				AID r = (AID) it.next();
				if (r.getLocalName().contains(aType)
						&& (!r.getLocalName().contains(aNotType) || aNotType
								.equals(""))
						&& !r.getLocalName().equals(this.getLocalName())) {
					aidList.add(r);
				}
			}
			return aidList;
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	// Query Platform AMS Locations
	protected ArrayList<Location> getAMSLocations() {
		getContentManager().registerLanguage(new SLCodec());
		getContentManager().registerOntology(MobilityOntology.getInstance());

		try {

			ArrayList<Location> arrayLoc = new ArrayList<Location>();

			// send request for available locations to AMS
			Action action = new Action(getAMS(),
					new QueryPlatformLocationsAction());

			ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
			request.setLanguage(new SLCodec().getName());
			request.setOntology(MobilityOntology.getInstance().getName());

			getContentManager().fillContent(request, action);
			request.addReceiver(action.getActor());
			send(request);

			// receive response from AMS
			MessageTemplate mt = MessageTemplate.and(
					MessageTemplate.MatchSender(getAMS()),
					MessageTemplate.MatchPerformative(ACLMessage.INFORM));

			ACLMessage resp = blockingReceive(mt);

			ContentElement ce = getContentManager().extractContent(resp);
			Result result = (Result) ce;

			jade.util.leap.Iterator it = result.getItems().iterator();
			while (it.hasNext()) {
				arrayLoc.add((Location) it.next());
			}
			return arrayLoc;

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	// Where Is Agent
	public Location getLocationByAID(AID aid) {
		try {
			WhereIsAgentAction wiaa = new WhereIsAgentAction();
			wiaa.setAgentIdentifier(aid);
			sendRequest(new Action(getAMS(), wiaa));
			// Receive response from AMS
			MessageTemplate mt = MessageTemplate.and(
					MessageTemplate.MatchSender(getAMS()),
					MessageTemplate.MatchPerformative(ACLMessage.INFORM));
			ACLMessage resp = blockingReceive(mt);
			ContentElement ce = getContentManager().extractContent(resp);

			Result result = (Result) ce;
			Location loc = (Location) result.getItems().get(0);
			return loc;
		} catch (UngroundedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CodecException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (OntologyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	protected void sendRequest(Action action) {
		ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
		request.setLanguage(new SLCodec().getName());
		request.setOntology(MobilityOntology.getInstance().getName());
		try {
			getContentManager().fillContent(request, action);
			request.addReceiver(action.getActor());
			send(request);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	protected Location getAMSLocationByIPAndName(String ip) {
		try {

			// send request for available locations to AMS
			Action action = new Action(getAMS(),
					new QueryPlatformLocationsAction());

			ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
			request.setLanguage(new SLCodec().getName());
			request.setOntology(MobilityOntology.getInstance().getName());

			getContentManager().fillContent(request, action);
			request.addReceiver(action.getActor());
			send(request);

			// receive response from AMS
			MessageTemplate mt = MessageTemplate.and(
					MessageTemplate.MatchSender(getAMS()),
					MessageTemplate.MatchPerformative(ACLMessage.INFORM));

			ACLMessage resp = blockingReceive(mt);

			ContentElement ce = getContentManager().extractContent(resp);
			Result result = (Result) ce;

			jade.util.leap.Iterator it = result.getItems().iterator();
			while (it.hasNext()) {
				Location l = (Location) it.next();
				if (l.getAddress().equals(ip)) {
					return l;
				}
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	protected ACLMessage createMulticastACLMessage(int performative,
			ArrayList<AID> aidList, String content) {
		try {
			ACLMessage request = new ACLMessage(performative);
			request.setLanguage(new SLCodec().getName());
			request.setOntology(MobilityOntology.getInstance().getName());
			request.setSender(this.getAID());
			for (int i = 0; i < aidList.size(); i++) {
				request.addReceiver(aidList.get(i));
			}
			request.setContent(content);
			return request;

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	protected ACLMessage createMulticastACLMessage(int performative,
			ArrayList<AID> aidList, java.io.Serializable contentObj) {
		try {
			ACLMessage request = new ACLMessage(performative);
			request.setLanguage(new SLCodec().getName());
			request.setOntology(MobilityOntology.getInstance().getName());
			request.setSender(this.getAID());
			for (int i = 0; i < aidList.size(); i++) {
				request.addReceiver(aidList.get(i));
			}
			request.setContentObject(contentObj);
			return request;

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	protected ACLMessage createUnicastACLMessage(int performative, AID aid,
			String content) {
		ACLMessage request = new ACLMessage(performative);
		request.setLanguage(new SLCodec().getName());
		request.setOntology(MobilityOntology.getInstance().getName());
		try {
			request.setSender(this.getAID());
			request.addReceiver(aid);
			request.setContent(content);
			return request;
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	protected ACLMessage createUnicastACLMessage(int performative,
			String content) {
		ACLMessage request = new ACLMessage(performative);
		request.setLanguage(new SLCodec().getName());
		request.setOntology(MobilityOntology.getInstance().getName());
		try {
			request.setSender(this.getAID());
			request.setContent(content);
			return request;
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	protected ACLMessage createUnicastACLMessage(int performative, AID aid,
			Serializable object) {
		try {
			ACLMessage request = new ACLMessage(performative);
			request.setLanguage(new SLCodec().getName());
			request.setOntology(MobilityOntology.getInstance().getName());
			request.setContentObject(object);
			request.setSender(this.getAID());
			request.addReceiver(aid);
			return request;

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	protected ACLMessage createFIPA_ACLMessage(AID to, int perf,
			String interProt, Location container) {
		ACLMessage request = new ACLMessage(perf);
		request.addReceiver(to);
		request.setLanguage(FIPANames.ContentLanguage.FIPA_SL0);
		request.setOntology(MobilityOntology.getInstance().getName());
		request.setProtocol(interProt);

		// creates the content of the ACLMessage
		Action act = new Action();
		act.setActor(to);
		QueryAgentsOnLocation action = new QueryAgentsOnLocation();
		action.setLocation(container);
		act.setAction(action);
		try {
			getContentManager().fillContent(request, act);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return request;
	}

	protected ArrayList<AID> getAMSAgentsByType(String aType, long maxReseults) {
		ArrayList<AID> amsAgents;
		try {
			amsAgents = new ArrayList<AID>();
			SearchConstraints searchConstraints = new SearchConstraints();
			searchConstraints.setMaxResults(new Long(maxReseults));
			AMSAgentDescription active = new AMSAgentDescription();
			active.setState(AMSAgentDescription.ACTIVE);
			AMSAgentDescription[] agentsFound = AMSService.search(this, active,
					searchConstraints);
			for (int i = 0; i < agentsFound.length; i++) {
				if (agentsFound[i].getName().getLocalName().contains(aType)
						&& !agentsFound[i].getName().getLocalName()
								.equals(this.getLocalName())) {
					amsAgents.add(agentsFound[i].getName());
				}
			}
			return amsAgents;
		} catch (FIPAException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	protected ArrayList<AID> searchDFByComponent(String component) {
		DFAgentDescription dfd = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		sd.setType(component);
		dfd.addServices(sd);

		SearchConstraints all = new SearchConstraints();
		int size = 2;
		all.setMaxResults(new Long(size));

		try {
			DFAgentDescription[] result = DFService.search(this, dfd, all);
			ArrayList<AID> agents = new ArrayList<AID>();
			for (int i = 0; i < result.length; i++) {
				agents.add(result[i].getName());
			}
			return agents;
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}
		return null;
	}

	protected void register(DFAgentDescription dfd) {
		try {
			DFService.register(this, dfd);
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}
	}

	protected void deregister(DFAgentDescription dfd) {
		try {
			DFService.deregister(this, dfd);
		} catch (Exception e) {
		}
	}

	public void log(String s) {
		log.info(this.getLocalName().toString() + ": " + s + ".");
	}

	protected void negotiate() {
		// to override
	}

	protected class SimpleCyclicReceiver extends CyclicBehaviour {

		MessageTemplate mt;
		private ACLMessage msg;

		public ACLMessage getMessage() {
			return msg;
		}

		public SimpleCyclicReceiver(Agent a, MessageTemplate mt) {
			this.mt = mt;
		}

		@Override
		public void action() {
			if (mt == null) {
				msg = myAgent.receive();
			} else {
				msg = myAgent.receive(mt);
			}
			if (msg != null) {
				handle(msg);
				return;
			}
		}

		protected void handle(ACLMessage msg) {
		}
	}

	protected class SimpleReceiver extends OneShotBehaviour {

		private MessageTemplate mt;
		private ACLMessage msg;

		public ACLMessage getMessage() {
			return msg;
		}

		public SimpleReceiver(MessageTemplate mt) {
			this.mt = mt;
		}

		@Override
		public void action() {
			if (mt == null) {
				msg = myAgent.receive();
			} else {
				msg = myAgent.receive(mt);
			}
			if (msg != null) {
				handle(msg);
				return;
			}
		}

		protected void handle(ACLMessage msg) {
		}
	}

	protected class ConditionalCyclic extends SimpleBehaviour {

		protected boolean ended;

		public ConditionalCyclic() {
			super();
			this.ended = false;
		}

		@Override
		public void action() {
			// To override

		}

		@Override
		public boolean done() {
			return ended;
		}

	}

	protected class ConditionalCyclicReceiver extends SimpleBehaviour {

		private MessageTemplate mt;
		private ACLMessage msg;

		protected int n;
		protected int nT;

		public ACLMessage getMessage() {
			return msg;
		}

		public int getNT() {
			return this.nT;
		}

		public ConditionalCyclicReceiver(MessageTemplate mt, int n) {
			this.mt = mt;
			this.n = n;
			this.nT = 0;
		}

		@Override
		public void action() {

			if (mt == null) {
				msg = myAgent.receive();
			} else {
				msg = myAgent.receive(mt);
			}
			if (msg != null) {
				handle(msg);
				this.nT++;
				return;
			}

		}

		protected void handle(ACLMessage msg) {
		}

		@Override
		public boolean done() {
			if (this.nT < this.n) {
				return false;
			} else {
				return true;
			}
		}
	}

}
