package jason.asSematics;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;

import jason.asSemantics.Emotion;
import jason.asSemantics.Mood;
import junit.framework.TestCase;

public class MoodTest extends TestCase {

    private Double round(Double val) {
        return new BigDecimal(val.toString()).setScale(5, RoundingMode.HALF_UP).doubleValue();
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
        m.stepDecay(defMood);

        // check that default Mood was not changed
        assertTrue(defMood.getP() == 0);
        assertTrue(defMood.getA() == 0);
        assertTrue(defMood.getD() == 0);
    }

    public void testMoodDecaySimple() throws Throwable {
        Field field = Mood.class.getDeclaredField("DECAY_STEP_LENGTH");
        field.setAccessible(true);
        field.set(null, 0.6);

        Mood defMood = new Mood(0, 0, 0);

        // check that the step length is computed correctly in (easiest in 1D)
        Mood m = new Mood(1, 0, 0);
        m.stepDecay(defMood);

        assertTrue("executed decay step had not the correct length as specified by Mood.DECAY_STEP_LENGTH",
                m.getP() == 0.4);
        assertTrue("executed decay step had wrong angle", m.getA() == 0);
        assertTrue("executed decay step had wrong angle", m.getD() == 0);
    }

    public void testMoodDecaySimpleNegative() throws Throwable {
        Field field = Mood.class.getDeclaredField("DECAY_STEP_LENGTH");
        field.setAccessible(true);
        field.set(null, 0.6);

        Mood defMood = new Mood(0, 0, 0);

        // check that the step length is computed correctly in (easiest in 1D)
        Mood m = new Mood(-1, 0, 0);
        m.stepDecay(defMood);

        assertTrue("executed decay step had not the correct length as specified by Mood.DECAY_STEP_LENGTH",
                m.getP() == -0.4);
        assertTrue("executed decay step had wrong angle", m.getA() == 0);
        assertTrue("executed decay step had wrong angle", m.getD() == 0);
    }

    public void testMoodDecayBehavior() throws Throwable {
        Field field = Mood.class.getDeclaredField("DECAY_STEP_LENGTH");
        field.setAccessible(true);
        field.set(null, 0.6);

        Mood defMood = new Mood(0, 0, 0);
        // check that default mood is reached when decay_step_length is bigger
        // than distance to default mood

        Mood m2 = new Mood(0.3, 0.4, 0); // distance to default mood is 0.5, but
                                            // step is 0.6
        m2.stepDecay(defMood);

        assertTrue(m2.getP() == defMood.getP());
        assertTrue(m2.getA() == defMood.getA());
        assertTrue(m2.getD() == defMood.getD());

        // check that the step length and angle is computed correctly in 2D
        double stepLength = 0.1;
        field.set(null, stepLength);
        Mood m3 = new Mood(0.3, 0.4, 0); // distance to default mood is 0.5, new
                                            // step is 0.1
        m3.stepDecay(defMood);

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
            m.stepDecay(defMood);
        }

        // 9 iterations are not enough to reach default mood
        assertFalse(m.getP() == defMood.getP());
        assertFalse(m.getA() == defMood.getA());
        assertFalse(m.getD() == defMood.getD());

        // 10 times is enough
        m.stepDecay(defMood);
        assertTrue(m.getP() == defMood.getP());
        assertTrue(m.getA() == defMood.getA());
        assertTrue(m.getD() == defMood.getD());
    }

    public void testUpdateMood() throws Throwable {
        Field field = Mood.class.getDeclaredField("UPDATE_STEP_LENGTH");
        field.setAccessible(true);
        field.set(null, Math.sqrt(0.03)); // results in dimension steps of 0.1

        // case: complete pull
        Mood m = new Mood(0, 0, 0);
        m.updateMood(Arrays.asList(Emotion.getEmotion("GRATITUDE"))); // GRATITUDE(0.4, 0.2,
                                                        // -0.3)
        assertEquals(0.1, m.getP());
        assertEquals(0.1, m.getA());
        assertEquals(-0.1, m.getD());

        // round to the first 5 decimals, to avoid small imprecision due to sqrt
        m.updateMood(Arrays.asList(Emotion.getEmotion("GRATITUDE")));
        assertEquals(0.2, round(m.getA())); // Mood.A reaches emCenter.A, next
                                            // step should push

        m.updateMood(Arrays.asList(Emotion.getEmotion("GRATITUDE")));
        assertEquals(0.3, round(m.getA())); // test that equality of.A pushed

        m.updateMood(Arrays.asList(Emotion.getEmotion("GRATITUDE")));
        assertEquals(0.4, round(m.getP()));
        assertEquals(0.4, round(m.getA())); // test that beyond A pushed, too
        assertEquals(-0.4, round(m.getD()));
    }

    public void testUpdateMoodBounds() throws Throwable {
        Field field = Mood.class.getDeclaredField("UPDATE_STEP_LENGTH");
        field.setAccessible(true);
        field.set(null, Math.sqrt(0.03)); // results in dimension steps of 0.1

        // case: complete pull
        Mood m = new Mood(1, 1, 1);
        m.updateMood(Arrays.asList(Emotion.getEmotion("GRATITUDE"))); // GRATITUDE(0.4, 0.2,
                                                                      // -0.3)
        assertEquals(1.0, m.getP());
        assertEquals(1.0, m.getA());
        assertEquals(0.9, m.getD());
    }
}
