package jason.asSematics;

import java.util.Arrays;
import java.util.List;

import jason.asSemantics.Emotion;
import javafx.geometry.Point3D;
import junit.framework.TestCase;

public class EmotionTest extends TestCase {

	public void testFindEmotionCenter() {
		// case 1: center of same emotions is at their locations
		List<Emotion> ems = Arrays.asList(Emotion.ANGER, Emotion.ANGER);
		
		Point3D center = Emotion.findEmotionCenter(ems);
		assertEquals(Emotion.ANGER.getP(), center.getX());
		assertEquals(Emotion.ANGER.getA(), center.getY());
		assertEquals(Emotion.ANGER.getD(), center.getZ());
		
		// case 2: center of different emotions
		List<Emotion> ems2 = Arrays.asList(Emotion.DISAPPOINTMENT, Emotion.SATISFACTION);
		Point3D center2 = Emotion.findEmotionCenter(ems2);
		assertEquals(0.0, center2.getX());
		assertEquals(-0.05, center2.getY());
		assertEquals(0.0, center2.getZ());
	}
}
