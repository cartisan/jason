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
    private boolean targetsChanged = false;

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
    
    protected AffectiveTransitionSystem getAffectiveTS() {
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
    
    /**
     * Checks if the current state of an agent conforms with the affective condition specified by a plan
     * annotation.<br>
     * This method recursively resolves the condition, of form e.g. <code>and(personality(E,high),mood(P,low))</code>, by
     * resolving the functor and collecting the results of subordinate checkConstraint calls executed on the terms
     * of this functor. Possible functors are: <i>and, or, not, mood</i> and <i> personality </i>.
     * @param condition
     * @return true if agent fits condition in it's current state, false otherwise 
     */
    public boolean checkConstraint(Literal condition) {
        switch(condition.getFunctor()) {
            case Personality.ANNOTATION_FUNCTOR: {
                //personality(E,hi)
                return this.personality.checkConstraint(condition);
            }
            case Mood.ANNOTATION_FUNCTOR:{
                //mood(P,lo)
                return this.getMood().checkConstraint(condition);
            }
            case AffectiveDimensionChecks.AND: {
                // and(personality(E,hi),mood(P,lo)))
                List<Term> conditions = condition.getTerms();               
                return conditions.stream().allMatch(term -> this.checkConstraint((Literal) term));
            }
            case AffectiveDimensionChecks.OR: {
                // or(personality(E,hi),mood(P,lo)))
                List<Term> conditions = condition.getTerms();               
                return conditions.stream().anyMatch(term -> this.checkConstraint((Literal) term));
            }
            case AffectiveDimensionChecks.NOT: {
                List<Term> innerCondition = condition.getTerms();
                if(!(innerCondition.size()==1)) {
                    // condition to be negated should contain a function, not a list
                    getLogger().severe("ERROR: Plan annotation " + condition.toString() + " is malformed, too many terms.");
                    throw new RuntimeException(condition.toString() + " not a valid affective constraint.");
                }
                
                // not(mood(arousal,high))
                // not(and(personality(E,hi),mood(P,lo)))
                // we want to return false if this agent fits the inner condition 
                return !(this.checkConstraint((Literal) innerCondition.get(0)));
            }
            default: {
                logger.severe("*** ERROR in AffectiveAgent::checkConstraint >> Plan annotation " + 
                              condition.toString() +
                              " has invalid functor.");
                throw new RuntimeException("plan annotation: " +condition.toString() + " has invalid functor.");
            }
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
    
        // make note to update affective_targets in beliefs in BB, too
        this.targetsChanged = true;
        
        // update sources and targets in Circumstance
    	this.getAffectiveTS().getAffectiveC().S.clear();
    	this.getAffectiveTS().getAffectiveC().T.clear();
    }
    
    private void updateMoodType() throws JasonException {
    	// update mood type in belief base
    	Literal moodLit = this.findBel(ASSyntax.createLiteral(Mood.ANNOTATION_FUNCTOR, ASSyntax.createVar()),
                                         new Unifier());
        this.delBel(moodLit);
        this.addBel(ASSyntax.createLiteral(Mood.ANNOTATION_FUNCTOR,
                                           ASSyntax.createAtom(this.getMood().getType())));
        
        // make note to update affective_targets in beliefs in BB, too
        this.targetsChanged = true;
        
        // update sources and targets in Circumstance
    	this.getAffectiveTS().getAffectiveC().S.clear();
    	this.getAffectiveTS().getAffectiveC().T.clear();
        

    }
    
    /**
     * Gets called by {@linkplain AffectiveTransitionSystem} each time the values of the current
     * mood are updated during the reasoning cycle. Subclasses can implement custom behavior to react.
     */
    protected void updateMoodValue(Mood newMood) {
        // abstract method, customer class can override this if required
    }
    
    /**
     * Adds a new target agent to the list that takes note of other agents that contributed to
     * the current mood. This will be relayed to the agent's BB at the end of 
     * {@link AffectiveTransitionSystem#applyUpMood()} when it calls {@link AffectiveAgent#updateMoodTarget()}.  
     * @param target
     */
    protected void addMoodTarget(String target) {
        this.getAffectiveTS().getAffectiveC().T.add(target);
        
        // make note to update affective_targets in beliefs in BB, too
        this.targetsChanged = true;
        
    }
    
    /**
     * Checks, whether affect_targets changed during this reasoning cycle, and updates the BB of the agent
     * accordingly.
     * 
     * @throws RevisionFailedException
     */
    protected void updateMoodTarget() throws RevisionFailedException {
        if (this.targetsChanged) {
            // update target beliefs in BB, too
            Literal targetLit = this.findBel(ASSyntax.createLiteral("affect_target", ASSyntax.createVar()), new Unifier());
            this.delBel(targetLit);
            
            List<Term> targets = getAffectiveTS().getAffectiveC().T.stream().map(ASSyntax::createAtom).collect(Collectors.toList());
            this.addBel(ASSyntax.createLiteral("affect_target", ASSyntax.createList(targets) ));
        }
        
        this.targetsChanged = false;
    }

    public void addMoodSource(String cause) {
        this.getAffectiveTS().getAffectiveC().S.add(cause);
        
    }
    
    public Mood getMood() {
        return this.getAffectiveTS().getAffectiveC().getM();
    }
    
    public List<Emotion> getEmotions() {
        return this.getAffectiveTS().getAffectiveC().getAllEmotions();
    }

}
