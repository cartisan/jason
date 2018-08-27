package jason.asSematics;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;

import org.junit.BeforeClass;

import jason.asSemantics.Emotion;
import jason.asSemantics.Mood;
import jason.asSemantics.Personality;
import junit.framework.TestCase;

public class MoodTest extends TestCase {

    Personality testPersNeutral = new Personality(0, 0, 0, 0, 0);
    Personality testPersNeuroHigh = new Personality(0, 0, 0, 0, 1);
    Personality testPersNeuroLow = new Personality(0, 0, 0, 0, -1);
    
    String gratitudeEmotionName = "gratitude";
    Field DECAY_STEP_LENGTH_TEST;
    Field UPDATE_STEP_LENGTH_TEST;
    
    private Double round(Double val) {
        return new BigDecimal(val.toString()).setScale(5, RoundingMode.HALF_UP).doubleValue();
    }

    @BeforeClass
    public void setUp() throws Exception {
        DECAY_STEP_LENGTH_TEST = Mood.class.getDeclaredField("DECAY_STEP_LENGTH");
        DECAY_STEP_LENGTH_TEST.setAccessible(true);
        
        UPDATE_STEP_LENGTH_TEST = Mood.class.getDeclaredField("UPDATE_STEP_LENGTH");
        UPDATE_STEP_LENGTH_TEST.setAccessible(true);
    }
 
    
    public void testHealthyParams() {
        new Mood(0.5, 0.5, 0.5);
        new Mood(-0.5, -0.5, -0.5);
    }

    public void testParamsOutOfBound() {
        try {
            new Mood(0.5, 0.5, 1.5);
            fail("failed to reject to big parameter.");
        } catch (IllegalArgumentException e) {
            try {
                new Mood(0.5, 0.5, -1.5);
                fail("failed to reject to small parameter.");
            } catch (IllegalArgumentException e2) {

            }
        }

    }

    public void testMoodDecayNoSideEffects() {
        Mood defMood = new Mood(0, 0, 0);
        Mood m = new Mood(1, 0, 0);
        m.stepDecay(defMood, testPersNeutral);

        // check that default Mood was not changed
        assertTrue(defMood.getP() == 0);
        assertTrue(defMood.getA() == 0);
        assertTrue(defMood.getD() == 0);
    }

    public void testMoodDecaySimple() throws Throwable {
        DECAY_STEP_LENGTH_TEST.set(null, 0.6);

        Mood defMood = new Mood(0, 0, 0);

        // check that the step length is computed correctly in (easiest in 1D)
        Mood m = new Mood(1, 0, 0);
        m.stepDecay(defMood, testPersNeutral);

        assertTrue("executed decay step had not the correct length as specified by Mood.DECAY_STEP_LENGTH",
                m.getP() == 0.4);
        assertTrue("executed decay step had wrong angle", m.getA() == 0);
        assertTrue("executed decay step had wrong angle", m.getD() == 0);
    }

    public void testMoodDecaySimpleNegative() throws Throwable {
        DECAY_STEP_LENGTH_TEST.set(null, 0.6);

        Mood defMood = new Mood(0, 0, 0);

        // check that the step length is computed correctly in (easiest in 1D)
        Mood m = new Mood(-1, 0, 0);
        m.stepDecay(defMood, testPersNeutral);

        assertTrue("executed decay step had not the correct length as specified by Mood.DECAY_STEP_LENGTH",
                m.getP() == -0.4);
        assertTrue("executed decay step had wrong angle", m.getA() == 0);
        assertTrue("executed decay step had wrong angle", m.getD() == 0);
    }

    public void testMoodDecayBehavior() throws Throwable {
        DECAY_STEP_LENGTH_TEST.set(null, 0.6);

        Mood defMood = new Mood(0, 0, 0);
        // check that default mood is reached when decay_step_length is bigger
        // than distance to default mood

        Mood m2 = new Mood(0.3, 0.4, 0); // distance to default mood is 0.5, but
                                            // step is 0.6
        m2.stepDecay(defMood, testPersNeutral);

        assertTrue(m2.getP() == defMood.getP());
        assertTrue(m2.getA() == defMood.getA());
        assertTrue(m2.getD() == defMood.getD());

        // check that the step length and angle is computed correctly in 2D
        double stepLength = 0.1;
        DECAY_STEP_LENGTH_TEST.set(null, stepLength);
        Mood m3 = new Mood(0.3, 0.4, 0); // distance to default mood is 0.5, new
                                            // step is 0.1
        m3.stepDecay(defMood, testPersNeutral);

        assertTrue(m3.getP() == 0.24);
        assertTrue(m3.getA() == 0.32);
        assertTrue(m3.getD() == 0);

        // make sure that this really means we stepped 0.1
        assertTrue("decay step executed had not the correct length as specified by Mood.getD()ECAY_STEgetP()_LENGTH",
                Math.sqrt(Math.pow(0.3 - m3.getP(), 2) + Math.pow(0.4 - m3.getA(), 2)) == stepLength);
    }

    public void testMoodDecayMaxDecayTime() throws Throwable {
        Mood.setMaxDecayTime(10);

        Mood m = new Mood(1, 1, 1);
        Mood defMood = new Mood(-1, -1, -1);

        for (int i = 1; i < 10; ++i) {
            m.stepDecay(defMood, testPersNeutral);
        }

        // 9 iterations are not enough to reach default mood
        assertFalse(m.getP() == defMood.getP());
        assertFalse(m.getA() == defMood.getA());
        assertFalse(m.getD() == defMood.getD());

        // 10 times is enough
        m.stepDecay(defMood, testPersNeutral);
        assertTrue(m.getP() == defMood.getP());
        assertTrue(m.getA() == defMood.getA());
        assertTrue(m.getD() == defMood.getD());
    }
    
    public void testUpdateMood() throws Throwable {
        UPDATE_STEP_LENGTH_TEST.set(null, Math.sqrt(0.03)); // results in dimension steps of 0.1

        // case: complete pull
        Mood m = new Mood(0, 0, 0);
        m.updateMood(Arrays.asList(Emotion.getEmotion(gratitudeEmotionName)), testPersNeutral); // GRATITUDE(0.4, 0.2, -0.3)
        assertEquals(0.1, m.getP());
        assertEquals(0.1, m.getA());
        assertEquals(-0.1, m.getD());

        // round to the first 5 decimals, to avoid small imprecision due to sqrt
        m.updateMood(Arrays.asList(Emotion.getEmotion(gratitudeEmotionName)), testPersNeutral);
        assertEquals(0.2, round(m.getA())); // Mood.A reaches emCenter.A, next
                                            // step should push

        m.updateMood(Arrays.asList(Emotion.getEmotion(gratitudeEmotionName)), testPersNeutral);
        assertEquals(0.3, round(m.getA())); // test that equality of.A pushed

        m.updateMood(Arrays.asList(Emotion.getEmotion(gratitudeEmotionName)), testPersNeutral);
        assertEquals(0.4, round(m.getP()));
        assertEquals(0.4, round(m.getA())); // test that beyond A pushed, too
        assertEquals(-0.4, round(m.getD()));
    }

    public void testUpdateMoodBounds() throws Throwable {
        DECAY_STEP_LENGTH_TEST.set(null, Math.sqrt(0.03)); // results in dimension steps of 0.1

        // case: complete pull
        Mood m = new Mood(1, 1, 1);
        m.updateMood(Arrays.asList(Emotion.getEmotion(gratitudeEmotionName)), testPersNeutral); // GRATITUDE(0.4, 0.2, -0.3)
        assertEquals(1.0, m.getP());
        assertEquals(1.0, m.getA());
        assertEquals(0.9, m.getD());
    }
    

    public void testUpdateMoodWithNeuroticism() throws Exception {
        UPDATE_STEP_LENGTH_TEST.set(null, Math.sqrt(0.03)); // results in dimension steps of 0.1
        
        Mood m = new Mood(0, 0, 0);
        m.updateMood(Arrays.asList(Emotion.getEmotion(gratitudeEmotionName)), testPersNeuroHigh); // GRATITUDE(0.4, 0.2, -0.3)
        
        // with N=0 we expected: m'=(0.1; 0.1; -0.1)  | with N=1: m'=(0.15; 0.1; -0.1 )... N doesn't affect A, and D reactivity
        assertEquals(0.15, m.getP(), 0.001);
        assertEquals(0.1, m.getA(), 0.001);
        assertEquals(-0.1, m.getD(), 0.001);  
        
        Mood m2 = new Mood(0, 0, 0);
        m2.updateMood(Arrays.asList(Emotion.getEmotion(gratitudeEmotionName)), testPersNeuroLow); // GRATITUDE(0.4, 0.2, -0.3)
        
        assertEquals(0.05, m2.getP(), 0.001);
        assertEquals(0.1, m2.getA(), 0.001);
        assertEquals(-0.1, m2.getD(), 0.001); 
    }
    
    public void testMoodDecayWithNeuroticism() throws Exception {
         // check that the step length and angle is computed correctly in 2D
        DECAY_STEP_LENGTH_TEST.set(null, 0.1);
        Mood m = new Mood(0, 0, 0);             
        Mood defMood = new Mood(0.3, 0.4, 0);
        
        // step with N=0: (0.06, 0.08, 0), with N=1: (0.03, 0.04, 0) -- slower decay
        m.stepDecay(defMood, testPersNeuroHigh);
        assertEquals(0.03, m.getP(), 0.001);
        assertEquals(0.04, m.getA(), 0.001);
        assertEquals(0.0, m.getD(), 0.001);

        Mood m2 = new Mood(0, 0, 0);            
        
        // step with N=0: (0.06, 0.08, 0), with N=1: (0.09, 0.12, 0) -- faster decay
        m2.stepDecay(defMood, testPersNeuroLow);
        assertEquals(0.09, m2.getP(), 0.001);
        assertEquals(0.12, m2.getA(), 0.001);
        assertEquals(0.0, m2.getD(), 0.001);

        
        // Make sure we jump right at default mood, even if step is modiefied by N trait
        DECAY_STEP_LENGTH_TEST.set(null, 0.1);
        Mood m3 = new Mood(0, 0, 0);            
        Mood defMood2 = new Mood(0.06, 0.08, 0);    // fast decay would jump to (0.09, 0.12, 0)
        
        m3.stepDecay(defMood2, testPersNeuroLow);
        assertEquals(0.06, m3.getP(), 0.001);
        assertEquals(0.08, m3.getA(), 0.001);
        assertEquals(0.0, m3.getD(), 0.001);
    } 
}
