package eu.su.mas.dedaleEtu.mas.behaviours.Communication;

import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.Treasure;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Reçoit des listes de trésors d'autres agents et fusionne si elles sont plus
 * récentes.
 */
public class ReceiveTresorBehaviour extends CyclicBehaviour {

    private static final long serialVersionUID = 1L;

    private final List<Treasure> listeTresors;

    public ReceiveTresorBehaviour(List<Treasure> listeTresors) {
        this.listeTresors = listeTresors;
    }

    @Override
    public void action() {
        // On s'attend à recevoir un message de type TRESOR-SHARE
        MessageTemplate mt = MessageTemplate.and(
                MessageTemplate.MatchProtocol("TRESOR-SHARE"),
                MessageTemplate.MatchPerformative(ACLMessage.INFORM));

        ACLMessage msg = myAgent.receive(mt);

        if (msg != null) {
            try {
                List<Treasure> tresorsRecus = (List<Treasure>) msg.getContentObject();
                System.out.println(
                        myAgent.getLocalName() + " a reçu la liste de trésors de " + msg.getSender().getLocalName());
                System.out.println("Liste de trésors reçue : " + tresorsRecus);

                if (tresorsRecus != null && !tresorsRecus.isEmpty()) {
                    // Listes temporaires pour les opérations
                    List<Treasure> tresorsAAjouter = new ArrayList<>();
                    List<Treasure> tresorsARemplacer = new ArrayList<>();

                    // Traiter chaque trésor reçu
                    for (Treasure tresorRecu : tresorsRecus) {
                        boolean tresorTrouve = false;

                        // Vérifier si ce trésor existe déjà dans notre liste
                        for (Treasure tresorLocal : listeTresors) {
                            if (tresorRecu.getPosition().equals(tresorLocal.getPosition()) &&
                                    tresorRecu.getType().equals(tresorLocal.getType())) {
                                tresorTrouve = true;

                                // Comparer les dates pour voir si on doit remplacer
                                if (tresorRecu.getRecordTime().isAfter(tresorLocal.getRecordTime())) {
                                    tresorsARemplacer.add(tresorLocal);
                                    tresorsAAjouter.add(tresorRecu);
                                    System.out.println(
                                            myAgent.getLocalName() + " - Remplacement d'un trésor plus ancien à " +
                                                    tresorRecu.getPosition());
                                } else {
                                    System.out.println(
                                            myAgent.getLocalName() + " - Trésor ignoré car plus ancien en position " +
                                                    tresorRecu.getPosition());
                                }
                                break; // Sortir de la boucle interne car trésor trouvé
                            }
                        }

                        // Si le trésor n'existe pas, l'ajouter
                        if (!tresorTrouve) {
                            tresorsAAjouter.add(tresorRecu);
                            System.out.println(myAgent.getLocalName() + " - Nouveau trésor ajouté à " +
                                    tresorRecu.getPosition());
                        }
                    }

                    // Appliquer les modifications APRÈS l'itération
                    listeTresors.removeAll(tresorsARemplacer); // Supprimer les trésors à remplacer
                    listeTresors.addAll(tresorsAAjouter); // Ajouter les nouveaux trésors

                    System.out.println(myAgent.getLocalName() + " - Liste de trésors après fusion: " +
                            listeTresors.size() + " trésors");
                }
            } catch (UnreadableException e) {
                System.err.println("Erreur lors de la lecture de l'objet: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            block(); // Bloquer jusqu'à ce qu'un message arrive
        }
    }

}
