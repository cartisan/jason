package jason.asSemantics;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.logging.Logger;


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
	
	public double P;
	public double A;
	public double D;

	public Mood(double p, double a, double d) {
		logger = Logger.getLogger(Mood.class.getName());
		
		if(p>1 | a>1 | d>1 | p<-1 | a<-1 | d<-1) {
			logger.warning("One of the Mood parameters exceeds the bounds (-1.0 < x < 1.0).");
			throw new IllegalArgumentException("One of the Mood parameters exceeds the bounds (-1.0 < x < 1.0).");
		}
		this.P=p; this.A=a; this.D=d;
	}
	
	public double strength() {
		return Math.sqrt(P+A+D);
	}

	public void updateMood(List<Emotion> emotions) {
		// TODO: methods stub
	}
	
	/*
	 * Applies one decay step that moves this mood closer to defaultMood.
	 * Each step shifts this mood 1 x DECAY_STEP_LENGTH. If the distance to defaultMood is smaller than that, this mood is
	 * set to defaultMood instead.
	 */
	public void stepDecay(Mood defaultMood) {
		double p_diff = defaultMood.P - P;		
		double a_diff = defaultMood.A - A;		
		double d_diff = defaultMood.D - D;
		
		double distance = distance(p_diff, a_diff, d_diff);
		
		// check if distance to default mood is smaller then one decay step
		// in that case, just set new mood to default mood
		if(distance <= DECAY_STEP_LENGTH) {
			this.P = defaultMood.P;
			this.A = defaultMood.A;
			this.D = defaultMood.D;
			return;
		}
		
		// compute unit vector that has right angle to the main axes, so that following it leads to defaultMood
		double p_step = p_diff / distance;
		double a_step = a_diff / distance;
		double d_step = d_diff / distance;
		
		// check if computation really resulted in a unit vector
		assert(round(distance(p_step, a_step, d_step), 2) == 1.0);
		
		// don't forget that squaring operations deleted the sign, and with it the traveling direction for each dim
		// of the unit vector --> indetify correct directions and apply decay.
		this.P += (p_step * DECAY_STEP_LENGTH);
		this.A += (a_step * DECAY_STEP_LENGTH);
		this.D += (d_step * DECAY_STEP_LENGTH);
		
	}
	
	/*
	 * Given a point in PAD space, computes the distance to the point of origin.
	 */
	private double distance(double x, double y, double z) {
		return Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2));
	}
	
	/*
	 * Rounds a double val to n decimal places. 
	 */
	private Double round(Double val, int n) {
	    return new BigDecimal(val.toString()).setScale(n,RoundingMode.HALF_UP).doubleValue();
	}
}
