package eu.su.mas.dedaleEtu.mas.behaviours;

import java.util.ArrayList;
import java.util.List;


import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.env.gs.GsLocation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.knowledge.TresorInfo;
import jade.core.behaviours.SimpleBehaviour;

public class Move2TreasureBehaviour extends SimpleBehaviour {

    private static final long serialVersionUID = -4182946470338993682L;
    private MapRepresentation myMap;
    private List<TresorInfo> listeTresors = new ArrayList<>();
    private List<String> myPath; // path to take to the nearest treasure of of the same treasure type as the
                                 // agent

    public Move2TreasureBehaviour(final AbstractDedaleAgent agent, MapRepresentation myMap,
            List<TresorInfo> treasuresList) {
        super(agent);
        this.myMap = myMap;
        this.listeTresors = treasuresList;
    }

    @Override
    public void action() {
        Location myPosition = ((AbstractDedaleAgent) this.myAgent).getCurrentPosition();
        Observation treasureType = ((AbstractDedaleAgent) this.myAgent).getMyTreasureType();
        // On donne un trajet au robot à réaliser vers le trésor le plus proche à sa
        // position
        if (myPath == null) { // when the agent doesn't have path to go to a treasure
            // TODO add FoundBehaviour and run the folowwing code when that B is finished
            for (TresorInfo tresor : listeTresors) {
                if (tresor.getType() == treasureType) {
                    // On récupère le chemin vers le trésor
                    myPath = myMap.getShortestPath(myPosition.getLocationId(), tresor.getPositionId());
                }
            }
        } else {
            // si le robot a déjà un trésor obj donc il a déjà un chemin à faire il suit le
            // chemin
            ((AbstractDedaleAgent) this.myAgent).moveTo(new GsLocation(myPath.getFirst()));
            myPath.removeFirst();
        }
    }

    @Override
    public boolean done() {
        return false;
    }

}
