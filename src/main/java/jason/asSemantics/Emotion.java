package jason.asSemantics;

import java.util.List;
import java.util.logging.Logger;

import javafx.geometry.Point3D;

/*
 * Represents the emotions from the OCC catalog [1] and represents them in the PAD space using the mapping derived by 
 * [2].
 * 
 * [1] Ortony A., Clore G. L., and Collins A. The Cognitive Structure of Emotions. Cambridge University Press, 
 *     Cambridge, MA, 1988.
 * [2] Gebhard, P. (2005). ALMA: a layered model of affect. In Proceedings of the fourth international joint
 *     conference on Autonomous agents and multiagent systems, pages 29–36, New York, USA. ACM.
 */
public enum Emotion {
	// TODO: Complete list of emotions
	ANGER(-0.51, 0.59, 0.25),
	DISAPPOINTMENT(-0.3, 0.1, -0.4),
	GRATITUDE(0.4, 0.2, -0.3),
	SATISFACTION(0.3, -0.2, 0.4);
	
	public final Point3D PAD;
	public double intensity; // intensity not really supported, it's either there (1) or decayed (0)
	
	Emotion(double p, double a, double d) {
		this.PAD = new Point3D(p, a, d);
		
		// Intensity currently not really supported
		// ideally, intensity is set by appraisal function
		this.intensity = 1.0;
	}
	
	public void stepDecay() {
		// ideally emotion decays {linear, exponentially, tan hyperbolic} over time
		// See: Gebhard P., Kipp M., Klesen M., Rist T. Adding the Emotional Dimension to Scripting Character Dialogues 
		// In: Proc. of the 4th International Working Conference on Intelligent Virtual Agents (IVA'03), 2003, 48-56.  
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
	
	public static Point3D findEmotionCenter(List<Emotion> emotions) {
		// Functional solution for brevity, time complexity in o(3n)
		// FIXME: Convert to single for-loop in case of performance issues
		double averageP = emotions.stream().mapToDouble( Emotion::getP ).average().getAsDouble();
		double averageA = emotions.stream().mapToDouble( Emotion::getA ).average().getAsDouble();
		double averageD = emotions.stream().mapToDouble( Emotion::getD ).average().getAsDouble();
		
		return new Point3D(averageP, averageA, averageD);
	}
}
