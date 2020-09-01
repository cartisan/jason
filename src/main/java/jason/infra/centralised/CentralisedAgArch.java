package jason.infra.centralised;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import jason.JasonException;
import jason.ReceiverNotFoundException;
import jason.architecture.AgArch;
import jason.asSemantics.ActionExec;
import jason.asSemantics.Agent;
import jason.asSemantics.Circumstance;
import jason.asSemantics.Message;
import jason.asSemantics.TransitionSystem;
import jason.asSyntax.Atom;
import jason.asSyntax.Literal;
import jason.mas2j.ClassParameters;
import jason.runtime.RuntimeServicesInfraTier;
import jason.runtime.Settings;
import jason.util.Config;

/**
 * This class provides an agent architecture when using Centralised
 * infrastructure to run the MAS inside Jason.
 *
 * Each agent has its own thread.
 *
 * <p>
 * Execution sequence:
 * <ul>
 * <li>initAg,
 * <li>setEnvInfraTier,
 * <li>setControlInfraTier,
 * <li>run (perceive, checkMail, act),
 * <li>stopAg.
 * </ul>
 */
public class CentralisedAgArch extends AgArch implements Runnable {

    protected CentralisedEnvironment    infraEnv     = null;
    private CentralisedExecutionControl infraControl = null;
    private BaseCentralisedMAS           masRunner    = BaseCentralisedMAS.getRunner();

    private String           agName  = "";
    private volatile boolean running = true;
    private Queue<Message>   mbox    = new ConcurrentLinkedQueue<>();
    protected Logger         logger  = Logger.getLogger(CentralisedAgArch.class.getName());

    private static List<MsgListener> msgListeners = null;
    public static void addMsgListener(MsgListener l) {
        if (msgListeners == null) {
            msgListeners = new ArrayList<>();
        }
        msgListeners.add(l);
    }
    public static void removeMsgListener(MsgListener l) {
        msgListeners.remove(l);
    }
    protected static List<MsgListener> getMsgListeners() {
        return msgListeners;
    }

    /**
     * Creates the user agent architecture, default architecture is
     * jason.architecture.AgArch. The arch will create the agent that creates
     * the TS.
     */
    public void createArchs(List<String> agArchClasses, String agClass, ClassParameters bbPars, String asSrc, Settings stts, BaseCentralisedMAS masRunner) throws JasonException {
        try {
            this.masRunner = masRunner;
            Agent.create(this, agClass, bbPars, asSrc, stts);
            this.insertAgArch(this);

            this.createCustomArchs(agArchClasses);

            // mind inspector arch
            if (stts.getUserParameter(Settings.MIND_INSPECTOR) != null) {
                this.insertAgArch( (AgArch)Class.forName( Config.get().getMindInspectorArchClassName()).newInstance() );
                this.getFirstAgArch().init();
            }

            this.setLogger();
        } catch (Exception e) {
            this.running = false;
            throw new JasonException("as2j: error creating the agent class! - "+e.getMessage(), e);
        }
    }

    /** init the agent architecture based on another agent */
    public void createArchs(List<String> agArchClasses, Agent ag, BaseCentralisedMAS masRunner) throws JasonException {
        try {
            this.masRunner = masRunner;
            this.setTS(ag.clone(this).getTS());
            this.insertAgArch(this);

            this.createCustomArchs(agArchClasses);

            this.setLogger();
        } catch (Exception e) {
            this.running = false;
            throw new JasonException("as2j: error creating the agent class! - ", e);
        }
    }


    public void stopAg() {
        this.running = false;
        this.wake(); // so that it leaves the run loop
        if (this.myThread != null) {
            this.myThread.interrupt();
        }
        this.getTS().getAg().stopAg();
        this.getUserAgArch().stop(); // stops all archs
    }


    public void setLogger() {
        this.logger = Logger.getLogger(CentralisedAgArch.class.getName() + "." + this.getAgName());
        if (this.getTS().getSettings().verbose() >= 0) {
            this.logger.setLevel(this.getTS().getSettings().logLevel());
        }
    }

    public Logger getLogger() {
        return this.logger;
    }

    public void setAgName(String name) throws JasonException {
        if (name.equals("self")) {
            throw new JasonException("an agent cannot be named 'self'!");
        }
        if (name.equals("percept")) {
            throw new JasonException("an agent cannot be named 'percept'!");
        }
        this.agName = name;
    }

    public String getAgName() {
        return this.agName;
    }

    public AgArch getUserAgArch() {
        return this.getFirstAgArch();
    }

    public void setEnvInfraTier(CentralisedEnvironment env) {
        this.infraEnv = env;
    }

    public CentralisedEnvironment getEnvInfraTier() {
        return this.infraEnv;
    }

    public void setControlInfraTier(CentralisedExecutionControl pControl) {
        this.infraControl = pControl;
    }

    public CentralisedExecutionControl getControlInfraTier() {
        return this.infraControl;
    }

    private Thread myThread = null;
    public void setThread(Thread t) {
        this.myThread = t;
        this.myThread.setName(this.agName);
    }

    public void startThread() {
        this.myThread.start();
    }

    public boolean isRunning() {
        return this.running;
    }

    protected void sense() {
        TransitionSystem ts = this.getTS();

        int i = 0;
        do {
            ts.sense(); // must run at least once, so that perceive() is called
        } while (this.running && ++i < this.cyclesSense && !ts.canSleepSense());
    }

    //int sumDel = 0; int nbDel = 0;
    protected void deliberate() {
        TransitionSystem ts = this.getTS();
        int i = 0;
        while (this.running && i++ < this.cyclesDeliberate && !ts.canSleepDeliberate()) {
            ts.deliberate();
        }
        //sumDel += i; nbDel++;
        //System.out.println("running del "+(sumDel/nbDel)+"/"+cyclesDeliberate);
    }

    //int sumAct = 0; int nbAct = 0;
    protected void act() {
        TransitionSystem ts = this.getTS();

        int i = 0;
        int ca = this.cyclesAct;
        if (this.cyclesAct == 9999) {
            ca = ts.getC().getIntentions().size();
        }

        while (this.running && i++ < ca && !ts.canSleepAct()) {
            ts.act();
        }
        //sumAct += i; nbAct++;
        //System.out.println("running act "+(sumAct/nbAct)+"/"+ca);
    }

    protected void reasoningCycle() {
        this.sense();
        this.deliberate();
        this.act();
    }

    public void run() {
        TransitionSystem ts = this.getTS();
        while (this.running) {
            if (ts.getSettings().isSync()) {
                this.waitSyncSignal();
                this.reasoningCycle();
                if (ts.canSleep()) {
                    this.sleep();
                }
                boolean isBreakPoint = false;
                try {
                    isBreakPoint = ts.getC().getSelectedOption().getPlan().hasBreakpoint();
                    if (this.logger.isLoggable(Level.FINE)) {
                        this.logger.fine("Informing controller that I finished a reasoning cycle "+this.getCycleNumber()+". Breakpoint is " + isBreakPoint);
                    }
                } catch (NullPointerException e) {
                    // no problem, there is no sel opt, no plan ....
                }
                this.informCycleFinished(isBreakPoint, this.getCycleNumber());
            } else {
                this.incCycleNumber();
                this.reasoningCycle();
                if (ts.canSleep()) {
                    this.sleep();
                }
            }
        }
        this.logger.fine("I finished!");
    }

    private Object sleepSync = new Object();
    private int    sleepTime = 50;

    public static final int MAX_SLEEP = 1000;

    public void sleep() {
        try {
            this.logger.fine("Entering in sleep mode....");
            synchronized (this.sleepSync) {
                this.sleepSync.wait(this.sleepTime); // wait for messages
                if (this.sleepTime < MAX_SLEEP) {
                    this.sleepTime += 100;
                }
            }
        } catch (InterruptedException e) {
        } catch (Exception e) {
            this.logger.log(Level.WARNING,"Error in sleep.", e);
        }
    }

    @Override
    public void wake() {
        synchronized (this.sleepSync) {
            this.sleepTime = 50;
            this.sleepSync.notifyAll(); // notify sleep method
        }
    }

    @Override
    public void wakeUpSense() {
        this.wake();
    }

    @Override
    public void wakeUpDeliberate() {
        this.wake();
    }

    @Override
    public void wakeUpAct() {
        this.wake();
    }

    // Default perception assumes Complete and Accurate sensing.
    @Override
    public Collection<Literal> perceive() {
        super.perceive();
        if (this.infraEnv == null) {
            return null;
        }
        Collection<Literal> percepts = this.infraEnv.getUserEnvironment().getPercepts(this.getAgName());
        if (this.logger.isLoggable(Level.FINE) && percepts != null) {
            this.logger.fine("percepts: " + percepts);
        }
        return percepts;
    }

    // this is used by the .send internal action in stdlib
    public void sendMsg(Message m) throws ReceiverNotFoundException {
        // actually send the message
        if (m.getSender() == null) {
            m.setSender(this.getAgName());
        }

        CentralisedAgArch rec = this.masRunner.getAg(m.getReceiver());

        if (rec == null) {
            if (this.isRunning()) {
                throw new ReceiverNotFoundException("Receiver '" + m.getReceiver() + "' does not exist! Could not send " + m);
            } else {
                return;
            }
        }
        rec.receiveMsg(m.clone()); // send a cloned message

        // notify listeners
        if (msgListeners != null) {
            for (MsgListener l: msgListeners) {
                l.msgSent(m);
            }
        }
    }

    public void receiveMsg(Message m) {
        this.mbox.offer(m);
        this.wakeUpSense();
    }

    public void broadcast(jason.asSemantics.Message m) throws Exception {
        for (String agName: this.masRunner.getAgs().keySet()) {
            if (!agName.equals(this.getAgName())) {
                m.setReceiver(agName);
                this.sendMsg(m);
            }
        }
    }

    // Default procedure for checking messages, move message from local mbox to C.mbox
    public void checkMail() {
        Circumstance C = this.getTS().getC();
        Message im = this.mbox.poll();
        while (im != null) {
            C.addMsg(im);
            if (this.logger.isLoggable(Level.FINE)) {
                this.logger.fine("received message: " + im);
            }
            im = this.mbox.poll();
        }
    }

    public Collection<Message> getMBox() {
        return this.mbox;
    }

    /** called by the TS to ask the execution of an action in the environment */
    @Override
    public void act(ActionExec action) {
        //if (logger.isLoggable(Level.FINE)) logger.fine("doing: " + action.getActionTerm());

        if (this.isRunning()) {
            if (this.infraEnv != null) {
                this.infraEnv.act(this.getAgName(), action);
            } else {
                action.setResult(false);
                action.setFailureReason(new Atom("noenv"), "no environment configured!");
                this.actionExecuted(action);
            }
        }
    }

    public boolean canSleep() {
        return this.mbox.isEmpty() && this.isRunning();
    }

    private Object  syncMonitor                = new Object();
    private volatile boolean inWaitSyncMonitor = false;

    /**
     * waits for a signal to continue the execution (used in synchronised
     * execution mode)
     */
    private void waitSyncSignal() {
        try {
            synchronized (this.syncMonitor) {
                this.inWaitSyncMonitor = true;
                this.syncMonitor.wait();
                this.inWaitSyncMonitor = false;
            }
        } catch (InterruptedException e) {
        } catch (Exception e) {
            this.logger.log(Level.WARNING,"Error waiting sync (1)", e);
        }
    }

    /**
     * inform this agent that it can continue, if it is in sync mode and
     * waiting a signal
     */
    public void receiveSyncSignal() {
        try {
            synchronized (this.syncMonitor) {
                while (!this.inWaitSyncMonitor && this.isRunning()) {
                    // waits the agent to enter in waitSyncSignal
                    this.syncMonitor.wait(50);
                }
                this.syncMonitor.notifyAll();
            }
        } catch (InterruptedException e) {
        } catch (Exception e) {
            this.logger.log(Level.WARNING,"Error waiting sync (2)", e);
        }
    }

    /**
     *  Informs the infrastructure tier controller that the agent
     *  has finished its reasoning cycle (used in sync mode).
     *
     *  <p><i>breakpoint</i> is true in case the agent selected one plan
     *  with the "breakpoint" annotation.
     */
    public void informCycleFinished(boolean breakpoint, int cycle) {
        this.infraControl.receiveFinishedCycle(this.getAgName(), breakpoint, cycle);
    }

    public RuntimeServicesInfraTier getRuntimeServices() {
        return new CentralisedRuntimeServices(this.masRunner);
    }

    private RConf conf;

    private int cycles = 1;

    private int cyclesSense      = 1;
    private int cyclesDeliberate = 1;
    private int cyclesAct        = 5;

    public void setConf(RConf conf) {
        this.conf = conf;
    }

    public RConf getConf() {
        return this.conf;
    }

    public int getCycles() {
        return this.cycles;
    }

    public void setCycles(int cycles) {
        this.cycles = cycles;
    }

    public int getCyclesSense() {
        return this.cyclesSense;
    }

    public void setCyclesSense(int cyclesSense) {
        this.cyclesSense = cyclesSense;
    }

    public int getCyclesDeliberate() {
        return this.cyclesDeliberate;
    }

    public void setCyclesDeliberate(int cyclesDeliberate) {
        this.cyclesDeliberate = cyclesDeliberate;
    }

    public int getCyclesAct() {
        return this.cyclesAct;
    }

    public void setCyclesAct(int cyclesAct) {
        this.cyclesAct = cyclesAct;
    }

}
