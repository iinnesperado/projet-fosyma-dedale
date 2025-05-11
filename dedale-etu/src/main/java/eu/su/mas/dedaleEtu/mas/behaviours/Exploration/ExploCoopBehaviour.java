package eu.su.mas.dedaleEtu.mas.behaviours.Exploration;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.env.gs.GsLocation;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;

import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import eu.su.mas.dedaleEtu.mas.knowledge.Treasure;
import eu.su.mas.dedaleEtu.mas.behaviours.Communication.TellTankerToMoveBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.Communication.ReceiveMapBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.Communication.ReceiveTresorBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.Communication.SendMapBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.Communication.SendTresorBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.Coordination.BesoinExpertise;
import eu.su.mas.dedaleEtu.mas.behaviours.Coordination.OffreExpertise;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.knowledge.TresorInfo;

import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import javafx.application.Platform;

/**
 * <pre>
 * This behaviour allows an agent to explore the environment and learn the associated topological map.
 * The algorithm is a pseudo - DFS computationally consuming because its not optimised at all.
 * 
 * When all the nodes around him are visited, the agent randomly select an open node and go there to restart its dfs. 
 * This (non optimal) behaviour is done until all nodes are explored. 
 * 
 * Warning, this behaviour does not save the content of visited nodes, only the topology.
 * Warning, the sub-behaviour ShareMap periodically share the whole map
 * </pre>
 * 
 * @author hc
 *
 */
public class ExploCoopBehaviour extends SimpleBehaviour {

	private static final long serialVersionUID = 8567689731496787661L;

	private boolean finished = false;

	/**
	 * Current knowledge of the agent regarding the environment
	 */
	private MapRepresentation myMap;

	private List<String> list_agentNames;

	private List<Couple<String, MapRepresentation>> list_map;

	private List<Treasure> listeTresors = new ArrayList<>();
	private List<Couple<String, Couple<LocalDateTime, LocalDateTime>>> lastContact; // last contacted and agent
																					// Couple<Map time, Treasure time>

	/**
	 * 
	 * @param myagent    reference to the agent we are adding this behaviour to
	 * @param myMap      known map of the world the agent is living in
	 * @param agentNames name of the agents to share the map with
	 */
	public ExploCoopBehaviour(final AbstractDedaleAgent myagent, MapRepresentation myMap, List<String> agentNames) {
		super(myagent);
		this.myMap = myMap;
		this.list_agentNames = agentNames;
		this.list_map = new ArrayList<>();
		this.listeTresors = new ArrayList<>();
		this.lastContact = new ArrayList<>();
	}

	@Override
	public void action() {

		if (this.myMap == null) {
			this.myMap = new MapRepresentation(this.myAgent.getLocalName());
			// this.myMap.openGui4();
			Platform.runLater(() -> {
				// openGui();
				this.myMap.openGui4();

			});
			for (String agent : this.list_agentNames) {
				this.list_map.add(new Couple<String, MapRepresentation>(agent,
						new MapRepresentation(this.myAgent.getLocalName())));
			}
		}

		// 0) Retrieve the current position
		Location myPosition = ((AbstractDedaleAgent) this.myAgent).getCurrentPosition();

		if (myPosition != null) {
			// List of observable from the agent's current position
			List<Couple<Location, List<Couple<Observation, String>>>> lobs = ((AbstractDedaleAgent) this.myAgent)
					.observe();// myPosition

			/**
			 * Just added here to let you see what the agent is doing, otherwise he will be
			 * too quick
			 */
			try {
				this.myAgent.doWait(3000);
			} catch (Exception e) {
				e.printStackTrace();
			}

			// 1) remove the current node from openlist and add it to closedNodes.
			this.myMap.addNode(myPosition.getLocationId(), MapAttribute.closed);
			for (Couple<String, MapRepresentation> coupleAgentMap : this.list_map) {
				coupleAgentMap.getRight().addNode(myPosition.getLocationId(), MapAttribute.closed);
			}

			// 2) get the surrounding nodes and, if not in closedNodes, add them to open
			// nodes.
			String nextNodeId = null;
			Iterator<Couple<Location, List<Couple<Observation, String>>>> iter = lobs.iterator();
			while (iter.hasNext()) {
				Location accessibleNode = iter.next().getLeft(); // on récupère le noeud accessible
				boolean isNewNode = this.myMap.addNewNode(accessibleNode.getLocationId());
				if (myPosition.getLocationId() != accessibleNode.getLocationId()) {
					this.myMap.addEdge(myPosition.getLocationId(), accessibleNode.getLocationId());
					for (Couple<String, MapRepresentation> coupleAgentMap : this.list_map) {
						// Ajouter le noeud accessible à la carte de l'agent ainsi que l'arc entre le
						// noeud actuel et le noeud accessible
						coupleAgentMap.getRight().addNode(accessibleNode.getLocationId(), MapAttribute.shared);
						coupleAgentMap.getRight().addEdge(myPosition.getLocationId(), accessibleNode.getLocationId());
					}
					if (nextNodeId == null && isNewNode)
						nextNodeId = accessibleNode.getLocationId();
				}
			}

			// 3) while openNodes is not empty, continues.
			// s'il n'y a plus de noeud ouvert et que tous les agents ont fini, on arrête le
			// comportement
			if (!this.myMap.hasOpenNode()) {
				this.finished = true;
				System.out.println(this.myAgent.getLocalName() + " - J'AI TERMINÉ L'EXPLORATION");
				System.out.println(this.myAgent.getLocalName() + " - Je commence la post-exploration");
				this.myAgent.addBehaviour(
						new PostExplorationBehaviour(((AbstractDedaleAgent) myAgent), myMap, list_agentNames,
								listeTresors, lastContact));
			} else {
				// 4) select next move.
				// 4.1 If there exist one open node directly reachable, go for it,
				// otherwise choose one from the openNode list, compute the shortestPath and go
				// for it
				if (lobs != null) {
					List<String> agentNames = new ArrayList<String>();
					Iterator<Couple<Location, List<Couple<Observation, String>>>> iter1 = lobs.iterator();

					while (iter1.hasNext()) {
						Couple<Location, List<Couple<Observation, String>>> couple = iter1.next();
						List<Couple<Observation, String>> observations = couple.getRight(); // Récupérer la liste des
																							// observations
						if (!observations.isEmpty()) {
							System.out.println(this.myAgent.getLocalName() + " - Observations : " + observations);
							for (Couple<Observation, String> obs : observations) {
								if (obs.getLeft().getName().equals("Gold")
										|| obs.getLeft().getName().equals("Diamond")) {
									// if (((AbstractDedaleAgent) this.myAgent).openLock(obs.getLeft())) {
									// System.out.println(
									// this.myAgent.getLocalName() + " - Ouverture du coffre contenant "
									// + obs.getLeft().getName());
									// } else {
									// System.out.println(this.myAgent.getLocalName()
									// + " - Impossible d'ouvrir le coffre contenant " +
									// obs.getLeft().getName());
									// Location positionCoffre = myPosition; // Important de capturer la position
									// // actuelle
									// this.myAgent
									// .addBehaviour(new BesoinExpertise((AbstractDedaleAgent) this.myAgent,
									// this.myMap, list_agentNames, positionCoffre));
									// }
									openTreasure(obs);
									Integer quantity = Integer.parseInt(obs.getRight());
									Treasure nouveauTresor = new Treasure(myPosition, obs.getLeft().getName(), quantity,
											LocalDateTime.now());
									boolean tresorExistant = false;
									Treasure tresorARemplacer = null;

									// Rechercher si le trésor existe déjà à cette position
									for (Treasure tresorActuel : listeTresors) {
										if (tresorActuel.getPosition().equals(myPosition)) {
											tresorExistant = true;

											// Si même type, mettre à jour quantité
											if (tresorActuel.getType().equals(nouveauTresor.getType())) {
												tresorActuel.setQuantity(nouveauTresor.getQuantity());
												tresorActuel.setRecordTime(LocalDateTime.now());
												System.out.println(this.myAgent.getLocalName()
														+ " - Mise à jour du trésor: " +
														nouveauTresor.getQuantity() + " " + nouveauTresor.getType() +
														" en position " + myPosition.getLocationId());
											} else {
												// Type différent, marquer pour remplacement
												tresorARemplacer = tresorActuel;
											}
											break;
										}
									}

									// Si besoin de remplacer (type différent à la même position)
									if (tresorARemplacer != null) {
										listeTresors.remove(tresorARemplacer);
										listeTresors.add(nouveauTresor);
										System.out.println("Remplacement d'un trésor de " + tresorARemplacer.getType() +
												" par " + nouveauTresor.getQuantity() + " " + nouveauTresor.getType() +
												" en position " + myPosition.getLocationId());
									}
									// Si nouveau trésor
									else if (!tresorExistant) {
										listeTresors.add(nouveauTresor);
										System.out.println(this.myAgent.getLocalName() + " - Nouveau trésor trouvé: " +
												nouveauTresor.getQuantity() + " " + nouveauTresor.getType() +
												" en position " + myPosition.getLocationId());
									}

									System.out.println("TRÉSORS ACTUELS: " + this.listeTresors);
								}
								if (obs.getLeft().getName().equals("AgentName")
										&& !obs.getRight().equals(this.myAgent.getLocalName())
										&& !obs.getRight().contains("Tank") && !obs.getRight().contains("Wumpus")) {
									agentNames.add(obs.getRight()); // Récupérer la valeur String et l'ajouter à
																	// agentNames
									this.myAgent
											.addBehaviour(new SendMapBehaviour((AbstractDedaleAgent) this.myAgent,
													this.myMap, obs.getRight()));
									this.myAgent
											.addBehaviour(
													new ReceiveMapBehaviour((AbstractDedaleAgent) this.myAgent,
															this.myMap, obs.getRight(), lastContact));
									this.myAgent
											.addBehaviour(new SendTresorBehaviour((AbstractDedaleAgent) this.myAgent,
													this.listeTresors, obs.getRight(), lastContact));
									this.myAgent.addBehaviour(new ReceiveTresorBehaviour(this.listeTresors));
									this.myAgent.addBehaviour(new OffreExpertise(
											(AbstractDedaleAgent) this.myAgent, this.myMap));

									MessageTemplate msgTemplate = MessageTemplate.and(
											MessageTemplate.MatchProtocol("ACK"),
											MessageTemplate.MatchPerformative(ACLMessage.INFORM));
									ACLMessage msgReceived = this.myAgent.blockingReceive(msgTemplate, 5000);

									if (msgReceived != null) {
										String sender = msgReceived.getSender().getLocalName();
										System.out.println(this.myAgent.getLocalName() + " a reçu un ACK de " + sender);
										// Supprimer la carte de l'agent qui a envoyé un ACK
										this.list_map
												.removeIf(coupleAgentMap -> coupleAgentMap.getLeft().equals(sender));
										this.list_map.add(
												new Couple<String, MapRepresentation>(sender,
														new MapRepresentation(sender)));

									}

								}

							}

						}

					}
				}
			}
			if (nextNodeId == null) {
				// Si aucun nœud ouvert n'est directement accessible, on calcule le chemin le
				// plus
				// court
				List<String> path = this.myMap.getShortestPathToClosestOpenNode(myPosition.getLocationId());
				if (path != null && !path.isEmpty()) {
					nextNodeId = path.get(0); // Premier pas du chemin
				}
			}
			// if (nextNodeId == null) {
			// // Si aucun nœud ouvert n'est directement accessible, on calcule le chemin le
			// // plus court
			// List<String> path =
			// this.myMap.getShortestPathToClosestOpenNode(myPosition.getLocationId());
			// if (path != null && !path.isEmpty()) {
			// nextNodeId = path.get(0); // Premier pas du chemin

			// boolean isAdjacent = false;
			// for (Couple<Location, List<Couple<Observation, String>>> couple : lobs) {
			// if (couple.getLeft().getLocationId().equals(nextNodeId)) {
			// isAdjacent = true;
			// break;
			// }
			// }

			// if (!isAdjacent) {
			// System.out.println(this.myAgent.getLocalName() + " - ATTENTION: Le nœud " +
			// nextNodeId +
			// " n'est pas adjacent à " + myPosition.getLocationId() +
			// ". Recherche d'une alternative...");

			// // Si le nœud n'est pas adjacent, on cherche un nœud adjacent accessible
			// for (Couple<Location, List<Couple<Observation, String>>> couple : lobs) {
			// String candidateId = couple.getLeft().getLocationId();
			// if (!candidateId.equals(myPosition.getLocationId())) {
			// nextNodeId = candidateId;
			// System.out.println(
			// this.myAgent.getLocalName() + " - Alternative trouvée: " + nextNodeId);
			// break;
			// }
			// }
			// }
			// } else {
			// System.out.println(
			// this.myAgent.getLocalName() + " - ERREUR: Aucun chemin trouvé vers un nœud
			// ouvert");
			// try {
			// this.myAgent.doWait(10000); // Attendre avant de réessayer
			// } catch (Exception e) {
			// e.printStackTrace();
			// }
			// return; // Sortir de l'action pour réessayer au prochain cycle
			// }
			// }

			// // Vérifier une dernière fois que le nœud cible est bien dans les
			// observations
			// boolean targetIsObservable = false;
			// for (Couple<Location, List<Couple<Observation, String>>> couple : lobs) {
			// if (couple.getLeft().getLocationId().equals(nextNodeId)) {
			// targetIsObservable = true;
			// break;
			// }
			// }

			// if (!targetIsObservable) {
			// System.out.println(this.myAgent.getLocalName() + " - ERREUR CRITIQUE: Le nœud
			// cible " + nextNodeId +
			// " n'est pas observable depuis la position actuelle");
			// return; // Sortir de l'action pour réessayer au prochain cycle
			// }

			if (nextNodeId == null) {
				System.out.println(this.myAgent.getLocalName() + " - Aucune alternative trouvée, attente prolongée");
				// Attendre plus longtemps avant de réessayer
				try {
					this.myAgent.doWait(5000);
				} catch (Exception e) {
					e.printStackTrace();
				}
				return; // Sortir de l'action pour réessayer au prochain cycle
			}

			// Maintenant on peut tenter le déplacement
			boolean moved = ((AbstractDedaleAgent) this.myAgent).moveTo(new GsLocation(nextNodeId));

			if (!moved) {
				System.out.println(this.myAgent.getLocalName() + " - Collision détectée vers " + nextNodeId);

				// Vérifier si un Tanker est présent sur le nœud cible
				boolean tankerFound = false;
				String tankerName = null;

				// Parcourir les observations pour trouver un éventuel Tanker
				for (Couple<Location, List<Couple<Observation, String>>> couple : lobs) {
					if (couple.getLeft().getLocationId().equals(nextNodeId)) {
						List<Couple<Observation, String>> observations = couple.getRight();
						for (Couple<Observation, String> obs : observations) {
							if (obs.getLeft().getName().equals("AgentName")
									&& (obs.getRight().contains("Tank") || obs.getRight().contains("tank"))) {
								tankerFound = true;
								tankerName = obs.getRight();
								break;
							}
						}
						if (tankerFound)
							break;
					}
				}

				if (tankerFound) {
					// Un Tanker est sur notre chemin, lui demander de se déplacer
					System.out.println(
							this.myAgent.getLocalName() + " - Tanker détecté sur " + nextNodeId);
					TellTankerToMoveBehaviour tellTankerBehaviour = new TellTankerToMoveBehaviour(
							(AbstractDedaleAgent) this.myAgent, tankerName, myPosition.getLocationId());
					this.myAgent.addBehaviour(tellTankerBehaviour);

					// Attendre la réponse du Tanker
					MessageTemplate msgTemplate = MessageTemplate.and(
							MessageTemplate.MatchProtocol("TANKER-MOVED"),
							MessageTemplate.MatchPerformative(ACLMessage.INFORM));
					ACLMessage msgReceived = this.myAgent.blockingReceive(msgTemplate, 10000);
					if (msgReceived != null) {
						System.out.println(this.myAgent.getLocalName() + " - Réponse du Tanker : "
								+ msgReceived.getContent());
						// Le Tanker a déménagé, on peut continuer
						moved = ((AbstractDedaleAgent) this.myAgent).moveTo(new GsLocation(nextNodeId));
					} else {
						// On choisit un autre nœud voisin
						System.out.println(this.myAgent.getLocalName()
								+ " - Pas de réponse du Tanker, on essaie un autre nœud voisin");
						// Essayer de trouver un autre nœud voisin
						boolean foundAlternative = false;
						for (Couple<Location, List<Couple<Observation, String>>> couple : lobs) {
							String alternative = couple.getLeft().getLocationId();
							if (!alternative.equals(myPosition.getLocationId())
									&& !alternative.equals(nextNodeId)) {
								System.out.println(
										this.myAgent.getLocalName() + " - Tentative d'alternative vers "
												+ alternative);
								moved = ((AbstractDedaleAgent) this.myAgent)
										.moveTo(new GsLocation(alternative));
								if (moved) {
									foundAlternative = true;
									break;
								}
							}
						}
						if (!foundAlternative) {
							System.out.println(this.myAgent.getLocalName()
									+ " - Aucune alternative trouvée, attente prolongée");
							// Attendre plus longtemps avant de réessayer
							try {
								this.myAgent.doWait(2000);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
				}

				// Si ce n'est pas un Tanker ou si la demande a échoué, continuer avec la
				// gestion
				// normale des collisions

				// Attendre un temps aléatoire pour désynchroniser les agents
				int waitTime = (int) (Math.random() * 2000 + 1000); // entre 1 et 3 secondes
				try {
					this.myAgent.doWait(waitTime);
				} catch (Exception e) {
					e.printStackTrace();
				}

				// Essayer un autre nœud voisin (exploration locale)
				boolean anySuccess = false;
				lobs = ((AbstractDedaleAgent) this.myAgent).observe();
				for (Couple<Location, List<Couple<Observation, String>>> couple : lobs) {
					if (!couple.getLeft().getLocationId().equals(myPosition.getLocationId())
							&& !couple.getLeft().getLocationId().equals(nextNodeId)) {
						String alternativeNodeId = couple.getLeft().getLocationId();
						System.out.println(this.myAgent.getLocalName() + " - Tentative d'alternative vers "
								+ alternativeNodeId);
						moved = ((AbstractDedaleAgent) this.myAgent).moveTo(new GsLocation(alternativeNodeId));
						if (moved) {
							anySuccess = true;
							break; // Sortir de la boucle si le déplacement a réussi
						}
					}
				}

				// Si aucune alternative n'a fonctionné
				if (!anySuccess) {
					System.out.println(this.myAgent.getLocalName()
							+ " - Toutes les alternatives ont échoué, attente plus longue");
					return; // Sortir de l'action pour réessayer au prochain cycle
				}
			}
		}

	}

	@Override
	public boolean done() {
		return finished;
	}

	public void openTreasure(Couple<Observation, String> obs) {
		if (((AbstractDedaleAgent) this.myAgent).openLock(obs.getLeft())) {
			System.out.println(
					this.myAgent.getLocalName() + " - Ouverture du coffre contenant "
							+ obs.getLeft().getName());
		} else {
			System.out.println(this.myAgent.getLocalName()
					+ " - Impossible d'ouvrir le coffre contenant " +
					obs.getLeft().getName());
			Location positionCoffre = ((AbstractDedaleAgent) this.myAgent).getCurrentPosition();
			this.myAgent
					.addBehaviour(new BesoinExpertise((AbstractDedaleAgent) this.myAgent,
							this.myMap, list_agentNames, positionCoffre));
		}
	}

}