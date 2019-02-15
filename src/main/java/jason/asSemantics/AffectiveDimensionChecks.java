package jason.asSemantics;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class AffectiveDimensionChecks {
    
    // Constants for logical operators used in ASL: affective plan annotations
    public static final String AND = "and";
    public static final String OR = "or";
    public static final String NOT = "not "; // For some weird reason jason appends a space to our 'not' annotations...
    
    // Constants for atoms used in ASL: affective plan annotations
    public static final String POS = "positive";    
    public static final String NEG = "negative";    
    public static final String LOW = "low"; 
    public static final String MED = "medium";  
    public static final String HIG = "high";    
    
    public static final Map<String, Function<Double, Boolean>> BOUNDARIES;
    static {
        BOUNDARIES = new HashMap<String, Function<Double, Boolean>>(); 
        BOUNDARIES.put(POS, val -> val > 0);
        BOUNDARIES.put(NEG, val -> val < 0);
        BOUNDARIES.put(LOW, val -> val <= -0.7);
        BOUNDARIES.put(MED, val -> val > -0.7 && val < 0.7);
        BOUNDARIES.put(HIG, val -> val >= 0.7);
    }
}
