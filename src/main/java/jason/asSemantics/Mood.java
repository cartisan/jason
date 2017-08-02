package jason.asSemantics;

import java.io.Serializable;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;

import javafx.geometry.Point3D;


/*
 * Stores an Affective Agent's mood using the PAD space model with the dimensions:
 *  pleasure, arousal, dominance.
 * Dimensions are represented using scalar double values in the range -1.0 <= x <= 1.0
 * 
 * See: A. Mehrabian. Analysis of the Big-Five Personality Factors in Terms of the PAD
 * Temperament Model. Australian Journal of Psychology, 48(2):86–92, 1996.
 * 
 */
public class Mood implements Serializable {
    private static final long serialVersionUID = 1L;

    // defines how many decay steps are needed at most for a mood to return to default mood
    // mood updates are performed two times as fast as the decay, this also influences UPDATE_STEP_LENGTH
    private static int MAX_DECAY_TIME = 10;
    private static double DECAY_STEP_LENGTH;    // gets set to ~0.35 if MAX_DECAY_TIME is 10
    private static double UPDATE_STEP_LENGTH;   // gets set to ~0.7 if MAX_DECAY_TIME is 10, 
                                                // results in 0.404 step in each dim
    static {
        // executed at class loading time to initialize DECAY_STEP_LENGTH and UPDATE_STEP_LENGTH 
        setMaxDecayTime(MAX_DECAY_TIME);
    }

    public static void setMaxDecayTime(int maxDecayTime) {
        MAX_DECAY_TIME = maxDecayTime;

        // maximal dist. in PDA space: (1,1,1) to (-1,-1,-1) --> d_max = sqrt(2²+2²+2²) = 3.46
        // we want a mood to completely decay back to default mood in at most 10 cycles
        // --> one step should be d_max / 10 = 0.35
        DECAY_STEP_LENGTH = Math.sqrt(12) / maxDecayTime;
        UPDATE_STEP_LENGTH = DECAY_STEP_LENGTH * 2;
    }
    
    
    private Logger logger = null;
    
    public Point3D PAD = null;

    public Mood(double p, double a, double d) {
        logger = Logger.getLogger(Mood.class.getName());
        
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
    
    /*
     * Returns new 3d point equal to pad if all three dimensions are within (-1,1) range.
     * If pad contains values outside this range, they are truncated in the return value. 
     */
    private Point3D ensureBounds(Point3D pad) {
        Function<Double,Double> limit = val -> Math.max(-1.0, Math.min(val, 1.0));  
        
        return new Point3D(limit.apply((pad.getX())),
                           limit.apply((pad.getY())),
                           limit.apply((pad.getZ())));
    }
    
    /*
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
    public String toString() {
        return String.format("(%.2f, %.2f, %.2f)", PAD.getX(), PAD.getY(), PAD.getZ());
    }

}
