package eu.su.mas.dedaleEtu.mas.behaviours.Communication;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.Treasure;
import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.io.IOException;
import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import dataStructures.tuple.Couple;

/**
 * Envoie la liste des trésors avec la date de dernière mise à jour à un agent
 * donné.
 */
public class SendTresorBehaviour extends OneShotBehaviour {

    private static final long serialVersionUID = 1L;

    private final List<Treasure> listeTresors;
    private final String receiverName;
    private List<Couple<String, Couple<LocalDateTime,LocalDateTime>>> lastContact;

    public SendTresorBehaviour(AbstractDedaleAgent agent, List<Treasure> listeTresors,
            String receiverName, List<Couple<String, Couple<LocalDateTime,LocalDateTime>>> lastContact) {
        super(agent);
        this.listeTresors = listeTresors;
        this.receiverName = receiverName;
        this.lastContact = lastContact;
    }

    @Override
    public void action() {

		System.out.println(myAgent.getLocalName() + " veut envoyer un PING à " + receiverName);
        if (shouldContact(receiverName)) {
            // On envoit un PING pour vérifier la présence de l'agent cible
            updateLastContact(receiverName);
            ACLMessage ping = new ACLMessage(ACLMessage.INFORM);
            ping.setProtocol("PING");
            ping.setSender(myAgent.getAID());
            ping.addReceiver(new AID(receiverName, AID.ISLOCALNAME));
            ((AbstractDedaleAgent) myAgent).sendMessage(ping);
            System.out.println(myAgent.getLocalName() + " a envoyé un PING à " + receiverName);
        }

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

    private boolean shouldContact(String agentName) {
		for (Couple<String, Couple<LocalDateTime,LocalDateTime>> agent : lastContact) {
            System.out.println(myAgent.getLocalName() + " - " + agent.getLeft() + " == " + agentName);
			if (agent.getLeft().equals(agentName)) {
			    Couple<LocalDateTime, LocalDateTime> contact = agent.getRight(); // contact.getRight() is the last contact time for Treasure sharing
				Duration timeSinceLastContact = Duration.between(contact.getRight(), LocalDateTime.now());
				return timeSinceLastContact.getSeconds() >= 20; // Update if 1+ minutes passed
			}
		}
		return true; // No previous contact found
	}

	private void updateLastContact(String agentName) {
        // System.out.println(myAgent.getLocalName() + " LAST TIME CONTACT LIST (trésor)" + lastContact);
		for (int i = 0; i < lastContact.size(); i++) {
			if (lastContact.get(i).getLeft().equals(agentName)) {
                LocalDateTime mapTime = lastContact.get(i).getRight().getLeft();
                Couple<LocalDateTime, LocalDateTime> times = new Couple<>(mapTime, LocalDateTime.now()); // Update only the time for Treasure sharing
				lastContact.set(i, new Couple<>(agentName, times));
                System.out.println(myAgent.getLocalName() + " a mis à jour le contact (trésor) avec " + agentName);
				return;
			}
		}
		lastContact.add(new Couple<>(agentName, new Couple<>(LocalDateTime.now(), LocalDateTime.now())));
        System.out.println(myAgent.getLocalName() + " a ajouté un nouveau contact (trésor) " + agentName + lastContact);
	}
}