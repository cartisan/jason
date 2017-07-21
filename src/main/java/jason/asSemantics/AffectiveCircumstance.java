package jason.asSemantics;

import java.util.LinkedList;
import java.util.List;

public class AffectiveCircumstance extends Circumstance {

	private static final long serialVersionUID = 1L;
	
	protected List<Emotion>  	PEM;
	protected List<Emotion>  	SEM;
	protected Mood				M;
	
	public AffectiveCircumstance(AffectiveAgent ag) {
		super();
		createMood(ag);
	}
	
	public AffectiveCircumstance(Circumstance c, AffectiveAgent ag) {
		super();
		createMood(ag);
		
		// clone non-affective circumstance
		if (c.getAtomicEvent() != null) {
			this.AE = (Event) c.getAtomicEvent().clone();
		}
		this.atomicIntSuspended = c.atomicIntSuspended;
        
		for (Event e: c.E) {
            this.E.add((Event)e.clone());
        }
        for (Intention i: c.I) {
            this.I.add((Intention)i.clone());
        }
        for (Message m: c.MB) {
            this.MB.add((Message)m.clone());
        }
        for (int k: c.PA.keySet()) {
            this.PA.put(k, (ActionExec)PA.get(k).clone());
        }
        for (String k: c.PI.keySet()) {
            this.PI.put(k, (Intention)PI.get(k).clone());
        }
        for (String k: c.PE.keySet()) {
            this.PE.put(k, (Event)PE.get(k).clone());
        }
        for (ActionExec ae: c.FA) {
            this.FA.add((ActionExec)ae.clone());
        }
	}
	
	private void createMood(AffectiveAgent ag) {
		// Usually, at this point this ag will have default personality, when personality gets changed during ag init
		// Agent class will take care of updating circumstance' mood
		this.M = ag.getPersonality().defaultMood();
	}

	public void setMood(Mood m) {
		this.M = m;
	}

	@Override
	public void create() {
		super.create();
		
		this.PEM = new LinkedList<Emotion>();
		this.SEM = new LinkedList<Emotion>();
		this.M = null;
	}
	
	@Override
    public void resetSense() {
		super.resetSense();
		this.M = null;
		this.PEM = null;
    }
     
	@Override
    public void resetDeliberate() {
        super.resetDeliberate();
		this.SEM = null;
    }
	
	@Override
    public String toString() {
        String s = super.toString();

        StringBuilder builder = new StringBuilder(s);
		builder.append("  PEM ="+PEM +"\n");
		builder.append("  SEM ="+SEM +"\n");
		builder.append("  M ="+M +"\n");

        return builder.toString();
	}
}
