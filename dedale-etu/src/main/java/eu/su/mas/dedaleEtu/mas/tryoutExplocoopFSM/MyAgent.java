package eu.su.mas.dedaleEtu.mas.tryoutExplocoopFSM;

import java.util.ArrayList;
import java.util.List;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.behaviours.platformManagment.StartMyBehaviours;
// import eu.su.mas.dedaleEtu.mas.behaviours.Move2TreasureBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import jade.core.behaviours.Behaviour;
// import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.FSMBehaviour;

public class MyAgent extends AbstractDedaleAgent{

    private static final long serialVersionUID = 1678501889141639201L;
    private MapRepresentation myMap ;
	private List<Treasure> ltreasures ;

	// public static final int KEEP_DOING = 0;
    // public static final int TREASURE_FOUND = 1;
	// public static final int FINISHED_EXPLO = 2;
	// public static final int SHARING_INFO = 3;


    /**
	 * This method is automatically called when "agent".start() is executed.
	 * Consider that Agent is launched for the first time. 
	 * 			1) set the agent attributes 
	 *	 		2) add the behaviours
	 *          
	 */
	protected void setup(){

		super.setup();
		
		//get the parameters added to the agent at creation (if any)
		final Object[] args = getArguments();
		
		List<String> list_agentNames=new ArrayList<String>();
		
		if(args.length==0){
			System.err.println("Error while creating the agent, names of agent to contact expected");
			System.exit(-1);
		}else{
			int i=2;// WARNING YOU SHOULD ALWAYS START AT 2. This will be corrected in the next release.
			while (i<args.length) {
				list_agentNames.add((String)args[i]);
				i++;
			}
		}

        /*
         * Init attributs
         */

        myMap = new MapRepresentation(this.getLocalName());
		ltreasures = new ArrayList<>();
		boolean askForHelp = false;  // means that if the agent cannot open the treasure it continues exploring

        /* 
         * FSM Behaviour
         */

        // FSMBehaviour fsm = new FSMBehaviour(this);

		// fsm.registerFirstState(new MapExploBehaviour(this, myMap, list_agentNames), "EXPLORE");
		// fsm.registerState(new TreasureProcessingBehaviour(this, ltreasures, askForHelp), "PROCESS_TREASURE");
		// // fsm.registerState(new Move2TreasureBehaviour(this, myMap, ltreasures), "ONLY_COLLECT");
		// fsm.registerState(new ShareInformationBehaviour(this, myMap, list_agentNames), "SHARE_INFO");

		// fsm.registerTransition("EXPLORE", "EXPLORE", KEEP_DOING);
		// fsm.registerTransition("EXPLORE", "PROCESS_TREASURE", TREASURE_FOUND);
		// fsm.registerTransition("EXPLORE", "ONLY_COLLECT", FINISHED_EXPLO);
		// fsm.registerTransition("EXPLORE", "SHARE_INFO", SHARING_INFO);
		// // fsm.registerTransition("EXPLORE", "ONLY_COLLECT", FINISHED_EXPLO);

		// fsm.registerDefaultTransition("PROCESS_TREASURE", "EXPLORE");

		/***
		 * MANDATORY TO ALLOW YOUR AGENT TO BE DEPLOYED CORRECTLY
		 */

		List<Behaviour> lb=new ArrayList<Behaviour>();
		// lb.add(fsm);
		lb.add(new MapExploBehaviour(this, myMap, list_agentNames));
		// lb.add(new TreasureProcessingBehaviour(this, ltreasures, askForHelp));

		addBehaviour(new StartMyBehaviours(this,lb));

		System.out.println("the  agent "+this.getLocalName()+ " is started");

	}



	/**
	 * This method is automatically called after doDelete()
	 */
	protected void takeDown(){
		super.takeDown();
	}

	protected void beforeMove(){
		super.beforeMove();
		myMap.prepareMigration();
		//System.out.println("I migrate");
	}

	protected void afterMove(){
		super.afterMove();
		myMap.loadSavedData();
		//System.out.println("I migrated");
	}


    
    
}
