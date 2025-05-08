package eu.su.mas.dedaleEtu.mas.behaviours.treasure;

import java.time.LocalDateTime;
import java.util.List;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.behaviours.communication.AskHelpOpenLockBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.Treasure;
import jade.core.behaviours.OneShotBehaviour;

public class OpenPickTreasureBehaviour extends OneShotBehaviour {

    private static final long serialVersionUID = 3973250783964285694L;
    private boolean finished = false;
    private List<Treasure> listeTresors;


    public OpenPickTreasureBehaviour(final AbstractDedaleAgent myagent, List<Treasure> listeTresors){
        super(myagent);
        this.listeTresors = listeTresors;
    }

    @Override
    public void action() {
        List<Couple<Location, List<Couple<Observation, String>>>> observations = ((AbstractDedaleAgent)this.myAgent).observe();
        for (Couple<Location, List<Couple<Observation, String>>> node : observations){
            for (Couple<Observation, String> obs : node.getRight()){
                Location myPosition = ((AbstractDedaleAgent) this.myAgent).getCurrentPosition();
                String typeTresor = ((AbstractDedaleAgent) this.myAgent).getMyTreasureType().getName();
                Integer quantity = Integer.parseInt(obs.getRight());
                Treasure nouveauTresor = new Treasure(myPosition, obs.getLeft().getName(), quantity,
                        LocalDateTime.now());
                if (this.getPlaceRestantTresor(typeTresor) != null && this.getPlaceRestantTresor(typeTresor) > 0) {
                    if (((AbstractDedaleAgent) this.myAgent)
                            .openLock(obs.getLeft())) {
                        int collected = ((AbstractDedaleAgent) this.myAgent).pick();
                        System.out
                                .println(this.myAgent.getLocalName() + " - Collecté : " + collected + " unités " + typeTresor);
                        nouveauTresor.setQuantity(quantity - collected);
                        System.out.println(this.myAgent.getLocalName() + " - Nouveau trésor trouvé: " +
                                nouveauTresor.getQuantity() + " " + nouveauTresor.getType() +
                                " en position " + myPosition.getLocationId());
                    } else {
                        System.out.println(
                                this.myAgent.getLocalName() + " - Impossible d'ouvrir le coffre contenant " + typeTresor);
                        nouveauTresor.setRequiredSkills(observations);
                        // this.myAgent.addBehaviour(new AskHelpOpenLockBehaviour((AbstractDedaleAgent)this.myAgent, nouveauTresor));
                    }
                }
                updateTreasureList(nouveauTresor, myPosition);
            }
        }
    }

    /**
     * Renvoie la place restante dans le sac-a-dos de l'agent
     * 
     * @param typeTresor le tésor dont on veux savoir l'expace restant dans le
     *                   sac-a-ados
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

    public void updateTreasureList(Treasure nouveauTresor, Location myPosition){
        boolean tresorExistant = false;
        Treasure tresorARemplacer = null;

        // Rechercher si le trésor existe déjà à cette position
        for (Treasure tresorActuel : listeTresors) {
            if (tresorActuel.getPosition().equals(nouveauTresor.getPosition())) {
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
                    " en position  " + myPosition.getLocationId());
        }
        System.out.println(this.myAgent.getLocalName() + " - TRÉSORS ACTUELS: " + this.listeTresors);
    }

}
