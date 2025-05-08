package eu.su.mas.dedaleEtu.mas.tryoutExplocoopFSM;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import jade.core.behaviours.OneShotBehaviour;

/**
 * NOTE Should add a signal before it moves ? like a msg to itself could be teh signal
 * TODO maybe add something that would take into account collisions on couloirs
 */

public class CollisionHandlingBehaviour extends OneShotBehaviour {

    private static final long serialVersionUID = 2482684691963062976L;
    private String blockedNodeId;

    public CollisionHandlingBehaviour(final AbstractDedaleAgent myagent, String notReachedNodeId){
        super(myagent);
        this.blockedNodeId = notReachedNodeId;
    }
    @Override
    public void action() {
        List<Couple<Location, List<Couple<Observation, String>>>> lobs = ((AbstractDedaleAgent)this.myAgent).observe();
        List<Location> freePositions = new ArrayList<>();
        // Attendre un temps aléatoire pour désynchroniser les agents
        // int waitTime = 500 + (int) (Math.random() * 1000);
        // try {
        //     this.myAgent.doWait(waitTime);
        // } catch (Exception e) {
        //     e.printStackTrace();
        // }
        
        for (Couple<Location, List<Couple<Observation, String>>> coupleInfo : lobs){
            Location loc = coupleInfo.getLeft();
            if (!loc.getLocationId().equals(blockedNodeId)){
                freePositions.add(loc);
            }
        }

        Location nextNodeId = null;
        if(!freePositions.isEmpty() && freePositions.size()>1) {
            Random random = new Random();
            int randomIndex = 1+random.nextInt(freePositions.size()-1);
            nextNodeId = freePositions.get(randomIndex);
        }
        if (nextNodeId != null){
            ((AbstractDedaleAgent)this.myAgent).moveTo(nextNodeId);
        } else {
            System.out.println(this.myAgent.getLocalName() + " - N'arrive plus à bouger !");
        }
    }
    
}