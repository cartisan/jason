package jason.asSemantics;

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
	

	public final double P;
	public final double A;
	public final double D;
	
	Emotion(double p, double a, double d) {
		this.P=p; this.A=a; this.D=d;
	}

}
