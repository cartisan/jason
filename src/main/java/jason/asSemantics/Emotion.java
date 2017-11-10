package jason.asSemantics;

import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import jason.asSyntax.ASSyntax;
import jason.asSyntax.Literal;
import javafx.geometry.Point3D;

/**
 * Captures the emotions from the OCC catalog [1] and represents them in the PAD space using the mapping derived by 
 * [2].
 * 
 * [1] Ortony A., Clore G. L., and Collins A. The Cognitive Structure of Emotions. Cambridge University Press, 
 *     Cambridge, MA, 1988.
 * [2] Gebhard, P. (2005). ALMA: a layered model of affect. In Proceedings of the fourth international joint
 *     conference on Autonomous agents and multiagent systems, pages 29–36, New York, USA. ACM.
 * @author Leonid Berov
 *
 */
public class Emotion implements Affect {
    static public HashMap<String, Supplier<Emotion>> EMOTIONS = new HashMap<>();
    static final String ANNOTATION_FUNCTOR = "emotion";
    
    /* Choosing an emotion acc. to OCC, decision tree:
     * [valenced reaction to] --- [aspects of objects] ----------------------------------------------------------- love / hate 
     * 						  |	
     * 						  --- [actions of agents] ---------- [other agent] ----------------------------------- admiration / reproach
     * 						  |						  |
     * 						  |						  ---------- [self agent]  ----------------------------------- pride / shame
     *						  | 
     * 						  --- [consequences of events] ----- [conseq. for self] --- [prospects irrelevant] --- joy / distress
     * 						  |							   |  				        |	
     * 						  |							   |					    --- [prospects relevant] ----- hope / fear
     * 						  |							   |						  	         [confirmed] ----- satisfaction / fears-confirmed
     * 						  |							   |								  [disconfirmed] ----- relief / disappointment
     * 						  |							   |
     *  					  |							   ----- [conseq. for others] --- [desirable for oth.] --- happy-for/resentment
     *  					  |													  	  |
     *  					  |														  --- [undesirable f. oth.] -- gloating / pity
     *  					  |
     *  					  --- [conseq. of agent actions] --- [other agent] ----------------------------------- gratitude /anger
     *  													 |
     *  													 --- [self agent] -------------------------------------gratification / remorse
     */	
    static {
        EMOTIONS.put("gratification",   () -> new Emotion(0.6, 0.5, 0.4, "gratification"));
    	EMOTIONS.put("admiration",      () -> new Emotion(0.5, 0.3, -0., "admiration"));
        EMOTIONS.put("pride",           () -> new Emotion(0.4, 0.3, 0.3, "pride"));
        EMOTIONS.put("happy_for",       () -> new Emotion(0.4, 0.2, 0.2, "happy_for"));
        EMOTIONS.put("joy",             () -> new Emotion(0.4, 0.2, 0.1, "joy"));
        EMOTIONS.put("gratitude",       () -> new Emotion(0.4, 0.2, -0.3, "gratitude"));
        EMOTIONS.put("love",            () -> new Emotion(0.3, 0.1, 0.2, "love"));
        EMOTIONS.put("satisfaction",    () -> new Emotion(0.3, -0.2, 0.4, "satisfaction"));
        EMOTIONS.put("gloating",        () -> new Emotion(0.3, -0.3, -0.1, "gloating"));
        EMOTIONS.put("hope",            () -> new Emotion(0.2, 0.2, -0.1, "hope"));
        EMOTIONS.put("relief",		    () -> new Emotion(0.2, -0.3, 0.4, "relief"));
        EMOTIONS.put("resentment",    	() -> new Emotion(-0.2, -0.3, -0.2, "resentment"));
        EMOTIONS.put("disappointment",  () -> new Emotion(-0.3, 0.1, -0.4, "disappointment"));
        EMOTIONS.put("remorse",    		() -> new Emotion(-0.3, 0.1, -0.6, "remorse"));
        EMOTIONS.put("shame",    		() -> new Emotion(-0.3, 0.1, -0.6, "shame"));
        EMOTIONS.put("reproach",    	() -> new Emotion(-0.3, -0.1, 0.4, "reproach"));
        EMOTIONS.put("distress",        () -> new Emotion(-0.4, -0.2, -0.5, "distress"));
        EMOTIONS.put("pity",    		() -> new Emotion(-0.4, -0.2, -0.5, "pity"));
        EMOTIONS.put("fears_confirmed", () -> new Emotion(-0.5, -0.3, -0.7, "fears_confirmed"));
        EMOTIONS.put("anger",           () -> new Emotion(-0.51, 0.59, 0.25, "anger"));
        EMOTIONS.put("hate",            () -> new Emotion(-0.6, 0.6, 0.3, "hate"));
        EMOTIONS.put("fear",            () -> new Emotion(-0.64, -0.6, -0.43, "fear"));
    }

    public final Point3D PAD;
    public final String name;
    public double intensity; // intensity not really supported, it's either there (1) or decayed (0)
    public String target;
    
    public static Point3D findEmotionCenter(List<Emotion> emotions) {
        /* Functional solution for brevity, time complexity in o(3n)
         * Convert to single for-loop in case of performance issues */ 

        double averageP = emotions.stream().mapToDouble(Emotion::getP).average().getAsDouble();
        double averageA = emotions.stream().mapToDouble(Emotion::getA).average().getAsDouble();
        double averageD = emotions.stream().mapToDouble(Emotion::getD).average().getAsDouble();
        
        return new Point3D(averageP, averageA, averageD);
    }
    
    public static Set<String> getAllEmotions() {
        return EMOTIONS.keySet();
    }

    public static Emotion getEmotion(String emotion) {
        return EMOTIONS.get(emotion).get();
    }
    
    public Emotion(double p, double a, double d, String name) {
        this.PAD = new Point3D(p, a, d);
        this.name= name;

        // Intensity currently not really supported
        // ideally, intensity is set by appraisal function
        this.intensity = 1.0;
    }

    public void stepDecay() {
        /* ideally emotion decays {linear, exponentially, tan hyperbolic} over time
         * See: Gebhard P., Kipp M., Klesen M., Rist T. Adding the Emotional Dimension to Scripting Character Dialogues
         * In: Proc. of the 4th International Working Conference on Intelligent Virtual Agents (IVA'03), 2003, 48-56. */
        this.intensity = 0;
    }

    public double getP() {
        return PAD.getX();
    }

    public double getA() {
        return PAD.getY();
    }

    public double getD() {
        return PAD.getZ();
    }
    
    @Override
    public String toString() {
        return String.format("(%.2f, %.2f, %.2f)", PAD.getX(), PAD.getY(), PAD.getZ());
    }
    
    public String getName() {
        return this.name;
    }
    
    public Literal toLiteral() {
        Literal emLit =  ASSyntax.createLiteral(ANNOTATION_FUNCTOR, ASSyntax.createAtom(this.name));
        if(this.target != null) {
            Literal annot = ASSyntax.createLiteral("target",  ASSyntax.createAtom(target));
            emLit.addAnnot(annot);
        }
        return emLit;
    }

    public void setTarget(String target) {
        this.target = target;
    }
    
    public boolean hasTarget() {
        return (this.target != null ? true : false);
    }
}
