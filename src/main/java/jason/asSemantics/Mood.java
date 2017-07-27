package jason.asSemantics;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
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
	public static int MAX_DECAY_TIME = 10;
	private static double DECAY_STEP_LENGTH; 	// gets set to ~0.35 if MAX_DECAY_TIME is 10
	static {
		// executed at class loading time to initialize DECAY_STEP_LENGTH 
		setMaxDecayTime(MAX_DECAY_TIME);
	}

	public static void setMaxDecayTime(int maxDecayTime) {
		MAX_DECAY_TIME = maxDecayTime;

		// maximal dist. in PDA space: (1,1,1) to (-1,-1,-1) --> d_max = sqrt(2²+2²+2²) = 3.46
		// we want a mood to completely decay back to default mood in at most 10 cycles
		// --> one step should be d_max / 10 = 0.35
		DECAY_STEP_LENGTH = Math.sqrt(12) / maxDecayTime;
	}
	
	
	private Logger logger = null;
	
	public Point3D PAD = null;

	public Mood(double p, double a, double d) {
		logger = Logger.getLogger(Mood.class.getName());
		
		if(p>1 | a>1 | d>1 | p<-1 | a<-1 | d<-1) {
			logger.warning("One of the Mood parameters exceeds the bounds (-1.0 < x < 1.0).");
			throw new IllegalArgumentException("One of the Mood parameters exceeds the bounds (-1.0 < x < 1.0).");
		}
//		this.P=p; this.A=a; this.D=d;
		this.PAD = new Point3D(p, a, d);
	}
	
	public double strength() {
		return PAD.magnitude();
	}

	public void updateMood(List<Emotion> emotions) {
		// TODO: methods stub
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

}
