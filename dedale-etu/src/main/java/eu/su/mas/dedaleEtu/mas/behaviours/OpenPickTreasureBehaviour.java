package eu.su.mas.dedaleEtu.mas.behaviours;

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
                        this.myAgent.addBehaviour(new AskHelpOpenLockBehaviour((AbstractDedaleAgent)this.myAgent, nouveauTresor));
                    }
                }
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

}
