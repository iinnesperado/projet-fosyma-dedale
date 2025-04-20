package eu.su.mas.dedaleEtu.mas.behaviours.tryoutExplocoopFSM;

import java.time.LocalDateTime;
import java.util.List;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.behaviours.PickTreasureBehaviour;
import jade.core.behaviours.OneShotBehaviour;

/**
 * Behaviour for when an agents finds a treasure on its current position
 * @param askHelp if true it adds TreasureAskHelpB to get other agents to come help him openLock(), else leaves the treasure
 */

public class TreasureProcessingBehaviour extends OneShotBehaviour{

    private static final long serialVersionUID = -582103378977630277L;
    private List<Treasure> ltreasures;
    private final boolean askHelp; // in charge of adding TreasureHelpB or not

    public TreasureProcessingBehaviour(final AbstractDedaleAgent myagent, List<Treasure> treasures, boolean askHelp){
        super(myagent);
        this.ltreasures = treasures;
        this.askHelp = askHelp;
    }

    @Override
    public void action() {
        // When treasure found add it to ltreasures if not there
        // else update the information of the treasure (keep it in list even if the amount is 0)
        // Observation list of things on agent's current node
        Couple<Location, List<Couple<Observation, String>>> lobs = ((AbstractDedaleAgent)this.myAgent).observe().get(0);
        for (Couple<Observation, String> obs : lobs.getRight()){
            if (isTreasure(obs.getLeft())){
                this.myAgent.addBehaviour(new PickTreasureBehaviour((AbstractDedaleAgent)this.myAgent, askHelp));
            }
            updateLTreasures(obs);
        }
    }
    

    @Override
    public int onEnd(){
        return 1;
    }

    public void updateLTreasures(Couple<Observation, String> obs){
        if (obs.getLeft().equals(Observation.ANY_TREASURE)){
            System.out.println(ltreasures); // For testing
            Location myPosition = ((AbstractDedaleAgent)this.myAgent).getCurrentPosition();
            Treasure treasure = new Treasure(myPosition, obs.getLeft().getName());
            treasure.toString(); // For testing
            if (ltreasures.contains(treasure)){
                System.out.println("Test : found a treasure that had previously information of");
                ltreasures.remove(treasure);
                System.out.println(ltreasures); // for testing
            }
            treasure.setRecordTime(LocalDateTime.now());
            System.out.println(this.myAgent.getLocalName() + " a trouvé " + treasure.toString());
            ltreasures.add(treasure);
        }
    }

    public boolean isTreasure(Observation o){
        return o.equals(Observation.ANY_TREASURE);
    }

}
