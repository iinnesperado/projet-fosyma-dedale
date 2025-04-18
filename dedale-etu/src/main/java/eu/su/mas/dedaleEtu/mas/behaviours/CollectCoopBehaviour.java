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

    private static final long serialVersionUID = 8567689731496787661L;

    private boolean finished = false;

    /**
     * Current knowledge of the agent regarding the environment
     */
    private MapRepresentation myMap;

    private List<String> list_agentNames;

    private List<Couple<String, MapRepresentation>> list_map;

    private List<String> agentsEnAttenteACK = new ArrayList<>();

    private List<String> agentsEnExploration;

    private List<TresorInfo> listeTresors = new ArrayList<>();

    private LocalDateTime derniereMajTresors;

    /**
     * 
     * @param myagent    reference to the agent we are adding this behaviour to
     * @param myMap      known map of the world the agent is living in
     * @param agentNames name of the agents to share the map with
     */
    public CollectCoopBehaviour(final AbstractDedaleAgent myagent, MapRepresentation myMap, List<String> agentNames,
            List<TresorInfo> listeTresors) {
        super(myagent);
        this.myMap = myMap;
        this.list_agentNames = agentNames;
        this.list_map = new ArrayList<>();
        this.listeTresors = new ArrayList<>();
        this.listeTresors = listeTresors;
        this.derniereMajTresors = LocalDateTime.now();
    }

    @Override
    public void action() {

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

            // 2) get the surrounding nodes and, if not in closedNodes, add them to open
            // nodes.
            String nextNodeId = null;
            Iterator<Couple<Location, List<Couple<Observation, String>>>> iter = lobs.iterator();
            while (iter.hasNext()) {
                Location accessibleNode = iter.next().getLeft(); // on récupère le noeud accessible
                if (nextNodeId == null)
                    nextNodeId = accessibleNode.getLocationId();

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
                                    System.out.println(this.myAgent.getLocalName() + " a trouvé un trésor : "
                                            + obs.getLeft().getName() + " à " + obs.getRight());
                                }
                                this.derniereMajTresors = LocalDateTime.now();
                            }
                            if (obs.getLeft().getName().equals("AgentName")
                                    && !obs.getRight().equals(this.myAgent.getLocalName())) {
                                agentNames.add(obs.getRight()); // Récupérer la valeur String et l'ajouter à
                                                                // agentNames
                                this.myAgent
                                        .addBehaviour(new SendTresorBehaviour((AbstractDedaleAgent) this.myAgent,
                                                this.listeTresors,
                                                this.derniereMajTresors, obs.getRight()));
                                this.myAgent.addBehaviour(new ReceiveTresorBehaviour(this.listeTresors,
                                        this.derniereMajTresors));
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
                for (Couple<Location, List<Couple<Observation, String>>> couple : lobs) {
                    String alternative = couple.getLeft().getLocationId();
                    if (!alternative.equals(myPosition.getLocationId()) && !alternative.equals(nextNodeId)) {
                        System.out.println(
                                this.myAgent.getLocalName() + " - Tentative d'alternative vers " + alternative);
                        boolean tryAlt = ((AbstractDedaleAgent) this.myAgent).moveTo(new GsLocation(alternative));
                        if (tryAlt)
                            break;
                    }
                }
            }
        }
    }

    @Override
    public boolean done() {
        return this.finished;
    }

}