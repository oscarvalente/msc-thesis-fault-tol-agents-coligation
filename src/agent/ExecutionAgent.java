package agent;

import jade.content.lang.sl.SLCodec;
import jade.core.AID;
import jade.core.Agent;
import jade.core.Location;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.ParallelBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.mobility.MobilityOntology;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.proto.ProposeResponder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import qos.Level;
import qos.Proposal;
import qos.Value;
import service.Component;
import service.Service;

public class ExecutionAgent extends FTAgent {

	public static final String AGENT_TYPE = "EXECUTION";
	public static final String AGENT_TYPE_RECKON = "RECKON";

	private Object[] arguments;
	private AID partnerAID;
	private int role;
	private DFAgentDescription dfd;

	protected static int cidCnt = 0;
	private String cidBase;
	private boolean executed;

	// private CyclicBehaviour attemptMoveB;

	private Service subService;
	private Component myComponent;
	private HashMap<String, String> input; // meter no String -> Serializable
	private String typeArg;
	private int nInput;
	private ArrayList<Component> succComponents;
	private String outputContent;

	private int nExecutions;

	private static final String STARTING_STATE = "STARTING";
	private static final String INPUTING_STATE = "INPUTING";
	private static final String EXECUTING_STATE = "EXECUTING";
	private static final String OUTPUTING_STATE = "OUTPUTING";
	private static final String FINISHING_STATE = "FINISHING";

	// negotiate
	private Location locationTo;
	private AID reckonAID;
	private AID exReckonAID;
	private boolean agreedProposal;

	// para fazer load a classe noutro host
	private Proposal currentProposal;

	String cid;

	// migração do secundário
	private long timeoutTime;

	// properties
	private long frequencyHB;
	private long timeoutHB;
	private long timeout_negotiation;
	private long delaySearchOutput;
	private int retriesMigration;

	@Override
	protected void setup() {
		super.setup();

		try {

			getContentManager().registerLanguage(new SLCodec());
			getContentManager().registerLanguage(
					new jade.content.lang.sl.SLCodec(0));
			getContentManager()
					.registerOntology(MobilityOntology.getInstance());

			log("initializing...");

			init();

			ParallelNegotiating parallelNegotB = new ParallelNegotiating(this);
			parallelNegotB.setBehaviourName("Parallel Negotiation at init");
			addBehaviour(parallelNegotB);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void beforeMove() {
		// informar o reckon que vai sair desse container
		if (getExReckonAID() != null) {
			ACLMessage msg = createUnicastACLMessage(ACLMessage.INFORM,
					getExReckonAID(), "moved");
			send(msg);
			log("notified " + getExReckonAID().getLocalName() + " of migration");
			// MessageTemplate msgTempMovePerm = MessageTemplate.and(
			// MessageTemplate.MatchPerformative(ACLMessage.INFORM),
			// MessageTemplate.MatchContent("move permission"));
			// ACLMessage msgPerm = blockingReceive(msgTempMovePerm);
		}
		log("moving to " + getLocationTo().toString() + ", ("
				+ System.currentTimeMillis() + ")");
	}

	@Override
	protected void afterMove() {
		super.afterMove();
		takeDown();
		log("arriving at " + here().toString() + ", ("
				+ System.currentTimeMillis() + ")");
		getContentManager().registerLanguage(new SLCodec());
		getContentManager().registerLanguage(
				new jade.content.lang.sl.SLCodec(0));
		getContentManager().registerOntology(MobilityOntology.getInstance());

		// informar o clone para ele se mover com ele
		if (getPartnerAID() != null) {
			addBehaviour(new OneShotBehaviour() {
				@Override
				public void action() {

					ACLMessage informMove = createUnicastACLMessage(
							ACLMessage.INFORM, "move");
					informMove.addReceiver(getPartnerAID());
					send(informMove);
				}
			});
		}

		// setUp(); // se quiser executar depois de migrar

		// send(createUnicastACLMessage(ACLMessage.INFORM, reckonAID,
		// "arrival"));

	}

	@Override
	protected void beforeClone() {
		super.beforeClone();
		takeDown();
		arguments[0] = (AID) getAID();
	}

	@Override
	protected void afterClone() {
		super.afterClone();
		getContentManager().registerLanguage(new SLCodec());
		getContentManager().registerLanguage(
				new jade.content.lang.sl.SLCodec(0));
		getContentManager().registerOntology(MobilityOntology.getInstance());
		partnerAID = (AID) arguments[0];
		log("i was cloned by master " + partnerAID.getLocalName().toString());
		role = 2;
		// registar o serviço que diz o componente
		dfd = new DFAgentDescription();
		dfd.setName(this.getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType(myComponent.getName());
		sd.setName(this.getName());
		dfd.addServices(sd);
		register(dfd);
		// receber ordem para migrar
		addBehaviour(new ConditionalCyclic() {

			@Override
			public void action() {
				MessageTemplate msgTempMove = MessageTemplate.and(
						MessageTemplate.MatchPerformative(ACLMessage.INFORM),
						MessageTemplate.MatchContent("move"));
				SimpleReceiver receiveMove = new SimpleReceiver(msgTempMove) {

					@Override
					protected void handle(ACLMessage msg) {
						takeDown();
						Location toOriginalLocation = getLocationByAID(partnerAID);

						log("moving to original's location "
								+ toOriginalLocation.toString() + ", ("
								+ System.currentTimeMillis() + ")");
						doMove(toOriginalLocation);
						ended = true;
					}

				};
				addBehaviour(receiveMove);
			}
		});
	}

	@Override
	public void takeDown() {
		super.takeDown();
		try {
			// removeBehaviour(attemptMoveB);
			agreedProposal = false;
		} catch (NullPointerException e) {

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void init() {
		this.setSubService(ControllingAgent.service
				.getSubServiceByComponentName(this.getArguments()[0].toString()));
		role = 1;
		nExecutions = 0;
		cid = genCID();
		executed = false;
		myComponent = getSubService().getComponentByName(
				this.getArguments()[0].toString());
		agreedProposal = false;
		if (getArguments().length > 1) {
			typeArg = getArguments()[1].toString();
			if (typeArg.equals("-input")) {
				if (getArguments().length > 2) {
					nInput = Integer.parseInt(getArguments()[2].toString());
				}
			}
		}
		frequencyHB = Long.parseLong(getArguments()[3].toString());
		timeoutHB = Long.parseLong(getArguments()[4].toString());
		timeout_negotiation = Long.parseLong(getArguments()[5].toString());
		delaySearchOutput = Long.parseLong(getArguments()[6].toString());
		retriesMigration = Integer.parseInt(getArguments()[7].toString());
		currentProposal = new Proposal(-1, -1, -1, null);
		succComponents = ControllingAgent.service
				.getSuccessorComponents(myComponent);
		dfd = new DFAgentDescription();
		dfd.setName(this.getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType(myComponent.getName());
		sd.setName(this.getName());
		dfd.addServices(sd);
		register(dfd);
	}

	public void setUp() {

		arguments = new Object[2];

		SequentialBehaviour sq1 = new SequentialBehaviour(this);
		if (partnerAID == null) {
			takeDown();
			log("cloning...");
			sq1.addSubBehaviour(new OneShotBehaviour(this) {

				@Override
				public void action() {
					partnerAID = new AID(getLocalName() + "-clone",
							AID.ISLOCALNAME);
					doClone(here(), getLocalName() + "-clone");
				}
			});
		}

		// receber upgrades e downgrades
		MessageTemplate msgTempDownUp = MessageTemplate
				.MatchPerformative(ACLMessage.INFORM_IF);

		SimpleCyclicReceiver downUpReceiver = new SimpleCyclicReceiver(this,
				msgTempDownUp) {

			protected void handle(ACLMessage msg) {
				try {
					// say("Nivel = " +
					// msg.getContent());
					Level newLevel = (Level) msg.getContentObject();
					myComponent.setCurrentLevel((Level) newLevel.clone());
					currentProposal.setLevel((Level) newLevel.clone());
					log("Changed level to:"
							+ myComponent.getCurrentLevel().toString());
					ACLMessage cloneMsg = createUnicastACLMessage(
							ACLMessage.PROPAGATE, getPartnerAID(), newLevel);
					send(cloneMsg);
				} catch (UnreadableException e) {
					log("unable to read level changing message!");
				} catch (NullPointerException e) {
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		downUpReceiver.setBehaviourName("Receive downgrades/upgrades");
		addBehaviour(downUpReceiver);

		// ------------------------------------------------------------

		// depois vai estar a escuta de novas propostas, a enviar/receber
		// heartbeats e a executar o componente
		sq1.addSubBehaviour(new OneShotBehaviour() {

			@Override
			public void action() {
				log("starting activity");
				// execução

				// mandar heartbeats um ao outro
				MessageTemplate msgTemp = MessageTemplate.and(MessageTemplate
						.and(MessageTemplate
								.MatchPerformative(ACLMessage.INFORM),
								MessageTemplate.MatchConversationId(cid)),
						MessageTemplate.MatchSender(partnerAID));

				if (!executed) {
					addBehaviour(new ParallelSendReceive(myAgent, frequencyHB,
							timeoutHB, msgTemp, cid));
				}

				executed = true;

				// FSMBehaviour

				FSMBehaviour execute = new FSMBehaviour();

				OneShotBehaviour starting = new OneShotBehaviour() {

					@Override
					public void action() {
						// log("starting");
					}

					@Override
					public int onEnd() {
						return 1;
					}

				};

				// receber o input de outro agente

				SimpleBehaviour inputB = new SimpleBehaviour() {

					private int state = 0;

					@Override
					public void onStart() {
						super.onStart();
						this.state = 0;
					}

					@Override
					public void action() {
						if (state == 0) {
							if (getLocalName().equals("EXECUTION1")) {
								log("BEGIN " + nExecutions + " -> "
										+ System.currentTimeMillis());
							}
							state = 2;
							outputContent = new String();
							if (typeArg != null) {
								if (typeArg.equals("-input")) {
									// INPUT
									// log("attempting to receive input...");
									input = new HashMap<String, String>();

									MessageTemplate msgTempInput = MessageTemplate
											.MatchPerformative(ACLMessage.SUBSCRIBE);

									ConditionalCyclicReceiver receiveInput = new ConditionalCyclicReceiver(
											msgTempInput, nInput) {

										protected void handle(
												final ACLMessage msg) {
											try {
												MessageTemplate msgTempWhatComp = MessageTemplate
														.MatchPerformative(ACLMessage.INFORM_REF);

												SimpleReceiver receiveComponentFrom = new SimpleReceiver(
														msgTempWhatComp) {
													@Override
													protected void handle(
															ACLMessage msgComp) {
														try {

															String compName = msgComp
																	.getContent();
															log("acknowledge input of component "
																	+ compName);
															outputContent += msg
																	.getContent();
															log("input '"
																	+ msg.getContent()
																	+ "' - component "
																	+ compName
																	+ ", from "
																	+ msg.getSender()
																			.getLocalName());

														} catch (Exception e) {
															e.printStackTrace();
														}
													}

												};
												addBehaviour(receiveComponentFrom);

											} catch (Exception e) {
												e.printStackTrace();
											}
										}

										@Override
										public int onEnd() {
											state = 1;
											return super.onEnd();
										}

									};
									receiveInput
											.setBehaviourName("conditional receiveInput");
									addBehaviour(receiveInput);
								} else {
									state = 1;
								}
							}
						}

					}

					@Override
					public boolean done() {
						if (state != 1) {
							return false;
						} else {
							return true;
						}
					}

					@Override
					public int onEnd() {
						return state;
					}

				};

				OneShotBehaviour executeB = new OneShotBehaviour() {

					private int state = 0;

					@Override
					public void action() {
						// EXECUTION
						log("executing component " + myComponent.getName()
								+ " with role "
								+ ((ExecutionAgent) myAgent).getRole());

						// ******************** CÓDIGO DE EXECUÇÃO
						// ********************

						Integer charPerSec = Integer
								.parseInt((String) myComponent
										.getCurrentLevel()
										.getDimension(0)
										.getCurrentValueByAttributeName(
												"Char Generation p/ sec")
										.getValue());
						for (int i = 0; i < (int) charPerSec; i++) {
							outputContent += "a";
						}

						// ******************** CÓDIGO DE EXECUÇÃO
						// ********************

						nExecutions++;
						state = 1;

					}

					@Override
					public int onEnd() {
						return state;
					}
				};

				// executar o componente em paralelo com
				// receber upgrades/downgrades

				// só ñ entra se for o componente final
				SimpleBehaviour outputB = new SimpleBehaviour() {

					private int state = 0;

					@Override
					public void onStart() {
						super.onStart();
						state = 0;

					}

					@Override
					public void action() {
						// OUTPUT
						if (state == 0) {
							state = -1;
							if (subService.outDegreeOf(myComponent) > 0) {

								if (!succComponents.isEmpty()) {
									log("attempting to send output...");
									addBehaviour(new SimpleBehaviour() {

										HashMap<String, Boolean> compOutput;
										ArrayList<AID> aids;
										int i;

										@Override
										public void onStart() {
											super.onStart();
											i = 0;

											compOutput = new HashMap<String, Boolean>();
											for (int j = 0; j < succComponents
													.size(); j++) {
												compOutput.put(succComponents
														.get(j).getName(),
														Boolean.FALSE);
											}
										}

										@Override
										public void action() {
											// tentar pôr o role 1 a mandar
											// para o role 1
											if (compOutput.get(succComponents
													.get(i).getName()) == Boolean.FALSE) {
												aids = searchDFByComponent(succComponents
														.get(i).getName());
												// log("searching for agents with "
												// +
												// succComponents.get(i).getName()
												// +
												// "...");
												if (!aids.isEmpty()) {
													try {
														// LIXO
														// say("Nr agents with component "
														// +
														// succComponents.get(i).getName()
														// + " = " +
														// aids.size());
														for (int j = 0; j < aids
																.size(); j++) {
															// se for o
															// original
															// manda
															// pro original
															// TODO: mudar
															if (!myAgent
																	.getLocalName()
																	.contains(
																			"-clone")
																	&& !aids.get(
																			j)
																			.getName()
																			.contains(
																					"-clone")) {
																ACLMessage outputMsg = createUnicastACLMessage(
																		ACLMessage.SUBSCRIBE,
																		aids.get(j),
																		outputContent);
																send(outputMsg);
																ACLMessage whatComponentMsg = createUnicastACLMessage(
																		ACLMessage.INFORM_REF,
																		aids.get(j),
																		myComponent
																				.getName());
																send(whatComponentMsg);
																compOutput
																		.put(succComponents
																				.get(i)
																				.getName(),
																				Boolean.TRUE);
																log("sending output to "
																		+ aids.get(
																				j)
																				.getLocalName());
															}
															// se for o
															// clone manda
															// pro
															// clone TODO:
															// mudar
															else if (myAgent
																	.getLocalName()
																	.contains(
																			"-clone")
																	&& aids.get(
																			j)
																			.getName()
																			.contains(
																					"-clone")) {
																ACLMessage outputMsg = createUnicastACLMessage(
																		ACLMessage.SUBSCRIBE,
																		aids.get(j),
																		outputContent);

																send(outputMsg);
																ACLMessage whatComponentMsg = createUnicastACLMessage(
																		ACLMessage.INFORM_REF,
																		aids.get(j),
																		myComponent
																				.getName());
																send(whatComponentMsg);
																compOutput
																		.put(succComponents
																				.get(i)
																				.getName(),
																				Boolean.TRUE);
																log("sending output to "
																		+ aids.get(
																				j)
																				.getLocalName());
															}
														}
													} catch (NullPointerException e) {
														e.printStackTrace();
													} catch (Exception e) {
														e.printStackTrace();
													}
												} else {
													log("didn't find agents with component "
															+ succComponents
																	.get(i)
																	.getName());
													doWait(delaySearchOutput);
												}
											}
											// se há algum por enviar
											// faz reset ao
											// counter
											if (compOutput
													.containsValue(Boolean.FALSE)) {
												i++;
												if (i >= (succComponents.size())) {
													i = 0;
												}
											} else {
												log("output complete");
												state = 1;
											}
										}

										@Override
										public boolean done() {
											if (state != 1) {
												return false;
											} else {
												return true;
											}
										}
									});
								}
							} else {
								state = 1;
								if (getLocalName().equals("EXECUTION5")) {
									log("END " + nExecutions + " -> "
											+ System.currentTimeMillis());
								}
								log("final output:\n" + outputContent);
								log("service execution complete");

							}
						}
					}

					@Override
					public boolean done() {
						if (state != 1) {
							return false;
						} else {
							return true;
						}
					}

					@Override
					public int onEnd() {
						return state;
					}

				};

				OneShotBehaviour finishing = new OneShotBehaviour() {

					@Override
					public void action() {
						// log("finishing");
					}

				};

				execute.registerFirstState(starting, STARTING_STATE);
				execute.registerState(inputB, INPUTING_STATE);
				execute.registerState(executeB, EXECUTING_STATE);
				execute.registerState(outputB, OUTPUTING_STATE);
				execute.registerLastState(finishing, FINISHING_STATE);

				execute.registerTransition(STARTING_STATE, INPUTING_STATE, 1);
				execute.registerTransition(INPUTING_STATE, EXECUTING_STATE, 1);
				execute.registerTransition(EXECUTING_STATE, OUTPUTING_STATE, 1);
				execute.registerTransition(OUTPUTING_STATE, INPUTING_STATE, 1,
						new String[] { INPUTING_STATE, EXECUTING_STATE,
								OUTPUTING_STATE });

				execute.registerTransition(OUTPUTING_STATE, FINISHING_STATE, 0);

				addBehaviour(execute);
			}
		});

		addBehaviour(sq1);
	}

	public AID getPartnerAID() {
		return partnerAID;
	}

	public int getRole() {
		return this.role;
	}

	public void setRole(int role) {
		this.role = role;
	}

	private String genCID() {
		if (cidBase == null) {
			cidBase = getLocalName() + hashCode() + System.currentTimeMillis()
					% 10000 + "_";
		}
		return cidBase + (cidCnt++);
	}

	public Service getSubService() {
		return this.subService;
	}

	public void setSubService(Service subService) {
		this.subService = subService;
	}

	public AID getReckonAID() {
		return reckonAID;
	}

	public void setReckonAID(AID reckonAID) {
		this.reckonAID = reckonAID;
	}

	public AID getExReckonAID() {
		return exReckonAID;
	}

	public void setExReckonAID(AID exReckonAID) {
		this.exReckonAID = exReckonAID;
	}

	public Component getMyComponent() {
		return myComponent;
	}

	public boolean getAgreedProposal() {
		return this.agreedProposal;
	}

	public void setAgreedProposal(boolean agreedProposal) {
		this.agreedProposal = agreedProposal;
	}

	public Proposal getCurrentProposal() {
		return this.currentProposal;
	}

	public void setCurrentProposal(Proposal proposal) {
		this.currentProposal = proposal;
	}

	public Location getLocationTo() {
		return this.locationTo;
	}

	public void setLocationTo(Location locationTo) {
		this.locationTo = locationTo;
	}

	public class ParallelNegotiating extends ParallelBehaviour {
		public ParallelNegotiating(final Agent ag) {
			super(ag, ParallelBehaviour.WHEN_ALL);

			MessageTemplate msgTempListen = ProposeResponder
					.createMessageTemplate(FIPANames.InteractionProtocol.FIPA_PROPOSE);

			// ouvir Ofertas e responder

			this.addSubBehaviour(new ProposeResponder(ag, msgTempListen) {

				private int state = -1;

				private long offerTime = -1;

				private boolean negotiating = false;

				@Override
				public void onStart() {
					super.onStart();
					log("listening...");
				}

				@Override
				protected ACLMessage prepareResponse(ACLMessage propose)
						throws NotUnderstoodException, RefuseException {
					ACLMessage reply = propose.createReply();
					AID reckonAID = propose.getSender();
					if (negotiating
							&& (offerTime != -1 && System.currentTimeMillis() > offerTime
									+ timeout_negotiation)) {
						// X ms para dar timeout à não continuação de um
						// reckon (e.g. porque nao conseguiu downgrade)
						log("timeout in last negotiation");
						negotiating = false;
					}
					if (propose.getContent().equals("Machine available")) {
						if ((getReckonAID() == null || !getReckonAID().equals(
								reckonAID))
								&& !negotiating) {
							try {
								offerTime = System.currentTimeMillis();

								log("machine available notified by "
										+ reckonAID.getLocalName());
								// vai buscar o mini-grafo com componente e
								// nós
								// sucessores
								// vai ver se "tem recursos" consegue
								// executar
								// component
								reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
								String content = getMyComponent().getName();
								reply.setContent(content);
								state = 1;
								negotiating = true;
								return reply;
							} catch (Exception e) {
								log("error at receiving machine available");
							}
						} else {
							log("reckonAID atual = " + getReckonAID()
									+ " Sender = " + reckonAID
									+ "Negotiating = " + negotiating);
						}

					} else if (negotiating) {
						try {

							setAgreedProposal(false);
							Serializable obj = (Serializable) propose
									.getContentObject();
							Proposal proposal = (Proposal) obj;
							log("received proposal "
									+ proposal.getLocalReward() + "|"
									+ proposal.getGlobalReward() + "|"
									+ proposal.getPCPMReward() + " from "
									+ propose.getSender().getLocalName());
							// ((ExecutionAgent) ag).log("current rewards "
							// + currentProposal.toString());
							double localReward = proposal.getLocalReward();
							double globalReward = proposal.getGlobalReward();
							double pcpmReward = proposal.getPCPMReward();
							String explanation = "";

							if (getReckonAID() != null) {
								// pedir localReward atualizada
								ACLMessage msg = createUnicastACLMessage(
										ACLMessage.CONFIRM, getReckonAID(),
										"rewards");
								send(msg);
								MessageTemplate msgUpd = MessageTemplate
										.MatchPerformative(ACLMessage.CONFIRM);
								ACLMessage msgUpdateRew = blockingReceive(msgUpd);
								Proposal updatedProp = (Proposal) msgUpdateRew
										.getContentObject();
								currentProposal.setLocalReward(updatedProp
										.getLocalReward());
								currentProposal.setGlobalReward(updatedProp
										.getGlobalReward());
								currentProposal.setPCPMReward(updatedProp
										.getPCPMReward());
								// fim pedir localReward atualizada
							}
							log("My current proposal is:\n"
									+ currentProposal.toString());

							if (localReward > currentProposal.getLocalReward()
									&& (localReward != -1 || currentProposal
											.getLocalReward() != -1)) {
								setAgreedProposal(true);
								explanation = "Greater local reward than the current";
							} else if (localReward == currentProposal
									.getLocalReward() || localReward == -1) {
								if (globalReward > currentProposal
										.getGlobalReward()) {
									setAgreedProposal(true);
									explanation = "In spite of offering equal local reward, global reward is greater than the current";
								} else if (globalReward < currentProposal
										.getGlobalReward()) {
									explanation = "Equal local reward, but global reward is lesser than the current";
								} else {
									if (pcpmReward > currentProposal
											.getPCPMReward()) {
										setAgreedProposal(true);
										explanation = "In spite of offering equal local and global rewards, PCPM reward is greater than the current";
									} else {
										explanation = "Equal local and global rewards, but PCPM reward is lesser or equal than the current";
									}
								}
							} else {
								explanation = "Lesser local reward than the current";
							}
							if (getAgreedProposal()) {
								setLocationTo(getLocationByAID(propose
										.getSender()));
								Level currentLevel = proposal.getLevel();
								getMyComponent().setCurrentLevel(currentLevel);
								setCurrentProposal(proposal);
								reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
								if (getReckonAID() == null) {
									setReckonAID(propose.getSender());
								} else {
									setExReckonAID(getReckonAID());
									setReckonAID(propose.getSender());
								}

								this.state = 1;
							} else {
								reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
							}
							reply.setContent(explanation);
							return reply;
						} catch (NullPointerException e) {
							e.printStackTrace();
						} catch (UnreadableException e) {
							e.printStackTrace();
							reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
							reply.setContent("Not expecting message's content");

							log(e.getLocalizedMessage()
									+ ", didn't understand message: "
									+ propose.getContent().toString());
							return reply;
						} catch (Exception e) {
							e.printStackTrace();
						} finally {
							negotiating = false;
							if (getAgreedProposal()) {
								myAgent.addBehaviour(new OneShotBehaviour(
										myAgent) {
									@Override
									public void action() {
										if (!getLocationTo().equals(here())) {
											takeDown();
											// attemptMoveB = new
											// CyclicBehaviour() {
											//
											// private int nRetry = 0;
											//
											// @Override
											// public void action() {
											// try {
											// if (nRetry >= retriesMigration) {
											// myAgent.doDelete();
											// }
											// log("attempting to move to "
											// + getLocationTo()
											// .toString()
											// + ", ("
											// + System.currentTimeMillis()
											// + ")");
											// nRetry++;
											// doMove(getLocationTo());
											// } catch (Exception e) {
											// log("error moving to "
											// + getLocationTo()
											// .toString());
											// }
											// }
											//
											// };
											// addBehaviour(attemptMoveB);
											doMove(getLocationTo());

											// VER ISTO; fazer attempt to move
										} else if (getLocationTo().equals(
												here())) {
											setUp();
										}
									}
								});
							}
						}
					}
					reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
					return reply;
				}

				@Override
				public int onEnd() {
					log("terminou negotiating:" + state);
					return state;
				}

			});
		}
	}

	public class ParallelSendReceive extends ParallelBehaviour {

		public ParallelSendReceive(final Agent ag, long msgFreq, long timeout,
				final MessageTemplate mt, final String cid) {
			super(ag, ParallelBehaviour.WHEN_ANY);

			// mandar heartbeat
			addSubBehaviour(new TickerBehaviour(ag, msgFreq) {
				@Override
				protected void onTick() {
					send(createACLMessage(getPartnerAID(), cid, "ok"));
				}

			});

			// receber heartbeat
			addSubBehaviour(new TimeoutCyclicReceiver(ag, timeout, mt) {
				public void handle(ACLMessage msg) {
					if (msg != null) {
						// System.out.println(" MSG de "
						// + msg.getSender().getLocalName() + " para "
						// + myAgent.getLocalName() + " -> "
						// + msg.getContent() + " - cid: "
						// + msg.getConversationId() + " - t: "
						// + System.currentTimeMillis());
					} else {
						// say("O " + partnerAID.getLocalName() + " falhou.");
						if (getRole() == 2) {
							log(" assuming primary role");
							setRole(1);
							timeoutTime = System.currentTimeMillis();
							log("timeout = " + timeoutTime);
							// addBehaviour(new ParallelNegotiating(myAgent));
							send(createACLMessage(getPartnerAID(), cid,
									"timeout"));
						}
						// System.out
						// .println(" - cid: " + msg.getConversationId());
					}

				}
			});

			// recebe mensagens a dizer que passa para secundario
			MessageTemplate msgTempTimeout = MessageTemplate.and(
					MessageTemplate.MatchPerformative(ACLMessage.INFORM),
					MessageTemplate.MatchContent("timeout"));
			addSubBehaviour(new SimpleCyclicReceiver(ag, msgTempTimeout) {
				@Override
				protected void handle(ACLMessage msg) {
					if (msg.getContent().equals("timeout")) {
						((ExecutionAgent) ag).log("assuming secondary role");
						setRole(2);
						// deregister(dfd);
					}
				}
			});

		}

		private ACLMessage createACLMessage(AID aid, String cid, String content) {

			ACLMessage request = new ACLMessage(ACLMessage.INFORM);
			request.setLanguage(new SLCodec().getName());
			request.setOntology(MobilityOntology.getInstance().getName());
			try {
				request.setSender(myAgent.getAID());
				request.addReceiver(aid);
				request.setContent(content);
				request.setConversationId(cid);
				return request;
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			return null;
		}
	}

	class TimeoutCyclicReceiver extends CyclicBehaviour {

		private MessageTemplate template;
		private long timeOut, wakeupTime;

		private ACLMessage msg;

		public ACLMessage getMessage() {
			return msg;
		}

		public TimeoutCyclicReceiver(Agent a, long timeout, MessageTemplate mt) {
			super(a);
			this.timeOut = timeout;
			this.template = mt;
		}

		public void onStart() {
			wakeupTime = (timeOut < 0 ? Long.MAX_VALUE : System
					.currentTimeMillis() + timeOut);
		}

		public void action() {
			if (template == null) {
				msg = myAgent.receive();
			} else {
				msg = myAgent.receive(template);
				// aqui faz match com o
				// ConversationID e com o Sender

			}
			if (msg != null) {
				handle(msg);
				reset();
				return;
			}
			long dt = wakeupTime - System.currentTimeMillis();
			if (dt > 0) {
				block(dt);
				// System.out.println("adiei " + dt + "ms!");
			} else {
				// System.out.println(dt);
				handle(msg); // mensagem nula
				reset();
			}
		}

		public void handle(ACLMessage m) { // override na subclasse
		}

		public void reset() {
			msg = null;
			super.reset();
		}

		public void reset(long dt) {
			timeOut = dt;
			reset();
		}

	}

}
