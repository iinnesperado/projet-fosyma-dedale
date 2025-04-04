package eu.su.mas.dedaleEtu.mas.behaviours;

import eu.su.mas.dedaleEtu.mas.knowledge.TresorInfo;
import eu.su.mas.dedaleEtu.mas.knowledge.TresorMessage;

import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Reçoit des listes de trésors d'autres agents et fusionne si elles sont plus
 * récentes.
 */
public class ReceiveTresorBehaviour extends CyclicBehaviour {

    private static final long serialVersionUID = 1L;

    private final List<TresorInfo> mesTresors;
    private LocalDateTime derniereMajLocale;

    public ReceiveTresorBehaviour(List<TresorInfo> mesTresors, LocalDateTime derniereMajLocale) {
        this.mesTresors = mesTresors;
        this.derniereMajLocale = derniereMajLocale;
    }

    @Override
    public void action() {
        MessageTemplate mt = MessageTemplate.and(
                MessageTemplate.MatchProtocol("TRESOR-SHARE"),
                MessageTemplate.MatchPerformative(ACLMessage.INFORM));

        ACLMessage msg = myAgent.receive(mt);

        if (msg != null) {
            try {
                TresorMessage dataRecu = (TresorMessage) msg.getContentObject();
                LocalDateTime dateRecu = dataRecu.getDateDerniereMaj();

                if (dateRecu.isAfter(derniereMajLocale)) {
                    List<TresorInfo> listeRecu = dataRecu.getTresors();
                    int count = 0;
                    for (TresorInfo t : listeRecu) {
                        if (!mesTresors.contains(t)) {
                            mesTresors.add(t);
                            count++;
                        }
                    }
                    derniereMajLocale = dateRecu;
                    System.out.println(myAgent.getLocalName() + " a mis à jour sa liste de trésors avec " + count
                            + " nouveaux éléments.");
                } else {
                    System.out.println(myAgent.getLocalName() + " a ignoré une liste de trésors obsolète.");
                }

            } catch (UnreadableException e) {
                System.err.println("Erreur lors de la lecture du message reçu : " + e.getMessage());
            }
        } else {
            block();
        }
    }
}
