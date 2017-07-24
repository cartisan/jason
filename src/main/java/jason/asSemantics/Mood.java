package jason.asSemantics;

import java.io.Serializable;
import java.util.List;

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
	
	public double P;
	public double A;
	public double D;

	public Mood(double p, double a, double d) {
		this.P=p; this.A=a; this.D=d;
	}
	
	public double strength() {
		return Math.sqrt(P+A+D);
	}
	
	public void updateMood(Emotion e) {
		// TODO: methods stub
	}

	public void updateMood(List<Emotion> emotions) {
		// TODO: methods stub
	}
}
