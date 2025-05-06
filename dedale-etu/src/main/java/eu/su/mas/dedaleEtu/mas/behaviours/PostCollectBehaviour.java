package eu.su.mas.dedaleEtu.mas.behaviours;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;

import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.knowledge.Treasure;

import jade.core.behaviours.TickerBehaviour;
import java.util.Random;

/**
 * <pre>
 * This behaviour allows an agent to explore the environment and learn the associated topological map.
 * The algorithm is a pseudo - DFS computationally consuming because its not optimised at all.
 * 
 * When all the nodes around him are visited, the agent randomly select an open node and go there to restart its dfs. 
 * This (non optimal) behaviour is done until all nodes are explored. 
 * 
 * Warning, this behaviour does not save the content of visited nodes, only the topology.
 * Warning, the sub-behaviour ShareMap periodically share the whole map
 * </pre>
 * 
 * @author hc
 *
 */
public class PostCollectBehaviour extends TickerBehaviour {

    private static final long serialVersionUID = 8567689731496787661L;

    /**
     * Current knowledge of the agent regarding the environment
     */
    private MapRepresentation myMap;
    private List<String> list_agentNames;
    private List<Treasure> listeTresors = new ArrayList<>();
    private Integer placeRestantGold = null;
    private Integer placeRestantDiamond = null;

    /**
     * 
     * @param myagent    reference to the agent we are adding this behaviour to
     * @param myMap      known map of the world the agent is living in
     * @param agentNames name of the agents to share the map with
     */
    public PostCollectBehaviour(final AbstractDedaleAgent myagent, MapRepresentation myMap,
            List<String> agentNames) {
        super(myagent, 500);
        this.myMap = myMap;
        this.list_agentNames = agentNames;
        this.listeTresors = new ArrayList<>();
        List<Couple<Observation, Integer>> backPack = ((AbstractDedaleAgent) this.myAgent).getBackPackFreeSpace();
        for (Couple<Observation, Integer> couple : backPack) {
            if (couple.getLeft().getName().equals("Gold")) {
                this.placeRestantGold = couple.getRight();
                System.out.println("Place restante pour l'or : " + this.placeRestantGold);
            } else if (couple.getLeft().getName().equals("Diamond")) {
                this.placeRestantDiamond = couple.getRight();
                System.out.println("Place restante pour le diamant : " + this.placeRestantDiamond);
            }
        }
    }

    @Override
    public void onTick() {

        Location myPosition = ((AbstractDedaleAgent) this.myAgent).getCurrentPosition();

        if (myPosition != null) {
            List<Couple<Location, List<Couple<Observation, String>>>> lobs = ((AbstractDedaleAgent) this.myAgent)
                    .observe();
            List<Couple<Observation, String>> lObservations = lobs.get(0).getRight();

            // example related to the use of the backpack for the treasure hunt
            for (Couple<Observation, String> obs : lObservations) {
                switch (obs.getLeft()) {
                    case DIAMOND, GOLD:
                        Integer quantity = Integer.parseInt(obs.getRight());
                        Treasure nouveauTresor = new Treasure(myPosition, obs.getLeft().getName(), quantity,
                                LocalDateTime.now());

                        // Gérer le ramassage
                        if (obs.getLeft().getName().equals("Gold")) {
                            if (this.placeRestantGold != null && this.placeRestantGold > 0) {
                                if (((AbstractDedaleAgent) this.myAgent)
                                        .openLock(obs.getLeft())) {
                                    int collected = ((AbstractDedaleAgent) this.myAgent).pick();
                                    System.out.println("Collecté : " + collected + " unités d'or.");
                                    nouveauTresor.setQuantity(quantity - collected);
                                    this.placeRestantGold -= collected;
                                } else {
                                    System.out.println("Impossible d'ouvrir le coffre contenant l'or.");

                                }
                            }
                        } else if (obs.getLeft().getName().equals("Diamond")) {
                            if (this.placeRestantDiamond != null && this.placeRestantDiamond > 0) {
                                if (((AbstractDedaleAgent) this.myAgent)
                                        .openLock(obs.getLeft())) {
                                    int collected = ((AbstractDedaleAgent) this.myAgent).pick();
                                    System.out.println("Collecté : " + collected + " unités de diamant.");
                                    nouveauTresor.setQuantity(quantity - collected);
                                    this.placeRestantDiamond -= collected;
                                } else {
                                    System.out
                                            .println("Impossible d'ouvrir le coffre contenant le diamant.");
                                    // Behaviour pour gérer le cas où le coffre
                                    // ne peut pas être ouvert
                                }
                            }
                        }

                        boolean tresorExistant = false;
                        Treasure tresorARemplacer = null;

                        // Rechercher si le trésor existe déjà à cette position
                        for (Treasure tresorActuel : listeTresors) {
                            if (tresorActuel.getPosition().equals(myPosition)) {
                                tresorExistant = true;

                                // Si même type, mettre à jour quantité
                                if (tresorActuel.getType().equals(nouveauTresor.getType())) {
                                    tresorActuel.setQuantity(nouveauTresor.getQuantity());
                                    tresorActuel.setRecordTime(LocalDateTime.now());
                                    System.out.println(this.myAgent.getLocalName()
                                            + " - Mise à jour du trésor: " +
                                            nouveauTresor.getQuantity() + " " + nouveauTresor.getType() +
                                            " en position " + myPosition.getLocationId());
                                } else {
                                    // Type différent, marquer pour remplacement
                                    tresorARemplacer = tresorActuel;
                                }
                                break;
                            }
                        }
                        // Si besoin de remplacer (type différent à la même position)
                        if (tresorARemplacer != null) {
                            listeTresors.remove(tresorARemplacer);
                            listeTresors.add(nouveauTresor);
                            System.out.println("Remplacement d'un trésor de " + tresorARemplacer.getType() +
                                    " par " + nouveauTresor.getQuantity() + " " + nouveauTresor.getType() +
                                    " en position " + myPosition.getLocationId());
                        }
                        // Si nouveau trésor
                        else if (!tresorExistant) {
                            listeTresors.add(nouveauTresor);
                            System.out.println(this.myAgent.getLocalName() + " - Nouveau trésor trouvé: " +
                                    nouveauTresor.getQuantity() + " " + nouveauTresor.getType() +
                                    " en position " + myPosition.getLocationId());
                        }

                        System.out.println("TRÉSORS ACTUELS: " + this.listeTresors);

                    case AGENTNAME:
                        this.myAgent
                                .addBehaviour(new SendMapBehaviour((AbstractDedaleAgent) this.myAgent,
                                        this.myMap, obs.getRight()));
                        this.myAgent
                                .addBehaviour(
                                        new ReceiveMapBehaviour((AbstractDedaleAgent) this.myAgent,
                                                this.myMap, list_agentNames));
                        this.myAgent
                                .addBehaviour(new SendTresorBehaviour((AbstractDedaleAgent) this.myAgent,
                                        this.listeTresors, obs.getRight()));
                        this.myAgent.addBehaviour(new ReceiveTresorBehaviour(this.listeTresors));

                    default:
                        break;
                }

            }
            // Random move from the current position
            Random r = new Random();
            int moveId = 1 + r.nextInt(lobs.size() - 1);
            // The move action (if any) should be the last action of your behaviour
            ((AbstractDedaleAgent) this.myAgent).moveTo(lobs.get(moveId).getLeft());
        }
    }
}