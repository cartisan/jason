/**
 * 
 */
package jason.stdlib;

import jason.asSemantics.AffectiveTransitionSystem;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.Literal;
import jason.asSyntax.Term;

/**
 *<p>Internal action: <b><code>.appraise_emotion</code></b>.
  
  <p>Description: chains the provided emotion for emotional appraisal by the affective architecture.
  Can only be used when AffectiveAgent (sub)class is selected as agentClass.
  
  <p>Parameters:<ul>
  <li>emotion (atom): the emotion to be appraised, see {@link jason.asSemantics.Emotion} for emotion list
  </ul>
  
  Examples:<ul> 
  <li> <code>.appraise_emotion(joy)</code>: adds "joy" to list of deliberative emotions to be appraised next cycle
  </li>
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
        return 1; 
    }
    
    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        checkArguments(args);

        ((AffectiveTransitionSystem) ts).scheduleForAppraisal(args[0].toString());
        return true;
    }
    
}
