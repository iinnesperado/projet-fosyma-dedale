package eu.su.mas.dedaleEtu.mas.tryoutExplocoopFSM;

import java.util.ArrayList;
import java.util.List;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.behaviours.ReceiveMapBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.SendMapBehaviourOld;
import jade.core.behaviours.SimpleBehaviour;

/*
 * In charge of sharing the Map information and the Treasor information encounter by myAgent to other agents he encounters.
 */

public class ShareInformationBehaviour extends SimpleBehaviour{
    private static final long serialVersionUID = -8130243764593286004L;
    private MapRepresentation myMap;
    private List<String> list_agentNames;
    // private List<Couple<String, MapRepresentation>> list_maps;
    private List<Treasure> ltreasures;

    private boolean finished = false;

    public ShareInformationBehaviour(final AbstractDedaleAgent myagent, MapRepresentation mymap, List<String> agentNames){
        super(myagent);
        this.myMap = mymap;
        // this.list_maps = new ArrayList<>();
        this.list_agentNames = agentNames;
    }

    @Override
    public void action(){
        List<Couple<Location, List<Couple<Observation, String>>>> lobs = ((AbstractDedaleAgent)this.myAgent).observe();
        
        List<String> nearAgents = new ArrayList<>();
        for (Couple<Location, List<Couple<Observation, String>>> coupleInfo : lobs) {
            List<Couple<Observation, String>> observations = coupleInfo.getRight();
            for (Couple<Observation, String> obs : observations){
                if (obs.getLeft().getName().equals("AgentName") && !obs.getRight().equals(this.myAgent.getLocalName())) {
                    String agentName = obs.getRight();
                    nearAgents.add(agentName); // Récupérer la valeur String et l'ajouter
                    this.myAgent.addBehaviour(new ReceiveMapBehaviour((AbstractDedaleAgent)this.myAgent, this.myMap, list_agentNames));
                    this.myAgent.addBehaviour(new ReceiveTreasureInfoBehaviour((AbstractDedaleAgent)this.myAgent,this.ltreasures));

                    this.myAgent.addBehaviour(new SendMapBehaviourOld((AbstractDedaleAgent) this.myAgent, this.myMap, agentName)); // TODO make it so it shares only the bit that's new using list_maps
                    this.myAgent.addBehaviour(new SendTreasureInfoBehaviour((AbstractDedaleAgent)this.myAgent,this.ltreasures, agentName));
                }
            }
        }
    }


    @Override
    public boolean done(){
        return finished;
    }
}
