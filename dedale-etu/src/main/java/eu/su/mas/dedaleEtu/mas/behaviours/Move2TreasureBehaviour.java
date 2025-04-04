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
    private List<Couple<String, Observation>> treasuresLocation; // TODO match code to type TresorInfo
    private List<String> myPath; // path to take to the nearest treasure of of the same treasure type as the agent

    public Move2TreasureBehaviour(final AbstractDedaleAgent agent, MapRepresentation myMap, List<Couple<String, Observation>> treasuresLocation){
        super(agent);
        this.myMap = myMap;
        this.treasuresLocation = treasuresLocation;
    }

    @Override
    public void action() {
        Location myPosition = ((AbstractDedaleAgent)this.myAgent).getCurrentPosition();
        Observation treasureType = ((AbstractDedaleAgent)this.myAgent).getMyTreasureType();
        
        // On donne un trajet au robot à réaliser vers le trésor le plus proche à sa position
        if (myPath == null) { //when the agent doesn't have path to go to a treasure
            // TODO add FoundBehaviour and run the folowwing code when that B is finished


            for (Couple<String, Observation> location : treasuresLocation) {
                // Only looks for shortedt path of treasures of its own type
                if (treasureType.getName().equals(location.getRight().getName())){
                    List<String> candidatePath = myMap.getShortestPath(myPosition.getLocationId(), location.getLeft());
                    if (candidatePath.size() < myPath.size()) {
                        myPath = candidatePath;
                    }
                }
            }
        } else {
            // si le robot a déjà un trésor obj donc il a déjà un chemin à faire il suit le chemin 
            ((AbstractDedaleAgent)this.myAgent).moveTo(new GsLocation(myPath.getFirst()));
            myPath.removeFirst();
        }
    }

    @Override
    public boolean done() {
        return false;
    }
    
}
