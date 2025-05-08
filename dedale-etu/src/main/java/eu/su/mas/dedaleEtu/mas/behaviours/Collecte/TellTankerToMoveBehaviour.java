package eu.su.mas.dedaleEtu.mas.behaviours.Collecte;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;

import org.apache.commons.math3.analysis.function.Abs;

import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.gs.GsLocation;

import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

/**
 * Comportement permettant à un agent d'exploration de demander à un Tanker de
 * se déplacer
 * temporairement pour libérer le passage.
 */
public class TellTankerToMoveBehaviour extends OneShotBehaviour {

    private static final long serialVersionUID = 1L;
    private final String tankerName;
    private final String myPosition;
    private boolean success = false;

    /**
     * @param myAgent      L'agent qui exécute ce comportement
     * @param tankerName   Le nom de l'agent Tanker à qui demander de se déplacer
     * @param targetNodeId L'ID du nœud où l'agent souhaite se déplacer (occupé par
     *                     le Tanker)
     * @param myPosition   L'ID du nœud actuel de l'agent
     */
    public TellTankerToMoveBehaviour(AbstractDedaleAgent myAgent, String tankerName,
            String myPosition) {
        super(myAgent);
        this.tankerName = tankerName;
        this.myPosition = myPosition;
    }

    @Override
    public void action() {
        // Créer un message pour demander au Tanker de se déplacer
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.setProtocol("REQUEST-TANKER-MOVE");
        msg.setSender(myAgent.getAID());
        msg.addReceiver(new AID(tankerName, AID.ISLOCALNAME));
        msg.setContent("Déplace-toi !");
        // Envoyer le message
        ((AbstractDedaleAgent) this.myAgent).sendMessage(msg);
        System.out.println(myAgent.getLocalName() + " a demandé à " + tankerName
                + " de se déplacer");

        // Attendre une réponse du Tanker
        MessageTemplate msgTemplate = MessageTemplate.and(
                MessageTemplate.MatchProtocol("TANKER-MOVED"),
                MessageTemplate.MatchPerformative(ACLMessage.INFORM));
        ACLMessage msgReceived = this.myAgent.blockingReceive(msgTemplate, 10000);
        if (msgReceived != null) {
            System.out.println(myAgent.getLocalName() + " a reçu une confirmation de " + tankerName);
            success = true;
        } else {
            System.out.println(myAgent.getLocalName() + " n'a pas reçu de confirmation de " + tankerName);
        }
        onEnd();
    }
}