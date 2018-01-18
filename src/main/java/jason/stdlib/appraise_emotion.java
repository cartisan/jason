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
  <li>target (atom): the agent the emotion is targeted at [optional]
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
        return 2;
    }
    
    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        checkArguments(args);
        
        if (args.length == 1) 
            ((AffectiveTransitionSystem) ts).scheduleForAppraisal(args[0].toString());
        else
            ((AffectiveTransitionSystem) ts).scheduleForAppraisal(args[0].toString(), args[1].toString());
        
        // TODO: add a version that sets the source of an emotion
        
        return true;
    }
    
}
