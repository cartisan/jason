package jason.asSematics;

import org.junit.Test;

import jason.asSemantics.Mood;
import jason.asSemantics.Personality;
import junit.framework.TestCase;

public class PersonalityTest extends TestCase{

    public void testDefMoodComputation() {
        Personality per = Personality.createDefaultPersonality();  // (0,0,0,0,0)
        Mood defMood = per.defaultMood();
        
        // test that neutral pers results in neutral mood
        assertEquals(0.0, defMood.getP());
        assertEquals(0.0, defMood.getA());
        assertEquals(0.0, defMood.getD());
        
        // test that Pleasure is maximized by correct personality
        per = new Personality(0, 0, 1, 1, -1);
        defMood = per.defaultMood();
        assertEquals(1.0, defMood.getP());
        
        // test that Arousal is maximized by correct personality
        per = new Personality(1, 0, 0, 1, 1);
        defMood = per.defaultMood();
        assertEquals(1.0, defMood.getA());
        
        // test that Dominance is maximized by correct personality
        per = new Personality(1, 1, 1, -1, 0);
        defMood = per.defaultMood();
        assertEquals(1.0, defMood.getD());  
    }
    
    @Test(expected = Test.None.class /* no exception expected */)
    public void testAllMoods() {
        double[] range = new double[]{-1.0, -0.7, -0.3, 0.0, 0.3, 0.7, 1.0};
        
        for(double o:range) {
            for(double c:range) {
                for(double e:range) {
                    for(double a:range) {
                        for(double n:range) {
                            try {
                                new Personality(o,c,e,a,n).defaultMood();
                            }
                            catch(IllegalArgumentException ex) {
//                              System.out.println(new Personality(o,c,e,a,n).toString());
//                              System.out.println(ex.getMessage() + "\n");
                                
                                fail("Personality " + new Personality(o,c,e,a,n).toString() + " caused illegal mood");
                            }
                        }
                    }
                }
            }
        }
    }
}
