package eu.su.mas.dedaleEtu.mas.tryoutExplocoopFSM;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;

public class SendTreasureInfoBehaviour extends OneShotBehaviour{
    
    private static final long serialVersionUID = 6881230340501732093L;

    private final List<Treasure> ltreasures;
    private final String receiverName;

    public SendTreasureInfoBehaviour(final AbstractDedaleAgent myagent, List<Treasure> treasures, String receiverName) {
        super(myagent);
        this.ltreasures = treasures;
        this.receiverName = receiverName;
    }

    @Override
    public void action() {
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.setProtocol("TREASURE-SHARE");
        msg.setSender(myAgent.getAID());
        msg.addReceiver(new AID(receiverName, AID.ISLOCALNAME));

        try {
            msg.setContentObject((Serializable)ltreasures);
            ((AbstractDedaleAgent)myAgent).sendMessage(msg);
            System.out.println(myAgent.getLocalName() + " a envoyé ses INFORMATIONS de trésors à " + receiverName);
        } catch (IOException e) {
            System.err.println("Erreur lors de l'envoi du message de trésors : " + e.getMessage());
        }
    }
    
}
