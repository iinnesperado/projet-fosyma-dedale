package eu.su.mas.dedaleEtu.mas.behaviours;

import java.util.List;

import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import jade.core.AID;
import jade.core.behaviours.SimpleBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
/**
 * This example behaviour try to send a hello message (every 3s maximum) to agents Collect2 Collect1
 * @author hc
 *
 */
public class SayHelloBehaviour extends SimpleBehaviour{

	/**
	 * 
	 */
	private static final long serialVersionUID = -2058134622078521998L;

	/**
	 * An agent tries to contact its friend and to give him its current position
	 * @param myagent the agent who posses the behaviour
	 *  
	 */
	private List<String> receivers;
	private boolean finished;

	/**
	 * Current knowledge of the agent regarding the environment
	 */
	private MapRepresentation myMap;

	
	public SayHelloBehaviour (final AbstractDedaleAgent myagent, MapRepresentation myMap, List<String> receivers) {
		super(myagent);
		//super(myagent);
		this.myMap=myMap;
		this.receivers=receivers;
	}


	@Override
	public void action() {
		Location myPosition=((AbstractDedaleAgent)this.myAgent).getCurrentPosition();

		//A message is defined by : a performative, a sender, a set of receivers, (a protocol),(a content (and/or contentOBject))
		ACLMessage msg=new ACLMessage(ACLMessage.INFORM);
		
		msg.setSender(this.myAgent.getAID());
		msg.setProtocol("Hello");

		if (myPosition!=null && myPosition.getLocationId()!=""){
			//System.out.println("Agent "+this.myAgent.getLocalName()+ " is trying to reach its friends");
			msg.setContent("Hello World, I'm at "+myPosition);

			for (String agentName : receivers) {
				msg.addReceiver(new AID(agentName,AID.ISLOCALNAME));
			}

			//Mandatory to use this method (it takes into account the environment to decide if someone is reachable or not)
			((AbstractDedaleAgent)this.myAgent).sendMessage(msg);
			
		}
		System.out.println("Agent "+this.myAgent.getLocalName()+ " a dit bonjour");
		this.finished=true;
	}

	@Override
	public boolean done() {
		return finished;
	}
}