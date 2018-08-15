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
  
  <p>Description: chains the provided emotion for emotional appraisal by the affective architecture.
  Can only be used when AffectiveAgent (sub)class is selected as agentClass.
  
  <p>Parameters:<ul>
  <li>emotion (atom): the emotion to be appraised, see {@link jason.asSemantics.Emotion} for emotion list
  <li>target  (atom): the agent the emotion is targeted at [optional]
  <li>source  (string): the event that caused the emotion [optional]
  </ul>
  
  Examples:<ul> 
  <li> <code>.appraise_emotion(joy)</code>: adds "joy" to list of deliberative emotions to be appraised next cycle </li>
  <li> <code>.appraise_emotion(angry,bob)</code>: adds an angry emotion targeted at the agent bob </li>
  </ul>
  
  @author Leonid Berov
 */
public class appraise_emotion extends DefaultInternalAction {
    
    @Override
    public int getMinArgs() { 
        return 1; 
    }

    @Override
    public int getMaxArgs() { 
        return 4;
    }
    
    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        checkArguments(args);
        
        switch(args.length) {
            case 1: ((AffectiveTransitionSystem) ts).scheduleForAppraisal(args[0].toString()); return true;
            case 2: ((AffectiveTransitionSystem) ts).scheduleForAppraisal(args[0].toString(), args[1].toString()); return true;
            case 3: {
                if (args[2].isString()) {
                    String source = ((StringTermImpl) args[2]).getString();
                    Term unifiedSource = ASSyntax.parseLiteral(source).capply(un);
                    
                    // in standard case, assume emotion results from addition of a believe
                    ((AffectiveTransitionSystem) ts).scheduleForAppraisal(args[0].toString(),
                                                                          args[1].toString(),
                                                                          unifiedSource.toString());  
                    return true;
                } else {
                    throw new JasonException("3rd argument of internal action: appraise_emotion should be a string");
                }
            }
            case 4: {
                if (args[2].isString()) {
                    String source = ((StringTermImpl) args[2]).getString();
                    Term unifiedSource = ASSyntax.parseLiteral(source).capply(un);
                    
                    boolean isAddition = args[3].equals(Literal.LTrue);
                    if(isAddition) {
                        source = "+";
                    } else 
                        source = "-";
                    source += unifiedSource.toString();
                    ((AffectiveTransitionSystem) ts).scheduleForAppraisal(args[0].toString(),
                                                                          args[1].toString(),
                                                                          source);
                    return true;
                } else {
                    throw new JasonException("3rd argument of internal action: appraise_emotion should be a string");
                }
            }
            default: throw new JasonException("Wrong number of arguments provided for internal action: appraise_emotion");
        }
    }
    
}
