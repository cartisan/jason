package jason.asSemantics;

import java.util.logging.Level;

import jason.JasonException;
import jason.architecture.AgArch;
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
        switch (stepSense) {
            case "MoodDecay": applyMoodDecay(); break;
            case "DerivePEM": applyDerivePEM(); break;
        default:
            super.applySemanticRuleSense();
        }
    }

    @Override
    protected void applySemanticRuleDeliberate() throws JasonException {
        switch (stepDeliberate) {
            // TODO: add case for emotion decay!
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
        this.stepSense = "MoodDecay";
    }
    
    protected void applyMoodDecay() {
        this.stepSense = "DerivePEM";
        // TODO: Implement Mood Decay
    }
    
    protected void applyDerivePEM() {
        this.stepSense = "SelEv";
        // TODO: Implement PEM derivation
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
        
        // if there are PEM or SEM, otherwise stepSense = originalStepDeliberate 
        this.stepDeliberate = "UpMood";
    }
    
    protected void applyUpMood() {
        this.stepDeliberate = this.originalStepDeliberate; 
        
        // TODO Implement Mood Update
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
}
