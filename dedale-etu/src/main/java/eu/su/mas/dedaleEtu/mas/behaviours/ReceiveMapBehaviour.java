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

public class ReceiveMapBehaviour extends OneShotBehaviour {

	private static final long serialVersionUID = -1082324081791796312L;

	// private List<String> list_agentNames;
	// private boolean finished = false;
	private String agent;

	/**
	 * Current knowledge of the agent regarding the environment
	 */
	private MapRepresentation myMap;

	public ReceiveMapBehaviour(final AbstractDedaleAgent myAgent, MapRepresentation myMap, String agentName) {
		super(myAgent);
		this.myMap = myMap;
		this.agent = agentName;
	}

	@Override
	public void action() {

		ACLMessage ping = new ACLMessage(ACLMessage.INFORM);
        ping.setProtocol("PING");
        ping.setSender(myAgent.getAID());
        ping.addReceiver(new AID(agent, AID.ISLOCALNAME));
        ((AbstractDedaleAgent) myAgent).sendMessage(ping);
        System.out.println(myAgent.getLocalName() + " a envoyé un PING à " + agent);

		// On s'attend à recevoir un PING
		// Création du template pour recevoir un message de type PING
		MessageTemplate msgTemplate = MessageTemplate.and(
				MessageTemplate.MatchProtocol("PING"),
				MessageTemplate.MatchPerformative(ACLMessage.INFORM));
		ACLMessage msgReceived = this.myAgent.blockingReceive(msgTemplate, 5000);

		// Si on reçoit un message PING on répond avec un PONG
		if (msgReceived != null) {
			System.out.println(this.myAgent.getName() + " a reçu un ping");
			// ACLMessage pong = new ACLMessage(ACLMessage.INFORM);
			// pong.setProtocol("PONG");
			// pong.setSender(this.myAgent.getAID());
			// pong.addReceiver(msgReceived.getSender());
			// try {
			// pong.setContentObject("PONG");
			// } catch (IOException e) {
			// e.printStackTrace();
			// }
			// this.myAgent.send(pong);
			// System.out.println(this.myAgent.getName() + " a envoyé un pong");

			// On s'attend à recevoir une map
			msgTemplate = MessageTemplate.and(
					MessageTemplate.MatchProtocol("SHARE-TOPO"),
					MessageTemplate.MatchPerformative(ACLMessage.INFORM));
			msgReceived = this.myAgent.blockingReceive(msgTemplate, 5000);

			if (msgReceived != null) {
				SerializableSimpleGraph<String, MapAttribute> sgreceived = null;
				try {
					sgreceived = (SerializableSimpleGraph<String, MapAttribute>) msgReceived.getContentObject();
				} catch (UnreadableException e) {
					e.printStackTrace();
				}

				if (sgreceived != null) {
					this.myMap.mergeMap(sgreceived);
					System.out.println(this.myAgent.getLocalName() + " a fusionné la carte de "
							+ msgReceived.getSender().getLocalName() + " avec sa carte");
				}

				// Accusé de réception de la map
				ACLMessage ack = new ACLMessage(ACLMessage.INFORM);
				ack.setProtocol("ACK");
				ack.setSender(this.myAgent.getAID());
				ack.addReceiver(msgReceived.getSender());
				((AbstractDedaleAgent) this.myAgent).sendMessage(ack);
				System.out.println(this.myAgent.getLocalName() + " a envoyé un ack");
			}
		}
		// finished = true;
	}

}