package eu.su.mas.dedaleEtu.mas.behaviours.Coordination;

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
            // Vérifier d'abord s'il y a des annulations ou des confirmations d'ouverture
            if (verifierMessagesCritiques()) {
                return; // Si un message critique a été traité, on ne continue pas
            }
            gererDeplacement();
            return;
        }

        // Vérifier d'abord les messages critiques même si on n'est pas en déplacement
        if (verifierMessagesCritiques()) {
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

    /**
     * Vérifie la présence de messages critiques (annulation, confirmation
     * d'ouverture)
     * 
     * @return true si un message critique a été traité
     */
    private boolean verifierMessagesCritiques() {
        // Vérifier les messages d'annulation
        MessageTemplate modeleAnnulation = MessageTemplate.and(
                MessageTemplate.MatchProtocol("ANNULATION-AIDE"),
                MessageTemplate.MatchPerformative(ACLMessage.CANCEL));

        ACLMessage msgAnnulation = myAgent.receive(modeleAnnulation);

        if (msgAnnulation != null) {
            String demandeur = msgAnnulation.getSender().getLocalName();
            System.out.println(myAgent.getLocalName() + " - Demande d'aide annulée par " + demandeur);

            // Si c'est le demandeur actuel qui annule, arrêter le déplacement
            if (demandeurEnAttente != null && demandeurEnAttente.equals(demandeur)) {
                System.out.println(myAgent.getLocalName() + " - Arrêt du déplacement vers " + positionCible);
                enDeplacement = false;
                positionCible = null;
                demandeurEnAttente = null;
            }

            return true;
        }

        // Vérifier les messages de confirmation d'ouverture du coffre
        MessageTemplate modeleOuverture = MessageTemplate.MatchProtocol("COFFRE-OUVERT");
        ACLMessage msgOuverture = myAgent.receive(modeleOuverture);

        if (msgOuverture != null) {
            String demandeur = msgOuverture.getSender().getLocalName();
            System.out.println(myAgent.getLocalName() + " - Le coffre a été ouvert avec succès par " + demandeur);

            // Si c'est le demandeur actuel, arrêter le déplacement
            if (demandeurEnAttente != null && demandeurEnAttente.equals(demandeur)) {
                System.out.println(myAgent.getLocalName() + " - Mission accomplie, arrêt du déplacement");
                enDeplacement = false;
                positionCible = null;
                demandeurEnAttente = null;
            }

            return true;
        }

        return false; // Aucun message critique trouvé
    }

    private void gererDeplacement() {
        Location positionActuelle = ((AbstractDedaleAgent) this.myAgent).getCurrentPosition();
        if (positionActuelle == null) {
            System.out.println(myAgent.getLocalName() + " - Position actuelle inconnue, arrêt du déplacement");
            enDeplacement = false;
            return;
        }

        // Vérifier si l'agent est arrivé à destination
        if (positionActuelle.getLocationId().equals(positionCible)) {
            System.out.println(myAgent.getLocalName() + " - Arrivé à destination: " + positionCible);
            enDeplacement = false;

            // Informer le demandeur que l'agent est arrivé
            ACLMessage msgArrivee = new ACLMessage(ACLMessage.INFORM);
            msgArrivee.setProtocol("ARRIVEE-EXPERTISE");
            msgArrivee.addReceiver(new AID(demandeurEnAttente, AID.ISLOCALNAME));
            msgArrivee.setContent("Je suis arrivé à destination");
            msgArrivee.setSender(myAgent.getAID());

            ((AbstractDedaleAgent) this.myAgent).sendMessage(msgArrivee);

            return;
        }

        // Recalculer le chemin à chaque étape pour éviter les problèmes
        List<String> chemin = this.map.getShortestPath(positionActuelle.getLocationId(), positionCible);

        if (chemin == null || chemin.isEmpty()) {
            System.out.println(myAgent.getLocalName() + " - Aucun chemin trouvé vers " + positionCible);
            return;
        }

        // Le premier nœud est la position actuelle, le second est la prochaine étape
        if (chemin.size() > 1) {
            String prochainNoeud = chemin.get(0); // Prendre le deuxième élément (le prochain nœud)
            boolean aBouge = ((AbstractDedaleAgent) this.myAgent).moveTo(new GsLocation(prochainNoeud));
            if (aBouge) {
                System.out.println(myAgent.getLocalName() + " - Déplacement vers " + prochainNoeud);
            } else {
                System.out.println(myAgent.getLocalName() + " - Impossible de me déplacer vers " + prochainNoeud);
                // Si le déplacement échoue, attendez un peu avant de réessayer
                block(500);
            }
        } else {
            // Si on est arrivé au dernier noeud, on ne peut pas avancer
            enDeplacement = false;
            // Informer le demandeur que l'agent est arrivé
            ACLMessage msgArrivee = new ACLMessage(ACLMessage.INFORM);
            msgArrivee.setProtocol("ARRIVEE-EXPERTISE");
            msgArrivee.addReceiver(new AID(demandeurEnAttente, AID.ISLOCALNAME));
            msgArrivee.setContent("Je suis arrivé près de la cible");
            msgArrivee.setSender(myAgent.getAID());

            ((AbstractDedaleAgent) this.myAgent).sendMessage(msgArrivee);
            System.out.println(myAgent.getLocalName() + " - Arrivé près de la cible: " + positionCible);
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