package eu.su.mas.dedaleEtu.mas.behaviours.Collecte;

import java.util.List;
import java.util.ArrayList;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.env.gs.GsLocation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.core.AID;

/**
 * Ce comportement permet à un agent de se déplacer vers un tanker
 * pour vider son sac à dos lorsqu'il est plein
 */
public class GoToTanker extends SimpleBehaviour {

    private static final long serialVersionUID = 1L;

    private AbstractDedaleAgent myAgent;
    private MapRepresentation myMap;
    private String tankerLocation;
    private boolean finished = false;
    private long debutRecherche = 0;
    private List<String> agentList;
    private int etape = 0; // 0=recherche, 1=déplacement, 2=transfert
    private String tankerName = null;

    /**
     * Constructeur
     * 
     * @param myAgent        l'agent qui exécute ce comportement
     * @param myMap          la carte de l'environnement
     * @param tankerLocation la dernière position connue du tanker (peut être null)
     */
    public GoToTanker(AbstractDedaleAgent myAgent, MapRepresentation myMap, String tankerLocation,
            List<String> agentList) {
        super(myAgent);
        this.myAgent = myAgent;
        this.myMap = myMap;
        this.tankerLocation = tankerLocation;
        this.debutRecherche = System.currentTimeMillis();
        this.agentList = agentList;

        // Si on n'a pas de position de tanker, commencer par la recherche
        if (tankerLocation == null) {
            etape = 0; // Recherche
        } else {
            etape = 1; // Déplacement
        }
    }

    @Override
    public void action() {
        try {
            switch (etape) {
                case 0: // Recherche du tanker
                    chercherTanker();
                    break;

                case 1: // Déplacement vers le tanker
                    deplacerVersLeTanker();
                    break;

                case 2: // Transfert des trésors
                    transfererTresors();
                    break;
            }

            // Vérifier le timeout global
            long tempsEcoule = System.currentTimeMillis() - debutRecherche;
            if (tempsEcoule > 20000) { // 20 secondes
                System.out.println(
                        myAgent.getLocalName() + " - Timeout lors de la recherche/déplacement vers tanker. Abandon.");
                finished = true;
            }

        } catch (Exception e) {
            System.err.println(myAgent.getLocalName() + " - Erreur dans GoToTanker: " + e.getMessage());
            e.printStackTrace();
            // En cas d'erreur, on termine le comportement pour éviter de tuer l'agent
            finished = true;
        }
    }

    /**
     * Recherche un tanker dans les environs ou utilise la dernière position connue
     */
    private void chercherTanker() {
        System.out.println(myAgent.getLocalName() + " - Recherche d'un tanker...");

        // Observer l'environnement pour trouver un tanker
        Location maPosition = ((AbstractDedaleAgent) this.myAgent).getCurrentPosition();
        if (maPosition == null) {
            return;
        }
        if (tankerLocation != null) {
            System.out.println(myAgent.getLocalName() + " - Dernière position connue du tanker: " + tankerLocation);
            deplacerVersLeTanker();
            return;
        }

        List<Couple<Location, List<Couple<Observation, String>>>> observations = ((AbstractDedaleAgent) this.myAgent)
                .observe();

        // Chercher un tanker parmi les agents visibles
        for (Couple<Location, List<Couple<Observation, String>>> obs : observations) {
            List<Couple<Observation, String>> entites = obs.getRight();
            for (Couple<Observation, String> entite : entites) {
                if (entite.getLeft() != null && entite.getLeft().getName().equals("AgentName") &&
                        (entite.getRight().contains("Tank") || entite.getRight().contains("tank"))) {

                    tankerName = entite.getRight();
                    tankerLocation = obs.getLeft().getLocationId();

                    System.out.println(myAgent.getLocalName() + " - Tanker " + tankerName + " trouvé à la position "
                            + tankerLocation);

                    // Si le tanker est sur le même noeud, passer directement au transfert
                    if (tankerLocation.equals(maPosition.getLocationId())) {
                        etape = 2; // Passer au transfert
                    } else {
                        etape = 1; // Passer au déplacement
                    }
                    return;
                }
            }
        }

        // Si on n'a pas trouvé de tanker, demander aux autres agents
        if (tankerLocation == null) {
            System.out.println(myAgent.getLocalName() + " - Aucun tanker visible, demande aux autres agents...");

            // Envoyer un message de demande de position de tanker
            ACLMessage demande = new ACLMessage(ACLMessage.REQUEST);
            demande.setProtocol("DEMANDE-POSITION-TANKER");

            // Ajouter tous les agents comme destinataires sauf soi-même
            for (String agent : agentList) {
                if (!agent.equals(myAgent.getLocalName())) {
                    demande.addReceiver(new AID(agent, AID.ISLOCALNAME));
                }
            }
            demande.setContent("Où est le tanker ?");
            demande.setSender(myAgent.getAID());

            ((AbstractDedaleAgent) this.myAgent).sendMessage(demande);

            // Attendre une réponse pendant max 2 secondes
            MessageTemplate mt = MessageTemplate.MatchProtocol("REPONSE-POSITION-TANKER");
            ACLMessage reponse = myAgent.receive(mt);

            if (reponse != null) {
                String contenu = reponse.getContent();
                if (contenu != null && !contenu.isEmpty()) {
                    tankerLocation = contenu;
                    System.out.println(myAgent.getLocalName() + " - Position du tanker reçue: " + tankerLocation);
                    etape = 1; // Passer au déplacement
                    return;
                }
            }

            // Si toujours pas de réponse, attendre un peu
            block(1000);
        }
    }

    /**
     * Déplacement vers la position du tanker
     */
    private void deplacerVersLeTanker() {
        Location myPosition = ((AbstractDedaleAgent) this.myAgent).getCurrentPosition();

        if (myPosition == null) {
            System.out.println(myAgent.getLocalName() + " - Position actuelle inconnue");
            return;
        }

        System.out.println(myAgent.getLocalName() + " - Position actuelle: " + myPosition.getLocationId()
                + ", Destination: " + tankerLocation);

        // Calculer le chemin vers le tanker (avec gestion d'erreur)
        List<String> chemin = null;
        if (tankerLocation != null) {
            chemin = myMap.getShortestPath(myPosition.getLocationId(), tankerLocation);
        } else {
            System.out.println(myAgent.getLocalName() + " - Pas de chemin vers le tanker, retour à la recherche");
            etape = 0; // Revenir à la recherche
            return;
        }

        // Vérifier si le chemin existe
        if (chemin == null || chemin.isEmpty()) {
            System.out.println(myAgent.getLocalName() + " - Aucun chemin trouvé vers le tanker");
            tankerLocation = null;
            etape = 0; // Revenir à la recherche
            return;
        }

        // Se déplacer d'un pas vers le tanker
        if (chemin.size() > 1) {
            String nextNode = chemin.get(0); // Premier nœud du chemin (après la position actuelle)
            System.out.println(myAgent.getLocalName() + " - Déplacement vers le tanker: " + nextNode);

            boolean moved = ((AbstractDedaleAgent) this.myAgent).moveTo(new GsLocation(nextNode));

            if (!moved) {
                System.out.println(myAgent.getLocalName() + " - Impossible de se déplacer vers " + nextNode);
                block(500); // Attendre un peu avant de réessayer
            }
        }
    }

    /**
     * Transfert des trésors au tanker
     */
    private void transfererTresors() {
        System.out.println(myAgent.getLocalName() + " - Tentative de transfert des trésors...");

        // Vérifier que le tanker est bien là
        boolean tankerPresent = false;
        List<Couple<Location, List<Couple<Observation, String>>>> observations = ((AbstractDedaleAgent) this.myAgent)
                .observe();

        // Trouver le tanker parmi les entités observées
        for (Couple<Location, List<Couple<Observation, String>>> obs : observations) {
            for (Couple<Observation, String> entite : obs.getRight()) {
                if (entite.getLeft() != null && entite.getLeft().getName().equals("AgentName") &&
                        (entite.getRight().contains("Tank") || entite.getRight().contains("tank"))) {
                    tankerName = entite.getRight();
                    tankerPresent = true;
                    break;
                }
            }
        }

        if (!tankerPresent) {
            System.out.println(myAgent.getLocalName() + " - Tanker non présent, retour à la recherche");
            tankerLocation = null;
            etape = 0; // Revenir à la recherche
            return;
        }

        // Transférer les trésors au tanker
        boolean transfert = ((AbstractDedaleAgent) this.myAgent).emptyMyBackPack(tankerName);

        if (transfert) {
            System.out.println(myAgent.getLocalName() + " - Trésors transférés avec succès au tanker " + tankerName);
            finished = true; // Terminer le comportement
        } else {
            System.out.println(myAgent.getLocalName() + " - Échec du transfert des trésors");
            // On peut réessayer plus tard
            block(500);
        }
    }

    @Override
    public boolean done() {
        // Si comportement terminé, relancer l'exploration
        if (finished) {
            // Relancer l'exploration
            this.myAgent.addBehaviour(
                    new ExploCollectBehaviour((AbstractDedaleAgent) this.myAgent, myMap, agentList));
            System.out.println(myAgent.getLocalName() + " - Fin du comportement GoToTanker, relance de l'exploration");
        }
        return finished;
    }
}
