package jason.asSemantics;

import java.util.HashMap;

import jason.architecture.AgArch;
import jason.asSemantics.AffectiveTransitionSystem;
import jason.asSemantics.Agent;
import jason.asSemantics.GoalListenerForMetaEvents;
import jason.asSemantics.InternalAction;

/*
 *  A subclass of jason.asSemantics.Agent that employs personality aware affective reasoning according to O3A.
 *  This reasoning cycle is implemented in the AffectiveTransitionSystem, a subclass of Transition System. The present
 *  class changes the initialization and cloning methods of its super class in order to inject the new TransitionSystem.
 * 
 *  In order to use affective reasoning capabilities in you Custom Agent implementations, subclass from this class
 *  or use 'agentClass jason.asSemantics.AffectiveAgent' in your mas2j file.
 */
public class AffectiveAgent extends Agent {

    public AffectiveAgent() {
        super();
    }

    @Override
    public void initAg() {
        ts = new AffectiveTransitionSystem(this.ts);
        super.initAg();
    }
    
    /** 
     *  Clone BB, PL, Circumstance. 
     *  A new TS is created (based on the cloned circumstance).
     */
    @Override
    public AffectiveAgent clone(AgArch arch) {
        AffectiveAgent a = null;
        try {
            a = this.getClass().newInstance();
        } catch (InstantiationException e1) {
            logger.severe(" cannot create derived class" +e1);
            return null;
        } catch (IllegalAccessException e2) {
            logger.severe(" cannot create derived class" +e2);
            return null;
        }
        
        a.setLogger(arch);
        if (this.getTS().getSettings().verbose() >= 0)
            a.logger.setLevel(this.getTS().getSettings().logLevel());

        synchronized (getBB().getLock()) {
            a.bb = this.bb.clone();
        }
        a.pl = this.pl.clone();
        try {
            fixAgInIAandFunctions(a);
        } catch (Exception e) {
            e.printStackTrace();
        }
        a.aslSource = this.aslSource;
        a.setInternalActions(new HashMap<String, InternalAction>());
        
        a.setTS(new AffectiveTransitionSystem(a, this.getTS().getC().clone(), this.getTS().getSettings(), arch));
        if (a.getPL().hasMetaEventPlans())
            a.getTS().addGoalListener(new GoalListenerForMetaEvents(a.getTS()));
        
        a.initAg(); //for initDefaultFunctions() and for overridden/custom agent 
        return a;
    }
}
