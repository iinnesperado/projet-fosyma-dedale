package eu.su.mas.dedaleEtu.mas.behaviours;

import java.util.List;

import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.env.gs.GsLocation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.behaviours.tryoutExplocoopFSM.Treasure;
import jade.core.behaviours.SimpleBehaviour;

/**
 * TODO Finish to implement this part of the code used for the déplacement des agent when FINISHED_EXPLO
 */

public class Move2TreasureBehaviour extends SimpleBehaviour{

    private static final long serialVersionUID = -4182946470338993682L;
    private MapRepresentation myMap;
    private List<Treasure> ltreasures;
    private List<String> myPath; // path to take to the nearest treasure of of the same treasure type as the agent

    public Move2TreasureBehaviour(final AbstractDedaleAgent agent, MapRepresentation myMap, List<Treasure> treasures){
        super(agent);
        this.myMap = myMap;
        this.ltreasures = treasures;
    }


    // NOTE make so that the path is redone when the list of treasures has been updated ?
    @Override
    public void action() {
        Location myPosition = ((AbstractDedaleAgent)this.myAgent).getCurrentPosition();
        Observation myTreasureType = ((AbstractDedaleAgent)this.myAgent).getMyTreasureType();
        
        // On donne un trajet au robot à réaliser vers le trésor le plus proche à sa position
        if (myPath == null) { //when the agent doesn't have path to go to a treasure
            // TODO add FoundBehaviour and run the folowing code when that B is finished


            for (Treasure treasure : ltreasures) {
                // Only looks for shortest path of treasures of its own type
                if (myTreasureType.getName().equals(treasure.getType()) && !treasure.getAmount().equals("0")){
                    List<String> candidatePath = myMap.getShortestPath(myPosition.getLocationId(), treasure.getPositionID());
                    myPath = candidatePath;
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
    
    // public List<String> getNearestTreasurePath(String myTreasureType, Location myPosition){
        
    // }
}
