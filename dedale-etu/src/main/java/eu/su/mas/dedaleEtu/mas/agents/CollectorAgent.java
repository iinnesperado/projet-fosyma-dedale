package eu.su.mas.dedaleEtu.mas.agents;

import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.behaviours.platformManagment.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import dataStructures.tuple.Couple;

import jade.core.behaviours.Behaviour;
import jade.core.behaviours.TickerBehaviour;

/**
 * This dummy collector moves randomly, tries all its methods at each time step,
 * store the treasure that match is treasureType
 * in its backpack and intends to empty its backPack in the Tanker agent. @see
 * {@link RandomWalkExchangeBehaviour}
 * 
 * @author hc
 *
 */
public class CollectorAgent extends AbstractDedaleAgent {

    /**
     * 
     */
    private static final long serialVersionUID = -1784844593772918359L;

    /**
     * This method is automatically called when "agent".start() is executed.
     * Consider that Agent is launched for the first time.
     * 1) set the agent attributes
     * 2) add the behaviours
     * 
     */
    protected void setup() {

        super.setup();

        List<Behaviour> lb = new ArrayList<Behaviour>();

        addBehaviour(new StartMyBehaviours(this, lb));

        System.out.println("the  agent " + this.getLocalName() + " is started");

    }

    /**
     * This method is automatically called after doDelete()
     */
    protected void takeDown() {
        super.takeDown();
    }

    protected void beforeMove() {
        super.beforeMove();
        // System.out.println("I migrate");
    }

    protected void afterMove() {
        super.afterMove();
        // System.out.println("I migrated");
    }
}
