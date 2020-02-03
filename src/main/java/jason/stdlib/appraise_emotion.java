/**
 *
 */
package jason.stdlib;

import jason.JasonException;
import jason.asSemantics.AffectiveTransitionSystem;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.Literal;
import jason.asSyntax.StringTermImpl;
import jason.asSyntax.Term;

/**
 *<p>Internal action: <b><code>.appraise_emotion</code></b>.

  <p>Description: chains the provided deliberative emotion for appraisal by the affective architecture during secondary
  emotion appraisal.  Can only be used when AffectiveAgent (sub)class is selected as agentClass.

  <p>Parameters:<ul>
  <li>emotion   (atom): the emotion to be appraised, see {@link jason.asSemantics.Emotion} for emotion list
  <li>source    (string): the event that caused the emotion, variables will be unified
  <li>target    (atom): the agent the emotion is targeted at [optional]
  <li>deletion  (boolean): true if source was a belief deletion, false if belief addition (default: false) [optional]
  </ul>

  Examples:<ul>
  <li> <code>.appraise_emotion(joy)</code>: adds "joy" to list of deliberative emotions to be appraised next cycle </li>
  <li> <code>.appraise_emotion(angry, "stole(_,X)[source(self)]")</code>: adds an angry emotion with the cause annotation
              <code>stole(_,cheese)</code> if X unifies to cheese. </li>
  <li> <code>.appraise_emotion(angry, "stole(bob,X)[source(self)]", bob)</code>: adds an angry emotion targeted at the agent
              bob with the cause annotation <code>stole(bob,cheese)</code> if X unifying to cheese. </li>
  <li> <code>.appraise_emotion(angry, "love(bob, self)", bob, true)</code>: adds an angry emotion targeted at the agent
              bob with the cause annotation <code>-love(bob,self)</code>. </li>
  </ul>

  @author Leonid Berov
 */
public class appraise_emotion extends DefaultInternalAction {

    @Override
    public int getMinArgs() {
        return 2;
    }

    @Override
    public int getMaxArgs() {
        return 4;
    }

    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        this.checkArguments(args);

        String emotion = args[0].toString();
        String cause;
        if (args[1].isString()) {
            String source = ((StringTermImpl) args[1]).getString();
            Term unifiedSource = ASSyntax.parseLiteral(source).capply(un);
            cause = unifiedSource.toString();
        } else {
            throw new JasonException("2nd argument of internal action: appraise_emotion should be a string, was: " + args[1]);
        }

        switch(args.length) {
            case 2:{
                // in standard case, assume emotion results from addition of a believe
                ((AffectiveTransitionSystem) ts).scheduleForAppraisal(emotion, "+" + cause);
                return true;
            }
            case 3: {
                // in standard case, assume emotion results from addition of a believe
                ((AffectiveTransitionSystem) ts).scheduleForAppraisal(emotion, "+" + cause, args[2].toString());
                return true;
            }
            case 4: {
                if(args[3].equals(Literal.LTrue)) {
                    ((AffectiveTransitionSystem) ts).scheduleForAppraisal(emotion, "-" + cause, args[2].toString());
                    return true;
                }
                if(args[3].equals(Literal.LFalse)) {
                    ((AffectiveTransitionSystem) ts).scheduleForAppraisal(emotion, "+" + cause, args[2].toString());
                    return true;
                }

                throw new JasonException("4th argument of internal action: appraise_emotion should be a boolean, was: " + args[3]);
            }
            default: throw new JasonException("Wrong number of arguments provided for internal action: appraise_emotion");
        }
    }

}
