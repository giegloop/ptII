/* A Manager governs the execution of an entire simulation.

 Copyright (c) 1997-1999 The Regents of the University of California.
 All rights reserved.
 Permission is hereby granted, without written agreement and without
 license or royalty fees, to use, copy, modify, and distribute this
 software and its documentation for any purpose, provided that the above
 copyright notice and the following two paragraphs appear in all copies
 of this software.

 IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY
 FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
 ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
 THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF
 SUCH DAMAGE.

 THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
 PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
 CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
 ENHANCEMENTS, OR MODIFICATIONS.

                                        PT_COPYRIGHT_VERSION_2
                                        COPYRIGHTENDKEY

@ProposedRating Yellow (neuendor@eecs.berkeley.edu)
@AcceptedRating Red
*/

package ptolemy.actor;

import ptolemy.graph.*;
import ptolemy.kernel.*;
import ptolemy.kernel.util.*;
import ptolemy.data.*;

import collections.LinkedList;
import collections.HashedSet;
import java.util.Enumeration;
import java.lang.reflect.*;

import java.util.Date;			// For timing measurements

//////////////////////////////////////////////////////////////////////////
//// Manager
/**
A Manager is a domain-independent object that manages the execution of
a model.   It provides several methods to control execution: run()
startRun(), pause(), resume(), terminate(), and finish().
Most often, methods in this object will be called by a
graphical user interface.  However, it is possible to manually call
these methods from a java object, a java applet, or an interactive
prompt, such as Tcl Blend.
<p>
Because user interaction will likely be occurring asynchronously to the
execution of the model, it is important that all the processing for the
model occur in a separate thread.   The Manager has the ability to start 
executing a model in the current model, or it can create
and manage a separate Java thread in which execution begins.  Notice that 
this does not preclude domains from spawning additional threads of their own.  
<p>
The Manager class implements the Runnable interface.   The run() method of 
interface begins the the execution of a model. The run() method call is a
blocking method call, in the sense that the flow of execution will be returned
to the caller only after the run() method finishes. On the other hand, the
startRun() method call is a non-blocking method call. It creates a separate
thread running the execution and the flow of execution will be returned
immediately to the caller.  Notice that because the Manager is a runnable
object, execution can also be started in a separate thread using the 
Thread(Runnable) constructor.
<p>
Manager also tries to optimize the simulation by making the workspace
read-only during the iteration period when none of the 
directors in the model require write access. It calls needWriteAccess() on 
the director of the toplevel composite, and if this method returns false, 
then the manager will set the workspace to be read-only during each iteration
of the system.  Notice that although the workspace is read-only, mutations 
may still occur, if they are queued with the manager.

@author Steve Neuendorffer, Lukito Muliadi
// Contributors: Mudit Goel, Edward A. Lee, John S. Davis II
@version $Id$
*/

public final class Manager extends NamedObj implements Runnable {

    /** Construct a manager in the default workspace with an empty string
     *  as its name. The manager is added to the list of objects in
     *  the workspace. Increment the version number of the workspace.
     */
    public Manager() {
        super();
        _ExecutionListeners = new HashedSet();
    }

    /** Construct a manager in the default workspace with the given name.
     *  If the name argument is null, then the name is set to the empty
     *  string. The manager is added to the list of objects in the workspace.
     *  Increment the version number of the workspace.
     *  @param name Name of this Manager.
     */
    public Manager(String name) {
        super(name);
        _ExecutionListeners = new HashedSet();
    }

    /** Construct a manager in the given workspace with the given name.
     *  If the workspace argument is null, use the default workspace.
     *  The manager is added to the list of objects in the workspace.
     *  If the name argument is null, then the name is set to the
     *  empty string. Increment the version number of the workspace.
     *
     *  @param workspace Object for synchronization and version tracking.
     *  @param name Name of this Manager.
     */
    public Manager(Workspace workspace, String name) {
        super(workspace, name);
        _ExecutionListeners = new HashedSet();
    }


    ///////////////////////////////////////////////////////////////////
    ////                         public methods                    ////

    /** Add the execution listener to the set of execution listeners.
     *  The execution listener will be notified when the appropriate
     *  execution events occur.
     *  @param el An execution listener.
     */
    public void addExecutionListener(ExecutionListener el) {
        if(el == null) return;
        _ExecutionListeners.include((Object) el);
    }

    /** Set a flag to request that the thread in which execution is running
     *  stop execution and exit gracefully. Call finish() on the top level
     *  CompositeActor. This thread is synchronized so that it runs atomically 
     *  with respect to other methods in Manager that control the simulation 
     *  thread. This method is non-blocking.
     */
    public void finish() {
        synchronized(this) {
            _keepIterating = false;
            _isPaused = false;
            getToplevel().finish();
            if(_simulationThread != null) {
                synchronized(_simulationThread) {
                    _simulationThread.notify();
                }
            }
        }
    }

    /** Encapsulate the exception within an execution event and issue an
     *  executionError event to all the Execution listeners.   In 
     *  addition, print the exception's stack trace on the console.
     *
     *  @param e The exception.
     */
    public void fireExecutionError(Exception e) {
        // if any exceptions get up to this level, then we have to tell
        // the gui by encapsulating in an event.
        ExecutionEvent event = new ExecutionEvent(this, _iteration, e);
        // dump the stack trace to the console;
        e.printStackTrace();
        // and issue the event to all the listeners.
        _fireExecutionEvent(_ExecutionEventType.EXECUTIONERROR, event);
    }


    /** Return the toplevel composite actor for which this manager
     *  controls execution.   This composite actor does not have a parent, and
     *  contains the entire hierarchy for an execution.
     *  @return The composite actor that this manager is responsible for.
     */
    public CompositeActor getToplevel() {
        return _toplevel;
    }

    /** Indicate that resolved types in the system may no longer be valid.
     *  This will force type resolution to be redone on the next iteration.
     */
    public void invalidateResolvedTypes() {
        _typeResolved = false;
    }

    /** If an execution is currently running, then set a flag requesting that
     *  execution pause at the next available opportunity between toplevel
     *  iterations.   When the pause flag is detected, the
     *  simulation thread will suspend itself and issue the
     *  executionPaused execution event to all execution listeners.
     *  This thread is synchronized so that it runs atomically with respect to
     *  the other methods in manager that control the simulation thread.
     *  This call is non-blocking.
     */
    public synchronized void pause() {
        if(_keepIterating) _isPaused = true;
    }

    /** Remove the execution listener from the set of execution listeners.
     *  The execution listener will be no longer be notified when
     *  execution events occur.
     *  @param el An execution listener
     */
    public void removeExecutionListener(ExecutionListener el) {
        if(el == null) return;
        _ExecutionListeners.exclude((Object) el);
    }

    /** Check types on all the connections and resolve undeclared types.
     *  If the container is not an instance of TypedCompositeActor,
     *  do nothing.
     *  This method requires write access on the workspace.
     *  @exception TypeConflictException If there is a type conflict anywhere
     *  in the model.
     */
    public void resolveTypes()
	    throws TypeConflictException {
	try {
	    workspace().getWriteAccess();
            CompositeActor toplevel = (CompositeActor)getToplevel();
            if ( !(toplevel instanceof TypedCompositeActor)) {
                return;
            }

	    LinkedList conflicts = new LinkedList();
	    conflicts.appendElements(
                    ((TypedCompositeActor)toplevel).checkTypes());

            Enumeration constraints =
                ((TypedCompositeActor)toplevel).typeConstraints();

	    if (constraints.hasMoreElements()) {
                InequalitySolver solver = new InequalitySolver(
                        TypeLattice.lattice());
	        while (constraints.hasMoreElements()) {
                    Inequality ineq = (Inequality)constraints.nextElement();
                    solver.addInequality(ineq);
	        }

                // find the least solution (most specific types)
                boolean resolved = solver.solveLeast();
                if ( !resolved) {
		    Enumeration unsatisfied = solver.unsatisfiedInequalities();
		    while (unsatisfied.hasMoreElements()) {
		        Inequality ineq =
                            (Inequality)unsatisfied.nextElement();
		        InequalityTerm term =
					(InequalityTerm)ineq.getLesserTerm();
		        Object typeObj = term.getAssociatedObject();
		        if (typeObj != null) {
			    // typeObj is a Typeable
			    conflicts.insertLast(typeObj);
		        }
		        term = (InequalityTerm)ineq.getGreaterTerm();
		        typeObj = term.getAssociatedObject();
		        if (typeObj != null) {
			    // typeObj is a Typeable
			    conflicts.insertLast(typeObj);
		        }
		    }
                }

	        // check whether resolved types are acceptable.
                // They might be, for example, NaT.
	        Enumeration var = solver.variables();
	        while (var.hasMoreElements()) {
		    InequalityTerm term = (InequalityTerm)var.nextElement();
		    if ( !term.isTypeAcceptable()) {
		        conflicts.insertLast(term.getAssociatedObject());
		    }
	        }
	    }

	    if (conflicts.size() > 0) {
		throw new TypeConflictException(conflicts.elements(),
                        "Type conflicts occurred in " + toplevel.getFullName()
			+ " on the following Typeables:");
	    }
	} catch (IllegalActionException iae) {
	    // this should not happen.
	    throw new InternalErrorException(iae.getMessage());
	} finally {
	    workspace().doneWriting();
	}
    }

    /** If the model is running and paused, resume the
     *  currently paused simulation by
     *  turning off the paused flag and waking the simulation thread up.
     *  This thread is synchronized so that it runs atomically with respect to
     *  the other methods in manager that control the simulation thread.
     */
    public synchronized void resume() {
        if(_simulationThread == null) return;
        if(_keepIterating && _isPaused) {
            _isPaused = false;
            synchronized (_simulationThread) {
                _simulationThread.notify();
            }
        }
    }

    /**
     * Run the sequence of execution.  This method executes the toplevel
     * composite actor until it returns false in postfire.
     * The execution begins by calling initialize() on the toplevel
     * composite actor.   Each iteration of execution consists of calling
     * prefire(), fire() and postfire() on the top-level composite actor.
     * If postfire returns false, then execution is finished by calling
     * wrapup() on the toplevel composite actor to clean up the execution.
     * <p>
     * The execution is performed by the current thread. In other words, 
     * execution is performed in the foreground and
     * the method returns only after the execution finishes.  However, it is
     * possible for other threads to come in and pause(), resume(),
     * terminate() or finish() the execution of the simulation.
     * To start execution in the background, use the startRun() method.
     * <p> 
     * If execution of the model is already in progress, then this method 
     * returns immediately.
     * @see Manager#startRun()
     */
    public void run() {
        // ensure that we only have one execution running.
        synchronized(this) {
            if (_simulationThread == null) {
                // set the execution control variables.
                 _simulationThread = Thread.currentThread();
                 _keepIterating = true;
                 _isPaused = false;
                 _iteration = 0;
                 _typeResolved = false;
            } else {
                System.out.println("The model is already executing. " + 
                        "Either wait for it to end, or call " +
                        "finish() or terminate().");
                return;
            }
        }

        // Used for profiling;
        long startTime = (new Date()).getTime();

        CompositeActor toplevel = ((CompositeActor)getToplevel());

        // Notify all the listeners that execution has started.
        ExecutionEvent event = new ExecutionEvent(this);
        _fireExecutionEvent(_ExecutionEventType.EXECUTIONSTARTED, event);

        try {
            try {
                // Initialize the topology
                toplevel.initialize();

                // Figure out the appropriate write access.
                _needWriteAccessDuringIteration =
                    _needWriteAccess();

                // Call _iterate() until:
                // _keepIterating is set to false (presumably by stop())
                // postfire() returns false.
                while (_keepIterating && _iterate()) {
                    try {
                        // if a pause has been requested
                        if(_isPaused) {
                            // Notify listeners that we are paused.
                            event =
                                new ExecutionEvent(this, _iteration);
                            _fireExecutionEvent(
                                    _ExecutionEventType.EXECUTIONPAUSED, 
                                    event);

                            synchronized (_simulationThread) {
                                // suspend this thread until
                                // somebody wakes us up.
                                while(_keepIterating && 
                                        _isPaused)
                                    _simulationThread.wait();
                            }

                            // Somebody woke us up, so notify all the
                            // listeners that we are resuming.
                            event =
                                new ExecutionEvent(this, _iteration);
                            _fireExecutionEvent(
                                    _ExecutionEventType.EXECUTIONRESUMED,
                                    event);
                        }
                    }
                    catch (InterruptedException e) {
                        // We don't care if we were interrupted..
                        // Just ignore.
                    }

                } // while (_keepIterating && _iterate())
            }
            // catch errors that happen during an iteration before the finally
            // clause.   Ensure that exception is reported before wrapup()
            // tries to get called.
            catch (Exception e) {
                fireExecutionError(e);
            }
            finally {
                // if we are done, then always be sure to reset the flags.
                _keepIterating = false;
                _isPaused = false;

                // try to wrapup the topology.
                toplevel.wrapup();

                // notify all listeners that we have been stopped.
                event =
                    new ExecutionEvent(this, _iteration);
                _fireExecutionEvent(_ExecutionEventType.EXECUTIONFINISHED,
                        event);
            }
        } catch (Exception e) {
            fireExecutionError(e);
        }

        long endTime = (new Date()).getTime();
        System.out.println("ptolemy.actor.Manager run(): elapsed time: "
                + (endTime - startTime) + " ms");

        // The simulation is finished, the thread has finished its job.
        _simulationThread = null;

    }

    /** Start an execution that will run for an unspecified number of
     *  toplevel iterations.   This will normally be stopped by
     *  calling finish(), terminate(), or returning false in a postfire method.
     *  This method is non-blocking.  In other words, the execution of the
     *  model occurs in the background.  The effect of this method is call the
     *  run method in a separate thread.
     * <p> 
     * If execution of the model is already in progress, then this method 
     * returns immediately.
     *  @see Manager#run()
     */
    public synchronized void startRun() {

     /*
      *    I've commented this stuff out because it's bogus... I want
      *    to see if anything breaks.
      *  if(_keepIterating) return;
      *
      *  // If the previous run hasn't totally finished yet, then be sure
      *  // it is good and dead before continuing.
      *
      *  if(_simulationThread != null) {
      *      _simulationThread.stop();
      *      try {
      *          _simulationThread.join();
      *      }
      *      catch (InterruptedException e) {
      *          // Well, if we bothered to kill it, then this should
      *          // always get thrown, so just ignore it.
      *      }
      *      _simulationThread = null;
      *  }
      */
        // This will execute Manager.run() in a separate thread.
        Thread futureRunningThread = new PtolemyThread(this);
        futureRunningThread.start();
    }

    /** Terminate any currently executing model with extreme prejudice.
     *  This method is not intended to be used as a normal route of 
     *  stopping execution. To normally stop exceution, call the finish() 
     *  method instead. This method should be called only 
     *  when execution fails to terminate by normal means due to certain
     *  kinds of programming errors (infinite loops, threading errors, etc.).
     *  <p>
     *  After this method completes, all resources in use should be
     *  released and any sub-threads should be killed.
     *  However, a consistent state is not guaranteed.   The
     *  topology should probably be recreated before attempting any
     *  further operations.
     *  <p>
     *  In this class, we kill the main execution thread and call 
     *  terminate on the toplevel compositeActor. Execution listeners are 
     *  also notified of an executionTerminated event.
     *  <p>
     *  This method is not synchronized because we want it to
     *  happen as soon as possible, no matter what.
     */
    public void terminate() {
        try {
            // kill the main thread and wait for it to die.
            if(_simulationThread != null) {
                _simulationThread.stop();
                try {
                    _simulationThread.join();
                }
                catch (InterruptedException e) {
                    // This will usually get thrown, since we are
                    // forcibly terminating
                    // the thread.   We just ignore it.
                }
            }
            // Terminate the entire hierarchy as best we can.
            CompositeActor toplevel = ((CompositeActor)getToplevel());
            toplevel.terminate();
            
            // notify all execution listeners that execution was terminated.
            ExecutionEvent event = new ExecutionEvent(this);
            _fireExecutionEvent(_ExecutionEventType.EXECUTIONTERMINATED, 
                    event);
        }
        finally {
                _simulationThread = null;
                _keepIterating = false;
                _isPaused = false;
        }
    }

    ///////////////////////////////////////////////////////////////////
    ////                         protected methods                 ////

    /** Make this manager the manager of the specified composite
     *  actor. If the composite actor is not null, then the manager is 
     *  removed from the directory of the workspace.  If the composite actor
     *  is null, then the mnager is <b>not</b> returned to the directory of the
     *  workspace, which may result in it being garbage collected.
     *  This method should not be called directly.  Instead, call
     *  setManager in the CompositeActor class (or a derived class).
     */
    protected void _makeManagerOf(CompositeActor ca) {
        if (ca != null) {
            workspace().remove(this);
        }
        _toplevel = ca;
    }

    ///////////////////////////////////////////////////////////////////
    ////                         private methods                   ////

    /** Propagate the execution event to all the execution listeners.
     */
    private void _fireExecutionEvent(_ExecutionEventType type,
            ExecutionEvent event) {
        Enumeration listeners = _ExecutionListeners.elements();
        while(listeners.hasMoreElements()) {
            ExecutionListener l =
                (ExecutionListener) listeners.nextElement();
            if(type == _ExecutionEventType.EXECUTIONSTARTED)
                l.executionStarted(event);
            else if(type == _ExecutionEventType.EXECUTIONPAUSED)
                l.executionPaused(event);
            else if(type == _ExecutionEventType.EXECUTIONRESUMED)
                l.executionResumed(event);
            else if(type == _ExecutionEventType.EXECUTIONERROR)
                l.executionError(event);
            else if(type == _ExecutionEventType.EXECUTIONFINISHED)
                l.executionFinished(event);
            else if(type == _ExecutionEventType.EXECUTIONTERMINATED)
                l.executionTerminated(event);
            else if(type == _ExecutionEventType.ITERATIONSTARTED)
                l.executionIterationStarted(event);
        }
    }

    /** Invoke one iteration.  An iteration consists of
     *  invocations of prefire(), fire(), and postfire(), in that
     *  order.  Prefire() will be called at the beginning of an iteration.
     *  If prefire() returns false, then fire() and postfire() are not
     *  invoked.   Otherwise, fire() will be called once, followed by
     *  invocation of postfire(). If postfire()
     *  returns false, then the execution will be terminated.
     *  This method is read-synchronized on the workspace.
     *
     *  @return true, If postfire() returns true.
     *  @exception IllegalActionException If any of the called methods
     *   throws it.
     */
    private boolean _iterate() throws IllegalActionException {

        _iteration++;

        CompositeActor toplevel = (CompositeActor)getToplevel();
        if (toplevel == null) {
            throw new InvalidStateException("Manager "+ getName() +
                    " attempted execution with no topology to execute!");
        }

        ExecutionEvent event = new ExecutionEvent(this, _iteration);
        _fireExecutionEvent(_ExecutionEventType.ITERATIONSTARTED, event);

        // Toplevel mutations will occur here.

        try {
            workspace().getReadAccess();

	    try {
		if (!_typeResolved) {
                    resolveTypes();
                    _typeResolved = true;
                }
            }
            catch (TypeConflictException e) {
                fireExecutionError(e);
            }

            // Set the appropriate write access, because we're about to
            // go into an iteration.
            try {
                if (!_needWriteAccessDuringIteration) {
                    workspace().setReadOnly(true);
                }

                if (toplevel.prefire()) {
                    toplevel.fire();
                    return toplevel.postfire();
                }
                return false;
            } finally {
                if (!_needWriteAccessDuringIteration) {
                    workspace().setReadOnly(false);
                }
            }
        } finally {
            workspace().doneReading();
        }
    }

    /** Check whether write access of the workspace is required during an
     *  iteration. An iteration consists of invocations of the prefire(),
     *  fire(), and postfire() methods of the top level composite actor in
     *  that order.
     *  <p>
     *  This method recursively calls the needWriteAccess() method of 
     *  the top level director. Intuitively, the workspace will only be made
     *  read-only if all the directors permit it.
     */
    private boolean _needWriteAccess() {
        // Get the top level composite actor.
        CompositeActor toplevel = (CompositeActor)getToplevel();
        if (toplevel == null) {
            throw new InvalidStateException("Manager "+ getName() +
                    " attempted execution with no topology to execute!");
        }
        // Call the needWriteAccess() method of the local director of the
        // top level composite actor.
        return toplevel.getDirector().needWriteAccess();
    }

    ///////////////////////////////////////////////////////////////////
    ////                         Inner Class                       ////

    private static final class _ExecutionEventType {

        private _ExecutionEventType(String name) {this._name = name;}

        public static final _ExecutionEventType EXECUTIONSTARTED =
        new _ExecutionEventType("Execution Started");
        public static final _ExecutionEventType EXECUTIONPAUSED =
        new _ExecutionEventType("Execution Paused");
        public static final _ExecutionEventType EXECUTIONRESUMED =
        new _ExecutionEventType("Execution Resumed");
        public static final _ExecutionEventType EXECUTIONERROR =
        new _ExecutionEventType("Execution Error");
        public static final _ExecutionEventType EXECUTIONFINISHED =
        new _ExecutionEventType("Execution Finished");
        public static final _ExecutionEventType EXECUTIONTERMINATED =
        new _ExecutionEventType("Execution Terminated");
        public static final _ExecutionEventType ITERATIONSTARTED =
        new _ExecutionEventType("Iteration Started");

        public String toString() {return _name;}

        private String _name;
    }

    ///////////////////////////////////////////////////////////////////
    ////                         private variables                 ////

    // The toplevel composite actor that contains this manager, and
    // that this manager is responsible for executing.
    private CompositeActor _toplevel = null;

    // Indicate whether the execution should keep iterating.
    // This flag is set to false when finish is called.  If this flag is
    // false at the end of an iteration, then execution will wrapup.
    private boolean _keepIterating;

    // A flag to request a pause in execution.  If this flag is true
    // at the start of a toplevel iteration, then pause the execution thread
    // until the flag becomes false again.
    private boolean _isPaused;

    // Count the number of toplevel iterations completed.
    private int _iteration;

    // A flag to indicate if the workspace should be set read-only during
    // an iteration (i.e. during prefire(), fire() and postfire()).
    private boolean _needWriteAccessDuringIteration;

    // _simulationThread is the thread that's executing the run() method.
    // It should be non-null whenever the model is executing (i.e.
    // the run() method hasn't finished yet) and should be set to null after
    // execution ends.
    private Thread _simulationThread;

    // Listeners for execution events.
    private HashedSet _ExecutionListeners;

    // An indicator of whether type resolution is valid.  If the flag is 
    // false at the start of a toplevel iteration, then type resolution will
    // be executed again.
    private boolean _typeResolved = false;
}
