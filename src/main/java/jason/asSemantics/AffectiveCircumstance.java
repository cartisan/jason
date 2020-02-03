package jason.asSemantics;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import jason.RevisionFailedException;

public class AffectiveCircumstance extends Circumstance {

    private static final long serialVersionUID = 1L;

    protected List<Emotion>     PEM;        // primary emotions (reactive)
    protected List<Emotion>     SEM;        // secondary emotions (deliberative)
    protected Mood              M;          // mood
    protected HashSet<String>   T;          // affective target: agents that current mood is directed at
    protected LinkedList<String>   S;       // affective source: event that caused a mood change

    public AffectiveCircumstance(AffectiveAgent ag) {
        super();
        this.createMood(ag);
    }

    public AffectiveCircumstance(Circumstance c, AffectiveAgent ag) {
        super();
        this.createMood(ag);

        // effectively clones c into this
        if (c.getAtomicEvent() != null) {
            this.AE = (Event) c.getAtomicEvent().clone();
        }
        this.atomicIntSuspended = c.atomicIntSuspended;

        for (Event e: c.E) {
            this.E.add((Event)e.clone());
        }
        for (Intention i: c.I) {
            this.I.add(i.clone());
        }
        for (Message m: c.MB) {
            this.MB.add(m.clone());
        }
        for (int k: c.PA.keySet()) {
            this.PA.put(k, this.PA.get(k).clone());
        }
        for (String k: c.PI.keySet()) {
            this.PI.put(k, this.PI.get(k).clone());
        }
        for (String k: c.PE.keySet()) {
            this.PE.put(k, (Event)this.PE.get(k).clone());
        }
        for (ActionExec ae: c.FA) {
            this.FA.add(ae.clone());
        }
    }



    public List<Emotion> getPEM() {
        return this.PEM;
    }

    public List<Emotion> getSEM() {
        return this.SEM;
    }

    public List<Emotion> getAllEmotions() {
        Stream<Emotion> ems = Stream.concat(this.PEM.stream(), this.SEM.stream());
        return ems.collect(Collectors.toList());
    }

    public Mood getM() {
        return this.M;
    }

    public LinkedList<String> getS() {
        return this.S;
    }

    public void stepDecayPEM() throws RevisionFailedException {
        this.PEM = this.stepDecayEmotions(this.getPEM());
    }

    public void stepDecaySEM() throws RevisionFailedException {
        this.SEM = this.stepDecayEmotions(this.getSEM());
    }

    private List<Emotion> stepDecayEmotions(List<Emotion> ems) throws RevisionFailedException {
        LinkedList<Emotion> newEms = new LinkedList<>();
        for (Emotion em: ems){
            em.stepDecay();
            if(em.intensity > 0) {
				newEms.add(em);
			} else {
                // remove belief that agent is experiencing the emotion from BB
                this.getAffectiveAg().removeEmotion(em);
            }
        }
        return newEms;
    }

    private AffectiveAgent getAffectiveAg() {
        return ((AffectiveTransitionSystem) this.ts).getAffectiveAg();

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

        this.PEM = new LinkedList<>();
        this.SEM = new LinkedList<>();
        this.T = new HashSet<>();
        this.S = new LinkedList<>();

        if(this.ts != null) {
			this.createMood(this.getAffectiveAg());
		}
    }


    @Override
    public String toString() {
        String s = super.toString();

        StringBuilder builder = new StringBuilder(s);
        builder.append("\n  PEM ="+this.PEM +"\n");
        builder.append("  SEM ="+this.SEM +"\n");
        builder.append("  M ="+this.M +"\n");

        return builder.toString();
    }

    @Override
    public Element getAsDOM(Document document) {
        Element c = super.getAsDOM(document);

        // add affective state to circumstance, e.g.
        //<affect mood="(0, 0.5, -0.7)">
        //  <emotion pad="(-0.3, 0.1, -0.4)" name="DISAPPOINTMENT"></emotion>
        //  <emotion pad="(0.4, 0.2, -0.3)" name="GRATITUDE"></emotion>
        //</affect>
        Element affect = document.createElement(Affect.ANNOTATION_FUNCTOR);
        affect.setAttribute(Mood.ANNOTATION_FUNCTOR, this.getM().toString());

        for(Emotion e: this.getAllEmotions()) {
            Element emotion = document.createElement(Emotion.ANNOTATION_FUNCTOR);
            emotion.setAttribute("pad", e.toString());
            emotion.setAttribute("name", e.getName());
            affect.appendChild(emotion);
        }

        c.appendChild(affect);

        return c;
    }
}
