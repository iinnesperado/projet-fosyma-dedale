package eu.su.mas.dedaleEtu.mas.behaviours;

import java.util.ArrayList;
import java.util.List;


import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.env.gs.GsLocation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.knowledge.Treasure;
import eu.su.mas.dedaleEtu.mas.knowledge.TresorInfo;
import jade.core.behaviours.SimpleBehaviour;

public class MoveToTreasureBehaviour extends SimpleBehaviour {

    private static final long serialVersionUID = -4182946470338993682L;
    private MapRepresentation myMap;
    private Treasure tresorCible;
    private boolean finished;

    public MoveToTreasureBehaviour(final AbstractDedaleAgent agent, MapRepresentation myMap,
            Treasure tresor) {
        super(agent);
        this.myMap = myMap;
        this.tresorCible = tresor;
    }

    @Override
    public void action() {
        Location myPosition = ((AbstractDedaleAgent) this.myAgent).getCurrentPosition();
        String nextNode = myMap.getShortestPath(myPosition.getLocationId(),tresorCible.getPosition().getLocationId()).get(0);
    }

    @Override
    public boolean done() {
        return finished;
    }

}
