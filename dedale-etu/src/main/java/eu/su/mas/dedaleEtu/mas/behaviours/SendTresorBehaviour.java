package eu.su.mas.dedaleEtu.mas.behaviours;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.Treasure;
import eu.su.mas.dedaleEtu.mas.knowledge.TresorMessage;

import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Envoie la liste des trésors avec la date de dernière mise à jour à un agent
 * donné.
 */
public class SendTresorBehaviour extends OneShotBehaviour {

    private static final long serialVersionUID = 1L;

    private final List<Treasure> listeTresors;
    private final String receiverName;

    public SendTresorBehaviour(AbstractDedaleAgent agent, List<Treasure> listeTresors,
            String receiverName) {
        super(agent);
        this.listeTresors = listeTresors;
        this.receiverName = receiverName;
    }

    @Override
    public void action() {

        // On envoit un PING pour vérifier la présence de l'agent cible

        ACLMessage ping = new ACLMessage(ACLMessage.INFORM);
        ping.setProtocol("PING");
        ping.setSender(myAgent.getAID());
        ping.addReceiver(new AID(receiverName, AID.ISLOCALNAME));
        ((AbstractDedaleAgent) myAgent).sendMessage(ping);
        System.out.println(myAgent.getLocalName() + " a envoyé un PING à " + receiverName);

        // Si je recçois un message de type PING, je peux envoyer ma liste de trésors

        MessageTemplate msgTemplate = MessageTemplate.and(
                MessageTemplate.MatchProtocol("PING"),
                MessageTemplate.MatchPerformative(ACLMessage.INFORM));
        ACLMessage msgReceived = myAgent.receive(msgTemplate);
        if (msgReceived != null) {
            System.out.println(myAgent.getLocalName() + " a reçu un PING de " + msgReceived.getSender().getLocalName()
                    + ", je vais lui envoyer ma liste de trésors.");
        } else {
            block();
            return;
        }
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.setProtocol("TRESOR-SHARE");
        msg.setSender(myAgent.getAID());
        msg.addReceiver(new AID(receiverName, AID.ISLOCALNAME));
        try {
            msg.setContentObject((Serializable) listeTresors);
            myAgent.send(msg);
            System.out.println(myAgent.getLocalName() + " a envoyé sa liste de trésors à " + receiverName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}