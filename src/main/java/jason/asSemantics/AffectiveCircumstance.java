package jason.asSemantics;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class AffectiveCircumstance extends Circumstance {

    private static final long serialVersionUID = 1L;
    
    protected List<Emotion>     PEM;
    protected List<Emotion>     SEM;
    protected Mood              M;
    
    public AffectiveCircumstance(AffectiveAgent ag) {
        super();
        createMood(ag);
    }
    
    public AffectiveCircumstance(Circumstance c, AffectiveAgent ag) {
        super();
        createMood(ag);
        
        // effectively clones c into this
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
    
    
    
    public List<Emotion> getPEM() {
        return PEM;
    }

    public List<Emotion> getSEM() {
        return SEM;
    }
    
    public List<Emotion> getAllEmotions() {
        Stream<Emotion> ems = Stream.concat(this.PEM.stream(), this.SEM.stream());
        return ems.collect(Collectors.toList());
    }

    public Mood getM() {
        return M;
    }
    
    public void stepDecayEmotions() {
        this.PEM = stepDecayEmotions(this.getPEM());
        this.SEM = stepDecayEmotions(this.getSEM());
    }
    
    private List<Emotion> stepDecayEmotions(List<Emotion> ems) {
        LinkedList<Emotion> newEms = new LinkedList<>();
        for (Emotion em: ems){
            em.stepDecay();
            if(em.intensity > 0) newEms.add(em);
        }
        return newEms;
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
        if(this.ts != null)
            this.createMood((AffectiveAgent) this.ts.getAg());
    }

    
    @Override
    public String toString() {
        String s = super.toString();

        StringBuilder builder = new StringBuilder(s);
        builder.append("\n  PEM ="+PEM +"\n");
        builder.append("  SEM ="+SEM +"\n");
        builder.append("  M ="+M +"\n");

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
        Element affect = document.createElement("affect");
        affect.setAttribute("mood", getM().toString());
        
        for(Emotion e: this.getAllEmotions()) {
            Element emotion = document.createElement("emotion");
            emotion.setAttribute("pad", e.toString());
            emotion.setAttribute("name", e.getName());
            affect.appendChild(emotion);
        }
        
        c.appendChild(affect);
        
        return c;
    }
}
