package eu.su.mas.dedaleEtu.mas.behaviours.collaboration;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.env.gs.GsLocation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class OffreExpertise extends CyclicBehaviour {

    private static final long serialVersionUID = 1L;
    private MapRepresentation map;
    int monLockpicking = 0;
    int maForce = 0;

    private boolean enDeplacement = false;
    private String positionCible = null;
    private String demandeurEnAttente = null;
    private long dernierEnvoi = 0;
    private final long intervalleEnvoi = 2000; // 2 secondes entre les messages
    private List<String> cheminVersCible = null;
    private String sender;

    public OffreExpertise(AbstractDedaleAgent monAgent, MapRepresentation map) {
        super(monAgent);
        this.map = map;
    }

    @Override
    public void action() {
        // Si l'agent est déjà en déplacement vers un coffre
        if (enDeplacement && positionCible != null) {
            gererDeplacement();
            return;
        }

        // Traitement normal des demandes
        MessageTemplate modele = MessageTemplate.MatchProtocol("BESOIN-EXPERTISE");
        ACLMessage msg = myAgent.receive(modele);

        if (msg != null) {
            String demandeur = msg.getSender().getLocalName();
            this.sender = demandeur;
            String contenu = msg.getContent();

            System.out.println(myAgent.getLocalName() + " - Demande d'aide reçue de " + demandeur);

            // Format attendu: lockpickingRequis;forceRequise;position
            String[] parties = contenu.split(";");
            if (parties.length >= 3) {
                try {
                    // Récupérer les compétences de l'agent
                    Set<Couple<Observation, Integer>> competences = ((AbstractDedaleAgent) this.myAgent)
                            .getMyExpertise();
                    for (Couple<Observation, Integer> competence : competences) {
                        if (competence.getLeft().getName().equals("LockPicking")) {
                            monLockpicking = competence.getRight();
                        } else if (competence.getLeft().getName().equals("Strength")) {
                            maForce = competence.getRight();
                        }
                    }
                    // Envoyer une réponse avec mon expertise
                    ACLMessage reponse = new ACLMessage(ACLMessage.INFORM);
                    reponse.setProtocol("OFFRE-EXPERTISE");
                    reponse.setContent(monLockpicking + ";" + maForce);
                    reponse.addReceiver(msg.getSender());

                    // AJOUT - Définir l'expéditeur
                    reponse.setSender(myAgent.getAID());

                    ((AbstractDedaleAgent) myAgent).sendMessage(reponse);

                    System.out.println(myAgent.getLocalName() + " - Offre d'aide envoyée (Lockpicking: " +
                            monLockpicking + ", Force: " + maForce + ")");

                    // Modification pour le déplacement
                    positionCible = parties[2];
                    demandeurEnAttente = msg.getSender().getLocalName();
                    enDeplacement = true;
                    dernierEnvoi = System.currentTimeMillis();

                    // Calculer le chemin vers la cible
                    calculerCheminVersCible();

                    // Confirmer immédiatement que l'aide est en route
                    ACLMessage confirmation = new ACLMessage(ACLMessage.INFORM);
                    confirmation.setProtocol("EN-ROUTE");
                    confirmation.setContent("Je viens vous aider");
                    confirmation.addReceiver(msg.getSender());

                    // AJOUT - Définir l'expéditeur
                    confirmation.setSender(myAgent.getAID());

                    ((AbstractDedaleAgent) myAgent).sendMessage(confirmation);

                    System.out.println(myAgent.getLocalName() + " - Début du déplacement vers " + positionCible);

                } catch (Exception e) {
                    System.err.println(
                            myAgent.getLocalName() + " - Erreur lors du traitement de la demande: " + e.getMessage());
                }
            }
        } else {
            block(500);
        }
    }

    private void gererDeplacement() {
        Location positionActuelle = ((AbstractDedaleAgent) this.myAgent).getCurrentPosition();
        if (positionActuelle == null) {
            System.out.println(myAgent.getLocalName() + " - Position actuelle inconnue, arrêt du déplacement");
            enDeplacement = false;
            return;
        }
        // Vérifier si l'agent est arrivé à destination
        List<String> chemin = this.map.getShortestPath(
                ((AbstractDedaleAgent) this.myAgent).getCurrentPosition().getLocationId(), positionCible);

        if (chemin != null && chemin.size() == 1 && chemin.get(0).equals(positionCible)) {
            System.out.println(myAgent.getLocalName() + " - Arrivé à destination: " + positionCible);
            enDeplacement = false;

            // Informer le demandeur que l'agent est arrivé
            ACLMessage msgArrivee = new ACLMessage(ACLMessage.INFORM);
            msgArrivee.setProtocol("ARRIVEE");
            msgArrivee.addReceiver(new AID(demandeurEnAttente, AID.ISLOCALNAME));
            msgArrivee.setContent("Je suis arrivé à destination");
            msgArrivee.setSender(new AID(sender, AID.ISLOCALNAME));

            ((AbstractDedaleAgent) this.myAgent).sendMessage(msgArrivee);

            return;
        }

        // Envoyer périodiquement des mises à jour
        long maintenant = System.currentTimeMillis();
        if (maintenant - dernierEnvoi > intervalleEnvoi) {
            // Informer le demandeur de la progression
            ACLMessage msgMaj = new ACLMessage(ACLMessage.INFORM);
            msgMaj.setProtocol("PROGRESSION-EXPERTISE");
            msgMaj.addReceiver(new AID(demandeurEnAttente, AID.ISLOCALNAME));
            msgMaj.setContent(
                    "En route: " + (positionActuelle != null ? positionActuelle.getLocationId() : "inconnue"));

            // AJOUT - Définir l'expéditeur
            msgMaj.setSender(myAgent.getAID());

            ((AbstractDedaleAgent) this.myAgent).sendMessage(msgMaj);

            dernierEnvoi = maintenant;
            System.out.println(myAgent.getLocalName() + " - Progression vers " + positionCible +
                    ", actuellement en position "
                    + (positionActuelle != null ? positionActuelle.getLocationId() : "position inconnue"));
        }

        // Faire un pas vers la destination
        if (cheminVersCible != null && !cheminVersCible.isEmpty()) {
            String prochainNoeud = cheminVersCible.remove(0);
            boolean aBouge = ((AbstractDedaleAgent) this.myAgent).moveTo(new GsLocation(prochainNoeud));
            if (aBouge) {
                System.out.println(myAgent.getLocalName() + " - Déplacement vers " + prochainNoeud);
            } else {
                System.out.println(myAgent.getLocalName() + " - Impossible de me déplacer vers " + prochainNoeud);
            }
        } else {
            System.out.println(myAgent.getLocalName() + " - Chemin vers la cible vide ou non défini");
        }
    }

    /**
     * Calculer le chemin vers la cible
     */
    private void calculerCheminVersCible() {
        // Récupérer le chemin vers la cible
        cheminVersCible = new ArrayList<>();
        List<String> chemin = this.map.getShortestPath(
                ((AbstractDedaleAgent) this.myAgent).getCurrentPosition().getLocationId(), positionCible);

        if (chemin != null && !chemin.isEmpty()) {
            cheminVersCible.addAll(chemin);
            System.out.println(myAgent.getLocalName() + " - Chemin calculé vers " + positionCible + ": " + chemin);
        } else {
            System.out.println(myAgent.getLocalName() + " - Aucun chemin trouvé vers " + positionCible);
        }
        // Si le chemin est vide, on ne peut pas avancer
        if (cheminVersCible.isEmpty()) {
            System.out.println(myAgent.getLocalName() + " - Chemin vers la cible vide, arrêt du déplacement");
            enDeplacement = false;
        }
        // Sinon, on commence à avancer
        else {
            System.out.println(myAgent.getLocalName() + " - Début du déplacement vers " + positionCible);
            cheminVersCible.remove(0); // On enlève le premier élément car c'est la position actuelle
        }
        // Si on arrive pas au dernier noeud, on ne peut pas avancer
        if (cheminVersCible.size() == 1) {
            System.out.println(myAgent.getLocalName() + " - Arrivée près du noeud cible, arrêt du déplacement");
        }

    }
}