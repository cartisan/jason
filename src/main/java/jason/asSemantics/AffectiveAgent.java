package jason.asSemantics;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import jason.JasonException;
import jason.RevisionFailedException;
import jason.architecture.AgArch;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.Literal;
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
        
        this.addInitialBel(ASSyntax.createLiteral(Mood.ANNOTATION_FUNCTOR,
                           ASSyntax.createAtom(this.getMood().getType())));
        this.addInitialBel(ASSyntax.createLiteral("affect_target", ASSyntax.createList() ));
        
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
    
    private AffectiveTransitionSystem getAffectiveTS() {
        return (AffectiveTransitionSystem) this.getTS();
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
        this.updateMoodType();
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
            case "SEM": this.getAffectiveTS().getAffectiveC().SEM.add(emotion); break;
            case "PEM": this.getAffectiveTS().getAffectiveC().PEM.add(emotion); break;
            default: throw new JasonException("Emotions can be either of type 'SEM' or 'PEM'. Type used was: " + type); 
        }
        
        // Add belief about experiencing this emotion to agents BB
        this.addBel(emotion.toLiteral());
    }

    public void removeEmotion(Emotion em) throws RevisionFailedException {
        this.delBel(em.toLiteral());
    }
    
    /**
     * Gets called by {@linkplain AffectiveTransitionSystem} each time the mood type changes
     * due to mood updated during the reasoning cycle.
     */
    public void updateMoodType(Mood oldMood, Mood newMood) throws JasonException {
        this.delBel(ASSyntax.createLiteral(Mood.ANNOTATION_FUNCTOR,
                                           ASSyntax.createAtom(oldMood.getType())));
        
        this.addBel(ASSyntax.createLiteral(Mood.ANNOTATION_FUNCTOR,
                                           ASSyntax.createAtom(newMood.getType())));
    }
    
    private void updateMoodType() throws JasonException {
        Literal targetLit = this.findBel(ASSyntax.createLiteral(Mood.ANNOTATION_FUNCTOR, ASSyntax.createVar()),
                                        new Unifier());
        this.delBel(targetLit);
        
        this.addBel(ASSyntax.createLiteral(Mood.ANNOTATION_FUNCTOR,
                                           ASSyntax.createAtom(this.getMood().getType())));

    }
    
    /**
     * Gets called by {@linkplain AffectiveTransitionSystem} each time the values of the current
     * mood are updated during the reasoning cycle. Subclasses can implement custom behavior to react.
     */
    protected void updateMoodValue(Mood newMood, int cycleNumber) {
        // abstract method, customer class can override this if required
    }
    
    protected void addAffectTarget(String target) throws RevisionFailedException {
        this.getAffectiveTS().getAffectiveC().T.add(target);
        
        // update target beliefs in BB, too
        Literal targetLit = this.findBel(ASSyntax.createLiteral("affect_target", ASSyntax.createVar()), new Unifier());
        this.delBel(targetLit);
        
        List<Term> targets = getAffectiveTS().getAffectiveC().T.stream().map(ASSyntax::createAtom).collect(Collectors.toList());
        this.addBel(ASSyntax.createLiteral("affect_target", ASSyntax.createList(targets) ));
        
    }
    
    protected void resetAffectTarget() throws RevisionFailedException {
        this.getAffectiveTS().getAffectiveC().T.clear();
        
        // reset target beliefs in BB, too
        Literal targetLit = this.findBel(ASSyntax.createLiteral("affect_target", ASSyntax.createVar()), new Unifier());
        this.delBel(targetLit);
        
        this.addBel(ASSyntax.createLiteral("affect_target", ASSyntax.createList() ));
    }
    
    public Mood getMood() {
        return this.getAffectiveTS().getAffectiveC().getM();
    }
    
    public List<Emotion> getEmotions() {
        return this.getAffectiveTS().getAffectiveC().getAllEmotions();
    }
}
