package jason.asSemantics;

import jason.architecture.AgArch;
import jason.runtime.Settings;

public class AffectiveTransitionSystem extends TransitionSystem {

    public AffectiveTransitionSystem(Agent a, Circumstance c, Settings s, AgArch ar) {
        super(a, c, s, ar);
    }
    
    public AffectiveTransitionSystem(TransitionSystem ts) {
        super(ts.getAg(), ts.getC(), ts.getSettings(), ts.getUserAgArch());
    }
    
//    @Override
//    public void deliberate() {
//        System.out.println("Test");
//        super.deliberate();
//    }

}
