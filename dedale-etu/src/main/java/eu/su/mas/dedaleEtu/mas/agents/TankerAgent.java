package eu.su.mas.dedaleEtu.mas.agents;

import java.util.ArrayList;
import java.util.List;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.behaviours.platformManagment.*;
import eu.su.mas.dedaleEtu.mas.behaviours.TankerMovingBehaviour;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.tools.sniffer.Message;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.ACLMessage;

/**
 * Dummy Tanker agent. It does nothing more than printing what it observes every
 * 10s and receiving the treasures from other agents.
 * <p>
 * Note that this last behaviour is hidden, every tanker agent automatically
 * possess it.
 * 
 * @author hc
 *
 */
public class TankerAgent extends AbstractDedaleAgent {

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
        lb.add(new TankerBehaviour(this));

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

/**************************************
 * 
 * 
 * BEHAVIOUR
 * 
 * 
 **************************************/

class TankerBehaviour extends TickerBehaviour {
    /**
     * When an agent choose to migrate all its components should be serializable
     * 
     */
    private static final long serialVersionUID = 908820940250775289L;

    public TankerBehaviour(final AbstractDedaleAgent myagent) {
        super(myagent, 1000);
    }

    @Override
    public void onTick() {
        // Si on reçoit un message pour bouger
        MessageTemplate msgTemplate = MessageTemplate.and(
                MessageTemplate.MatchProtocol("REQUEST-TANKER-MOVE"),
                MessageTemplate.MatchPerformative(ACLMessage.INFORM));
        ACLMessage msgReceived = this.myAgent.blockingReceive(msgTemplate, 5000);

        if (msgReceived != null) {
            this.myAgent.addBehaviour(new TankerMovingBehaviour((AbstractDedaleAgent) this.myAgent,
                    msgReceived.getSender().getLocalName()));
        }

    }

}