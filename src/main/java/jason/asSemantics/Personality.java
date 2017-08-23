package jason.asSemantics;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;

import jason.asSyntax.Literal;

/**
 * <p> Stores an Affective Agent's personality using the Big Five Personality traits:
 *  <i>openness, conscientiousness, extraversion, agreeableness, neuroticism</i>.
 * Traits are represented using scalar double values in the range -1.0 <= x <= 1.0. <br>
 * 
 * <p>Adjectives associated with traits in questionnaires:
 *  <ul>
 *   <li> openness: curious, insightful </li>
 *   <li> conscientiousness: reliable, responsible </li>
 *   <li> extraversion: talkative, active </li>
 *   <li> agreeableness: forgiving, generous </li>
 *   <li> neuroticism: unstable, touchy </li>
 *  </ul>
 * 
 * <p>See: R. R. McCrae and O. P. John. An Introduction to the Five-Factor Model and its
 * Applications. Journal of personality, 60(2):175–215, 1992.
 *  @author Leonid Berov
 */
public class Personality implements Serializable {
    private static final long serialVersionUID = 1L;
    static Logger logger = Logger.getLogger(Personality.class.getName());
    
    static final String ANNOTATION_FUNCTOR = "personality";
    public static final List<String> TRAITS = 
            Arrays.asList("openness", "conscientiousness", "extraversion", "agreeableness", "neuroticism");

    public static final Map<String, Function<Double, Boolean>> TRAIT_CHECKS;
    static {
        TRAIT_CHECKS = new HashMap<String, Function<Double, Boolean>>(); 
        TRAIT_CHECKS.put("low",    val -> val <= -0.7);
        TRAIT_CHECKS.put("medium", val -> val > -0.7 && val < 0.7);
        TRAIT_CHECKS.put("high",   val -> val >= 0.7);
    }
    
    public double O;
    public double C;
    public double E;
    public double A;
    public double N;
    
    public Personality(double o, double c, double e, double a, double n) {
        this.O = o; this.C=c; this.E=e; this.A=a; this.N=n;
    }
    
    public static Personality createDefaultPersonality() {
        return new Personality(0, 0, 0, 0, 0);
    }
    
    /*
     * The mapping from personality traits to default mood is derived by:
     * Gebhard, P. (2005). ALMA: a layered model of affect. In Proceedings of the fourth International joint
     * conference on Autonomous agents and multiagent systems, pages 29–36, New York, USA. ACM.
     * 
     */
    public Mood defaultMood() {
        double p = 0.21*E + 0.59*A - 0.19*N;
        double a = 0.15*O + 0.30*A + 0.57*N;
        double d = 0.25*O + 0.17*C + 0.60*E - 0.32*A;
        return new Mood(p, a, d);
    }

    public boolean checkConstraint(Literal personalityLit) {
        // check that literal complies with form: personality(trait, trait-bound)
        if(personalityLit.getArity() != 2) {
            logger.severe("personality annotation: " + personalityLit.toString() + " has wrong arity. Should be 2.");
            return false;
        }
        
        // check correctness of trait and bound terms
        String trait = personalityLit.getTerm(0).toString();
        String bound = personalityLit.getTerm(1).toString();
        if (!TRAITS.contains(trait)) {
            logger.severe("personality annotation: " + personalityLit.toString() + " uses an illegal trait name");
            return false;
        }
        if (!TRAIT_CHECKS.containsKey(bound)) {
            logger.severe("personality annotation: " + personalityLit.toString() + " uses an illegal trait boundary");
            return false;
        }
            
        switch(trait) {
            case "openness":            return TRAIT_CHECKS.get(bound).apply(this.O);
            case "conscientiousness":   return TRAIT_CHECKS.get(bound).apply(this.C);
            case "extraversion":        return TRAIT_CHECKS.get(bound).apply(this.E);
            case "agreeableness":       return TRAIT_CHECKS.get(bound).apply(this.A);
            case "neuroticism":         return TRAIT_CHECKS.get(bound).apply(this.N);
            default:                    return false;
        }
        
    }
    
    @Override
    public String toString() {
    	return String.format("O: %s C: %s E: %s A: %s N: %s", O, C, E, A, N);
    }
}
