package jason.asSemantics;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import jason.JasonException;
import jason.architecture.AgArch;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.ListTerm;
import jason.asSyntax.Literal;
import jason.asSyntax.PredicateIndicator;
import jason.asSyntax.Term;
import jason.asSyntax.parser.ParseException;
import jason.runtime.Settings;

public class AffectiveTransitionSystem extends TransitionSystem {
    private String originalStepDeliberate = "";
    private LinkedList<String> deliberative_appraisal = new LinkedList<>();

    AffectiveTransitionSystem(Agent a, Circumstance c, Settings s, AgArch ar) {
        super(a, c, s, ar);
        init();
    }
    
    AffectiveTransitionSystem(TransitionSystem ts) {
        super(ts.getAg(), ts.getC(), ts.getSettings(), ts.getUserAgArch());
        init();
    }
    
    private void init() {
        AffectiveCircumstance affC = new AffectiveCircumstance(this.getC(), (AffectiveAgent) this.getAg());
        this.C = affC;
        affC.setTS(this);
    }
    
    @Override
    protected void applySemanticRuleSense() throws JasonException {
        getLogger().fine(this.toString() + " sense step: " + stepSense);
        switch (stepSense) {
            case "AffectDecay": applyAffectDecay(); break;
            case "DerivePEM": applyDerivePEM(); break;
        default:
            super.applySemanticRuleSense();
        }
    }

    @Override
    protected void applySemanticRuleDeliberate() throws JasonException {
        getLogger().fine(this.toString() + " deliberate step: " + stepSense);
        switch (stepDeliberate) {
            case "DeriveSEM":     applyDeriveSEM(); break; 
            case "UpMood":        applyUpMood(); break; 
        default:
            super.applySemanticRuleDeliberate();  
        }
    }
    
    /* ------ Sense States ------------ */
    @Override
    protected void applyProcMsg() throws JasonException {
        super.applyProcMsg();
        this.stepSense = "AffectDecay";
    }
    
    protected void applyAffectDecay() throws JasonException {
        this.stepSense = "DerivePEM";
        
        String oldMood = this.getAffectiveC().getM().getType();
        this.getAffectiveC().getM().stepDecay(this.getAffectiveAg().getDefaultMood());
        String newMood = this.getAffectiveC().getM().getType();
        
        if(oldMood != newMood){
            updateMoodBelief(oldMood, newMood);
        }
        
        this.getAffectiveC().stepDecayEmotions();
    }
    
    protected void applyDerivePEM() throws JasonException {
        this.stepSense = "SelEv";
        
        Iterator<Literal> perceptsIt = this.getAg().getBB().getPercepts();
        
        while (perceptsIt.hasNext()) {
            Literal percept = perceptsIt.next();
            
            // get all terms of form ´emotion(X)´ in annotations, or empty list if none present
            ListTerm emotions = percept.getAnnots("emotion");
            for(Term emotionTerm: emotions) {
                try {
                    String emotion = ASSyntax.parseLiteral(emotionTerm.toString()).getTerm(0).toString(); //gets X from ´emotion(X)´
                    if (!Emotion.getAllEmotions().contains(emotion)) {
                        throw new JasonException(emotion + " is not a valid OCC emotion, check the catalogue in jason.asSemantics.Emotion");
                    }
                    this.getAffectiveC().PEM.add(Emotion.getEmotion(emotion));
                    
                    // Add belief about experiencing this emotion to agents BB
                    this.getAg().addBel(ASSyntax.createLiteral("emotion",
                                                               ASSyntax.createAtom(emotion)));
                } catch (ParseException e) {
                    throw new JasonException(e.getMessage());
                }
            }
        }
    }
    
    /* ------ Deliberate States ------------ */
    @Override
    protected void applySelEv() throws JasonException {
        super.applySelEv();
        if (stepDeliberate == "ProcAct") {
            this.originalStepDeliberate = "ProcAct";
            this.stepDeliberate = "DeriveSEM";
            return;
        }
    }
    
    @Override
    protected void applyFindOp() throws JasonException {
        // can't use optimized AplPl selection cause SEM need access to C.RP which are not generated here
        // go back to original reasoning cycle
        this.stepDeliberate = "RelPl";
    }
    
    @Override
    protected void applyRelPl() throws JasonException {
        super.applyRelPl();
        // original RelPl can have three potential next steps:
        // -ApplPl, when relevant plans were found
        // -ProcAct, when no relevant plans were found 
        // -SelEv, if "irrelevant" event was selected 
        
        if (stepDeliberate == "SelEv")
            return;
        
        this.originalStepDeliberate = this.stepDeliberate;
        this.stepDeliberate = "DeriveSEM";
    }
    
    protected void applyDeriveSEM() throws JasonException {
        // perform more appraisal that e.g. takes into account C.RP?
        
        synchronized(deliberative_appraisal) {
            for(String emotionString : this.deliberative_appraisal) {
                Emotion emotion = Emotion.getEmotion(emotionString);
                this.getAffectiveC().SEM.add(emotion); 
                
                // Add belief about experiencing this emotion to agents BB
                this.getAg().addBel(emotion.toLiteral());
            }
            this.deliberative_appraisal.clear();
        }
        
        // if there are PEM or SEM, update mood, otherwise stepDeliberate = originalStepDeliberate 
        if(getAffectiveC().getAllEmotions().size() > 0)
            this.stepDeliberate = "UpMood";
        else 
            this.stepDeliberate = this.originalStepDeliberate;
    }
    
    protected void applyUpMood() throws JasonException {
        this.stepDeliberate = this.originalStepDeliberate;

        List<Emotion> emotions = this.getAffectiveC().getAllEmotions();
        
        String oldMood = this.getAffectiveC().getM().getType();
        this.getAffectiveC().getM().updateMood(emotions);
        String newMood = this.getAffectiveC().getM().getType();
        
        if(oldMood != newMood){
            updateMoodBelief(oldMood, newMood);
        }
    }

    protected void updateMoodBelief(String oldMood, String newMood) throws JasonException {
        this.getAg().delBel(ASSyntax.createLiteral("mood",
                                                   ASSyntax.createAtom(oldMood)));
        
        this.getAg().addBel(ASSyntax.createLiteral("mood",
                                                   ASSyntax.createAtom(newMood)));
    }
    
    public void updateMoodBelief() throws JasonException {
        Iterator<Literal> it = this.getAg().getBB().getCandidateBeliefs(new PredicateIndicator("mood", 1));
        if(it != null) {            // for some reason "getCandidateBeliefs" returns null instead of empty iterators -.- 
            while (it.hasNext()){
                Literal moodLit = it.next();
                this.getAg().delBel(moodLit);
            }
        }
        
        this.getAg().addBel(ASSyntax.createLiteral("mood", 
                                                   ASSyntax.createAtom(this.getAffectiveC().getM().getType())));
    }
    
    @Override
    protected void applySelAppl() throws JasonException {
        // TODO: Implement changes
        
        // Rule SelAppl
        getC().SO = getAg().selectOption(getC().AP);

        if (getC().SO != null) {
            stepDeliberate = "AddIM";
            if (getLogger().isLoggable(Level.FINE)) getLogger().fine("Selected option "+getC().SO+" for event "+getC().SE);
        } else {
            getLogger().fine("** selectOption returned null!");
            generateGoalDeletionFromEvent(JasonException.createBasicErrorAnnots("no_option", "selectOption returned null"));
            // can't carry on, no applicable plan.
            stepDeliberate = "ProcAct";
        }
    }
    
    public void scheduleForAppraisal(String emotion) throws JasonException {
        if (!Emotion.getAllEmotions().contains(emotion)) {
            throw new JasonException(emotion + " is not a valid OCC emotion, check the catalogue in jason.asSemantics.Emotion");
        }
        this.deliberative_appraisal.add(emotion);
    }
    
    public AffectiveCircumstance getAffectiveC() {
        return (AffectiveCircumstance) this.getC();
    }
    
    public AffectiveAgent getAffectiveAg() {
        return (AffectiveAgent) this.getAg();
    }
    
}
