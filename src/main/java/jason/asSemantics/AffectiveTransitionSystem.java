package jason.asSemantics;

import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import jason.JasonException;
import jason.architecture.AgArch;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.ListTerm;
import jason.asSyntax.Literal;
import jason.asSyntax.Term;
import jason.asSyntax.parser.ParseException;
import jason.runtime.Settings;

public class AffectiveTransitionSystem extends TransitionSystem {
    private String originalStepDeliberate = "";

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
    
    protected void applyAffectDecay() {
        this.stepSense = "DerivePEM";
        
        this.getAffectiveC().getM().stepDecay(this.getAffectiveAg().getDefaultMood());
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
    
    protected void applyDeriveSEM() {
        // TODO: Implement SEM derivation
        // takes into account C.RP
        
        // if there are PEM or SEM, update mood, otherwise stepDeliberate = originalStepDeliberate 
        if(getAffectiveC().getAllEmotions().size() > 0)
            this.stepDeliberate = "UpMood";
        else 
            this.stepDeliberate = this.originalStepDeliberate;
    }
    
    protected void applyUpMood() {
        this.stepDeliberate = this.originalStepDeliberate;

        List<Emotion> emotions = this.getAffectiveC().getAllEmotions();
        this.getAffectiveC().getM().updateMood(emotions);
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
    
    public AffectiveCircumstance getAffectiveC() {
        return (AffectiveCircumstance) this.getC();
    }
    
    public AffectiveAgent getAffectiveAg() {
        return (AffectiveAgent) this.getAg();
    }
    
}
