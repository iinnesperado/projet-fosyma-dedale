package eu.su.mas.dedaleEtu.mas.behaviours;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.TresorInfo;
import eu.su.mas.dedaleEtu.mas.knowledge.TresorMessage;

import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Envoie la liste des trésors avec la date de dernière mise à jour à un agent
 * donné.
 */
public class SendTresorBehaviour extends OneShotBehaviour {

    private static final long serialVersionUID = 1L;

    private final List<TresorInfo> listeTresors;
    private final LocalDateTime dateMaj;
    private final String receiverName;

    public SendTresorBehaviour(AbstractDedaleAgent agent, List<TresorInfo> listeTresors, LocalDateTime dateMaj,
            String receiverName) {
        super(agent);
        this.listeTresors = listeTresors;
        this.dateMaj = dateMaj;
        this.receiverName = receiverName;
    }

    @Override
    public void action() {
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.setProtocol("TRESOR-SHARE");
        msg.setSender(myAgent.getAID());
        msg.addReceiver(new AID(receiverName, AID.ISLOCALNAME));

        TresorMessage data = new TresorMessage(listeTresors, dateMaj);

        try {
            msg.setContentObject(data);
            myAgent.send(msg);
            System.out.println(myAgent.getLocalName() + " a envoyé sa liste de trésors à " + receiverName);
        } catch (IOException e) {
            System.err.println("Erreur lors de l'envoi du message de trésors : " + e.getMessage());
        }
    }
}
