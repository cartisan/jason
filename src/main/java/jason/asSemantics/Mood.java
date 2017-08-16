package jason.asSemantics;

import java.io.Serializable;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;

import javafx.geometry.Point3D;


/**
 * <p> Stores an Affective Agent's mood using the PAD space model with the dimensions:
 *  <i>pleasure, arousal, dominance</i>.
 * Dimensions are represented using scalar double values in the range -1.0 <= x <= 1.0
 * 
 * <p> See: A. Mehrabian. Analysis of the Big-Five Personality Factors in Terms of the PAD
 * Temperament Model. Australian Journal of Psychology, 48(2):86–92, 1996.
 * 
 */
public class Mood implements Serializable {
    private static final long serialVersionUID = 1L;
    static final String ANNOTATION_FUNCTOR = "mood";

    // defines how many decay steps are needed at most for a mood to return to default mood
    // mood updates are performed UPDATE_2_DECAY_RATIO times as fast as the decay, so this also influences 
    // UPDATE_STEP_LENGTH
    private static int MAX_DECAY_TIME = 60;     // was 30
    private static double DECAY_STEP_LENGTH;    // gets set to ~0.12 if MAX_DECAY_TIME is 30
    
    //private static double UPDATE_2_DECAY_RATIO = 5;
    private static double MAX_UPDATE_TIME = 5;
    private static double UPDATE_STEP_LENGTH;   // gets set to ~0.7
                                                // results in 0.4 step in each dim
    
                                                // -P-A-D  +P-A-D
                                                //  |  +A   |   +A
                                                //  |+D |+D |+D |+D  
                                                //  | | | | | | | | 
                                                //  [0|1|2|3|4|5|6|7]
    private static final String[] MOOD_NAMES= {"bored", "disdainful", "anxious", "hostile",      // -P 
                                               "docile","relaxed",    "dependent", "exuberant"}; // +P
    
    static {
        // executed at class loading time to initialize DECAY_STEP_LENGTH and UPDATE_STEP_LENGTH 
        setStepLengths();
    }

    public static void setStepLengths() {
        // maximal dist. in PDA space: (1,1,1) to (-1,-1,-1) --> d_max = sqrt(2²+2²+2²) = 3.46
        // we want a mood to completely decay back to default mood in at most 10 cycles
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
            logger.warning("One of the Mood parameters exceeds the bounds (-1.0 < x < 1.0).");
            throw new IllegalArgumentException("One of the Mood parameters exceeds the bounds (-1.0 < x < 1.0).");
        }

        this.PAD = new Point3D(p, a, d);
    }
    
    public double strength() {
        return PAD.magnitude();
    }

    public void updateMood(List<Emotion> emotions) {
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
        
        // compute new Mood, take bounds into account
        assert(3==step.size());
        Point3D stepVec = new Point3D(step.get(0),
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
    public void stepDecay(Mood defaultMood) {
        Point3D diffVec = defaultMood.PAD.subtract(this.PAD);
        
        // check if distance to default mood is smaller then one decay step
        // in that case, just set new mood to default mood
        if(diffVec.magnitude() <= DECAY_STEP_LENGTH) {
            this.PAD = defaultMood.PAD;
            return;
        }
        
        // compute the unit vector with length 1 and angle leading to defaultMood
        Point3D stepVec = diffVec.normalize();
        
        // perform a step if length DECAY_STEP_LENGTH
        this.PAD = this.PAD.add(stepVec.multiply(DECAY_STEP_LENGTH));
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
    public Mood clone() {
        return new Mood(this.getP(), this.getA(), this.getD());
    }
    
    @Override
    public String toString() {
        return String.format("(%.4f, %.4f, %.4f) ", PAD.getX(), PAD.getY(), PAD.getZ()) + this.getFullName();
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
        Function<Double,Integer> pos = d -> (d >= 0 ? 1 : 0);  // returns 1 if d is positive, else 0
        int index = 4 * pos.apply(getP()) + 2 * pos.apply(getA()) + 1* pos.apply(getD());
        return MOOD_NAMES[index];
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
}
