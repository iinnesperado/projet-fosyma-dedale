package eu.su.mas.dedaleEtu.mas.behaviours.tryoutExplocoopFSM;

import java.util.ArrayList;
import java.util.List;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

public class ReceiveTreasureInfoBehaviour extends CyclicBehaviour{

    private static final long serialVersionUID = 6845596473642476071L;
    private List<Treasure> ltreasures ;
    
    public ReceiveTreasureInfoBehaviour(final AbstractDedaleAgent myagent, List<Treasure> treasures) {
        super(myagent);
        this.ltreasures = treasures != null ? treasures : new ArrayList<>();
    }

    @Override
    public void action() {
        MessageTemplate mt = MessageTemplate.and(
                MessageTemplate.MatchProtocol("TREASURE-SHARE"),
                MessageTemplate.MatchPerformative(ACLMessage.INFORM));

        ACLMessage msg = myAgent.receive(mt);

        if (msg != null) {
            try {
                @SuppressWarnings("unchecked")
                List<Treasure> receivedTreasures = (List<Treasure>) msg.getContentObject();
                if (receivedTreasures != null){
                    updateTreasureList(receivedTreasures);
                }

            } catch (UnreadableException e) {
                System.err.println(myAgent.getLocalName() + " - Erreur lors de la lecture du message reçu : " + e.getMessage());
            }
        } else {
            System.out.println(myAgent.getLocalName() + " - Waiting for treasure info to be passed ");
            block();
        }
    }

    public void updateTreasureList(List<Treasure> receivedTreasures){
        if(receivedTreasures.isEmpty()){
            return ;
        }
        
        for (Treasure receivedTreasure : receivedTreasures) {
            boolean treasureExists = false;
            
            // Check if treasure already exists and update if newer
            for (int i = 0; i < ltreasures.size(); i++) {
                Treasure currentTreasure = ltreasures.get(i);
                if (currentTreasure.getPositionID().equals(receivedTreasure.getPositionID())) {
                    treasureExists = true;
                    if (receivedTreasure.getRecordTime().isAfter(currentTreasure.getRecordTime())) {
                        ltreasures.set(i, receivedTreasure);
                        System.out.println(myAgent.getLocalName() + " - Updated treasure at " + 
                            receivedTreasure.getPositionID());
                    }
                    break;
                }
            }
            
            // Add new treasure if it doesn't exist
            if (!treasureExists) {
                ltreasures.add(receivedTreasure);
                System.out.println(myAgent.getLocalName() + " - Added new treasure at " + 
                    receivedTreasure.getPositionID());
            }
        }
    }
}
