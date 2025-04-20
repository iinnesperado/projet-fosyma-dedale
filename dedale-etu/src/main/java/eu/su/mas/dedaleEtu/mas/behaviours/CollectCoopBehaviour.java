package eu.su.mas.dedaleEtu.mas.behaviours;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.env.gs.GsLocation;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;

import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.behaviours.ShareMapBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.TresorInfo;
import eu.su.mas.dedaleEtu.mas.knowledge.TresorMessage;

import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.core.AID;
import java.util.Random;

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
public class CollectCoopBehaviour extends SimpleBehaviour {

    private static final long serialVersionUID = 8567689731496787664L;

    private boolean finished = false;

    /**
     * Current knowledge of the agent regarding the environment
     */
    private MapRepresentation myMap;

    private List<String> list_agentNames;

    private List<Couple<String, MapRepresentation>> list_map;

    private List<String> agentsEnExploration;

    private List<TresorInfo> listeTresors = new ArrayList<>();
    private LocalDateTime derniereMajTresors;

    /**
     * 
     * @param myagent    reference to the agent we are adding this behaviour to
     * @param myMap      known map of the world the agent is living in
     * @param agentNames name of the agents to share the map with
     */
    public CollectCoopBehaviour(final AbstractDedaleAgent myagent, MapRepresentation myMap, List<String> agentNames) {
        super(myagent);
        this.myMap = myMap;
        this.list_agentNames = agentNames;
        this.list_map = new ArrayList<>();
        this.agentsEnExploration = new ArrayList<>(agentNames); // Initialisation avec tous les agents
        this.listeTresors = new ArrayList<>();
        this.derniereMajTresors = LocalDateTime.now();
    }

    @Override
    public void action() {

        if (this.myMap == null) {
            this.myMap = new MapRepresentation(this.myAgent.getLocalName());
            // this.myMap.openGui4();
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
            if (!this.myMap.hasOpenNode() && agentsEnExploration.isEmpty()) {
                // Si tous les agents ont fini, on met fin au comportement
                finished = true;
                System.out.println(this.myAgent.getLocalName()
                        + " - A fini d'explorer et sait que les autres agents ont fini.");
                // On envoie un message de fin d'exploration à tous les agents
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.setProtocol("END");
                msg.setSender(this.myAgent.getAID());
                for (String agent : this.list_agentNames) {
                    msg.addReceiver(new AID(agent, AID.ISLOCALNAME));
                }
                this.myAgent.send(msg);
            } else {
                // 4) select next move.
                // 4.1 If there exist one open node directly reachable, go for it,
                // otherwise choose one from the openNode list, compute the shortestPath and go
                // for it

                if (!this.myMap.hasOpenNode() && !agentsEnExploration.isEmpty()) {
                    // On continue à attendre que les autres agents
                    // finissent en explorant les alentours
                    // On envoie un message de fin d'exploration à tous les agents
                    ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                    msg.setProtocol("END");
                    msg.setSender(this.myAgent.getAID());
                    for (String agent : this.list_agentNames) {
                        msg.addReceiver(new AID(agent, AID.ISLOCALNAME));
                    }
                    this.myAgent.send(msg);
                }
                if (nextNodeId == null) {
                    // si l'agent a terminé mais que d'autres agents sont encore en exploration
                    if (!this.myMap.hasOpenNode() && !agentsEnExploration.isEmpty()) {
                        // On continue à attendre que les autres agents finissent en explorant les
                        // alentours
                        System.out.println(
                                this.myAgent.getLocalName() + " - a terminé mais est en attente des autres agents.");
                        // On attend un message de fin d'exploration
                        MessageTemplate msgTemplate = MessageTemplate.and(
                                MessageTemplate.MatchProtocol("END"),
                                MessageTemplate.MatchPerformative(ACLMessage.INFORM));
                        ACLMessage msgReceived = this.myAgent.blockingReceive(msgTemplate, 1000); // Temps d'attente
                                                                                                  // réduit
                        if (msgReceived != null) {
                            String sender = msgReceived.getSender().getLocalName();
                            System.out.println(this.myAgent.getLocalName() + " a reçu un END de " + sender);
                            agentsEnExploration.remove(sender);
                        }

                        // Au lieu d'un simple déplacement aléatoire, on explore plus efficacement les
                        // alentours
                        // en sélectionnant le nœud le moins visité récemment parmi les nœuds voisins
                        if (lobs != null && !lobs.isEmpty()) {
                            // On va essayer de visiter les nœuds qu'on n'a pas visités récemment
                            List<String> recentlyVisitedNodes = new ArrayList<>(5); // Garder trace des derniers nœuds
                                                                                    // visités

                            // Chercher un nœud non récemment visité
                            Couple<Location, List<Couple<Observation, String>>> bestNode = null;
                            for (Couple<Location, List<Couple<Observation, String>>> node : lobs) {
                                String nodeId = node.getLeft().getLocationId();
                                if (!nodeId.equals(myPosition.getLocationId())
                                        && !recentlyVisitedNodes.contains(nodeId)) {
                                    bestNode = node;
                                    break;
                                }
                            }

                            // Si tous les nœuds ont été récemment visités, on en prend un aléatoirement
                            if (bestNode == null) {
                                Random r = new Random();
                                int randomIndex = r.nextInt(lobs.size());
                                bestNode = lobs.get(randomIndex);
                            }

                            nextNodeId = bestNode.getLeft().getLocationId();

                            // Mettre à jour les nœuds récemment visités
                            if (!recentlyVisitedNodes.contains(nextNodeId)) {
                                recentlyVisitedNodes.add(nextNodeId);
                                if (recentlyVisitedNodes.size() > 5) {
                                    recentlyVisitedNodes.remove(0); // On garde une taille fixe
                                }
                            }
                        }
                    } else {
                        // si l'agent n'a pas terminé, on continue l'exploration
                        nextNodeId = this.myMap.getShortestPathToClosestOpenNode(myPosition.getLocationId()).get(0);
                    }

                }
                if (lobs != null) {
                    List<String> agentNames = new ArrayList<String>();
                    Iterator<Couple<Location, List<Couple<Observation, String>>>> iter1 = lobs.iterator();

                    while (iter1.hasNext()) {
                        Couple<Location, List<Couple<Observation, String>>> couple = iter1.next();
                        List<Couple<Observation, String>> observations = couple.getRight(); // Récupérer la liste des
                                                                                            // agents proches

                        // Si on a des observations et que le noeud n'est pas notre position actuelle
                        if (!observations.isEmpty()) {
                            for (Couple<Observation, String> obs : observations) {
                                if (obs.getLeft().getName().equals("Gold")
                                        || obs.getLeft().getName().equals("Diamond")) {
                                    TresorInfo tresor = new TresorInfo(obs.getLeft().getName(), obs.getRight());
                                    if (!this.listeTresors.contains(tresor)) {
                                        this.listeTresors.add(tresor);
                                        System.out.println(this.myAgent.getLocalName() + " a trouvé un trésor : " +
                                                obs.getRight() + " " + obs.getLeft().getName() + " à la position "
                                                + myPosition.getLocationId());
                                    }
                                    this.derniereMajTresors = LocalDateTime.now();
                                }
                                if (obs.getLeft().getName().equals("AgentName")
                                        && !obs.getRight().equals(this.myAgent.getLocalName())) {
                                    agentNames.add(obs.getRight()); // Récupérer la valeur String et l'ajouter à
                                                                    // agentNames
                                    this.myAgent
                                            .addBehaviour(new SendMapBehaviourOld((AbstractDedaleAgent) this.myAgent,
                                                    this.myMap, obs.getRight()));
                                    this.myAgent
                                            .addBehaviour(
                                                    new ReceiveMapBehaviour(this.myAgent, this.myMap, agentNames));
                                    this.myAgent
                                            .addBehaviour(new SendTresorBehaviour((AbstractDedaleAgent) this.myAgent,
                                                    this.listeTresors,
                                                    this.derniereMajTresors, obs.getRight()));
                                    this.myAgent.addBehaviour(new ReceiveTresorBehaviour(this.listeTresors,
                                            this.derniereMajTresors));

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
                boolean moved = ((AbstractDedaleAgent) this.myAgent).moveTo(new GsLocation(nextNodeId));

                if (!moved) {
                    System.out.println(this.myAgent.getLocalName() + " - Collision détectée vers " + nextNodeId);

                    // Attendre un temps aléatoire pour désynchroniser les agents
                    int waitTime = 500 + (int) (Math.random() * 1000);
                    try {
                        this.myAgent.doWait(waitTime);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    // Essayer un autre nœud voisin (exploration locale)
                    boolean anySuccess = false;
                    for (Couple<Location, List<Couple<Observation, String>>> couple : lobs) {
                        String alternative = couple.getLeft().getLocationId();
                        if (!alternative.equals(myPosition.getLocationId()) && !alternative.equals(nextNodeId)) {
                            System.out.println(
                                    this.myAgent.getLocalName() + " - Tentative d'alternative vers " + alternative);
                            boolean tryAlt = ((AbstractDedaleAgent) this.myAgent).moveTo(new GsLocation(alternative));
                            if (tryAlt) {
                                anySuccess = true;
                                break;
                            }
                        }
                    }

                    // Si aucune alternative n'a fonctionné
                    if (!anySuccess) {
                        System.out.println(this.myAgent.getLocalName()
                                + " - Toutes les alternatives ont échoué, attente plus longue");
                        // Attendre plus longtemps avant de réessayer
                        try {
                            this.myAgent.doWait(2000);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

        }

    }

    @Override
    public boolean done() {
        return finished;
    }

}