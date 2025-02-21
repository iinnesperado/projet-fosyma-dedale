package eu.su.mas.dedaleEtu.mas.behaviours;

import java.io.IOException;
import java.util.List;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

public class ReceiveMapBehaviour extends OneShotBehaviour{

	private static final long serialVersionUID = -1082324081791796312L;

	
	private List<String> list_agentNames;	
	private boolean finished = false;

	/**
	 * Current knowledge of the agent regarding the environment
	 */
	private MapRepresentation myMap;

	
	public ReceiveMapBehaviour(final Agent myAgent, MapRepresentation myMap, List<String> agentNames) {
		super(myAgent);
		this.myMap=myMap;
		this.list_agentNames=agentNames;
	}
	
	@Override
	public void action() {
		/*
	}
		MessageTemplate msgTemplate=MessageTemplate.and(
				MessageTemplate.MatchProtocol("SHARE-TOPO"),
				MessageTemplate.MatchPerformative(ACLMessage.INFORM));
		ACLMessage msgReceived=this.myAgent.receive(msgTemplate);
		
		if (msgReceived!=null) {
			ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
			msg.setProtocol("BIEN-REÇU");
			msg.setSender(this.myAgent.getAID());
			for (String agentName : list_agentNames) {
				msg.addReceiver(new AID(agentName,AID.ISLOCALNAME));
			}
				
			SerializableSimpleGraph<String, MapAttribute> sg=this.myMap.getSerializableGraph();
			try {					
				msg.setContentObject(sg);
			} catch (IOException e) {
				e.printStackTrace();
			}
			((AbstractDedaleAgent)this.myAgent).sendMessage(msg);
		*/
		MessageTemplate msgTemplate=MessageTemplate.and(
				MessageTemplate.MatchProtocol("SHARE-TOPO"),
				MessageTemplate.MatchPerformative(ACLMessage.INFORM));
		ACLMessage msgReceived=this.myAgent.receive(msgTemplate);
		if (msgReceived!=null) {
			SerializableSimpleGraph<String, MapAttribute> sgreceived=null;
			try {
				sgreceived = (SerializableSimpleGraph<String, MapAttribute>)msgReceived.getContentObject();
			} catch (UnreadableException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			this.myMap.mergeMap(sgreceived);
			System.out.println(this.myAgent.getName()+" a reçu une map");
		}else {
			block(3000);
		}

	}

}
