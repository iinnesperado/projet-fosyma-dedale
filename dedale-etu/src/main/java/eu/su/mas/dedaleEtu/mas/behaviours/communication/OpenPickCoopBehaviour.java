package eu.su.mas.dedaleEtu.mas.behaviours.communication;

import java.time.LocalDateTime;
import java.util.List;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.Treasure;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class OpenPickCoopBehaviour extends OneShotBehaviour{

    private static final long serialVersionUID = -480474568264579007L;

    private List<Treasure> listeTresors;

    public OpenPickCoopBehaviour(final AbstractDedaleAgent myagent, List<Treasure> treasures){
        super(myagent);
        this.listeTresors = treasures;
    }

    @Override
    public void action() {
        int waitTime = 5000; // 5 secondes
        int helpers = 0;
        
        while (helpers < 1 && waitTime > 0) { // Attendre au moins 1 assistant
            MessageTemplate msgTemplate = MessageTemplate.and(MessageTemplate.MatchProtocol("GO-HELP"), MessageTemplate.MatchPerformative(ACLMessage.AGREE));
            ACLMessage reply = this.myAgent.blockingReceive(msgTemplate,1000);
            if (reply != null && reply.getPerformative() == ACLMessage.AGREE) {
                helpers++;
            }
            waitTime -= 1000;
        }
        
        if (helpers > 0) {
            Location myPosition = ((AbstractDedaleAgent) this.myAgent).getCurrentPosition();
            Treasure nouveauTresor = openPickTreasure(myPosition); // Tenter avec les nouveaux arrivants
            updateTreasureList(nouveauTresor, myPosition);
        } else {
            return ; // Abandonner après timeout
        }    
    }

    public Treasure openPickTreasure(Location myPosition){
        String typeTresor = ((AbstractDedaleAgent) this.myAgent).getMyTreasureType().getName();
        Treasure nouveauTresor = new Treasure(myPosition, LocalDateTime.now());
    
        List<Couple<Location, List<Couple<Observation, String>>>> observations = ((AbstractDedaleAgent)this.myAgent).observe();
        for (Couple<Location, List<Couple<Observation, String>>> node : observations){
            for (Couple<Observation, String> obs : node.getRight()){
                Integer quantity = Integer.parseInt(obs.getRight());
                nouveauTresor.setQuantity(quantity);
                nouveauTresor.setType(obs.getLeft().getName());

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
        return nouveauTresor;
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
