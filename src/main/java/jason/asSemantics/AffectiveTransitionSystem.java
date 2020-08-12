package jason.asSemantics;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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
            case "UpMood":        this.applyUpMood(); break;
            case "DeriveSEM":     this.applyDeriveSEM(); break;
        default:
            super.applySemanticRuleSense();
        }
    }

    @Override
    protected void applySemanticRuleDeliberate() throws JasonException {
        this.getLogger().fine(this.toString() + " deliberate step: " + this.stepDeliberate);
        switch (this.stepDeliberate) {
            case "DerivePEM":     this.applyDerivePEM(); break;
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

    protected void applyUpMood() throws JasonException {
        this.stepSense = "DeriveSEM";

        Mood oldMood = this.getAffectiveC().getM().clone();
        List<Emotion> emotions = this.getAffectiveC().getAllEmotions();

        if(!emotions.isEmpty()) {
            // perform one step of mood update
            this.getLogger().fine("Performing mood update");
            this.getAffectiveC().getM().updateMood(emotions, this.getAffectiveAg().getPersonality());
            Mood newMood = this.getAffectiveC().getM().clone();

            // update beliefs
            this.getAffectiveAg().updateMoodValue(newMood);
            if(!oldMood.getType().equals(newMood.getType())){
                this.getAffectiveAg().updateMoodType();
            }

            // see if some of the current emotions contributed directly to current mood, if yes extract targets and sources
            for (Emotion emotion: this.getAffectiveC().getAllEmotions()) {
                if (Affect.getOctant(emotion).equals(Affect.getOctant(this.getAffectiveC().getM()))) {
                    //emotion contributes to current mood
                    this.getAffectiveAg().addMoodSource(emotion.cause);

                    if (emotion.hasTarget()) {
                        this.getAffectiveAg().addMoodTarget(emotion.target);
                    }
                }
            }
            this.getAffectiveAg().updateMoodTarget();

            //decay emotion that have been integrated into mood
            this.getAffectiveC().stepDecayActiveEmotions();
        } else if (!oldMood.equals(this.getAffectiveAg().getDefaultMood())) {
            // perform one step of decay on old mood
            this.getLogger().fine("Performing mood decay");
            this.getAffectiveC().getM().stepDecay(this.getAffectiveAg().getDefaultMood(),
                                                  this.getAffectiveAg().getPersonality());

            // update beliefs
            Mood newMood = this.getAffectiveC().getM().clone();
            this.getAffectiveAg().updateMoodValue(newMood);
            if(!oldMood.getType().equals(newMood.getType())){
                this.getAffectiveAg().updateMoodType();
                this.getAffectiveAg().updateMoodTarget();
            }
        }
    }

    protected void applyDeriveSEM() throws JasonException {
        this.stepSense = "SelEv";

        // derive new sem
        synchronized(this.deliberative_appraisal) {
            this.getLogger().fine("Secondary emotions active: " + this.deliberative_appraisal);
            for(Emotion emotion : this.deliberative_appraisal) {
                this.getAffectiveAg().addEmotion(emotion, "SEM");
            }
            this.deliberative_appraisal.clear();
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
            this.stepDeliberate = "DerivePEM";
            return;
        }

        if (this.C.hasEvent()) {
            // first deal with +/-affective events, so we don't end up deliberating based on wrong beliefs
        	// example: +mood(hostile) might trigger the intention !punish -> here we encode that these things take precedence
        	// also means that unimportant internal events like -affect_target([]) won't pollute events view in debugger
            Iterator<Event> it = this.C.getEvents().iterator();
            while(it.hasNext()) {
                Event ev = it.next();
                if (ev.getTrigger().getPredicateIndicator().getFunctor().endsWith("mood") |
                     ev.getTrigger().getPredicateIndicator().getFunctor().endsWith("affect_target") |
                     ev.getTrigger().getPredicateIndicator().getFunctor().endsWith("emotion")) {
                    it.remove();
                    this.C.SE = ev;
                    this.getLogger().fine("Selected event "+this.C.SE);
                    this.stepDeliberate = "DerivePEM";
                    return;
                }
            }

            // Rule SelEv1 -- like original TransitionSystem.applySelEv
            this.C.SE = this.getAg().selectEvent(this.C.getEvents());
            if (this.getLogger().isLoggable(Level.FINE)) {
                this.getLogger().fine("Selected event "+this.C.SE);
            }
            if (this.C.SE != null) {
                this.stepDeliberate = "DerivePEM";
                return;
            }
        }
        // Rule SelEv2
        // directly to ProcAct if no event to handle
        this.stepDeliberate = "ProcAct";
    }

    protected void applyDerivePEM() throws JasonException {
        this.stepDeliberate = "RelPl";

        // appraise new pem
        String selectedEvent = this.C.SE.toString().substring(1); // remove +/- add begin, to translate event notation to literal notation
        try {
            if (this.C.SE.isInternal() || selectedEvent.startsWith("!")) {
                // selected event is internal (or processing an intention), no primary emotions could have been added to it
                return;
            }
            Literal percept = ASSyntax.parseLiteral(selectedEvent);

            // get all terms of form ´emotion(X)´ in annotations, or empty list if none present
            ListTerm emotions = percept.getAnnots(Emotion.ANNOTATION_FUNCTOR);
            this.getLogger().fine("Active primary emotions identifed in: " + percept + " are: " + emotions);
            for(Term emotionTerm: emotions) {
                Literal emotionLit = ASSyntax.parseLiteral(emotionTerm.toString());
                String emotionType = emotionLit.getTerm(0).toString(); //gets X from ´emotion(X)´
                if (!Emotion.getAllEmotions().contains(emotionType)) {
                    throw new JasonException(emotionType + " is not a valid OCC emotion, check the catalogue in jason.asSemantics.Emotion");
                }
                Emotion emotion = Emotion.getEmotion(emotionType);

                Literal targetLit = percept.getAnnot("target");
                if(targetLit != null) {
                    emotion.setTarget(targetLit.getTerm(0).toString());
                }

                String causeNoAnnots = this.removeAnnots(percept.toString());
                emotion.setCause(causeNoAnnots);

                this.getAffectiveAg().addEmotion(emotion, "PEM");
            }
        } catch (ParseException e) {
            throw new JasonException(e.getMessage());
        }
    }

    protected void applyProcMsg() throws JasonException {
        super.applyProcMsg();
        this.stepSense = "UpMood";

    }

    @Override
    protected void applyFindOp() throws JasonException {
        // can't use optimized AplPl selection cause SEM need access to C.RP which are not generated here
        // go back to original reasoning cycle
        this.stepDeliberate = "RelPl";
    }

    @Override
    protected void applyApplPl() throws JasonException {
        super.applyApplPl();

        if(this.C.AP == null) {
            return;
        }

        /** there are applicable plans available, check all of them for affect annotations
        *    if annotation is present, it has to fit this agent in order for the plan to be applicable, otherwise remove from AP
        *    if no annotation is present, the plan also fits (but plans with annotations will be given precedence during selection
        *    {@see AffectiveAgent#selectOption)}
        **/
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

                // inner affective condition is present but no valid for this agent
                if(!this.getAffectiveAg().checkConstraint((Literal) innerConditions.get(0))) {
                    it.remove();
                }
            }
            // no annotation present, this option is fine
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

    private String removeAnnots(String s) {
        String result = "";
        int openParens = 0;

        for(char c : s.toCharArray()) {
            if( c == '[' & openParens == 0) {
                break;
            }

            if(c == '(') {
                openParens += 1;
            } else if(c == ')' ) {
                openParens -= 1;
            }
            result += c;
        }
        return result;
    }

    public Logger getLogger(String agName) {
        if (this.getUserAgArch().getAgName().equals(agName)) {
            return this.getLogger();
        } else {
            return new SilentLogger();
        }
    }

    private class SilentLogger extends Logger {

        protected SilentLogger() {
            super("silent", null);
        }

        @Override
        public void log(Level level, String msg) {
            // we silently omit the message
            return;
        }
    }
}
