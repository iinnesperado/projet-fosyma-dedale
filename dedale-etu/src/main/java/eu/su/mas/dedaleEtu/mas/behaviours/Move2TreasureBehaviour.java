package eu.su.mas.dedaleEtu.mas.behaviours;

import java.util.List;

import org.netlib.util.booleanW;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.env.gs.GsLocation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.knowledge.MapRepresentation;
import jade.core.behaviours.SimpleBehaviour;

public class Move2TreasureBehaviour extends SimpleBehaviour{

    private static final long serialVersionUID = -4182946470338993682L;
    private MapRepresentation myMap;
    private List<Couple<String, Observation>> treasuresLocation;
    private List<String> treasurePath;

    public Move2TreasureBehaviour(final AbstractDedaleAgent agent, MapRepresentation myMap, List<Couple<String, Observation>> treasuresLocation){
        super(agent);
        this.myMap = myMap;
        this.treasuresLocation = treasuresLocation;
    }

    @Override
    public void action() {
        Location myPosition = ((AbstractDedaleAgent)this.myAgent).getCurrentPosition();
        
        
        // On donne un trajet au robot à réaliser vers le trésor le plus proche à sa position
        if (treasurePath == null) {
            for (Couple<String, Observation> location : treasuresLocation) {
                List<String> candidatePath = myMap.getShortestPath(myPosition.getLocationId(), location.getLeft());
                if (candidatePath.size() < treasurePath.size()) {
                    treasurePath = candidatePath;
                }
            }
        }

        // si le robot a déjà un trésor obj donc il a déjà un chemin à faire il suit le chemin 
        ((AbstractDedaleAgent)this.myAgent).moveTo(new GsLocation(treasurePath.getFirst()));
        treasurePath.removeFirst();
    }

    @Override
    public boolean done() {
        return false;
    }
    
}
