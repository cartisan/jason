package jason.asSemantics;

import java.util.function.Function;

/**
 * Affects are located in the 3-dimensional PAD space. The space is divided into octants, which have a proper name.
 * @author Leonid Berov
 *
 */
public interface Affect {

    static public final String ANNOTATION_FUNCTOR = "affect";

    public double getP();
    public double getA();
    public double getD();

                                                // -P-A-D  +P-A-D
                                                //  |  +A   |   +A
                                                //  |+D |+D |+D |+D
                                                //  | | | | | | | |
                                                //  [0|1|2|3|4|5|6|7]
    public static final String[] PAD_OCTANT= {"bored", "disdainful", "anxious", "hostile",      // -P
                                              "docile","relaxed",    "dependent", "exuberant"}; // +P


    /**
     * Identifies in which octant of the PAD space the affect is located.
     * @param affect
     * @return the String name of the octant in PAD space where this affect is located
     */
    public static String getOctant(Affect affect) {
        Function<Double,Integer> pos = d -> (d >= 0 ? 1 : 0);  // returns 1 if d is positive, else 0
        int index = 4 * pos.apply(affect.getP()) + 2 * pos.apply(affect.getA()) + 1* pos.apply(affect.getD());
        return PAD_OCTANT[index];
    }

    public static boolean contributeTo(Emotion em, Mood m) {
        int overlap = 0;
        if(Math.signum(em.getP()) == Math.signum(m.getP())) {overlap += 1;}
        if(Math.signum(em.getA()) == Math.signum(m.getA())) {overlap += 1;}
        if(Math.signum(em.getD()) == Math.signum(m.getD())) {overlap += 1;}

        return overlap >= 2;
    }
}
