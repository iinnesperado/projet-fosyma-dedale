package eu.su.mas.dedaleEtu.mas.behaviours.Coordination;

import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.env.gs.GsLocation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.behaviours.Exploration.ExploCoopBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.Collecte.ExploCollectBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
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
    private Observation treasure;

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

    // Ajoutez ces variables d'instance
    private long debutAttenteGlobal = 0;
    private final long tempsAttenteMaxGlobal = 120000; // 2 minutes max au total

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
                detecterCoffre(maPosition);
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
        try {
            Location maPositionActuelle = ((AbstractDedaleAgent) this.myAgent).getCurrentPosition();
            if (maPositionActuelle == null) {
                System.out.println(
                        myAgent.getLocalName() + " - Position actuelle inconnue, impossible de retourner au coffre");
                return;
            }

            String nodeSource = maPositionActuelle.getLocationId();
            String nodeDestination = positionCoffre.getLocationId();

            System.out.println(myAgent.getLocalName() + " - Retour vers le nœud du coffre : " + nodeDestination +
                    " depuis " + nodeSource);

            // Vérifier que les deux nœuds existent dans le graphe
            if (!myMap.hasNode(nodeSource)) {
                System.out.println(myAgent.getLocalName() + " - Le nœud source " + nodeSource +
                        " n'existe pas dans la carte. Mise à jour de la carte...");

                // On peut ajouter le nœud source si on est dessus
                myMap.addNewNode(nodeSource);

                // Mettre à jour la carte avec les voisins
                List<Couple<Location, List<Couple<Observation, String>>>> observations = ((AbstractDedaleAgent) this.myAgent)
                        .observe();

                for (Couple<Location, List<Couple<Observation, String>>> obs : observations) {
                    String nodeId = obs.getLeft().getLocationId();
                    if (!nodeId.equals(maPositionActuelle.getLocationId())) {
                        // C'est un voisin, ajoutons-le avec un lien
                        myMap.addNode(nodeId, MapRepresentation.MapAttribute.open);
                        myMap.addEdge(nodeSource, nodeId);
                    }
                }
            }

            if (!myMap.hasNode(nodeDestination)) {
                System.out.println(myAgent.getLocalName() + " - Le nœud destination " + nodeDestination +
                        " n'existe pas dans la carte. Impossible de calculer le chemin.");
                return;
            }

            // Calculer le chemin vers le coffre
            List<String> cheminVersCoffre = null;
            try {
                cheminVersCoffre = myMap.getShortestPath(nodeSource, nodeDestination);
            } catch (Exception e) {
                System.out.println(myAgent.getLocalName() + " - Erreur lors du calcul du chemin: " + e.getMessage());
                etatActuel = Etat.ECHEC;
                return;
            }

            if (cheminVersCoffre == null || cheminVersCoffre.isEmpty()) {
                System.out.println(myAgent.getLocalName() + " - Aucun chemin trouvé vers le coffre");
                etatActuel = Etat.ECHEC;
                return;
            }

            // Se déplacer d'un pas à la fois vers le coffre
            if (cheminVersCoffre.size() > 0) {
                String prochainNoeud = cheminVersCoffre.get(0);
                System.out.println(myAgent.getLocalName() + " - Déplacement vers " + prochainNoeud);
                boolean moved = ((AbstractDedaleAgent) this.myAgent).moveTo(new GsLocation(prochainNoeud));

                if (moved) {
                    System.out.println(myAgent.getLocalName() + " - Déplacement réussi vers " + prochainNoeud);
                    // Si on est arrivé au coffre
                    if (prochainNoeud.equals(nodeDestination)) {
                        System.out.println(myAgent.getLocalName() + " - Arrivé au coffre, prêt à ouvrir!");
                        etatActuel = Etat.OUVERTURE;
                    }
                } else {
                    System.out.println(myAgent.getLocalName() + " - Impossible de se déplacer vers " + prochainNoeud);
                }
            }
        } catch (Exception e) {
            System.out.println(myAgent.getLocalName() + " - Exception lors du retour au coffre: " + e.getMessage());
            e.printStackTrace();
            etatActuel = Etat.ECHEC;
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
                // Vérifier si c'est un coffre
                if (obs.getLeft().getName().equals("Diamond") ||
                        obs.getLeft().getName().equals("Gold")) {
                    coffreTrouve = true;
                    this.treasure = obs.getLeft();
                    positionCoffre = couple.getLeft();
                    System.out.println(myAgent.getLocalName() + " - Coffre trouvé à " +
                            positionCoffre.getLocationId());
                }
                String nomObservation = obs.getLeft().getName();

                // Vérifier si c'est un coffre avec besoin de crochetage
                if (nomObservation.equals("LockPicking")) {
                    coffreTrouve = true;
                    treasure = obs.getLeft();
                    lockpickingRequis = Integer.parseInt(obs.getRight());
                    System.out.println(
                            myAgent.getLocalName() + " - Coffre trouvé! Lockpicking requis: " + lockpickingRequis);
                }

                // Vérifier si c'est un coffre avec besoin de force
                if (nomObservation.equals("Strength")) {
                    coffreTrouve = true;
                    treasure = obs.getLeft();
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
            // Pas de coffre trouvé, continuer à explorer
            System.out.println(myAgent.getLocalName() + " - Pas de coffre trouvé, continuer à explorer");
            // Vérifier si l'agent est sur le nœud du coffre
            if (!maPosition.getLocationId().equals(positionCoffre.getLocationId())) {
                retournerAuCoffre();
            }
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
        // Initialiser le décompte global lors du premier appel
        if (debutAttenteGlobal == 0) {
            debutAttenteGlobal = System.currentTimeMillis();
        }

        // Vérifier le timeout global absolu - ce timeout ne sera jamais réinitialisé
        if (System.currentTimeMillis() - debutAttenteGlobal > tempsAttenteMaxGlobal) {
            System.out.println(myAgent.getLocalName() + " - Temps d'attente global dépassé, abandon définitif");
            etatActuel = Etat.ECHEC;
            return;
        }

        // Vérifier si le nombre maximum de demandes a été atteint
        if (compteurDemandes >= maxDemandes) {
            System.out.println(myAgent.getLocalName() + " - Nombre maximum de demandes atteint (" +
                    maxDemandes + "), abandon de l'ouverture");
            etatActuel = Etat.ECHEC;
            return;
        }

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

            // Réinitialiser le compteur de timeout avec une limitation
            // Ne pas réinitialiser si on est déjà dans la deuxième moitié du timeout global
            if (System.currentTimeMillis() - debutAttenteGlobal < tempsAttenteMaxGlobal / 2) {
                debutAttente = System.currentTimeMillis();
            }
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

        // Écouter les confirmations de mise en route
        MessageTemplate modeleEnRoute = MessageTemplate.MatchProtocol("EN-ROUTE");
        ACLMessage msgEnRoute = myAgent.receive(modeleEnRoute);
        if (msgEnRoute != null) {
            String aidant = msgEnRoute.getSender().getLocalName();
            System.out.println(myAgent.getLocalName() + " - Agent " + aidant + " est en route pour aider!");

            // Réinitialiser le timeout si on est dans la première moitié du timeout global
            if (System.currentTimeMillis() - debutAttenteGlobal < tempsAttenteMaxGlobal / 2) {
                debutAttente = System.currentTimeMillis();
            }
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

        // Vérifier si l'observation du trésor est valide
        if (treasure == null) {
            System.out.println(myAgent.getLocalName() + " - Erreur: observation du trésor manquante");
            // Réessayer de détecter le coffre
            Location maPosition = ((AbstractDedaleAgent) this.myAgent).getCurrentPosition();
            if (maPosition != null) {
                detecterCoffre(maPosition);

                // Si après détection, le trésor est toujours null, abandonner
                if (treasure == null) {
                    System.out.println(myAgent.getLocalName() + " - Impossible de trouver le trésor, abandon");
                    etatActuel = Etat.ECHEC;
                    return;
                }
            } else {
                System.out.println(myAgent.getLocalName() + " - Position actuelle inconnue, abandon");
                etatActuel = Etat.ECHEC;
                return;
            }
        }

        // Maintenant on peut essayer d'ouvrir le coffre
        try {
            // On peut ouvrir le coffre avec l'aide collective
            if (((AbstractDedaleAgent) this.myAgent).openLock(treasure)) {
                System.out.println(myAgent.getLocalName() + " - Coffre ouvert avec succès!");

                // Envoyer un message de confirmation aux agents qui ont aidé
                for (String nomAidant : expertsDisponibles.keySet()) {
                    ACLMessage msgMerci = new ACLMessage(ACLMessage.INFORM);
                    msgMerci.setProtocol("COFFRE-OUVERT");
                    msgMerci.setSender(myAgent.getAID());
                    msgMerci.addReceiver(new AID(nomAidant, AID.ISLOCALNAME));
                    msgMerci.setContent("Merci pour votre aide!");
                    ((AbstractDedaleAgent) this.myAgent).sendMessage(msgMerci);
                }

                // AJOUT: Relancer l'exploration après succès
                this.myAgent.addBehaviour(
                        new ExploCollectBehaviour((AbstractDedaleAgent) this.myAgent, myMap, listeAgents));
                System.out.println(myAgent.getLocalName() + " - Relance de l'exploration après succès d'ouverture");

                etatActuel = Etat.TERMINE;
                termine = true;
            } else {
                System.out.println(myAgent.getLocalName() + " - Échec de l'ouverture du coffre");
                etatActuel = Etat.ECHEC;
            }
        } catch (Exception e) {
            System.err.println(myAgent.getLocalName() + " - Erreur lors de l'ouverture: " + e.getMessage());
            e.printStackTrace();
            etatActuel = Etat.ECHEC;
        }
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

        // Déplacer l'agent pour l'éloigner du coffre
        deplacementPostEchec();

        if (this.myAgent.getLocalName().contains("C")) {
            this.myAgent.addBehaviour(
                    new ExploCollectBehaviour((AbstractDedaleAgent) this.myAgent, myMap, listeAgents));
        } else {
            this.myAgent.addBehaviour(
                    new ExploCoopBehaviour((AbstractDedaleAgent) this.myAgent, myMap, listeAgents));
        }

        System.out.println(myAgent.getLocalName() + " - Relance de l'exploration après abandon d'ouverture");

        etatActuel = Etat.TERMINE;
        termine = true;
    }

    /**
     * Déplace l'agent vers un nœud voisin après un échec d'ouverture
     * pour éviter que l'agent ne reste bloqué sur le même coffre
     */
    private void deplacementPostEchec() {
        Location maPosition = ((AbstractDedaleAgent) this.myAgent).getCurrentPosition();
        if (maPosition == null) {
            System.out.println(myAgent.getLocalName() + " - Position actuelle inconnue, impossible de se déplacer");
            return;
        }

        // Récupérer la liste des nœuds voisins accessibles
        List<Couple<Location, List<Couple<Observation, String>>>> observations = ((AbstractDedaleAgent) this.myAgent)
                .observe();

        List<String> voisinsAccessibles = new ArrayList<>();

        for (Couple<Location, List<Couple<Observation, String>>> observation : observations) {
            // Ne pas inclure le nœud actuel
            if (!observation.getLeft().getLocationId().equals(maPosition.getLocationId())) {
                voisinsAccessibles.add(observation.getLeft().getLocationId());
            }
        }

        if (voisinsAccessibles.isEmpty()) {
            System.out.println(myAgent.getLocalName() + " - Aucun nœud voisin accessible pour s'éloigner");
            return;
        }

        // Choisir un nœud voisin aléatoirement
        int indexAleatoire = (int) (Math.random() * voisinsAccessibles.size());
        String noeudCible = voisinsAccessibles.get(indexAleatoire);

        System.out.println(myAgent.getLocalName() + " - Je m'éloigne du coffre vers le nœud " + noeudCible);

        // Déplacement vers le nœud choisi
        boolean moved = ((AbstractDedaleAgent) this.myAgent).moveTo(new GsLocation(noeudCible));

        if (moved) {
            System.out.println(myAgent.getLocalName() + " - Déplacement réussi vers " + noeudCible);
        } else {
            System.out.println(myAgent.getLocalName() + " - Échec du déplacement vers " + noeudCible);
        }
    }

    @Override
    public boolean done() {
        return this.termine;
    }
}