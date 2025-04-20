package eu.su.mas.dedaleEtu.mas.behaviours;

import java.io.IOException;
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
import jade.core.behaviours.TickerBehaviour;
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
public class PostExplorationBehaviour extends TickerBehaviour {

    private static final long serialVersionUID = 8567689731496787661L;

    private boolean finished = false;

    /**
     * Current knowledge of the agent regarding the environment
     */
    private MapRepresentation myMap;
    private List<String> list_agentNames;
    private List<TresorInfo> listeTresors = new ArrayList<>();
    private LocalDateTime derniereMajTresors;

    /**
     * 
     * @param myagent    reference to the agent we are adding this behaviour to
     * @param myMap      known map of the world the agent is living in
     * @param agentNames name of the agents to share the map with
     */
    public PostExplorationBehaviour(final AbstractDedaleAgent myagent, MapRepresentation myMap,
            List<String> agentNames) {
        super(myagent, 500);
        this.myMap = myMap;
        this.list_agentNames = agentNames;
        this.listeTresors = new ArrayList<>();
        this.derniereMajTresors = LocalDateTime.now();
    }

    @Override
    public void onTick() {

        // 0) Retrieve the current position
        Location myPosition = ((AbstractDedaleAgent) this.myAgent).getCurrentPosition();

        System.out.println(this.myAgent.getLocalName() + " -- myCurrentPosition is: " + myPosition);
        if (myPosition != null) {
            // List of observable from the agent's current position
            List<Couple<Location, List<Couple<Observation, String>>>> lobs = ((AbstractDedaleAgent) this.myAgent)
                    .observe();// myPosition
            System.out.println(this.myAgent.getLocalName() + " -- list of observables: " + lobs);

            // Little pause to allow you to follow what is going on
            try {
                System.out.println("Press enter in the console to allow the agent " + this.myAgent.getLocalName()
                        + " to execute its next move");
                System.in.read();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // list of observations associated to the currentPosition
            List<Couple<Observation, String>> lObservations = lobs.get(0).getRight();

            // example related to the use of the backpack for the treasure hunt
            Boolean b = false;
            for (Couple<Observation, String> o : lObservations) {
                switch (o.getLeft()) {
                    case DIAMOND:
                    case GOLD:
                        TresorInfo tresor = new TresorInfo(o.getLeft().getName(), myPosition.getLocationId(),
                                Integer.parseInt(o.getRight()));
                        if (!this.listeTresors.contains(tresor)) {
                            this.listeTresors.add(tresor);
                            System.out.println(this.myAgent.getLocalName() + " a trouvé un trésor : " +
                                    o.getRight() + " " + o.getLeft().getName() + " à la position "
                                    + myPosition.getLocationId());
                        }
                        this.derniereMajTresors = LocalDateTime.now();

                        break;
                    case AGENTNAME:
                        this.myAgent.addBehaviour(new SendMapBehaviour((AbstractDedaleAgent) this.myAgent,
                                this.myMap, o.getRight()));
                        this.myAgent.addBehaviour(new ReceiveMapBehaviour((AbstractDedaleAgent) this.myAgent,
                                this.myMap, list_agentNames));
                        this.myAgent.addBehaviour(new SendTresorBehaviour((AbstractDedaleAgent) this.myAgent,
                                this.listeTresors, this.derniereMajTresors, o.getRight()));
                        this.myAgent.addBehaviour(new ReceiveTresorBehaviour(this.listeTresors,
                                this.derniereMajTresors));
                    default:
                        break;
                }

            }

            // If the agent picked (part of) the treasure
            if (b) {
                List<Couple<Location, List<Couple<Observation, String>>>> lobs2 = ((AbstractDedaleAgent) this.myAgent)
                        .observe();// myPosition
                System.out.println(this.myAgent.getLocalName()
                        + " - State of the observations after trying to pick something " + lobs2);
            }

            // Random move from the current position
            Random r = new Random();
            int moveId = 1 + r.nextInt(lobs.size() - 1);// removing the current position from the list of target, not
                                                        // necessary as to stay is an action but allow quicker random
                                                        // move

            // The move action (if any) should be the last action of your behaviour
            ((AbstractDedaleAgent) this.myAgent).moveTo(lobs.get(moveId).getLeft());
        }
    }
}