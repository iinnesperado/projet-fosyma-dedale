package eu.su.mas.dedaleEtu.mas.behaviours.collaboration;

import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.env.gs.GsLocation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.core.AID;

import java.util.List;
import java.util.Set;

import dataStructures.tuple.Couple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class BesoinExpertise extends SimpleBehaviour {

    private static final long serialVersionUID = 1L;
    private boolean termine = false;
    private List<String> listeAgents;
    private MapRepresentation myMap;

    // Expertise requise et actuelle
    private Integer monLockpicking = 0;
    private Integer maForce = 0;
    private Integer lockpickingRequis = 0;
    private Integer forceRequise = 0;

    // Suivi des agents ayant répondu
    private HashMap<String, Couple<Integer, Integer>> expertsDisponibles = new HashMap<>();

    // Gestion du timeout
    private long debutAttente = 0;
    private final long tempsAttenteMax = 50000;

    // Ajoutez ces variables d'instance à BesoinExpertise
    private long derniereDemande = 0;
    private final long intervalleDemande = 5000; // 5 secondes entre les demandes
    private int compteurDemandes = 0;
    private final int maxDemandes = 10; // Maximum 10 demandes avant d'abandonner

    private Location positionCoffre;

    // Suivi des derniers contacts avec les agents
    private HashMap<String, Long> derniersContactsAgents = new HashMap<>();

    // États du comportement
    private enum Etat {
        DETECTION, RETOUR_COFFRE, ATTENTE_AIDE, OUVERTURE, ECHEC, TERMINE
    }

    private Etat etatActuel = Etat.DETECTION;

    public BesoinExpertise(AbstractDedaleAgent myAgent, MapRepresentation myMap,
            List<String> agentNames, Location positionCoffre) {
        super(myAgent);
        this.myMap = myMap;
        this.listeAgents = agentNames;
        this.positionCoffre = positionCoffre; // Stocke la position du coffre

        // Récupérer les compétences de l'agent
        Set<Couple<Observation, Integer>> competences = ((AbstractDedaleAgent) this.myAgent)
                .getMyExpertise();
        for (Couple<Observation, Integer> competence : competences) {
            if (competence.getLeft().getName().equals("LockPicking")) {
                this.monLockpicking = competence.getRight();
            } else if (competence.getLeft().getName().equals("Strength")) {
                this.maForce = competence.getRight();
            }
        }

        System.out.println(myAgent.getLocalName() + " - BesoinExpertise démarré (Lockpicking: " +
                this.monLockpicking + ", Force: " + this.maForce + ")");
    }

    @Override
    public void action() {
        Location maPosition = ((AbstractDedaleAgent) this.myAgent).getCurrentPosition();

        if (maPosition == null) {
            return; // Pas de position, on ne peut rien faire
        }

        switch (etatActuel) {
            case DETECTION:
                // Si on est sur le nœud du coffre, on détecte directement
                if (maPosition.getLocationId().equals(positionCoffre.getLocationId())) {
                    detecterCoffre(maPosition);
                }
                // Sinon, il faut d'abord retourner à la position du coffre
                else {
                    retournerAuCoffre();
                }
                break;

            case ATTENTE_AIDE:
                attendreAide();
                break;

            case OUVERTURE:
                ouvrirCoffre();
                break;

            case ECHEC:
                gererEchec();
                break;

            case TERMINE:
                break;
        }
    }

    /**
     * Si l'agent n'est plus sur le nœud du coffre,
     * il doit y retourner avant de pouvoir l'ouvrir
     */
    private void retournerAuCoffre() {
        System.out.println(
                myAgent.getLocalName() + " - Retour vers le nœud du coffre : " + positionCoffre.getLocationId());
        // Calculer le chemin vers le coffre
        List<String> cheminVersCoffre = myMap.getShortestPath(
                ((AbstractDedaleAgent) myAgent).getCurrentPosition().getLocationId(),
                positionCoffre.getLocationId());
        if (cheminVersCoffre == null || cheminVersCoffre.isEmpty()) {
            System.out.println(myAgent.getLocalName() + " - Aucun chemin trouvé vers le coffre");
            return;
        }
        while (cheminVersCoffre.size() > 0) {
            String prochainNoeud = cheminVersCoffre.remove(0);
            boolean moved = ((AbstractDedaleAgent) this.myAgent).moveTo(new GsLocation(prochainNoeud));
            if (!moved) {
                System.out.println(myAgent.getLocalName() + " - Impossible de me déplacer vers " + prochainNoeud);
                break; // Sortir de la boucle si le mouvement échoue
            }
        }
    }

    /**
     * Détecte un coffre et détermine si une aide est nécessaire
     */
    private void detecterCoffre(Location maPosition) {
        List<Couple<Location, List<Couple<Observation, String>>>> observations = ((AbstractDedaleAgent) this.myAgent)
                .observe();

        boolean coffreTrouve = false;

        // Recherche de serrures et de coffres
        for (Couple<Location, List<Couple<Observation, String>>> couple : observations) {
            for (Couple<Observation, String> obs : couple.getRight()) {
                String nomObservation = obs.getLeft().getName();

                // Vérifier si c'est un coffre avec besoin de crochetage
                if (nomObservation.equals("LockPicking")) {
                    coffreTrouve = true;
                    lockpickingRequis = Integer.parseInt(obs.getRight());
                    System.out.println(
                            myAgent.getLocalName() + " - Coffre trouvé! Lockpicking requis: " + lockpickingRequis);
                }

                // Vérifier si c'est un coffre avec besoin de force
                if (nomObservation.equals("Strength")) {
                    coffreTrouve = true;
                    forceRequise = Integer.parseInt(obs.getRight());
                    System.out.println(myAgent.getLocalName() + " - Coffre trouvé! Force requise: " + forceRequise);
                }
            }
        }

        if (coffreTrouve) {
            // Vérifier si je peux ouvrir le coffre seul
            if (monLockpicking >= lockpickingRequis && maForce >= forceRequise) {
                System.out.println(myAgent.getLocalName() + " - Je peux ouvrir le coffre seul!");
                etatActuel = Etat.OUVERTURE;
            } else {
                // Besoin d'aide pour ouvrir le coffre
                System.out.println(myAgent.getLocalName() +
                        " - Je ne peux pas ouvrir ce coffre seul. Manque: " +
                        Math.max(0, lockpickingRequis - monLockpicking) + " Lockpicking, " +
                        Math.max(0, forceRequise - maForce) + " Force");

                // Demander de l'aide aux autres agents
                demanderAide();

                // Passer à l'état d'attente
                etatActuel = Etat.ATTENTE_AIDE;
                debutAttente = System.currentTimeMillis();
            }
        } else {
            System.out.println(myAgent.getLocalName() + " - Aucun coffre détecté à cette position");
            etatActuel = Etat.ECHEC;
        }
    }

    /**
     * Envoi des demandes d'aide aux agents à proximité
     * 
     * @param forcerTousAgents Si vrai, envoie à tous les agents connus même si non
     *                         visibles
     */
    private void demanderAide(boolean forcerTousAgents) {
        // Observer les agents à proximité
        List<Couple<Location, List<Couple<Observation, String>>>> observations = ((AbstractDedaleAgent) this.myAgent)
                .observe();

        ArrayList<String> agentsProches = new ArrayList<>();

        // Recherche d'agents à proximité
        for (Couple<Location, List<Couple<Observation, String>>> couple : observations) {
            for (Couple<Observation, String> obs : couple.getRight()) {
                if (obs.getLeft().getName().equals("AgentName") && !obs.getRight().equals(myAgent.getLocalName())) {
                    agentsProches.add(obs.getRight());
                }
            }
        }

        // Si pas d'agents à proximité ou si on force l'envoi à tous, utiliser la liste
        // complète
        if (agentsProches.isEmpty() || forcerTousAgents) {
            agentsProches.addAll(listeAgents);
        }

        // Vérifier contre les agents déjà contactés dans les dernières secondes
        long maintenant = System.currentTimeMillis();
        ArrayList<String> agentsAContacter = new ArrayList<>();

        for (String nomAgent : agentsProches) {
            // Ne pas s'envoyer à soi-même
            if (nomAgent.equals(myAgent.getLocalName()))
                continue;

            // Si l'agent n'a pas été contacté récemment, l'ajouter à la liste
            if (!derniersContactsAgents.containsKey(nomAgent) ||
                    (maintenant - derniersContactsAgents.get(nomAgent) > 5000)) { // 5 secondes minimum entre messages
                agentsAContacter.add(nomAgent);
            }
        }

        // Envoyer une demande d'aide aux agents sélectionnés
        for (String nomAgent : agentsAContacter) {
            ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
            msg.setProtocol("BESOIN-EXPERTISE");

            // Format du message: lockpickingRequis;forceRequise;positionCoffre
            String contenuMessage = lockpickingRequis + ";" + forceRequise + ";" + positionCoffre.getLocationId();
            msg.setContent(contenuMessage);

            msg.setSender(this.myAgent.getAID());
            msg.addReceiver(new AID(nomAgent, AID.ISLOCALNAME));
            ((AbstractDedaleAgent) this.myAgent).sendMessage(msg);

            // Enregistrer le moment de l'envoi
            derniersContactsAgents.put(nomAgent, maintenant);

            System.out.println(myAgent.getLocalName() + " - Demande d'aide envoyée à " + nomAgent);
        }

        // Mettre à jour les variables de timing
        derniereDemande = maintenant;
        compteurDemandes++;
    }

    /**
     * Surcharge pour garder la compatibilité avec le code existant
     */
    private void demanderAide() {
        demanderAide(true);
    }

    /**
     * Gère l'attente des réponses des autres agents
     */
    private void attendreAide() {
        // Relancer périodiquement les demandes d'aide
        long maintenant = System.currentTimeMillis();
        if (maintenant - derniereDemande > intervalleDemande && compteurDemandes < maxDemandes) {
            System.out.println(myAgent.getLocalName() + " - Relance de la demande d'aide (" +
                    (compteurDemandes + 1) + "/" + maxDemandes + ")");
            demanderAide();
            derniereDemande = maintenant;
            compteurDemandes++;
        }

        // Vérifier si le timeout est atteint
        if (System.currentTimeMillis() - debutAttente > tempsAttenteMax) {
            System.out.println(myAgent.getLocalName() + " - Temps d'attente dépassé, abandon de l'ouverture");
            etatActuel = Etat.ECHEC;
            return;
        }

        // Écouter les réponses
        MessageTemplate modele = MessageTemplate.and(
                MessageTemplate.MatchProtocol("OFFRE-EXPERTISE"),
                MessageTemplate.MatchPerformative(ACLMessage.INFORM));

        ACLMessage msg = myAgent.receive(modele);

        if (msg != null) {
            // Un agent a répondu avec son expertise
            String nomAidant = msg.getSender().getLocalName();
            String[] parties = msg.getContent().split(";");

            if (parties.length >= 2) {
                try {
                    int lockpickingOffert = Integer.parseInt(parties[0]);
                    int forceOfferte = Integer.parseInt(parties[1]);

                    // Ajouter l'agent à la liste des experts disponibles
                    expertsDisponibles.put(nomAidant, new Couple<>(lockpickingOffert, forceOfferte));

                    System.out.println(myAgent.getLocalName() + " - Offre d'aide reçue de " + nomAidant +
                            " (Lockpicking: " + lockpickingOffert + ", Force: " + forceOfferte + ")");

                    // Vérifier si on a suffisamment d'expertise collective
                    int totalLockpicking = monLockpicking;
                    int totalForce = maForce;

                    for (Couple<Integer, Integer> expertise : expertsDisponibles.values()) {
                        totalLockpicking += expertise.getLeft();
                        totalForce += expertise.getRight();
                    }

                    if (totalLockpicking >= lockpickingRequis && totalForce >= forceRequise) {
                        // On a assez d'expertise pour ouvrir le coffre
                        System.out.println(myAgent.getLocalName() + " - Expertise collective suffisante! " +
                                "Total: " + totalLockpicking + " Lockpicking, " + totalForce + " Force");
                        etatActuel = Etat.OUVERTURE;
                    } else {
                        System.out.println(myAgent.getLocalName() + " - Pas assez d'expertise collective " +
                                "(Total: " + totalLockpicking + " Lockpicking, " + totalForce + " Force)");
                        // Si pas assez d'expertise, on continue d'attendre
                        if (System.currentTimeMillis() - debutAttente > tempsAttenteMax) {
                            System.out.println(
                                    myAgent.getLocalName() + " - Temps d'attente dépassé, abandon de l'ouverture");
                            etatActuel = Etat.ECHEC;
                            return;
                        }
                    }
                } catch (NumberFormatException e) {
                    System.err.println(myAgent.getLocalName() + " - Format de message d'aide incorrect");
                }
            }
        } else {
            block(100); // Attendre un peu avant de vérifier à nouveau
        }

        // Écouter les messages de progression
        MessageTemplate modeleProgression = MessageTemplate.MatchProtocol("PROGRESSION-EXPERTISE");
        ACLMessage msgProgression = myAgent.receive(modeleProgression);
        if (msgProgression != null) {
            String aidant = msgProgression.getSender().getLocalName();
            String progression = msgProgression.getContent();
            System.out.println(myAgent.getLocalName() + " - Progression de " + aidant + ": " + progression);
            // Réinitialiser le compteur de timeout puisqu'on a eu des nouvelles
            debutAttente = System.currentTimeMillis();
        }

        // Écouter les messages d'arrivée
        MessageTemplate modeleArrivee = MessageTemplate.MatchProtocol("ARRIVEE-EXPERTISE");
        ACLMessage msgArrivee = myAgent.receive(modeleArrivee);
        if (msgArrivee != null) {
            String aidant = msgArrivee.getSender().getLocalName();
            System.out.println(myAgent.getLocalName() + " - Agent " + aidant + " est ARRIVÉ!");
            // Force la vérification physique immédiate
            verifierPresenceAgents();
        }

        // Vérifier si des agents sont arrivés physiquement
        verifierPresenceAgents();
    }

    /**
     * Vérifie si des agents sont physiquement présents pour aider
     */
    private void verifierPresenceAgents() {
        List<Couple<Location, List<Couple<Observation, String>>>> observations = ((AbstractDedaleAgent) this.myAgent)
                .observe();

        int totalLockpicking = monLockpicking;
        int totalForce = maForce;
        boolean agentsPresents = false;

        for (Couple<Location, List<Couple<Observation, String>>> couple : observations) {
            for (Couple<Observation, String> obs : couple.getRight()) {
                if (obs.getLeft().getName().equals("AgentName") && !obs.getRight().equals(myAgent.getLocalName())) {
                    String nomAgent = obs.getRight();

                    // Si on a des infos sur cet agent
                    if (expertsDisponibles.containsKey(nomAgent)) {
                        Couple<Integer, Integer> expertise = expertsDisponibles.get(nomAgent);
                        totalLockpicking += expertise.getLeft();
                        totalForce += expertise.getRight();
                        agentsPresents = true;

                        System.out.println(myAgent.getLocalName() + " - Agent " + nomAgent + " est arrivé pour aider!");
                    }
                }
            }
        }

        // Si on a assez d'expertise collective et des agents sont présents
        if (agentsPresents && totalLockpicking >= lockpickingRequis && totalForce >= forceRequise) {
            System.out.println(myAgent.getLocalName() + " - Les agents sont là! On peut ouvrir le coffre!");
            etatActuel = Etat.OUVERTURE;
        }
    }

    /**
     * Tente d'ouvrir le coffre avec l'aide collective
     */
    private void ouvrirCoffre() {
        System.out.println(myAgent.getLocalName() + " - Tentative d'ouverture du coffre...");

        // Dans une implémentation réelle, ici on coordonnerait l'ouverture avec les
        // agents présents
        // Pour cet exemple, on suppose que le coffre est ouvert avec succès

        // Envoyer un message de confirmation aux agents qui ont aidé
        for (String nomAidant : expertsDisponibles.keySet()) {
            ACLMessage msgMerci = new ACLMessage(ACLMessage.INFORM);
            msgMerci.setProtocol("COFFRE-OUVERT");
            msgMerci.setSender(myAgent.getAID());
            msgMerci.addReceiver(new AID(nomAidant, AID.ISLOCALNAME));
            msgMerci.setContent("Merci pour votre aide!");
            ((AbstractDedaleAgent) this.myAgent).sendMessage(msgMerci);
        }

        System.out.println(myAgent.getLocalName() + " - Coffre ouvert avec succès!");
        etatActuel = Etat.TERMINE;
        termine = true;

    }

    /**
     * Gère l'échec de l'ouverture du coffre
     */
    private void gererEchec() {
        System.out.println(myAgent.getLocalName() + " - Abandon de l'ouverture du coffre");

        // Informer les agents qui ont proposé leur aide
        for (String nomAidant : expertsDisponibles.keySet()) {
            ACLMessage msgAnnulation = new ACLMessage(ACLMessage.CANCEL);
            msgAnnulation.setProtocol("ANNULATION-AIDE");
            msgAnnulation.setSender(myAgent.getAID());
            msgAnnulation.addReceiver(new AID(nomAidant, AID.ISLOCALNAME));
            ((AbstractDedaleAgent) this.myAgent).sendMessage(msgAnnulation);
        }

        etatActuel = Etat.TERMINE;
        termine = true;

    }

    @Override
    public boolean done() {
        return this.termine;
    }
}