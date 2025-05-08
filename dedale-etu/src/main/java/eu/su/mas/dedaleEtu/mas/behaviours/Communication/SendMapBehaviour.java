package eu.su.mas.dedaleEtu.mas.behaviours.Communication;

import java.io.IOException;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class SendMapBehaviour extends OneShotBehaviour {

    private static final long serialVersionUID = 8566698877771124934L;
    private boolean finished = false;

    /**
     * Current knowledge of the agent regarding the environment
     */
    private MapRepresentation myMap;

    private String agent;

    /**
     * @param myagent   reference to the agent we are adding this behaviour to
     * @param myMap     known map of the world the agent is living in
     * @param agentName name of the agent to share the map with
     */
    public SendMapBehaviour(final AbstractDedaleAgent myagent, MapRepresentation myMap, String agentName) {
        super(myagent);
        this.myMap = myMap;
        this.agent = agentName;
    }

    @Override
    public void action() {

        // Création du ping pour vérifier la présence de l'agent cible
        // ACLMessage ping = new ACLMessage(ACLMessage.INFORM);
        // ping.setProtocol("PING");
        // ping.setSender(this.myAgent.getAID());
        // ping.addReceiver(new AID(agent, AID.ISLOCALNAME));
        // ((AbstractDedaleAgent) this.myAgent).sendMessage(ping);
        // System.out.println(this.myAgent.getLocalName() + " a envoyé un PING à " +
        // agent);

        // // Attente de réponse PONG
        // MessageTemplate msgTemplate = MessageTemplate.and(
        // MessageTemplate.MatchProtocol("PONG"),
        // MessageTemplate.MatchPerformative(ACLMessage.INFORM));
        MessageTemplate msgTemplate = MessageTemplate.and(MessageTemplate.MatchProtocol("PING"),
                MessageTemplate.MatchPerformative(ACLMessage.INFORM));
        ACLMessage msgReceived = this.myAgent.blockingReceive(msgTemplate, 5000);
        if (msgReceived != null) {
            System.out.println(this.myAgent.getLocalName() + " envoie topologie à " + agent);

            // Création du message de partage de carte
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.setProtocol("SHARE-TOPO");
            msg.setSender(this.myAgent.getAID());
            msg.addReceiver(new AID(agent, AID.ISLOCALNAME));

            // Sérialisation de la carte
            SerializableSimpleGraph<String, MapAttribute> sg = this.myMap.getSerializableGraph();
            try {
                msg.setContentObject(sg);
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Envoi de la carte
            ((AbstractDedaleAgent) this.myAgent).sendMessage(msg);
            System.out.println(this.myAgent.getLocalName() + " a envoyé une carte à " + agent);
            onEnd();
        }
    }
}