package eu.su.mas.dedaleEtu.mas.behaviours;

import java.util.List;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import jade.core.behaviours.SimpleBehaviour;

public class FoundTreasureBehaviour extends SimpleBehaviour{

    private static final long serialVersionUID = 3973250783964285694L;

    @Override
    public void action() {
        Location myPosition=((AbstractDedaleAgent)this.myAgent).getCurrentPosition();

			if (myPosition!=null && myPosition.getLocationId()!=""){
				List<Couple<Location,List<Couple<Observation,String>>>> lobs=((AbstractDedaleAgent)this.myAgent).observe();//myPosition
				// System.out.println(this.myAgent.getLocalName()+" -- list of observables: "+lobs);

                //list of observations associated to the currentPosition
				List<Couple<Observation,String>> lObservations= lobs.get(0).getRight();

                
            }
}

    @Override
    public boolean done() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'done'");
    }
    
}
