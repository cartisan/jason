package jason.asSemantics;

import java.io.Serializable;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;

import jason.asSyntax.Literal;
import javafx.geometry.Point3D;


/**
 * <p> Stores an Affective Agent's mood using the PAD space model with the dimensions:
 *  <i>pleasure, arousal, dominance</i>.
 * Dimensions are represented using scalar double values in the range -1.0 <= x <= 1.0
 * 
 * <p> See: A. Mehrabian. Analysis of the Big-Five Personality Factors in Terms of the PAD
 * Temperament Model. Australian Journal of Psychology, 48(2):86–92, 1996.
 *  @author Leonid Berov
 */
public class Mood implements Serializable, Affect {
    private static final long serialVersionUID = 1L;
    static final String ANNOTATION_FUNCTOR = "mood";

    // defines how many decay steps are needed at most for a mood to return to default mood at N==0
    // N <> 0 affects DECAY_TIME: the higher N, the slower Mood decays (and inverse, too)
    private static int MAX_DECAY_TIME = 50;     // was 30
    private static double DECAY_STEP_LENGTH;    // gets set to ~0.12 if MAX_DECAY_TIME is 30
    
    // defines how many decay steps are needed at most for a mood to reach the maximal value of the target octant at N==0
    // N <> 0 affects UPDATE_TIME: the higher N, the faster Mood moves along the P-dimension (and inverse, too)
    private static double MAX_UPDATE_TIME = 5;
    private static double UPDATE_STEP_LENGTH;   // gets set to ~0.7
                                                // results in 0.4 step in each dim
    
    public static final List<String> DIMENSIONS = Arrays.asList("pleasure", "arousal", "dominance");

    static {
        // executed at class loading time to initialize DECAY_STEP_LENGTH and UPDATE_STEP_LENGTH 
        setStepLengths();
    }
    
    public static void setStepLengths() {
        // maximal dist. in PDA space: (1,1,1) to (-1,-1,-1) --> d_max = sqrt(2²+2²+2²) = 3.46
        // if we want a mood to completely decay back to default mood in at most 10 cycles
        // --> one step should be d_max / 10 = 0.35
        DECAY_STEP_LENGTH = Math.sqrt(12) / MAX_DECAY_TIME;
              
        UPDATE_STEP_LENGTH = Math.sqrt(12) /  MAX_UPDATE_TIME;
    }
    
    public static void setMaxDecayTime(int decayTime){
        MAX_DECAY_TIME = decayTime;
        setStepLengths();
    }

    protected Logger logger = Logger.getLogger(Mood.class.getName());
    public Point3D PAD = null;

    public Mood(double p, double a, double d) {
        if(p>1 | a>1 | d>1 | p<-1 | a<-1 | d<-1) {
            IllegalArgumentException ex =  new IllegalArgumentException("One of the Mood parameters: (" + p + "," +a + "," + d + ") exceeds the bounds (-1.0 < x < 1.0)");
            throw ex;
        }

        this.PAD = new Point3D(p, a, d);
    }
    
    public double strength() {
        return PAD.magnitude();
    }

    public void updateMood(List<Emotion> emotions, Personality personality) {
        if (emotions.isEmpty()) {
            return;
        }
        
        List<Double> step = new LinkedList<Double>();
        double oneDimStep = Math.sqrt(Math.pow(UPDATE_STEP_LENGTH, 2)/3);   // root(3*dim_step²) = UPDATE_STEP_LENGTH  
        
        Point3D emotionCenter = Emotion.findEmotionCenter(emotions);
        double averageIntensity = emotions.stream().mapToDouble( e -> e.intensity).average().getAsDouble();
        assert(1==averageIntensity);        // we don't yet support non-binary intensities
        
        
        // each coordinate moves further into the octant where the emotion center is located
        List<Function<Point3D, Double>> dimensions = Arrays.asList(Point3D::getX, Point3D::getY, Point3D::getZ);
        for(Function<Point3D, Double> getFunc : dimensions) {
            double emCenter_coord = getFunc.apply(emotionCenter);
            Double direction = Math.signum(emCenter_coord);  
            step.add(direction * oneDimStep * averageIntensity);
        }
        assert(3==step.size());
        
        // trait neuroticism correlates with higher emotional reactivity of negative affect (i.e. P-dimension)
        // --> higher values in N should result with faster changes along the P-dimension
        // N /in [-1,1] | neurot_factor_func: (-1) -> 0.5; 1 -> 1.5; 0 -> 0 | neurot_factor = 1 + 0.5N
        double neuroticism_factor = 1.0 + 0.5 * personality.N;
        
        // compute new Mood, take bounds into account
        Point3D stepVec = new Point3D(step.get(0) * neuroticism_factor,
                                      step.get(1),
                                      step.get(2));
        
        this.PAD = ensureBounds(this.PAD.add(stepVec));
    }
    
    /**
     * Returns new 3d point equal to pad if all three dimensions are within (-1,1) range.
     * If pad contains values outside this range, they are truncated in the return value. 
     */
    private Point3D ensureBounds(Point3D pad) {
        Function<Double,Double> limit = val -> Math.max(-1.0, Math.min(val, 1.0));  
        
        return new Point3D(limit.apply((pad.getX())),
                           limit.apply((pad.getY())),
                           limit.apply((pad.getZ())));
    }
    
    /**
     * Applies one decay step, which moves this mood closer to defaultMood.
     * Each step shifts this mood 1 x DECAY_STEP_LENGTH in a 3D space. If the distance to defaultMood is smaller
     * than that, this mood is set to defaultMood instead. Method computes unit vector from this in the direction
     * of defaultMood, than updates this.PAD values by adding the unit vector times DECAY_STEP_LENGTH.  
     */
    public void stepDecay(Mood defaultMood, Personality personality) {
        Point3D diffVec = defaultMood.PAD.subtract(this.PAD);

        // compute direction vector: length 1 and angle leading to defaultMood
        Point3D stepDirection = diffVec.normalize();
        
        // trait neuroticism correlates with higher emotional reactivity of negative affect (i.e. P-dimension)
        // --> higher values in N should result in slower decay along the P-dimension
        // N /in [-1,1] | neurot_factor_func: (-1) -> 1.5; 1 -> 0.5; 0 -> 0 | neurot_factor = 1 - 0.5N
        double neuroticism_factor = 1.0 - 0.5 * personality.N;
        
        // direction vec needs to be (scalar) multiplied with step_length so its magnitude changes to step_length
        // /sqrt((a²+b²)) = 1  --  *STEP --> sqrt((a²+b²))*STEP = STEP ---> sqrt((STEP²a² + STEP²b²)) = STEP ---> a' = STEP*a, b' = STEP * b
        double stepLengths = DECAY_STEP_LENGTH * neuroticism_factor;
        
        // check if distance to default mood is smaller then one decay step
        // in that case, just set new mood to default mood
        if(diffVec.magnitude() <= stepLengths) {
            this.PAD = defaultMood.PAD;
            return;
        }
        
        // else do the step
        Point3D step = stepDirection.multiply(stepLengths);
        this.PAD = this.PAD.add(step);
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
    
    /**
     * Returns this mood's dimension as supplied by the first parameter. Throws RuntimeException if wrong dimension.
     * @param dimension
     * @return 
     */
    public double get(String dimension) {
        switch(dimension) {
        case "pleasure": return this.getP();
        case "arousal": return this.getA();
        case "dominance": return this.getD();
        default: throw new RuntimeException("Attempted to get non-existing dimension: " + dimension);
        }
    }
    
    @Override
    public Mood clone() {
        return new Mood(this.getP(), this.getA(), this.getD());
    }
    
    @Override
    public String toString() {
        return String.format("(%.4f, %.4f, %.4f) ", PAD.getX(), PAD.getY(), PAD.getZ()) + this.getFullName();
    }
    
    @Override
    public int hashCode() {
        return this.PAD.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
       if (!(obj instanceof Mood))
            return false;
       
        if (obj == this)
            return true;

        Mood other = (Mood) obj;
        return this.PAD.equals(other.PAD);
    }
    
    /**
     * The types of a mood depend on which octant of the PAD space it is located in and are defined by
     * (Gebhard 2005). They are stored in the array MOOD_NAMES, at index 4*P + 2*A + D, with P, A, D
     * being either 1 (x>=0) or 0 (else).
     * The strength of a mood depends on its distance to the zero-point: slightly, moderately, fully; with
     * the borders being determined by splitting the maximal distance into thirds.   
     * @return The name of the mood
     */
    public String getFullName() {
        return getStrength() + " " + this.getType();
    }
    
    public String getType(){
        return Affect.getOctant(this);
    }

    public String getStrength() {
        double max_dist = Math.sqrt(3); String strength;
        
        if(this.PAD.magnitude() >= max_dist / 3 * 2)  
            strength = "fully";
        else if(this.PAD.magnitude() >= max_dist / 3) 
            strength = "moderately";
        else 
            strength = "slightly";
        return strength;
    }

    public boolean checkConstraint(Literal constraint) {
        // check that literal complies with form: personality(trait, trait-bound)
        if(constraint.getArity() != 2) {
            logger.severe("*** ERROR: Mood annotation " + constraint.toString() + " has wrong arity. Should be 2.");
            return false;
        }
        
        // check correctness of trait and bound terms
        String dimension = constraint.getTerm(0).toString();
        String bound = constraint.getTerm(1).toString();
        if (!DIMENSIONS.contains(dimension)) {
            logger.severe("*** ERROR: Mood annotation: " + constraint.toString() + " uses an illegal dimension name");
            return false;
        }
        if (!AffectiveDimensionChecks.BOUNDARIES.containsKey(bound)) {
            logger.severe("*** ERROR: Mood annotation: " + constraint.toString() + " uses an illegal trait boundary");
            return false;
        }
            
        switch(dimension) {
            case "pleasure":    return AffectiveDimensionChecks.BOUNDARIES.get(bound).apply(this.getP());
            case "arousal":     return AffectiveDimensionChecks.BOUNDARIES.get(bound).apply(this.getA());
            case "dominance":   return AffectiveDimensionChecks.BOUNDARIES.get(bound).apply(this.getD());
            default:            return false;
        }
    }
}
