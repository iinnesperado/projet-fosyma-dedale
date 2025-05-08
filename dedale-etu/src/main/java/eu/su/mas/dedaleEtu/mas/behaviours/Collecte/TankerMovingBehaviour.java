package eu.su.mas.dedaleEtu.mas.behaviours.Collecte;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.gs.GsLocation;
import eu.su.mas.dedale.env.Observation;
import dataStructures.tuple.Couple;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Comportement permettant à un agent Tanker de se déplacer temporairement
 * pour laisser passer un autre agent, puis revenir à sa position initiale.
 */
public class TankerMovingBehaviour extends SimpleBehaviour {

    private static final long serialVersionUID = 1L;
    private Location originalPosition;
    private Location targetNodeId;
    private Location currentPosition;
    private String agentName;
    private boolean success = false;

    public TankerMovingBehaviour(Agent myAgent, String agentName) {
        super(myAgent);
        // Initialiser la position d'origine du Tanker
        this.originalPosition = ((AbstractDedaleAgent) myAgent).getCurrentPosition();
        this.agentName = agentName;
    }

    @Override
    public void action() {
        // Récupérer la position actuelle du Tanker
        currentPosition = ((AbstractDedaleAgent) this.myAgent).getCurrentPosition();
        List<Couple<Location, List<Couple<Observation, String>>>> lobs = ((AbstractDedaleAgent) this.myAgent).observe();
        // Le Tanker choisit un noeud voisin pour se déplacer
        String targetNodeId = null;
        Iterator<Couple<Location, List<Couple<Observation, String>>>> iter = lobs.iterator();
        while (iter.hasNext()) {
            Couple<Location, List<Couple<Observation, String>>> obs = iter.next();
            if (obs.getLeft().getLocationId() != currentPosition.getLocationId()) {
                if (obs.getRight().isEmpty()) {
                    targetNodeId = obs.getLeft().getLocationId();
                    break;
                }
            }
        }
        if (targetNodeId != null) {
            // Afficher le message de déplacement
            System.out.println(myAgent.getLocalName() + " se déplace vers le noeud cible : " + targetNodeId);
            // Envoyer un message à l'agent pour lui informer du déplacement
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.setProtocol("TANKER-MOVED");
            msg.setSender(this.myAgent.getAID());
            msg.addReceiver(new AID(agentName, AID.ISLOCALNAME));
            msg.setContent("Je me déplace vers le noeud cible : " + targetNodeId);
            ((AbstractDedaleAgent) this.myAgent).sendMessage(msg);

            // Déplacer le Tanker vers le noeud cible
            boolean moved = ((AbstractDedaleAgent) this.myAgent).moveTo(new GsLocation(targetNodeId));
            if (!moved) {
                System.out.println(
                        myAgent.getLocalName() + " n'a pas pu se déplacer vers le noeud cible : " + targetNodeId);
            }
            // Retourner à la position d'origine après un délai de 20 secondes
            try {
                Thread.sleep(20000); // Attendre 20 secondes
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            ((AbstractDedaleAgent) this.myAgent).moveTo(originalPosition);
            System.out.println(myAgent.getLocalName() + " est revenu à sa position d'origine : "
                    + originalPosition.getLocationId());

        }
    }

    @Override
    public boolean done() {
        // Vérifier si le Tanker a terminé son déplacement
        if (currentPosition.getLocationId().equals(originalPosition.getLocationId())) {
            success = true;
            return true;
        }
        return false;
    }
}
