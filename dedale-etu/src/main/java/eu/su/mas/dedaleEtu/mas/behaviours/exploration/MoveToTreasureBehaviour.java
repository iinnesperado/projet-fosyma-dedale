package eu.su.mas.dedaleEtu.mas.behaviours.exploration;

import java.util.List;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.env.gs.GsLocation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.knowledge.Treasure;
import jade.core.behaviours.SimpleBehaviour;

public class MoveToTreasureBehaviour extends SimpleBehaviour {

    private static final long serialVersionUID = -4182946470338993682L;
    private MapRepresentation myMap;
    private Treasure tresorCible;
    private boolean finished;
    private String sender;

    public MoveToTreasureBehaviour(final AbstractDedaleAgent agent, MapRepresentation myMap, Treasure tresor, String sender) {
        super(agent);
        this.myMap = myMap;
        this.tresorCible = tresor;
        this.sender = sender;
    }

    @Override
    public void action() {
        Location myPosition = ((AbstractDedaleAgent) this.myAgent).getCurrentPosition();
        List<String> path = myMap.getShortestPath(myPosition.getLocationId(),tresorCible.getPosition().getLocationId());
		List<Couple<Location, List<Couple<Observation, String>>>> lobs = ((AbstractDedaleAgent) this.myAgent).observe();
        String nextNodeId = null;
        if (path != null && !path.isEmpty()) {
            nextNodeId = path.get(0); // Premier pas du chemin

            boolean isAdjacent = false;
            for (Couple<Location, List<Couple<Observation, String>>> couple : lobs) {
                if (couple.getLeft().getLocationId().equals(nextNodeId)) {
                    isAdjacent = true;
                    break;
                }
            }

            if (!isAdjacent) {
                System.out.println(this.myAgent.getLocalName() + " - ATTENTION: Le nœud " + nextNodeId +
                        " n'est pas adjacent à " + myPosition.getLocationId() +
                        ". Recherche d'une alternative...");

                // Si le nœud n'est pas adjacent, on cherche un nœud adjacent accessible
                for (Couple<Location, List<Couple<Observation, String>>> couple : lobs) {
                    String candidateId = couple.getLeft().getLocationId();
                    if (!candidateId.equals(myPosition.getLocationId())) {
                        nextNodeId = candidateId;
                        System.out.println(
                                this.myAgent.getLocalName() + " - Alternative trouvée: " + nextNodeId);
                        break;
                    }
                }
            }
        } else {
            System.out.println(
                    this.myAgent.getLocalName() + " - ERREUR: Aucun chemin trouvé vers un nœud ouvert");
            try {
                this.myAgent.doWait(10000); // Attendre avant de réessayer
            } catch (Exception e) {
                e.printStackTrace();
            }
            return; // Sortir de l'action pour réessayer au prochain cycle
        }

        boolean moved = ((AbstractDedaleAgent) this.myAgent).moveTo(new GsLocation(nextNodeId));
        if (!moved){ // si collision c'est parce que l'agent est arrivé au noeud adjacent du trésor
			System.out.println(this.myAgent.getLocalName() + " - Collision détectée vers " + nextNodeId);
            for (Couple<Location, List<Couple<Observation, String>>> node : lobs){
                for (Couple<Observation, String> obs : node.getRight()){
                    if (obs.getLeft().getName().equals("AgentName")){ 
                        // FIXME faudrait qu'il vérifie que c'est l'agent qui demande aide ou un autre agent qui est la pour aider mais comment ?
                        finished = true;
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
