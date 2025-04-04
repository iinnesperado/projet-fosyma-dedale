package eu.su.mas.dedaleEtu.mas.behaviours;

import java.io.IOException;
import java.util.List;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import jade.core.AID;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

public class ShareUpdateMapBehaviour extends SimpleBehaviour{

	private static final long serialVersionUID = -3970545599627954120L;

	public ShareUpdateMapBehaviour(MapRepresentation mymap, List<String> receivers) {
		this.myMap = mymap;
		this.receivers = receivers;
	}
	private MapRepresentation myMap;
	private List<String> receivers;
	
	@Override
	public void action() {
		// If received a map then the agents sends back the difference only
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
			
		}
	}

	@Override
	public boolean done() {
		// TODO Auto-generated method stub
		return false;
	}

}
