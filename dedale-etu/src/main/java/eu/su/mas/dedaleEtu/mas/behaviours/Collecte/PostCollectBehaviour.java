package eu.su.mas.dedaleEtu.mas.behaviours.Collecte;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.env.gs.GsLocation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.behaviours.Communication.ReceiveTresorBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.Communication.SendMapBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.Communication.SendTresorBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.Coordination.BesoinExpertise;
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
    private List<Treasure> listeTresors;
    private Integer placeRestantGold = null;
    private Integer placeRestantDiamond = null;
    private String tankerLocation = null;
    private List<Couple<String, Couple<LocalDateTime, LocalDateTime>>> lastContact;

    /**
     * 
     * @param myagent    reference to the agent we are adding this behaviour to
     * @param myMap      known map of the world the agent is living in
     * @param agentNames name of the agents to share the map with
     */
    public PostCollectBehaviour(final AbstractDedaleAgent myagent, MapRepresentation myMap,
            List<String> agentNames, List<Treasure> treasures,
            List<Couple<String, Couple<LocalDateTime, LocalDateTime>>> lastContact, String tankerLocation) {
        super(myagent, 500);
        this.myMap = myMap;
        this.list_agentNames = agentNames;
        this.listeTresors = treasures;
        // List<Couple<Observation, Integer>> backPack = ((AbstractDedaleAgent)
        // this.myAgent).getBackPackFreeSpace();
        // for (Couple<Observation, Integer> couple : backPack) {
        // if (couple.getLeft().getName().equals("Gold")) {
        // this.placeRestantGold = couple.getRight();
        // System.out.println("Place restante pour l'or : " + this.placeRestantGold);
        // } else if (couple.getLeft().getName().equals("Diamond")) {
        // this.placeRestantDiamond = couple.getRight();
        // System.out.println("Place restante pour le diamant : " +
        // this.placeRestantDiamond);
        // }
        // }
        this.tankerLocation = tankerLocation;
        this.lastContact = lastContact;
    }

    @Override
    public void onTick() {

        Location myPosition = ((AbstractDedaleAgent) this.myAgent).getCurrentPosition();

        if (myPosition != null) {
            List<Couple<Location, List<Couple<Observation, String>>>> lobs = ((AbstractDedaleAgent) this.myAgent)
                    .observe();
            List<Couple<Observation, String>> lObservations = lobs.get(0).getRight();
            if (getPlaceRestantTresor("Gold") == 0 || getPlaceRestantTresor("Diamond") == 0) {
                // Si le sac à dos est plein, on ne peut pas ramasser de trésor
                System.out.println(this.myAgent.getLocalName() + " - Sac à dos plein, recherche du Tanker");
                this.myAgent.addBehaviour(new GoToTanker(
                        (AbstractDedaleAgent) this.myAgent, this.myMap, this.tankerLocation, this.list_agentNames));
            }

            // example related to the use of the backpack for the treasure hunt
            for (Couple<Observation, String> obs : lObservations) {
                switch (obs.getLeft()) {
                    case DIAMOND, GOLD:
                        Treasure nouveauTresor = openPickTreasure(obs);

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

                        System.out.println(this.myAgent.getLocalName() + " - TRÉSORS ACTUELS: " + this.listeTresors);

                    case AGENTNAME:
                        if (obs.getLeft().getName().equals("AgentName")
                                && (obs.getRight().contains("Tank") || obs.getRight().contains("tank"))) {
                            tankerLocation = lobs.get(0).getLeft().getLocationId();
                            String tankerName = obs.getRight();
                            System.out.println(this.myAgent.getLocalName() + " - Tanker trouvé: " + tankerName);
                            ((AbstractDedaleAgent) this.myAgent).emptyMyBackPack(tankerName);
                            break;
                        }
                        this.myAgent
                                .addBehaviour(new SendMapBehaviour((AbstractDedaleAgent) this.myAgent,
                                        this.myMap, obs.getRight()));
                        // this.myAgent
                        // .addBehaviour(
                        // new ReceiveMapBehaviour((AbstractDedaleAgent) this.myAgent,
                        // this.myMap, obs.getRight()));
                        this.myAgent
                                .addBehaviour(new SendTresorBehaviour((AbstractDedaleAgent) this.myAgent,
                                        this.listeTresors, obs.getRight(), lastContact));
                        // this.myAgent.addBehaviour(new ReceiveTresorBehaviour(this.listeTresors));

                    default:
                        break;
                }

            }

            // // Random move from the current position
            // Random r = new Random();
            // int moveId = 1 + r.nextInt(lobs.size() - 1);
            // // The move action (if any) should be the last action of your behaviour
            // ((AbstractDedaleAgent) this.myAgent).moveTo(lobs.get(moveId).getLeft());
            List<String> minPath = new ArrayList<>();
            int min_path_length = Integer.MAX_VALUE;
            // On va chercher le trésor le plus proche pour le récolter
            if (this.listeTresors.size() > 0) {
                for (Treasure tresor : this.listeTresors) {
                    List<String> path = this.myMap.getShortestPath(myPosition.getLocationId(),
                            tresor.getPosition().getLocationId());
                    if (path.size() < min_path_length) {
                        min_path_length = path.size();
                        minPath = path;
                    }
                }
            }
            if (minPath.size() > 0) {
                System.out.println(this.myAgent.getLocalName() + " - Meilleur chemin vers le trésor: " + minPath);
                // On se déplace vers le trésor le plus proche
                Location nextLocation = new GsLocation(minPath.get(0));
                ((AbstractDedaleAgent) this.myAgent).moveTo(nextLocation);
            } else {
                // Si pas de trésor, on se déplace aléatoirement
                Random r = new Random();
                int moveId = 1 + r.nextInt(lobs.size() - 1);
                // The move action (if any) should be the last action of your behaviour
                ((AbstractDedaleAgent) this.myAgent).moveTo(lobs.get(moveId).getLeft());
            }

        }
    }

    /**
     * 
    
     */
    public Integer getPlaceRestantTresor(String typeTresor) {
        List<Couple<Observation, Integer>> backPack = ((AbstractDedaleAgent) this.myAgent).getBackPackFreeSpace();
        for (Couple<Observation, Integer> couple : backPack) {
            if (couple.getLeft().getName().equals(typeTresor)) {
                return couple.getRight();
            }
        }
        return null;
    }

    /**
     * Essaye d'ouvrir le coffre du trésor et de le ramasser
     * 
     * @return entité tresor qui étais dans le noeuds (reste ou non pas de trésor à
     *         ramasser encore)
     */
    public Treasure openPickTreasure(Couple<Observation, String> obs) {
        Location myPosition = ((AbstractDedaleAgent) this.myAgent).getCurrentPosition();
        Integer quantity = Integer.parseInt(obs.getRight());
        Treasure nouveauTresor = new Treasure(myPosition, obs.getLeft().getName(), quantity,
                LocalDateTime.now());
        String typeTresor = obs.getLeft().getName();
        if (this.getPlaceRestantTresor(typeTresor) != null && this.getPlaceRestantTresor(typeTresor) > 0) {
            if (((AbstractDedaleAgent) this.myAgent)
                    .openLock(obs.getLeft())) {

                int collected = ((AbstractDedaleAgent) this.myAgent).pick();
                System.out
                        .println(this.myAgent.getLocalName() + " - Collecté : " + collected + " unités " + typeTresor);
                nouveauTresor.setQuantity(quantity - collected);
            } else {
                System.out.println(
                        this.myAgent.getLocalName() + " - Impossible d'ouvrir le coffre contenant " + typeTresor);
                Location positionCoffre = myPosition;
                this.myAgent.addBehaviour(
                        new BesoinExpertise((AbstractDedaleAgent) this.myAgent,
                                this.myMap, list_agentNames, positionCoffre));
            }
        }
        return nouveauTresor;
    }
}