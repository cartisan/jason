package jason.asSemantics;

import jason.architecture.AgArch;
import jason.runtime.Settings;

public class AffectiveTransitionSystem extends TransitionSystem {

    public AffectiveTransitionSystem(Agent a, Circumstance c, Settings s, AgArch ar) {
        super(a, c, s, ar);
    }
    
    @Override
    public void deliberate() {
        System.out.println("Test");
        super.deliberate();
    }

}
