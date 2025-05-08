package eu.su.mas.dedaleEtu.mas.tryoutExplocoopFSM;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.env.gs.GsLocation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import jade.core.behaviours.SimpleBehaviour;

public class MapExploBehaviour extends SimpleBehaviour{

    private static final long serialVersionUID = 533195209419339577L;
    private MapRepresentation myMap;
    // private AbstractDedaleAgent myAgent;
    private List<String> list_agentNames;
    private List<Couple<String, MapRepresentation>> list_maps;

    private boolean finished = false;
    private int transitionValue = 0;


    public MapExploBehaviour(final AbstractDedaleAgent myagent, MapRepresentation map, List<String> agents){
        super(myagent);
        this.myMap = map;
        this.list_agentNames = agents;
        this.list_maps = new ArrayList<>();

        if (this.myMap == null) {
			this.myMap = new MapRepresentation(this.myAgent.getLocalName());
            for (String agent : this.list_agentNames){
                this.list_maps.add(new Couple<String, MapRepresentation>(agent, new MapRepresentation(agent)));
            }
		}
    }

    @Override
    public void action() {
        Location myPosition = ((AbstractDedaleAgent)this.myAgent).getCurrentPosition();
        if (myPosition == null){
            System.out.println("Position == null");
            return ;
        }
        // List of observable from the agents current pos
        List<Couple<Location, List<Couple<Observation, String>>>> lobs = ((AbstractDedaleAgent) this.myAgent).observe();
       
        //1) remove the current node from openlist and add it to closedNodes.
        this.myMap.addNode(myPosition.getLocationId(), MapAttribute.closed);
        for (Couple<String, MapRepresentation> coupleAgentMap : this.list_maps) {
            coupleAgentMap.getRight().addNode(myPosition.getLocationId(), MapAttribute.closed);
        }
        
        //2) get the surrounding nodes and, if not in closedNodes, add them to open nodes.
        String nextNodeId = null ;
        Iterator<Couple<Location, List<Couple<Observation, String>>>> iter = lobs.iterator();
        while (iter.hasNext()) {
            Location accessibleNode = iter.next().getLeft(); // on récupère le noeud accessible
            boolean isNewNode = this.myMap.addNewNode(accessibleNode.getLocationId());
            if (myPosition.getLocationId() != accessibleNode.getLocationId()) {
                this.myMap.addEdge(myPosition.getLocationId(), accessibleNode.getLocationId());
                for (Couple<String, MapRepresentation> coupleAgentMap : this.list_maps) {
                    // Ajouter le noeud accessible à la carte de l'agent ainsi que l'arc entre le
                    // noeud actuel et le noeud accessible
                    coupleAgentMap.getRight().addNode(accessibleNode.getLocationId(), MapAttribute.shared);
                    coupleAgentMap.getRight().addEdge(myPosition.getLocationId(), accessibleNode.getLocationId());
                }
                if (nextNodeId==null && isNewNode) nextNodeId=accessibleNode.getLocationId();
            }
        }
        
        //3) while openNodes is not empty, continues.
        if (!this.myMap.hasOpenNode()){
            //Explo finished
            finished=true;
            transitionValue = 2;
            System.out.println(this.myAgent.getLocalName()+" - Exploration successufully done, behaviour removed.");
        }else{
            //4) select next move.
                    //4.1 If there exist one open node directly reachable, go for it,
                    //	 otherwise choose one from the openNode list, compute the shortestPath and go for it
            if (nextNodeId==null){
                //no directly accessible openNode
                //chose one, compute the path and take the first step.
                List<String> currentPath = this.myMap.getShortestPathToClosestOpenNode(myPosition.getLocationId());
                if (currentPath == null){
                    System.out.println(this.myAgent.getLocalName() + " Pas the path vers neoud ouvert");
                }
                nextNodeId=currentPath.get(0);
                //System.out.println(this.myAgent.getLocalName()+"-- list= "+this.myMap.getOpenNodes()+"| nextNode: "+nextNode);
            }else {
                //System.out.println("nextNode notNUll - "+this.myAgent.getLocalName()+"-- list= "+this.myMap.getOpenNodes()+"\n -- nextNode: "+nextNode);
            }

            boolean moved = ((AbstractDedaleAgent)this.myAgent).moveTo(new GsLocation(nextNodeId));
            if (nextNodeId!=null && !moved){
                // TODO add collition conflict resolution B
                transitionValue = 3; // ShareInfo
                System.out.println(this.myAgent.getLocalName() + " - Collision détectée vers " + nextNodeId);
                this.myAgent.addBehaviour(new CollisionHandlingBehaviour((AbstractDedaleAgent)this.myAgent, nextNodeId));
                finished = true;
            }
        }
        
    }

    @Override
    public int onEnd(){
        return transitionValue;
    }
    @Override
    public boolean done() {
        return finished;
    }
    
}
