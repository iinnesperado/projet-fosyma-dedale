package eu.su.mas.dedaleEtu.mas.behaviours.communication;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.Treasure;
import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class AskHelpOpenLockBehaviour extends OneShotBehaviour{

    private static final long serialVersionUID = 2565313647619629446L;

    private Treasure tresor;

    public AskHelpOpenLockBehaviour(final AbstractDedaleAgent myagent, Treasure tresor){
        super(myagent);
        this.tresor = tresor;
    }

    @Override
    public void action() {
        List<Couple<Location, List<Couple<Observation, String>>>> observations = ((AbstractDedaleAgent)this.myAgent).observe();
        ACLMessage helpRequest = new ACLMessage(ACLMessage.REQUEST);
        helpRequest.setProtocol("HELP-TRESOR");
        for (String agentName : getNearbyAgents(observations)){
            helpRequest.addReceiver(new AID(agentName, AID.ISLOCALNAME));
        }
        helpRequest.setSender(this.myAgent.getAID());

        try{
            helpRequest.setContentObject((Serializable) tresor);
        } catch (IOException e){
            e.printStackTrace();
        }

        ((AbstractDedaleAgent)this.myAgent).sendMessage(helpRequest);
    }

    public List<String> getNearbyAgents(List<Couple<Location, List<Couple<Observation, String>>>> observations){
        List<String> agents = new ArrayList<>();
        for (Couple<Location, List<Couple<Observation, String>>> node : observations){
            for (Couple<Observation, String> obs : node.getRight()){
                if(obs.getLeft().getName().equals("AgentName")){
                    agents.add(obs.getRight());
                }
            }
        }
        return agents;
    }
}
