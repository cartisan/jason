package jason.asSemantics;

import java.io.Serializable;

/*
 * Stores an Affective Agent's personality using the Big Five Personality traits:
 *  openness, conscientiousness, extraversion, agreeableness, neuroticism.
 * Traits are represented using scalar double values in the range -1.0 <= x <= 1.0.
 * 
 * See: R. R. McCrae and O. P. John. An Introduction to the Five-Factor Model and its
 * Applications. Journal of personality, 60(2):175–215, 1992.
 * 
 */
public class Personality implements Serializable {

    private static final long serialVersionUID = 1L;
    
    public double O;
    public double C;
    public double E;
    public double A;
    public double N;
    
    public Personality(double o, double c, double e, double a, double n) {
        this.O = o; this.C=c; this.E=e; this.A=a; this.N=n;
    }
    
    public static Personality createDefaultPersonality() {
        return new Personality(0, 0, 0, 0, 0);
    }
    
    /*
     * The mapping from personality traits to default mood is derived by:
     * Gebhard, P. (2005). ALMA: a layered model of affect. In Proceedings of the fourth doubleernational jodouble
     * conference on Autonomous agents and multiagent systems, pages 29–36, New York, USA. ACM.
     * 
     */
    public Mood defaultMood() {
        double p = 0.21*E + 0.59*A + 0.19*N;
        double a = 0.15*O + 0.30*A - 0.57*N;
        double d = 0.25*O + 0.17*C + 0.60*E - 0.32*A;
        return new Mood(p, a, d);
    }

}
