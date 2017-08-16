package jason.asSemantics;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import jason.JasonException;
import jason.RevisionFailedException;
import jason.architecture.AgArch;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.Literal;
import jason.asSyntax.PredicateIndicator;
import jason.asSyntax.Term;

/**
 *  A subclass of jason.asSemantics.Agent that employs personality aware affective reasoning according to O3A.
 *  This reasoning cycle is implemented in the AffectiveTransitionSystem, a subclass of Transition System. The present
 *  class changes the initialization and cloning methods of its super class in order to inject the new TransitionSystem.
 * 
 *  In order to use affective reasoning capabilities in you Custom Agent implementations, subclass from this class
 *  or use 'agentClass jason.asSemantics.AffectiveAgent' in your mas2j file.
 */
public class AffectiveAgent extends Agent {

    private Personality personality;

    public AffectiveAgent() {
        super();
        this.personality = Personality.createDefaultPersonality();
    }

    @Override
    public void initAg() {
        ts = new AffectiveTransitionSystem(this.ts);  // the TransitionSystem sets this.ts to itself in its own init
        super.initAg();
        
        try {
            this.updateMood();
        } catch (JasonException e) {
            logger.severe("Failed to initialized mood-belief for agent: " + this.getTS().getUserAgArch().getAgName());
            e.printStackTrace();
        }
    }

    /**
     * Clone BB, PL, Circumstance. A new TS is created (based on the cloned
     * circumstance).
     */
    @Override
    public AffectiveAgent clone(AgArch arch) {
        AffectiveAgent a = null;
        try {
            a = this.getClass().newInstance();
        } catch (InstantiationException e1) {
            logger.severe(" cannot create derived class" + e1);
            return null;
        } catch (IllegalAccessException e2) {
            logger.severe(" cannot create derived class" + e2);
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

        a.initAg(); // for initDefaultFunctions() and for overridden/custom agent
        return a;
    }

    public Personality getPersonality() {
        return personality;
    }
    
    public AffectiveTransitionSystem getAffectTS() {
        return (AffectiveTransitionSystem) this.ts;
    }

    /**
     * Used to set an AffectiveAgent's personality during initialization. On
     * creation, AffectiveAgent will have a neutral personality, this method
     * should be used to set it up. It automatically takes care of updating all
     * personality-related values in relevant for the reasoning cycle.
     * 
     * Do not use this to change an agents personality after the initialization
     * phase.
     * 
     * @param personality A personality instance that will be used by the agent's reasoning cycle
     * @throws JasonException 
     */
    public void initializePersonality(Personality personality) throws JasonException {
        this.personality = personality;
        ((AffectiveCircumstance) this.ts.getC()).setMood(this.personality.defaultMood());
        this.updateMood();
    }

    public Mood getDefaultMood() {
        return this.personality.defaultMood();
    }
    
    public boolean checkConstraint(Literal constraint) {
        switch(constraint.getFunctor()) {
            case Personality.ANNOTATION_FUNCTOR:    return this.personality.checkConstraint(constraint);
            case Mood.ANNOTATION_FUNCTOR:           return this.getMood().checkConstraint(constraint);
            default: logger.severe("plan annotation: " +constraint.toString() + " has invalid functor."); return false;
                
        }
        
    }
    
    public void addEmotion(Emotion emotion, String type) throws JasonException {
        switch(type) {
            case "SEM": this.getAffectTS().getAffectiveC().SEM.add(emotion); break;
            case "PEM": this.getAffectTS().getAffectiveC().PEM.add(emotion); break;
            default: throw new JasonException("Emotions can be either of type 'SEM' or 'PEM'. Type used was: " + type); 
        }
        
        // Add belief about experiencing this emotion to agents BB
        this.addBel(emotion.toLiteral());
    }

    public void removeEmotion(Emotion em) throws RevisionFailedException {
        this.delBel(em.toLiteral());
    }
    
    public void updateMood(Mood oldMood, Mood newMood) throws JasonException {
        this.delBel(ASSyntax.createLiteral(Mood.ANNOTATION_FUNCTOR,
                                           ASSyntax.createAtom(oldMood.getType())));
        
        this.addBel(ASSyntax.createLiteral(Mood.ANNOTATION_FUNCTOR,
                                           ASSyntax.createAtom(newMood.getType())));
    }
    
    private void updateMood() throws JasonException {
        Iterator<Literal> it = this.getBB().getCandidateBeliefs(new PredicateIndicator("mood", 1));
        if(it != null) {            // for some reason "getCandidateBeliefs" returns null instead of empty iterators -.- 
            while (it.hasNext()){
                Literal moodLit = it.next();
                this.delBel(moodLit);
            }
        }
        
        this.addBel(ASSyntax.createLiteral(Mood.ANNOTATION_FUNCTOR,
                                           ASSyntax.createAtom(this.getMood().getType())));
    }

    private AffectiveTransitionSystem getAffectiveTS() {
        return (AffectiveTransitionSystem) this.getTS();
    }
    
    public Mood getMood() {
        return this.getAffectiveTS().getAffectiveC().getM();
    }
    
    public List<Emotion> getEmotions() {
        return this.getAffectiveTS().getAffectiveC().getAllEmotions();
    }
}
