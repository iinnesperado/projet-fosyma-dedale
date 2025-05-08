package eu.su.mas.dedaleEtu.mas.behaviours.treasure;

import java.util.ArrayList;
import java.util.List;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.knowledge.Treasure;
import jade.core.behaviours.SimpleBehaviour;

/**
 * Not implemented
 */
public class CollectTreasureBehaviour extends SimpleBehaviour {

    private static final long serialVersionUID = 3973250783964285694L;
    private AbstractDedaleAgent agent;
    private List<Treasure> listeTresors = new ArrayList<>();
    private MapRepresentation myMap;
    private boolean finished = false;

    public CollectTreasureBehaviour(final AbstractDedaleAgent agent, MapRepresentation map,
            List<Treasure> listeTresors) {
        super(agent);
        this.agent = agent;
        this.myMap = map;
        this.listeTresors = listeTresors;
    }

    @Override
    public void action() {
        if (listeTresors.isEmpty()) {
            System.out.println("No treasures to collect.");
            finished = true; // Mark as finished if there are no treasures
            return;
        }
        // agent.addBehaviour(new MoveToTreasureBehaviour((AbstractDedaleAgent) agent, myMap, listeTresors));
    }

    @Override
    public boolean done() {
        return finished;
    }

}
