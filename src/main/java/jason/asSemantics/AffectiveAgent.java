package jason.asSemantics;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import jason.JasonException;
import jason.RevisionFailedException;
import jason.architecture.AgArch;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.Literal;
import jason.asSyntax.Plan;
import jason.asSyntax.Rule;
import jason.asSyntax.Term;
import jason.asSyntax.Trigger;
import jason.asSyntax.Trigger.TEOperator;
import jason.asSyntax.Trigger.TEType;
import jason.asSyntax.directives.Include;
import jason.bb.BeliefBase;
import jason.bb.DefaultBeliefBase;
import jason.bb.StructureWrapperForLiteral;
import jason.mas2j.ClassParameters;
import jason.runtime.Settings;

/*
 * A subclass of agent that employs personality aware affective reasoning according to O3A.
 * This reasoning cycle is implemented in the AffectiveTransitionSystem, a subclass of Transition System. The ts
 * instance variable in this class is changed to reference an AffectiveTransitionSystem, and all methods returning
 * ts instances are updated to return the right type. In order to prevent control to slip to the super class, a
 * transitive closure of methods accessing or returning ts is copied from the super class.
 *  
 */
public class AffectiveAgent extends Agent {

    protected AffectiveTransitionSystem ts = null;
	
	public AffectiveAgent() {
	}

	 /**
     * Setup the default agent configuration.
     * 
     * Creates the agent class defined by <i>agClass</i>, default is jason.asSemantics.AffectiveAgent. 
     * Creates the TS for the agent.
     * Creates the belief base for the agent. 
     */
    public static AffectiveAgent create(AgArch arch, String agClass, ClassParameters bbPars, String asSrc, Settings stts) throws JasonException {
        try {
        	AffectiveAgent ag = (AffectiveAgent) Class.forName(agClass).newInstance();
        	new AffectiveTransitionSystem(ag, null, stts, arch);
            

            BeliefBase bb = null;
            if (bbPars == null)
                bb = new DefaultBeliefBase();
            else
                bb = (BeliefBase) Class.forName(bbPars.getClassName()).newInstance();

            ag.setBB(bb);     // the agent's BB have to be already set for the BB initialisation
            ag.initAg();

            if (bbPars != null)
                bb.init(ag, bbPars.getParametersArray());  
            ag.load(asSrc); // load the source code of the agent
            return ag;
        } catch (Exception e) {
            throw new JasonException("as2j: error creating the customised Agent class! - "+agClass, e);
        }
    }
        
    @Override
    public void initAg() {
        if (ts == null) ts = new AffectiveTransitionSystem(this, null, null, new AgArch());
        super.initAg();
        super.ts = null;
    }
    
    /** TS Initialisation (called by the AgArch) */
    public void setTS(AffectiveTransitionSystem ts) {
        this.ts = ts;
        setLogger(ts.getUserAgArch());
        if (ts.getSettings().verbose() >= 0)
            logger.setLevel(ts.getSettings().logLevel());        
    }

    @Override
    public AffectiveTransitionSystem getTS() {
        return ts;
    }
    
    
    /** 
     *  Clone BB, PL, Circumstance. 
     *  A new TS is created (based on the cloned circumstance).
     */
    @Override
    public AffectiveAgent clone(AgArch arch) {
        AffectiveAgent a = null;
        try {
            a = this.getClass().newInstance();
        } catch (InstantiationException e1) {
            logger.severe(" cannot create derived class" +e1);
            return null;
        } catch (IllegalAccessException e2) {
            logger.severe(" cannot create derived class" +e2);
            return null;
        }
        
        a.setLogger(arch);
        if (this.getTS().getSettings().verbose() >= 0)
            a.logger.setLevel(this.getTS().getSettings().logLevel());

        synchronized (getBB().getLock()) {
            a.bb = this.bb.clone();
        }
        a.pl = this.pl.clone();
        try {
            fixAgInIAandFunctions(a);
        } catch (Exception e) {
            e.printStackTrace();
        }
        a.aslSource = this.aslSource;
        a.internalActions = new HashMap<String, InternalAction>();
        
        a.setTS(new AffectiveTransitionSystem(a, this.getTS().getC().clone(), this.getTS().getSettings(), arch));
        if (a.getPL().hasMetaEventPlans())
            a.getTS().addGoalListener(new GoalListenerForMetaEvents(a.getTS()));
        
        a.initAg(); //for initDefaultFunctions() and for overridden/custom agent 
        return a;
    }
    
    @Override
    public Element getAsDOM(Document document) {
    	Element ag = super.getAsDOM(document);
    	
    	// change TS related attributes to our affective ts
        ag.setAttribute("name", ts.getUserAgArch().getAgName());
        ag.setAttribute("cycle", ""+ts.getUserAgArch().getCycleNumber());
        
        return ag;
    }
    
    /** Gets the agent "mind" (beliefs, plans, and circumstance) as XML */
    @Override
    public Document getAgState() {
        if (builder == null) {
            try {
                builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error creating XML builder\n");
                return null;
            }
        }
        Document document = builder.newDocument();
        document.appendChild(document.createProcessingInstruction("xml-stylesheet", "href='http://jason.sf.net/xml/agInspection.xsl' type='text/xsl' "));

        Element ag = getAsDOM(document);
        document.appendChild(ag);

        ag.appendChild(ts.getC().getAsDOM(document));
        return document;
    }
    
    /**
     * Adds <i>bel</i> in belief base (calling brf) and generates the
     * events. If <i>bel</i> has no source, add
     * <code>source(self)</code>. (the belief is not cloned!)
     */
    @Override
    public boolean addBel(Literal bel) throws RevisionFailedException {
        if (!bel.hasSource()) {
            bel.addAnnot(BeliefBase.TSelf);
        }
        List<Literal>[] result = brf(bel, null, Intention.EmptyInt);
        if (result != null && ts != null) {
            ts.updateEvents(result, Intention.EmptyInt);
            return true;
        } else {
            return false;
        }
    }

    /**
     * If the agent believes in <i>bel</i>, removes it (calling brf)
     * and generate the event.
     */
    @Override
    public boolean delBel(Literal bel) throws RevisionFailedException {
        if (!bel.hasSource()) {
            bel.addAnnot(BeliefBase.TSelf);
        }
        List<Literal>[] result = brf(null, bel, Intention.EmptyInt);
        if (result != null && ts != null) {
            ts.updateEvents(result, Intention.EmptyInt);
            return true;
        } else {
            return false;
        }
    }
    
    
    /** Belief Update Function: adds/removes percepts into belief base.
     * 
     *  @return the number of changes (add + dels)
     */
    @Override
    public int buf(Collection<Literal> percepts) {
        /*
        // complexity 3n
         
        HashSet percepts = clone from the list of current environment percepts // 1n

        for b in BBPercept (the set of perceptions already in BB) // 1n
            if b not in percepts // constant time test
                remove b in BBPercept // constant time
                remove b in percept

        for p still in percepts // 1n
            add p in BBPercepts         
        */
        
        if (percepts == null) {
            return 0;
        }
        
        // stat
        int adds = 0;
        int dels = 0;        
        //long startTime = qProfiling == null ? 0 : System.nanoTime();

        // to copy percepts allows the use of contains below
        Set<StructureWrapperForLiteral> perW = new HashSet<StructureWrapperForLiteral>();
        Iterator<Literal> iper = percepts.iterator();
        while (iper.hasNext())
            perW.add(new StructureWrapperForLiteral(iper.next()));
        

        // deleting percepts in the BB that is not perceived anymore
        Iterator<Literal> perceptsInBB = getBB().getPercepts();
        while (perceptsInBB.hasNext()) { 
            Literal l = perceptsInBB.next();
            if (! perW.remove(new StructureWrapperForLiteral(l))) { // l is not perceived anymore
                dels++;
                perceptsInBB.remove(); // remove l as perception from BB
                
                // new version (it is sure that l is in BB, only clone l when the event is relevant)
                Trigger te = new Trigger(TEOperator.del, TEType.belief, l);
                if (ts.getC().hasListener() || pl.hasCandidatePlan(te)) {
                    l = ASSyntax.createLiteral(l.getFunctor(), l.getTermsArray());
                    l.addAnnot(BeliefBase.TPercept);
                    te.setLiteral(l);
                    ts.getC().addEvent(new Event(te, Intention.EmptyInt));
                }
            }
        }
        
        for (StructureWrapperForLiteral lw: perW) {
            try {
                Literal lp = lw.getLiteral().copy().forceFullLiteralImpl();
                lp.addAnnot(BeliefBase.TPercept);
                if (getBB().add(lp)) {
                    adds++;
                    ts.updateEvents(new Event(new Trigger(TEOperator.add, TEType.belief, lp), Intention.EmptyInt));
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error adding percetion " + lw.getLiteral(), e);
            }
        }
        
        return adds + dels;
    }

    
    
    /** parse and load the initial agent code, asSrc may be null */
    @Override
    public void load(String asSrc) throws JasonException {
        // set the agent
        try {
            boolean parsingOk = true;
            if (asSrc != null) {
                asSrc = asSrc.replaceAll("\\\\", "/");
                setASLSrc(asSrc);
    
                if (asSrc.startsWith(Include.CRPrefix)) {
                    // loads the class from a jar file (for example)
                    parseAS(Agent.class.getResource(asSrc.substring(Include.CRPrefix.length())).openStream());
                } else {
                    // check whether source is an URL string
                    try {
                        parsingOk = parseAS(new URL(asSrc));
                    } catch (MalformedURLException e) {
                        parsingOk = parseAS(new File(asSrc));
                    }
                }
            }

            
            if (parsingOk) {
                if (getPL().hasMetaEventPlans())
                    getTS().addGoalListener(new GoalListenerForMetaEvents(getTS()));
                
                addInitialBelsFromProjectInBB();
                addInitialBelsInBB();
                addInitialGoalsFromProjectInBB();
                addInitialGoalsInTS();
                fixAgInIAandFunctions(this); // used to fix agent reference in functions used inside includes
            }
            
            loadKqmlPlans();
            
            setASLSrc(asSrc);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error creating customised Agent class!", e);
            throw new JasonException("Error creating customised Agent class! - " + e);
        }
    }
    
    
    /** add the initial beliefs in BB and produce the corresponding events */
    @Override
    public void addInitialBelsInBB() throws RevisionFailedException {
        // Once beliefs are stored in a Stack in the BB, insert them in inverse order
        for (int i=initialBels.size()-1; i >=0; i--) {
            Literal b = initialBels.get(i);

            // if l is not a rule and has free vars (like l(X)), convert it into a rule like "l(X) :- true."
            if (!b.isRule() && !b.isGround())
                b = new Rule(b,Literal.LTrue);
            
            // does not do BRF for rules (and so do not produce events +bel for rules)
            if (b.isRule()) {
                getBB().add(b);
            } else {
                b = (Literal)b.capply(null); // to solve arithmetic expressions
                addBel(b);
            }
        }
        initialBels.clear();
    }
    
    @Override
    protected void addInitialBelsFromProjectInBB() {
        String sBels = getTS().getSettings().getUserParameter(Settings.INIT_BELS);
        if (sBels != null) {
            try {
                for (Term t: ASSyntax.parseList("["+sBels+"]")) {
                    Literal b = ((Literal)t).forceFullLiteralImpl();
                    addBel(b);
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Initial beliefs from project '["+sBels+"]' is not a list of literals.");
            }
        }
    }
    
    /** includes all initial goals in the agent reasoner */
    @Override
    public void addInitialGoalsInTS() {
        for (Literal g: initialGoals) {
            g.makeVarsAnnon();
            if (! g.hasSource())
                g.addAnnot(BeliefBase.TSelf);
            getTS().getC().addAchvGoal(g,Intention.EmptyInt);            
        }
        initialGoals.clear();
    }

    @Override
    protected void addInitialGoalsFromProjectInBB() {
        String sGoals = getTS().getSettings().getUserParameter(Settings.INIT_GOALS);
        if (sGoals != null) {
            try {
                for (Term t: ASSyntax.parseList("["+sGoals+"]")) {
                    Literal g = ((Literal)t).forceFullLiteralImpl();
                    g.makeVarsAnnon();
                    if (! g.hasSource())
                        g.addAnnot(BeliefBase.TSelf);
                    getTS().getC().addAchvGoal(g,Intention.EmptyInt);            
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Initial goals from project '["+sGoals+"]' is not a list of literals.");
            }
        }
    }

    
    /** Imports beliefs, plans and initial goals from another agent. Initial beliefs and goals 
     *  are stored in "initialBels" and "initialGoals" lists but not included in the BB / TS.
     *  The methods addInitialBelsInBB and addInitialGoalsInTS should be called in the sequel to
     *  add those beliefs and goals into the agent. */
    @Override
    public void importComponents(Agent a) throws JasonException {
        if (a != null) {
            for (Literal b: a.initialBels) {
                this.addInitialBel(b);
                try {
                    fixAgInIAandFunctions(this,b);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            
            for (Literal g: a.initialGoals) 
                this.addInitialGoal(g);
                        
            for (Plan p: a.getPL()) 
                this.getPL().add(p, false);

            if (getPL().hasMetaEventPlans())
                getTS().addGoalListener(new GoalListenerForMetaEvents(getTS()));
        }
    }
    

    
    /** Removes all occurrences of <i>bel</i> in BB. 
    If <i>un</i> is null, an empty Unifier is used. 
	**/
    @Override
	public void abolish(Literal bel, Unifier un) throws RevisionFailedException {
	    List<Literal> toDel = new ArrayList<Literal>();
	    if (un == null) un = new Unifier();
	    synchronized (bb.getLock()) {
	        Iterator<Literal> il = getBB().getCandidateBeliefs(bel, un);
	        if (il != null) {
	            while (il.hasNext()) {
	                Literal inBB = il.next();
	                if (!inBB.isRule()) {
	                    // need to clone unifier since it is changed in previous iteration
	                    if (un.clone().unifiesNoUndo(bel, inBB)) {
	                        toDel.add(inBB);
	                    }
	                }
	            }
	        }
	        
	        for (Literal l: toDel) {
	            delBel(l);
	        }
	    }
	}

    

}
