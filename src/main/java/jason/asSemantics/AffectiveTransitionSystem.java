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
import jason.asSyntax.Term;
import jason.asSyntax.parser.ParseException;
import jason.runtime.Settings;

public class AffectiveTransitionSystem extends TransitionSystem {

    /**
     * Contains emotion names of emotions appraised during ASL reasoning.
     * Gets populated by {@link jason.stdlib.appraise_emotion}.
     * @see jason.asSemantics.Emotion
     */
    private LinkedList<Emotion> deliberative_appraisal = new LinkedList<>();

    AffectiveTransitionSystem(Agent a, Circumstance c, Settings s, AgArch ar) {
        super(a, c, s, ar);
        this.init();
    }

    AffectiveTransitionSystem(TransitionSystem ts) {
        super(ts.getAg(), ts.getC(), ts.getSettings(), ts.getUserAgArch());
        this.init();
    }

    private void init() {
        AffectiveCircumstance affC = new AffectiveCircumstance(this.getC(), (AffectiveAgent) this.getAg());
        this.C = affC;
        affC.setTS(this);
    }

    @Override
    protected String startingStepDeliberate() {
        return "DeriveSEM";
    }

    @Override
    public boolean canSleepDeliberate() {
        boolean canSleep = super.canSleepDeliberate() && this.getAffectiveC().getPEM().isEmpty();
        if(canSleep) {
            this.getLogger().fine("Recommended that deliberate sleeps for this reasoning cycle");
        }
        return canSleep;
    }

    @Override
    protected void applySemanticRuleSense() throws JasonException {
        this.getLogger().fine(this.toString() + " sense step: " + this.stepSense);
        switch (this.stepSense) {
            case "DerivePEM": this.applyDerivePEM(); break;
        default:
            super.applySemanticRuleSense();
        }
    }

    @Override
    protected void applySemanticRuleDeliberate() throws JasonException {
        this.getLogger().fine(this.toString() + " deliberate step: " + this.stepDeliberate);
        switch (this.stepDeliberate) {
            case "DeriveSEM":     this.applyDeriveSEM(); break;  // TODO: change order to derive only 1 SEM after SelEv
            case "UpMood":        this.applyUpMood(); break;
        default:
            super.applySemanticRuleDeliberate();
        }
    }

    @Override
    protected void applySemanticRuleAct() throws JasonException {
        this.getLogger().fine(this.toString() + " act step: " + this.stepAct);
        super.applySemanticRuleAct();
    }

    /* ------ Sense States ------------ */
    @Override
    protected void applyProcMsg() throws JasonException {
        super.applyProcMsg();
        this.stepSense = "DerivePEM";
    }

    protected void applyDerivePEM() throws JasonException {
        this.stepSense = "SelEv";

        // apply one step of decay to old pem
        this.getAffectiveC().stepDecayPEM();

        // appraise new pem
        Iterator<Literal> perceptsIt = this.getAg().getBB().getPercepts();

        while (perceptsIt.hasNext()) {
            Literal percept = perceptsIt.next();

            // get all terms of form ´emotion(X)´/´emotion(X, Target)´ in annotations, or empty list if none present
            ListTerm emotions = percept.getAnnots(Emotion.ANNOTATION_FUNCTOR);
            for(Term emotionTerm: emotions) {
                try {
                    Literal emotionLit = ASSyntax.parseLiteral(emotionTerm.toString());
                    String emotionType = emotionLit.getTerm(0).toString(); //gets X from ´emotion(X)´
                    if (!Emotion.getAllEmotions().contains(emotionType)) {
                        throw new JasonException(emotionType + " is not a valid OCC emotion, check the catalogue in jason.asSemantics.Emotion");
                    }
                    Emotion emotion = Emotion.getEmotion(emotionType);

                    if(emotionLit.getArity() == 2) {
                        emotion.setTarget(emotionLit.getTerm(1).toString());
                    }

                    emotion.setCause(percept.toString());

                    this.getAffectiveAg().addEmotion(emotion, "PEM");
                } catch (ParseException e) {
                    throw new JasonException(e.getMessage());
                }
            }
        }
    }

    /* ------ Deliberate States ------------ */
    @Override
    protected void applySelEv() throws JasonException {
        // Rule for atomic, if there is an atomic intention, do not select event
        if (this.C.hasAtomicIntention()) {
            this.stepDeliberate = "ProcAct"; // need to go to ProcAct to see if an atomic intention received a feedback action
            return;
        }

        // Rule for atomic, events from atomic intention have priority
        this.C.SE = this.C.removeAtomicEvent();
        if (this.C.SE != null) {
            this.stepDeliberate = "RelPl";
            return;
        }

        if (this.C.hasEvent()) {
            // first deal with +/-affective events, so we don't end up deliberating based on wrong beliefs
            for(Event ev : this.C.getEvents()) {
                if (ev.getTrigger().getPredicateIndicator().getFunctor().endsWith("mood") |
                     ev.getTrigger().getPredicateIndicator().getFunctor().endsWith("affect_target") |
                     ev.getTrigger().getPredicateIndicator().getFunctor().endsWith("emotion")) {
                    // TODO: refactor this into AffectiveAgent#selectEvent!
                    this.C.getEvents().remove(ev);
                    this.C.SE = ev;
                    this.stepDeliberate = "RelPl";
                    return;
                }
            }

            // Rule SelEv1 -- like original TransitionSystem.applySelEv
            this.C.SE = this.getAg().selectEvent(this.C.getEvents());
            if (this.getLogger().isLoggable(Level.FINE)) {
                this.getLogger().fine("Selected event "+this.C.SE);
            }
            if (this.C.SE != null) {
                this.stepDeliberate = "RelPl"; // TODO: DeriveSEM, UpMood after this
                return;
            }
        }
        // Rule SelEv2
        // directly to ProcAct if no event to handle
        this.stepDeliberate = "ProcAct";
    }

    @Override
    protected void applyFindOp() throws JasonException {
        // can't use optimized AplPl selection cause SEM need access to C.RP which are not generated here
        // go back to original reasoning cycle
        this.stepDeliberate = "RelPl";
    }

    protected void applyDeriveSEM() throws JasonException {
        this.stepDeliberate = "UpMood";

        // apply one decay step on old sem
        this.getAffectiveC().stepDecaySEM();


        // derive new sem
        // TODO: find if currentEvent triggers a secondary emotion, using something like this.C.SE == emotion.cause
        synchronized(this.deliberative_appraisal) {
            for(Emotion emotion : this.deliberative_appraisal) {
                this.getAffectiveAg().addEmotion(emotion, "SEM");
            }
            this.deliberative_appraisal.clear();
        }
    }

    protected void applyUpMood() throws JasonException {
        this.stepDeliberate = "SelEv";
        Mood oldMood = this.getAffectiveC().getM().clone();
        List<Emotion> emotions = this.getAffectiveC().getAllEmotions();

        if(emotions.isEmpty()) {
            // perform one step of decay on old mood
            this.getAffectiveC().getM().stepDecay(this.getAffectiveAg().getDefaultMood(),
                                                  this.getAffectiveAg().getPersonality());
        }
        else {
            // perform one step of mood update
            this.getAffectiveC().getM().updateMood(emotions, this.getAffectiveAg().getPersonality());
        }

        Mood newMood = this.getAffectiveC().getM().clone();

        // allow agent to react to change of mood values
        if(!oldMood.equals(newMood)) {
            this.getAffectiveAg().updateMoodValue(newMood);

            // if mood changed octants, update agent beliefs and reset target list
            if(oldMood.getType() != newMood.getType()){
                this.getAffectiveAg().updateMoodType(oldMood, newMood);
            }
        }

        // see if some of the current emotions contributed directly to current mood, if yes extract targets and sources
        for (Emotion emotion: this.getAffectiveC().getAllEmotions()) {
            if (Affect.getOctant(emotion).equals(Affect.getOctant(newMood))) {
                //emotion contributes to current mood
                this.getAffectiveAg().addMoodSource(emotion.cause);

                if (emotion.hasTarget()) {
                    this.getAffectiveAg().addMoodTarget(emotion.target);
                }
            }
        }

        this.getAffectiveAg().updateMoodTarget();
    }

    @Override
    protected void applyApplPl() throws JasonException {
        super.applyApplPl();

        if(this.C.AP == null) {
            return;
        }

        // there are applicable plans available, check all of them for personality annotations
        //   if annotations are present, they all have to fit this agent in order for the plan to be applicable, otherwise remove from AP
        //   if plans with fitting personality annotations are present, consider only these specialized options for AP
        List<Option> specialisedOptions = new LinkedList<>();

        for (Iterator<Option> it = this.C.AP.iterator(); it.hasNext(); ) {
            Option o = it.next();

            Literal affect_condition = (Literal) o.getPlan().getLabel().getAnnots(Affect.ANNOTATION_FUNCTOR).getTerm();
            if (affect_condition != null) {
              List<Term> innerConditions = affect_condition.getTerms();
              if(!(innerConditions.size()==1)) {
                  // list of conditions instead of: functor(term1,term2)
                  // e.g. affect(personality(E,hi),mood(P,lo))
                  this.getLogger().severe("*** ERROR in AffectiveTransitionSystem::applyApplPl >> Plan annotation " +
                                     affect_condition.toString() +
                                     " is malformed, too many terms.");
                  throw new RuntimeException(affect_condition.toString() + " not a valid affective constraint.");
              }

                // a valid inner affective condition is present in this option
                // form e.g. :[affect(and(personality(openness, high),not(mood(...)), ...))]
                if(this.getAffectiveAg().checkConstraint((Literal) innerConditions.get(0))) {
                    // if all terms in the annotation fit our agent, save this option as a specialized option
                    specialisedOptions.add(o);
                } else {
                    // at least one annotation doesn't fit this personality, can't use this option
                    it.remove();
                }
            }

        }

        // specialized plans that have a personality annotation fitting to this agent have been found, prefer these plans
        if (!specialisedOptions.isEmpty()) {
            this.C.AP = specialisedOptions;
            return;
        }

        //we've been removing options in AP, if no options are left, proceed as usual in such a case
        if (this.C.AP.isEmpty()) {
            this.applyRelApplPlRule2("applicable");
        }
    }

    public void scheduleForAppraisal(String emotion, String source) throws JasonException {
        this.scheduleForAppraisal(emotion, source, null);
    }

    public void scheduleForAppraisal(String emotion, String source, String target) throws JasonException {
        if (!Emotion.getAllEmotions().contains(emotion)) {
            throw new JasonException(emotion + " is not a valid OCC emotion, check the catalogue in jason.asSemantics.Emotion");
        }

        Emotion em = Emotion.getEmotion(emotion);
        em.setCause(source);

        if(null != target && !target.equals("\"\"")) {
            em.setTarget(target);
        }

        this.deliberative_appraisal.add(em);
    }

    public AffectiveCircumstance getAffectiveC() {
        return (AffectiveCircumstance) this.getC();
    }

    public AffectiveAgent getAffectiveAg() {
        return (AffectiveAgent) this.getAg();
    }

}
