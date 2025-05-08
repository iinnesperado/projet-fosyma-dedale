package eu.su.mas.dedaleEtu.mas.behaviours.communication;

import java.util.List;
import java.util.Set;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.behaviours.exploration.MoveToTreasureBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.Treasure;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

public class GoHelpOpenLockBehaviour extends CyclicBehaviour{

    private static final long serialVersionUID = -7652269498493020185L;

    private MapRepresentation myMap;

    public GoHelpOpenLockBehaviour(final AbstractDedaleAgent myagent, MapRepresentation mymap){
        super(myagent);
        this.myMap = mymap;
    }

    @Override
    public void action() {
        MessageTemplate msgTemplate = MessageTemplate.and(MessageTemplate.MatchProtocol("HELP-TRESOR"), 
                MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
        ACLMessage msg = this.myAgent.receive(msgTemplate);
        if (msg != null) {
            Treasure treasure = null;
            try {
                treasure = (Treasure) msg.getContentObject();
            } catch (UnreadableException e){
                e.printStackTrace();
            }
            
            if (treasure != null && canHelp(treasure.getRequiredSkills())) {
                // Accepter si compétences suffisantes
                ACLMessage agree = new ACLMessage(ACLMessage.AGREE);
                agree.setProtocol("GO-HELP");
                agree.setSender(this.myAgent.getAID());
                agree.addReceiver(msg.getSender());
                ((AbstractDedaleAgent)this.myAgent).sendMessage(agree);
                
                // Move to treasure
                this.myAgent.addBehaviour(new MoveToTreasureBehaviour((AbstractDedaleAgent)this.myAgent, myMap, treasure, msg.getSender().getName()));
          }
        }
    }

    public boolean canHelp(Couple<Integer,Integer> requiredSkill) {
        Set<Couple<Observation,Integer>> expertise = ((AbstractDedaleAgent)this.myAgent).getMyExpertise();
        
        // Check if agent has both required skills at sufficient levels
        boolean hasLockpicking = expertise.stream()
            .anyMatch(e -> e.getLeft().equals(Observation.LOCKPICKING) 
                && e.getRight() >= requiredSkill.getLeft());
                
        boolean hasStrength = expertise.stream()
            .anyMatch(e -> e.getLeft().equals(Observation.STRENGH) 
                && e.getRight() >= requiredSkill.getRight());
                
        return hasLockpicking && hasStrength;
    }
}
