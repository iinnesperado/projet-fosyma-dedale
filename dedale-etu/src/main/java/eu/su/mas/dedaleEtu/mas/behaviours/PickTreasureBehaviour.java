package eu.su.mas.dedaleEtu.mas.behaviours;

import java.util.List;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import jade.core.behaviours.OneShotBehaviour;

public class PickTreasureBehaviour extends OneShotBehaviour{

    private static final long serialVersionUID = 3973250783964285694L;
    private boolean finished = false;
    private final boolean askHelp;

    public PickTreasureBehaviour(final AbstractDedaleAgent myagent, boolean askHelp){
        super(myagent);
        this.askHelp = askHelp;
    }

    @Override
    public void action() {
        Location myPosition=((AbstractDedaleAgent)this.myAgent).getCurrentPosition();

        if (myPosition!=null && myPosition.getLocationId()!=""){
            List<Couple<Location,List<Couple<Observation,String>>>> lobs=((AbstractDedaleAgent)this.myAgent).observe();//myPosition
            // System.out.println(this.myAgent.getLocalName()+" -- list of observables: "+lobs);

            //list of observations associated to the currentPosition
            List<Couple<Observation,String>> posObs= lobs.get(0).getRight();

            for (Couple<Observation, String> attribute : posObs) {
                Observation myTreasureType = ((AbstractDedaleAgent)this.myAgent).getMyTreasureType();
                if (attribute.getLeft() == myTreasureType) {
                    // 3. Open treasure
                    if (((AbstractDedaleAgent)this.myAgent).openLock(myTreasureType)) {
                        // 4. Pick treasure 
                        int collected = ((AbstractDedaleAgent)this.myAgent).pick(); 
                        System.out.println("Collecté : " + collected + " unités d'or.");
                        //TODO ajout B pour que si la capacité sac est sup 60% de cap total il cherche le silo
                    } else {
                        // TODO add behaviour to ask help to other agents

                    }
                }
            }
        }
        finished = true; // Terminer après une collecte
    }
    
}
