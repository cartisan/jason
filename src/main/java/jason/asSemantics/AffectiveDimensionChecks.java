package jason.asSemantics;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class AffectiveDimensionChecks {
    
    public static final Map<String, Function<Double, Boolean>> BOUNDARIES;
    static {
        BOUNDARIES = new HashMap<String, Function<Double, Boolean>>(); 
        BOUNDARIES.put("positive", val -> val > 0);
        BOUNDARIES.put("negative", val -> val < 0);
        BOUNDARIES.put("low",    val -> val <= -0.7);
        BOUNDARIES.put("medium", val -> val > -0.7 && val < 0.7);
        BOUNDARIES.put("high",   val -> val >= 0.7);
    }
}
