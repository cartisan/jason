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
    
    static {
        // TODO: Complete list of emotions
        EMOTIONS.put("anger",           () -> new Emotion(-0.51, 0.59, 0.25, "anger"));
        EMOTIONS.put("disappointment",  () -> new Emotion(-0.3, 0.1, -0.4, "disapointment"));
        EMOTIONS.put("gratitude",       () -> new Emotion(0.4, 0.2, -0.3, "gratitude"));
        EMOTIONS.put("satisfaction",    () -> new Emotion(0.3, -0.2, 0.4, "satisfaction"));
        EMOTIONS.put("joy",             () -> new Emotion(0.4, 0.2, 0.1, "joy"));
        EMOTIONS.put("pride",           () -> new Emotion(0.4, 0.3, 0.3, "pride"));
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
