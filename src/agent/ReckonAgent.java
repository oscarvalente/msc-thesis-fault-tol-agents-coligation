package agent;

import jade.content.lang.sl.SLCodec;
import jade.core.AID;
import jade.core.Location;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.FIPANames;
import jade.domain.mobility.MobilityOntology;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.proto.ProposeInitiator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;
import java.util.Vector;

import qos.Level;
import qos.Proposal;
import service.Component;
import service.Service;
import util.NotDowngradableException;
import util.NotUpgradableException;
import util.RuntimeInfo;
import util.Util;

public class ReckonAgent extends FTAgent {

	public static final String AGENT_TYPE = "RECKON";
	private static final String AGENT_TYPE_EXEC = "EXECUTION";
	private static final String AGENT_TYPE_CLONE = "clone";
	// agents c/ componentes nesta máquina
	private HashMap<String, AID> agentComponents;
	private ArrayList<Component> componentsHere;
	private Service service;

	// private double penalty = 1;

	// private transient ThreadedBehaviourFactory tbf;

	private boolean downgrading;

	private Float availability;
	private FSMBehaviour reckonBehaviour;

	private Location toLocation;

	private CyclicBehaviour attemptMoveB;

	// negotiation
	private static final String STARTING_STATE = "STARTING";
	private static final String SEARCHING_STATE = "SEARCHING";
	private static final String DELAYING_STATE = "DELAYING";
	private static final String OFFERING_STATE = "OFFERING";
	private static final String NEGOTIATING_STATE = "NEGOTIATING";
	private static final String FINISHING_STATE = "FINISHING";
	private boolean isFeasible;
	private AID executionAgent;
	private Component componentNegot;
	private Boolean isNegotiating;

	// agentes já questionados
	private ArrayList<String> queriedAgents;
	private int totalNegotiations;

	// downgrades
	private Float tempAvail;
	private ArrayList<Component> downComps;
	private boolean toDowngrade;

	// properties
	private long delaySearch;
	private int retriesMigration;
	private long timeout_negotiation;

	// "ftdert-reckon"

	@Override
	protected void setup() {
		super.setup();
		getContentManager().registerLanguage(new SLCodec());
		getContentManager().registerLanguage(
				new jade.content.lang.sl.SLCodec(0));
		getContentManager().registerOntology(MobilityOntology.getInstance());

		try {

			log("initializing...");

			init();

			ArrayList<Location> locations = getAMSLocations();
			toLocation = null;
			int i = 0;
			do {
				toLocation = locations.get(i);
				i++;
			} while (isDeviceSetUp(toLocation) && i < locations.size());
			if (!isDeviceSetUp(toLocation)) {
				if (toLocation.equals(here())) {
					negotiate();
				} else {

					// ver ciclicamente se já dá para mover para a máquina
					attemptMoveB = new CyclicBehaviour(this) {

						private int nRetry = 0;

						@Override
						public void action() {
							try {
								if (nRetry >= retriesMigration) {
									myAgent.doDelete();
								}
								log("attempting to move to "
										+ toLocation.toString() + ", ("
										+ System.currentTimeMillis() + ")");
								nRetry++;
								doMove(toLocation);
							} catch (Exception e) {
								log("error moving to " + toLocation.toString());
							}
						}
					};
					attemptMoveB.setBehaviourName("Attempt to move");
					addBehaviour(attemptMoveB);
				}
			} else {
				log("all available hosts were assessed");
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}

	private void init() {
		service = ControllingAgent.service;
		tempAvail = (float) 0;
		queriedAgents = new ArrayList<String>();
		agentComponents = new HashMap<String, AID>();
		downgrading = false;
		toDowngrade = false;
		isNegotiating = false;
		componentsHere = new ArrayList<Component>();
		totalNegotiations = 0;
		delaySearch = Long.parseLong(getArguments()[0].toString());
		retriesMigration = Integer.parseInt(getArguments()[1].toString());
		timeout_negotiation = Long.parseLong(getArguments()[2].toString());
	}

	@Override
	protected void takeDown() {
		super.takeDown();
		try {
			removeBehaviour(attemptMoveB);
		} catch (NullPointerException e) {

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	protected void beforeMove() {
		super.beforeMove();
	}

	@Override
	protected void afterMove() {
		super.afterMove();
		this.takeDown();
		getContentManager().registerLanguage(new SLCodec());
		getContentManager().registerLanguage(
				new jade.content.lang.sl.SLCodec(0));
		getContentManager().registerOntology(MobilityOntology.getInstance());
		log("arriving at " + here().toString() + ", ("
				+ System.currentTimeMillis() + ")");
		if (isDeviceSetUp(here())) {
			doDelete();
		}
		negotiate();
	}

	protected void negotiate() {
		getContentManager().registerLanguage(new SLCodec());
		getContentManager().registerLanguage(
				new jade.content.lang.sl.SLCodec(0));
		getContentManager().registerOntology(MobilityOntology.getInstance());
		try {
			MessageTemplate msgInfMoved = MessageTemplate.and(
					MessageTemplate.MatchPerformative(ACLMessage.INFORM),
					MessageTemplate.MatchContent("moved"));
			SimpleCyclicReceiver updateVars = new SimpleCyclicReceiver(this,
					msgInfMoved) {

				@Override
				protected void handle(ACLMessage msg) {
					// while (downgrading) {
					// System.out.println("downgrading");
					// }

					// 1. atualizar lista de agentes-componentes na máquina
					// (agentComponents)
					// 2. atualizar a lista de agentes para fazer negociar
					// (queriedAgents)
					log("agent " + msg.getSender().getLocalName() + " left");

					synchronized ((Boolean) downgrading) {

						synchronized (agentComponents) {
							String compName = Util.getComponentByAID(
									agentComponents, (AID) msg.getSender());
							agentComponents.remove(compName);
							// vai analisar a nova availability
							synchronized (componentsHere) {
								for (int j = 0; j < componentsHere.size(); j++) {
									if (componentsHere.get(j).getName()
											.equals(compName)) {
										// LIXO
										// log(getComponentsSummary());
										//
										// log(compName
										// + " -> "
										// + componentsHere.get(j)
										// .getCurrentLevel()
										// .getWaste() + " -> ");
										// LIXO
										synchronized (availability) {
											availability += componentsHere
													.get(j).getCurrentLevel()
													.getWaste();

										}
										synchronized (tempAvail) {
											tempAvail += componentsHere.get(j)
													.getCurrentLevel()
													.getWaste();
										}
										componentsHere.remove(j);
									}
								}
								log(getHostSummary());

							}
							// nova availability: atualizar queriedAgents
							synchronized (queriedAgents) {
								ArrayList<AID> executionList = getAMSAgentsByType(
										AGENT_TYPE_EXEC, 9999);
								for (int i = 0; i < executionList.size(); i++) {
									if (queriedAgents.contains(executionList
											.get(i).getLocalName()) // se
																	// estiver
																	// na
																	// queriedList
											&& !agentComponents
													.containsValue(executionList
															.get(i)) // se
																		// não
																		// estiver
																		// nesta
																		// máquina
											&& !executionList
													.get(i)
													.getLocalName()
													.equals(msg.getSender()
															.getLocalName())) { // e
																				// se
																				// não
																				// for
																				// o
																				// que
																				// acabou
																				// de
																				// sair)
										queriedAgents.remove(executionList.get(
												i).getLocalName());
									}
								}
							}
						}
					}
					// ACLMessage msgPerm = createUnicastACLMessage(
					// ACLMessage.INFORM, msg.getSender(),
					// "move permission");
					// send(msgPerm);
					log(getHostSummary() + "\n" + getQoSSummary());

				}

			};
			updateVars.setBehaviourName("Update vars");
			addBehaviour(updateVars);

			log("inpecting at " + here().toString());

			// TODO: Calcular Availability
			Random random = new Random();
			availability = new Float(Util.randomNumber(random, 99, 60));

			// LIXO: TESTES
			// if (this.getLocalName().contains("1")) {
			availability = (float) 95;
			// } else if (this.getLocalName().contains("2")) {
			// availability = (float) 60;
			// }
			// LIXO: TESTES; ate AQUI

			reckonBehaviour = new FSMBehaviour(this);

			OneShotBehaviour starting = new OneShotBehaviour(this) {

				@Override
				public void action() {
					// ((ReckonAgent) myAgent).say("starting");

				}

				@Override
				public int onEnd() {
					return 1;
				}

			};

			ConditionalCyclic searching = new ConditionalCyclic() {

				private int state;

				@Override
				public void onStart() {
					super.onStart();
					state = 0;

					// LIXO
					// if (!queriedAgents.isEmpty()) {
					// log("already queried: " + queriedAgents.toString());
					// }

					this.ended = false;
					log("searching with availability " + availability + "...");
					log(RuntimeInfo.getSummary() + "\n" + getHostSummary()
							+ "\n" + getQoSSummary());

				}

				@Override
				public void action() {
					try {
						// if (availability >= 10) { // 10% de minimo
						ArrayList<Location> locations = getAMSLocations();
						String qAgent = "";
						int i = 0;
						while (i < locations.size() && !this.ended) {
							ArrayList<AID> agents = getAMSAgentsByLocation(
									locations.get(i), AGENT_TYPE_EXEC,
									AGENT_TYPE_CLONE);
							int j = 0;
							while (j < agents.size() && !this.ended) {
								synchronized (queriedAgents) {
									if (!queriedAgents.contains(agents.get(j)
											.getLocalName())
											&& !Util.containsAgentName(agents
													.get(j).getLocalName(),
													agentComponents)) {
										executionAgent = agents.get(j);
										queriedAgents.add(agents.get(j)
												.getLocalName());
										qAgent = agents.get(j).getLocalName();
										log("going to query "
												+ executionAgent.getLocalName());
										totalNegotiations++;
										state = 1;
										this.ended = true;
										break;
									}
								}
								j++;
							}
							i++;
						}
						// }
					} catch (Exception e) {
						e.printStackTrace();
					}

				}

				@Override
				public int onEnd() {
					// say(".state. " + state); // LIXO
					return state;
				}
			};

			OneShotBehaviour delaying = new OneShotBehaviour(this) {

				@Override
				public void action() {
					// ((ReckonAgent) myAgent)
					// .say("delaying the search for 1 sec");
					doWait(delaySearch);
				}

			};

			ACLMessage request = new ACLMessage(ACLMessage.PROPOSE);
			request.setProtocol(FIPANames.InteractionProtocol.FIPA_PROPOSE);

			ProposeInitiator offering = new ProposeInitiator(this, request) {

				private int state;

				private long offerTime;
				private long respTime;

				@Override
				public void onStart() {
					super.onStart();
					state = -1;
				}

				@Override
				protected Vector prepareInitiations(ACLMessage propose) {
					try {
						isNegotiating = true;
						isFeasible = false;
						propose = createUnicastACLMessage(ACLMessage.PROPOSE,
								"Machine available");
						propose.setProtocol(FIPANames.InteractionProtocol.FIPA_PROPOSE);
						// log("LIXO " + queriedAgents.toString());
						log("offering machine to "
								+ executionAgent.getLocalName());
						propose.addReceiver(executionAgent);

						// X ms para timeout
						offerTime = System.currentTimeMillis()
								+ timeout_negotiation;

						propose.setReplyByDate(new Date(offerTime));

						return super.prepareInitiations(propose);
					} catch (NullPointerException e) {
						log(e.getMessage());
						e.printStackTrace();
					} catch (Exception e) {
						log(e.getMessage());
						e.printStackTrace();
					}
					return super.prepareInitiations(propose);
				}

				@Override
				protected void handleAcceptProposal(ACLMessage accept_proposal) {
					try {
						String name = accept_proposal.getContent();
						if (!componentsHere.contains(name)) {

							log("Agent "
									+ accept_proposal.getSender()
											.getLocalName()
									+ " wants to execute " + name);
							componentNegot = (Component) service
									.getComponentByName(name);
							synchronized (tempAvail) {
								tempAvail = availability;
							}
							if ((componentNegot.isFeasibile(availability))) {
								isFeasible = true;
								state = 1;
							} else {
								synchronized ((Boolean) downgrading) {

									// tentar fazer DOWNGRADES
									// se tiver outros componentes
									if (!componentsHere.isEmpty()) {
										downgrading = true;
										log("trying to downgrade to accomodate "
												+ name
												+ " from "
												+ accept_proposal.getSender()
														.getLocalName());
										OneShotBehaviour downgrade = new OneShotBehaviour() {
											@Override
											public void action() {
												int i = 0;
												ArrayList<Component> tmpComps = new ArrayList<Component>(
														(ArrayList<Component>) Util
																.cloneComponentList(componentsHere));
												downComps = new ArrayList<Component>();
												// vai testar o downgrade nos
												// componentes
												// locais para ver se consegue o
												// negociado
												while (!componentNegot
														.isFeasibile(tempAvail)
														&& i < tmpComps.size()) {
													try {
														float oldWaste = tmpComps
																.get(i)
																.getCurrentLevel()
																.getWaste();
														while (tmpComps
																.get(i)
																.downgradeLevel() == false) {
														}
														float newWaste = tmpComps
																.get(i)
																.getCurrentLevel()
																.getWaste();
														float gains = oldWaste
																- newWaste;
														if (Util.containsComponent(
																downComps,
																tmpComps.get(i))) {
															downComps.remove(i);
														}
														downComps
																.add((Component) tmpComps
																		.get(i)
																		.clone());
														tempAvail += gains;
														// log("\nGains = OldWaste - NewWaste\n"
														// + gains
														// + " = "
														// + oldWaste
														// + " - "
														// + newWaste
														// +
														// "\nNew Availability = "
														// + (tempAvail));
													} catch (NotDowngradableException e) {
														log(e.getMessage()
																+ " anymore");
														i++;
													} catch (Exception e) {
														e.printStackTrace();
													}
												}
												if (availability < tempAvail) {
													log("downgraded SLA availability "
															+ tempAvail);
													// conseguiu achar um novo
													// SLA
													// para
													// aceitar
													// o negociado
													if (componentNegot
															.isFeasibile(tempAvail)) {
														log("can achieve new downgraded SLA");
														isFeasible = true;
														state = 1;
														toDowngrade = true;
													} else {
														log("cannot achieve new downgraded SLA");
													}
													// se o exec aceitar a
													// proposta
													// faz
													// o downgrade preparado
												}
												downgrading = false;
											}
										};
										downgrade.setBehaviourName("Downgrade");
										addBehaviour(downgrade);
									}
									if (!isFeasible) {
										log("component "
												+ componentNegot.getName()
												+ " isn't feasible");
									}
								}
							}
						} else {
							state = -1;
						}
					} catch (Throwable e) {
						e.printStackTrace();
					}
				}

				@Override
				protected void handleAllResponses(Vector responses) {
					respTime = System.currentTimeMillis();
					if (respTime > offerTime) {
						state = -1;
						isNegotiating = false;
						queriedAgents.remove(executionAgent.getLocalName());
					}
					super.handleAllResponses(responses);
				}

				@Override
				protected void handleRejectProposal(ACLMessage reject_proposal) {
					queriedAgents.remove(executionAgent.getLocalName());
					log("Agent " + reject_proposal.getSender().getLocalName()
							+ " refused to negotiate");
					isNegotiating = false;
				}

				@Override
				public int onEnd() {
					return state;
				}
			};

			ACLMessage proposalMsg = new ACLMessage(ACLMessage.PROPOSE);

			proposalMsg.setProtocol(FIPANames.InteractionProtocol.FIPA_PROPOSE);

			ProposeInitiator negotiating = new ProposeInitiator(this,
					proposalMsg) {

				private Level proposedLevel;

				private long offerTime;
				private long respTime;

				@Override
				protected Vector prepareInitiations(ACLMessage propose) {
					try {
						propose = createUnicastACLMessage(ACLMessage.PROPOSE,
								null, "");
						propose.setProtocol(FIPANames.InteractionProtocol.FIPA_PROPOSE);
						// say("creating proposal level with availability "
						// + availability);

						proposedLevel = createLevelForProposal(componentNegot
								.getMinimumLevel());

						double localReward = 0;
						double pcpmReward = 0;
						if (componentsHere.isEmpty()) {
							localReward = -1;
							pcpmReward = getPCPMReward(componentsHere);
						} else {
							ArrayList<Component> compsSLA = new ArrayList<Component>();
							Component compNeg = ((Component) service
									.getComponentByName(componentNegot
											.getName()));
							compNeg.setCurrentLevel((Level) proposedLevel
									.clone());
							if (toDowngrade) {
								for (int i = 0; i < componentsHere.size(); i++) {
									if (Util.containsComponent(downComps,
											componentsHere.get(i))) {
										compsSLA.add((Component) Util
												.getComponentByName(
														downComps,
														componentsHere.get(i)
																.getName())
												.clone());
									} else {
										compsSLA.add((Component) componentsHere
												.get(i).clone());
									}
								}
								compsSLA.add(compNeg);
								localReward = getLocalReward(compsSLA);
							} else {
								compsSLA = ((ArrayList<Component>) Util
										.cloneComponentList(componentsHere));
								compsSLA.add(compNeg);
								localReward = getLocalReward(compsSLA);
							}
							pcpmReward = getPCPMReward(compsSLA);
						}
						double globalReward = getGlobalReward(proposedLevel,
								componentNegot.getName());

						// pcpmReward = Parallel Nodes
						// Processing Maximization reward

						Proposal proposal = new Proposal(globalReward,
								localReward, pcpmReward, proposedLevel);
						log("proposing " + proposal.toString() + " to "
								+ executionAgent.getLocalName());

						propose.setContentObject(proposal);

						propose.addReceiver(executionAgent);

						// X ms para timeout
						offerTime = System.currentTimeMillis()
								+ timeout_negotiation;
						propose.setReplyByDate(new Date(offerTime));

						return super.prepareInitiations(propose);
					} catch (IOException e) {
						e.printStackTrace();
					} catch (Exception e) {
						e.printStackTrace();
					}
					return super.prepareInitiations(propose);
				}

				@Override
				protected void handleAcceptProposal(ACLMessage accept_proposal) {
					// MessageTemplate msgArrival = MessageTemplate.and(
					// MessageTemplate.MatchContent("arrival"),
					// MessageTemplate
					// .MatchPerformative(ACLMessage.INFORM));
					// addBehaviour(new SimpleReceiver(msgArrival) {
					// @Override
					// protected void handle(ACLMessage msg) {
					// say(executionAgent.getLocalName()
					// + " has just arrived");
					//
					// }
					// });
					if (toDowngrade) {

						for (int j = 0; j < downComps.size(); j++) {
							for (int k = 0; k < componentsHere.size(); k++) {
								if (downComps
										.get(j)
										.getName()
										.equals(componentsHere.get(k).getName())) {

									try {
										synchronized (availability) {
											float gains = componentsHere.get(k)
													.getCurrentLevel()
													.getWaste()
													- downComps.get(j)
															.getCurrentLevel()
															.getWaste();
											availability += gains;
										}
										componentsHere.get(k).setCurrentLevel(
												(Level) downComps.get(j)
														.getCurrentLevel()
														.clone());

									} catch (CloneNotSupportedException e) {
										e.printStackTrace();
									}
									// informar o agent
									// execution
									// responsável
									// pelo componente
									ACLMessage downgradeMsg = createUnicastACLMessage(
											ACLMessage.INFORM_IF,
											agentComponents.get(componentsHere
													.get(k).getName()),
											downComps.get(j).getCurrentLevel());
									log("informing "
											+ agentComponents.get(
													componentsHere.get(k)
															.getName())
													.getLocalName()
											+ " to downgrade "
											+ componentsHere.get(k).getName()
											+ " level to:"
											+ downComps.get(j)
													.getCurrentLevel()
													.toString());
									send(downgradeMsg);
								}
							}
						}
						// log("(downgraded) Availability: " + availability);
					}

					try {
						componentNegot.setCurrentLevel((Level) proposedLevel
								.clone());
					} catch (CloneNotSupportedException e1) {
						e1.printStackTrace();
					}
					synchronized (availability) {

						availability -= componentNegot.getCurrentLevel()
								.getWaste();
						// log("(new component) Availability: " + availability);
					}
					synchronized (componentsHere) {
						try {
							componentsHere.add((Component) componentNegot
									.clone());
						} catch (CloneNotSupportedException e) {
							e.printStackTrace();
						}
					}
					synchronized (agentComponents) {
						agentComponents.put(componentNegot.getName(),
								executionAgent);
					}

					if (toDowngrade) {

						if (componentsHere.size() > 1) {
							addBehaviour(new OneShotBehaviour() {
								@Override
								public void action() {
									ArrayList<AID> reckonList = getAMSAgentsByType(
											AGENT_TYPE, 9999);
									if (!reckonList.isEmpty()) {
										ArrayList<AID> agentsToReset = new ArrayList<AID>();
										for (int i = 0; i < downComps.size(); i++) {
											agentsToReset.add(agentComponents
													.get(downComps.get(i)
															.getName()));
										}
										ACLMessage msg = createMulticastACLMessage(
												ACLMessage.PROPAGATE,
												reckonList,
												(java.io.Serializable) agentsToReset);
										send(msg);
									}
								}
							});
						}
					}
					isNegotiating = false;
					log("Agent " + accept_proposal.getSender().getLocalName()
							+ " agreed proposal, because:\n"
							+ accept_proposal.getContent());
				}

				@Override
				protected void handleRejectProposal(ACLMessage reject_proposal) {
					if (!queriedAgents.contains(executionAgent.getLocalName())) {
						queriedAgents.add(executionAgent.getLocalName());
					}
					isNegotiating = false;
					log("Agent " + reject_proposal.getSender().getLocalName()
							+ " refused proposal, because:\n"
							+ reject_proposal.getContent());
				}

				@Override
				protected void handleNotUnderstood(ACLMessage notUnderstood) {
					super.handleNotUnderstood(notUnderstood);
					log("Agent " + notUnderstood.getSender().getLocalName()
							+ " didn't understand, because:\n"
							+ notUnderstood.getContent());
				}

				@Override
				protected void handleAllResponses(Vector responses) {
					respTime = System.currentTimeMillis();
					if (respTime > offerTime) {
						isNegotiating = false;
					}
					super.handleAllResponses(responses);
				}

				@Override
				public int onEnd() {
					toDowngrade = false;
					isNegotiating = false;
					log("terminating negotiation with "
							+ executionAgent.getLocalName());
					executionAgent = null;
					return 1;
				}
			};

			OneShotBehaviour finishing = new OneShotBehaviour(this) {

				@Override
				public void action() {
					log("finishing");
				}

			};

			reckonBehaviour.registerFirstState(starting, STARTING_STATE);
			reckonBehaviour.registerState(searching, SEARCHING_STATE);
			reckonBehaviour.registerState(delaying, DELAYING_STATE);
			reckonBehaviour.registerState(offering, OFFERING_STATE);
			reckonBehaviour.registerState(negotiating, NEGOTIATING_STATE);
			reckonBehaviour.registerLastState(finishing, FINISHING_STATE);

			reckonBehaviour.registerTransition(STARTING_STATE, SEARCHING_STATE,
					1);
			reckonBehaviour.registerTransition(SEARCHING_STATE, DELAYING_STATE,
					0);
			reckonBehaviour.registerTransition(DELAYING_STATE, SEARCHING_STATE,
					0);
			reckonBehaviour.registerTransition(SEARCHING_STATE, OFFERING_STATE,
					1);
			reckonBehaviour.registerTransition(OFFERING_STATE,
					NEGOTIATING_STATE, 1);

			reckonBehaviour.registerTransition(NEGOTIATING_STATE,
					SEARCHING_STATE, 1, new String[] { SEARCHING_STATE,
							OFFERING_STATE, NEGOTIATING_STATE });

			reckonBehaviour.registerTransition(OFFERING_STATE, SEARCHING_STATE,
					-1, new String[] { SEARCHING_STATE, OFFERING_STATE });

			reckonBehaviour.registerTransition(NEGOTIATING_STATE,
					FINISHING_STATE, 0);

			addBehaviour(reckonBehaviour);

			// receber mudanças em outros agentes para atualizar queriedAgents
			MessageTemplate msgTmpQuery = MessageTemplate
					.MatchPerformative(ACLMessage.PROPAGATE);
			SimpleCyclicReceiver updateQueried = new SimpleCyclicReceiver(this,
					msgTmpQuery) {

				@Override
				protected void handle(ACLMessage msg) {
					try {
						ArrayList<AID> agentsToReset = (ArrayList<AID>) msg
								.getContentObject();

						for (int i = 0; i < queriedAgents.size(); i++) {
							for (int j = 0; j < agentsToReset.size(); j++) {
								if (agentsToReset.get(j).getLocalName()
								// err
										.equals(queriedAgents.get(i))
										&& !Util.containsAgentName(
												agentsToReset.get(j)
														.getLocalName(),
												agentComponents)) {
									log("removing " + queriedAgents.get(i));
									queriedAgents.remove(i);
								}
							}
						}
					} catch (NullPointerException e) {
						e.printStackTrace();
					} catch (UnreadableException e) {
						e.printStackTrace();
					} catch (IndexOutOfBoundsException e) {
						e.printStackTrace();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			};
			updateQueried.setBehaviourName("Update queried agents");
			addBehaviour(updateQueried);
			CyclicBehaviour upgrade = new CyclicBehaviour(this) {

				private int i = 0;

				@Override
				public void action() {
					try {

						synchronized (isNegotiating) {
							if (!isNegotiating) {

								synchronized (componentsHere) {
									synchronized ((Boolean) downgrading) {
										if (!componentsHere.isEmpty()
												&& !downgrading) {
											// if (i == componentsHere.size()) {
											i = 0;
											// }
											ArrayList<Component> tmpComps = new ArrayList<Component>(
													(ArrayList<Component>) Util
															.cloneComponentList(componentsHere));
											ArrayList<Component> upComps = new ArrayList<Component>();
											// vai testar o upgrade nos
											// componentes
											synchronized (availability) {
												float avail = availability;

												// log("trying to upgrade components with "
												// + avail
												// + " of availability"); // ***
												// log("size: " +
												// tmpComps.size());
												if (tmpComps != null) {
													while (i < tmpComps.size()) {
														try {
															float oldWaste = tmpComps
																	.get(i)
																	.getCurrentLevel()
																	.getWaste();
															while (tmpComps
																	.get(i)
																	.upgradeLevel() == false) {
															}
															float newWaste = tmpComps
																	.get(i)
																	.getCurrentLevel()
																	.getWaste();
															// LIXO
															// System.out
															// .println("Availability = "
															// + avail + " | " +
															// i
															// + "º waste = "
															// + newWaste);
															float losses = newWaste
																	- oldWaste;
															// log("\nLosses = NewWaste - OldWaste\n"
															// + losses
															// + " = "
															// + newWaste
															// + " - "
															// + oldWaste); //
															// log("i: " + i);
															// ***
															// se
															// as
															// perdas
															// nao
															// baixarem
															// 0%
															// de
															// availability
															// (minimo)
															if (tmpComps
																	.get(i)
																	.isFeasibile(
																			avail)) {
																if (Util.containsComponent(
																		upComps,
																		tmpComps.get(i))) {
																	upComps.remove(i);
																}
																log("new upgrade to "
																		+ tmpComps
																				.get(i)
																				.getName());
																upComps.add(tmpComps
																		.get(i));
																avail -= losses;
																// log("\nLosses = NewWaste - OldWaste\n"
																// + losses
																// + " = "
																// +
																// newWaste
																// + " - "
																// +
																// oldWaste
																// +
																// "\nNew Availability = "
																// +
																// (avail));

															} else {
																// log("not enough availability to upgrade, rolling back...");
																// // ***
																while (tmpComps
																		.get(i)
																		.rollbackUpgrade() == false) {
																}
															}
														} catch (NotUpgradableException e) {
															// log(e.getMessage()
															// + " anymore");
															i++;
														} catch (NullPointerException e) {
															if (tmpComps
																	.get(i)
																	.getCurrentLevel() == null) {
																i++;
															}
														} catch (Exception e) {
															e.printStackTrace();
														}
													}
												}

												// say("upgraded SLA availability "
												// +
												// avail);
												// conseguiu achar um novo SLA
												// para
												// aceitar
												// o negociado

												if (!upComps.isEmpty()) {
													log("achieved new upgraded SLA");
													isFeasible = true;
													float dif = availability
															- avail;
													synchronized (tempAvail) {
														tempAvail -= dif;
													}
													synchronized (availability) {
														availability -= dif;
													}
													// log("(upgraded) Availability: "
													// + availability);
													for (int j = 0; j < upComps
															.size(); j++) {
														for (int k = 0; k < componentsHere
																.size(); k++) {
															if (upComps
																	.get(j)
																	.getName()
																	.equals(componentsHere
																			.get(k)
																			.getName())) {
																componentsHere
																		.get(k)
																		.setCurrentLevel(
																				(Level) upComps
																						.get(j)
																						.getCurrentLevel()
																						.clone());
																componentsHere
																		.get(k)
																		.resetUpDownPointers();
																// informar o
																// agent
																// execution
																// responsável
																// pelo
																// componente
																ACLMessage upgradeMsg = createUnicastACLMessage(
																		ACLMessage.INFORM_IF,
																		agentComponents
																				.get(componentsHere
																						.get(k)
																						.getName()),
																		upComps.get(
																				j)
																				.getCurrentLevel());
																log("informing "
																		+ agentComponents
																				.get(componentsHere
																						.get(k)
																						.getName())
																				.getLocalName()
																		+ " to upgrade "
																		+ componentsHere
																				.get(k)
																				.getName()
																		+ " level to:"
																		+ upComps
																				.get(j)
																				.getCurrentLevel()
																				.toString());
																send(upgradeMsg);
															}
														}
													}
												}
											}
										}
									}
								}
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}

				}
			};
			upgrade.setBehaviourName("Upgrade");
			addBehaviour(upgrade);

			MessageTemplate msgRewards = MessageTemplate.and(
					MessageTemplate.MatchContent("rewards"),
					MessageTemplate.MatchPerformative(ACLMessage.CONFIRM));

			SimpleCyclicReceiver requestUpdateRewards = new SimpleCyclicReceiver(
					this, msgRewards) {

				@Override
				protected void handle(ACLMessage msg) {
					ACLMessage resp = msg.createReply();
					try {
						Proposal prop = new Proposal(0, 0, 0, null);
						synchronized (componentsHere) {

							// if (toDowngrade) {
							// prop.setLocalReward(getLocalReward(downComps));
							// String componentName = Util
							// .getComponentNameByAID(agentComponents,
							// msg.getSender());
							// Component comp = Util.getComponentByName(
							// downComps, componentName);
							// prop.setGlobalReward(getGlobalReward(
							// comp.getCurrentLevel(), comp.getName()));
							// prop.setPCPMReward(getPCPMReward(downComps));
							// // log("PIFT 1:  " + comp.getName() + " \n"
							// // + comp.getCurrentLevel());
							// } else if (!toDowngrade) {
							prop.setLocalReward(getLocalReward(componentsHere));
							String componentName = Util.getComponentNameByAID(
									agentComponents, msg.getSender());
							Component comp = Util.getComponentByName(
									componentsHere, componentName);
							prop.setGlobalReward(getGlobalReward(
									comp.getCurrentLevel(), comp.getName()));
							prop.setPCPMReward(getPCPMReward(componentsHere));

							// log("PIFT 2: " + comp.getName() + " \n"
							// + comp.getCurrentLevel());
							// }

						}
						resp.setContentObject(prop);
						send(resp);
					} catch (IOException e) {
						e.printStackTrace();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

			};
			requestUpdateRewards.setBehaviourName("Send rewards updates");
			addBehaviour(requestUpdateRewards);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private boolean isDeviceSetUp(Location loc) {
		if (loc != null) {
			try {
				ArrayList<AID> aidList = getAMSAgentsByLocation(loc,
						ReckonAgent.AGENT_TYPE, "");
				if (!aidList.isEmpty()) {
					return true;
				}
				// tbf = new ThreadedBehaviourFactory();
				return false;
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			return true;
		} else {
			return true;
		}
	}

	private double getGlobalReward(Level proposedLevel, String name) {
		try {
			Component comp = service.getComponentByName(name);
			return 1 / proposedLevel.distanceTo(comp.getPreferredLevel());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return (Double) null;
	}

	private Level createLevelForProposal(Level template) {
		Level proposedLevel = new Level(
				(Level) template.createLevelByType(" - Proposed"));
		float avail = tempAvail;
		boolean upgradeFeasible = true;
		while (upgradeFeasible) {
			try {
				while (proposedLevel.upgrade() == false) {
				}
				if ((avail - proposedLevel.getWaste()) <= 10) {
					upgradeFeasible = false;
					while (proposedLevel.rollbackUpgrade() == false) {
					}
				}
			} catch (NotUpgradableException e) {
				log(e.getMessage() + " anymore");
				break;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return proposedLevel;
	}

	// TODO: local reward - estou a fazer por distanceTo (preferredLevel),i.e.,
	// distancia do nivel atual para o nível preferido, e não com a formula
	// original
	// quanto menor a localReward melhor é, pois menor é a distância relativa
	// para o nível preferido

	private double getLocalReward(ArrayList<Component> components) {
		if (components.isEmpty()) {
			return Double.MAX_VALUE;
		}
		double localReward = 0;
		// System.out.println("Local reward based on " + components);
		for (int i = 0; i < components.size(); i++) {
			localReward += components.get(i).getDistanceToPreferred();
		}
		return (1 / localReward) / components.size();
	}

	private double getLocalQuality(ArrayList<Component> components) {
		if (components.isEmpty()) {
			return 0;
		}
		double localReward = 0;
		// System.out.println("Local quality based on " + components);
		for (int i = 0; i < components.size(); i++) {
			localReward += components.get(i).getDistanceToPreferred();
		}
		return ((double) ((1 / (localReward + 1)) / components.size()));
	}

	private double getPCPMReward(ArrayList<Component> components) {
		try {
			return ((double) 1 / service.sumSignificanceDegreeOf(service
					.getParallelComponents(componentNegot, components)));
		} catch (ArithmeticException e) {
			return ((double) 0.0);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0.0;
	}

	private String getHostSummary() {
		return "Host Info\n\tAgent " + this.getAID().getLocalName() + " @"
				+ toLocation.getName() + "\n\tComponents: "
				+ componentsHere.toString() + "\n\tTotal components: "
				+ componentsHere.size() + "\n\tAgent-components: "
				+ agentComponents.toString() + "\n\tAvailability: "
				+ availability + "\n\tQueried agents: " + queriedAgents
				+ "\n\tNegotiations: " + totalNegotiations + "\n\t"
				+ getComponentsSummary();

	}

	private String getQoSSummary() {
		return "QoS Info\n\t" + "Local reward: "
				+ getLocalReward(componentsHere) + "\n\tLocal quality: "
				+ getLocalQuality(componentsHere);
	}

	private String getComponentsSummary() {
		return "Components Info\n\t" + "Total waste: " + getTotalWaste()
				+ "\n\tComponents waste: " + getComponentsWaste();
	}

	private String getComponentsWaste() {
		String s = "";
		for (int i = 0; i < componentsHere.size(); i++) {
			s += componentsHere.get(i).getName() + " = "
					+ componentsHere.get(i).getCurrentLevel().getWaste() + "\t";
		}
		return s;
	}

	private float getTotalWaste() {
		float totalWaste = 0;
		for (int i = 0; i < componentsHere.size(); i++) {
			totalWaste += componentsHere.get(i).getCurrentLevel().getWaste();
		}
		return totalWaste;
	}
}
