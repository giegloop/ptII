package ptolemy.domains.ptides.kernel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import ptolemy.actor.Actor;
import ptolemy.actor.CompositeActor;
import ptolemy.actor.IOPort;
import ptolemy.actor.lib.Clock;
import ptolemy.actor.util.FunctionDependency;
import ptolemy.actor.util.FunctionDependencyOfCompositeActor;
import ptolemy.data.ArrayToken;
import ptolemy.data.BooleanToken;
import ptolemy.data.DoubleToken;
import ptolemy.data.expr.Parameter;
import ptolemy.domains.de.lib.TimedDelay;
import ptolemy.graph.DirectedAcyclicGraph;
import ptolemy.graph.Edge;
import ptolemy.graph.Node;
import ptolemy.kernel.Port;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Nameable;
import ptolemy.kernel.util.NamedObj;

/**
 * 
 * does not support topological changes
 * 
 * @author Patricia Derler
 *
 */
public class PtidesGraphUtilities {
	
	public PtidesGraphUtilities(NamedObj container) {
		this.topLevelContainer = (CompositeActor) container;
	}
	
	private void _adjustMinDelayAddingUpstreamDelays(DirectedAcyclicGraph graph) throws IllegalActionException {
		// adjust min delays by adding minDelays of upstream actors
		for (Iterator it = topLevelContainer.entityList().iterator(); it.hasNext();) {
			Actor actor = (Actor) it.next();
			for (Iterator inputs = actor.inputPortList().iterator(); inputs.hasNext(); ) {
				IOPort input = (IOPort) inputs.next();
				double oldMinDelay = PtidesGraphUtilities.getMinDelayTime(input);
				ArrayList list = new ArrayList();
				double newMinDelay = _getMinDelayUpstream(graph, input, list);
				if (oldMinDelay != newMinDelay) {
					PtidesGraphUtilities.setMinDelay(input, newMinDelay);
				}
			}
		}
	}

	private void _adjustMinDelaysOfEquivalenceClasses() throws IllegalActionException {
		// if ports are in the same equivalence class they get the maximum of their minimum delays
		for (Iterator it = topLevelContainer.entityList().iterator(); it.hasNext();) {
			Actor actor = (Actor) it.next();
			if (actor instanceof CompositeActor) {
				PtidesEmbeddedDirector dir = (PtidesEmbeddedDirector) actor.getDirector();
				Set equivalenceClasses = getEquivalenceClassesPortLists(actor);
				for (Iterator classes = equivalenceClasses.iterator(); classes.hasNext(); ) {
					Set equivalenceClass = (Set) classes.next();
					double minMinDelay = Double.MAX_VALUE;
					for (Iterator ports = equivalenceClass.iterator(); ports.hasNext(); ) {
						IOPort port = (IOPort) ports.next();
						double minDelay = PtidesGraphUtilities.getMinDelayTime(port);
						if (minMinDelay > minDelay)
							minMinDelay = minDelay;
					}
					for (Iterator ports = equivalenceClass.iterator(); ports.hasNext(); ) {
						IOPort port = (IOPort) ports.next();
						if (PtidesGraphUtilities.getMinDelayTime(port) != minMinDelay) {
							PtidesGraphUtilities.setMinDelay(port, minMinDelay);
						}
					}
				}
			}
		}
	}
	
	private void _transferDelaysFromOutputsToInputs(DirectedAcyclicGraph graph) throws IllegalActionException {
		
		// transferring min delays from outputs to inputs; outputs can have mindelays if 
		// upstream actors are not connected to an input of the same actor
		for (Iterator it = topLevelContainer.entityList().iterator(); it.hasNext();) {
			Actor actor = (Actor) it.next();
			for (Iterator outputs = actor.outputPortList().iterator(); outputs.hasNext(); ) {
				IOPort output = (IOPort) outputs.next();
				Collection sinks = graph.outputEdges(graph.node(output));
				for (Iterator sinksIt = sinks.iterator(); sinksIt.hasNext(); ) { // should only be one
					Edge edge = (Edge) sinksIt.next();
					IOPort sink = (IOPort) edge.sink().getWeight();
					double minDelay = PtidesGraphUtilities.getMinDelayTime(output);
					PtidesGraphUtilities.setMinDelay(sink, minDelay);
				}
			}
		}
		
	}
	
    public void calculateMinDelays() throws IllegalActionException {
    	for (Iterator it = topLevelContainer.entityList().iterator(); it.hasNext();) {
    		Actor actor = (Actor) it.next();
    		if (actor instanceof CompositeActor)
    			getMinDelays(actor);
    	}
    	FunctionDependencyOfCompositeActor functionDependency = (FunctionDependencyOfCompositeActor) (topLevelContainer).getFunctionDependency();
		DirectedAcyclicGraph graph = functionDependency.getDetailedDependencyGraph().toDirectedAcyclicGraph();

    	_transferDelaysFromOutputsToInputs(graph);
    	_adjustMinDelaysOfEquivalenceClasses();
		_adjustMinDelayAddingUpstreamDelays(graph);
		_adjustMinDelaysOfEquivalenceClasses();
	}

	private double _getMinDelayUpstream(DirectedAcyclicGraph graph, IOPort inputPort, List traversedActors) {
		double mindel = Double.MAX_VALUE;
		double delay = PtidesGraphUtilities.getMinDelayTime(inputPort);
		for (Iterator it = graph.inputEdges(graph.node(inputPort)).iterator(); it.hasNext(); ) {
			IOPort port = (IOPort)( (Edge)it.next()).source().getWeight();
			Actor actor = (Actor) port.getContainer();
			if (traversedActors.contains(actor))
				break;
			traversedActors.add(actor);
			PtidesEmbeddedDirector dir = (PtidesEmbeddedDirector) actor.getDirector();
			for(Iterator inputs = actor.inputPortList().iterator(); inputs.hasNext();) {
				delay = PtidesGraphUtilities.getMinDelayTime(inputPort);
				IOPort input = (IOPort) inputs.next();
				if (isInputConnectedToOutput(actor, input, port)) {
					delay += _getMinDelayUpstream(graph, input, traversedActors);
					if (mindel > delay)
						mindel = delay;
				}
			}	
		}
		if (mindel == Double.MAX_VALUE)
			mindel = delay;
		return mindel;		
	}
	
    private DirectedAcyclicGraph _computeGraph(Actor container) {
    	if (graphs.get(container) == null) {
	    	FunctionDependencyOfCompositeActor functionDependency = (FunctionDependencyOfCompositeActor) (container).getFunctionDependency();
	    	DirectedAcyclicGraph _graph = functionDependency.getDetailedDependencyGraph().toDirectedAcyclicGraph();
			
	    	// add edges between timeddelay inputs and outputs
			for (Iterator nodeIterator = _graph.nodes().iterator(); nodeIterator.hasNext();) {
	    		IOPort port = (IOPort) ((Node)nodeIterator.next()).getWeight();
	    		Actor a = (Actor) port.getContainer();
	    		if (port.isOutput() && a instanceof TimedDelay) 
	    			_graph.addEdge(a.inputPortList().get(0), a.outputPortList().get(0));
	    	}
			// remove edges between actors and sensors
			for (Iterator nodeIterator = _graph.nodes().iterator(); nodeIterator.hasNext();) {
				IOPort sinkPort = (IOPort) ((Node)nodeIterator.next()).getWeight();
	    		Actor a = (Actor) sinkPort.getContainer();
	    		if (PtidesGraphUtilities.isSensor(a) && sinkPort.isInput()) {
	    			Collection edgesToRemove = new java.util.ArrayList();
	    			for (Iterator it = _graph.inputEdges(_graph.node(sinkPort)).iterator(); it.hasNext(); ) {
	    				Edge edge = (Edge)it.next();
	    				edgesToRemove.add(edge);
	    			}
	    			for (Iterator it = edgesToRemove.iterator(); it.hasNext(); ) {
	    				_graph.removeEdge((Edge) it.next());
	    			}
	    		}
			}
			graphs.put(container, _graph);
    	}
    	return (DirectedAcyclicGraph) graphs.get(container);
    }
    
    public void getEquivalenceClasses(Actor container) {
    	DirectedAcyclicGraph graph = _computeGraph(container);
    	List outputPortList = container.outputPortList();
    	List inputPortList = container.inputPortList();
    	double[][] connections = new double[outputPortList.size()][inputPortList.size()];
    	for (Iterator it = outputPortList.iterator(); it.hasNext(); ) {
    		IOPort out = (IOPort) it.next();
    		Collection col = graph.backwardReachableNodes(graph.node(out));
    		for (Iterator it2 = inputPortList.iterator(); it2.hasNext(); ) {
    			IOPort in = (IOPort) it2.next();
    			if (col.contains(graph.node(in)))
    				connections[outputPortList.indexOf(out)][inputPortList.indexOf(in)] = 1;
    		}
    	}
    	System.out.println("----------" + container.getName());
    	System.out.print("\t");
    	for (int j = 0; j < inputPortList.size(); j++) {
    		System.out.print(((IOPort)inputPortList.get(j)).getName() + "\t");
    	}
    	System.out.println();
    	for (int i = 0; i < outputPortList.size(); i++) {
    		System.out.print(((IOPort)outputPortList.get(i)).getName() + "\t");
    		for (int j = 0; j < inputPortList.size(); j++) {
    			System.out.print(connections[i][j] + "\t");
    		}
    		System.out.println();
    	}
    	System.out.println("------------------------------------------");
    }
    
    public void getMinDelays(Actor container) throws IllegalActionException {
    	DirectedAcyclicGraph graph = _computeGraph(container);
    	for (Iterator it = container.outputPortList().iterator(); it.hasNext(); ) {
    		IOPort out = (IOPort) it.next();
    		System.out.println("---------" + out.getName());
    		double minDelay = _getMinDelay(container, false, graph.node(out), graph, Double.MAX_VALUE, new HashSet());
    		PtidesGraphUtilities.setMinDelay(out, minDelay);
    	}
    	for (Iterator it = container.inputPortList().iterator(); it.hasNext(); ) {
    		IOPort in = (IOPort) it.next();
    		System.out.println("---------" + in.getName());
    		if (!_isInputConnectedToAnyOutput(container, graph, in)) {
    			double minDelay = _getMinDelay(container, true, graph.node(in), graph, Double.MAX_VALUE, new HashSet());
    			PtidesGraphUtilities.setMinDelay(in, minDelay);
    		}
    	}
    }
    
    public boolean isInputConnectedToOutput(Actor container, IOPort in, IOPort out) {
    	DirectedAcyclicGraph graph = _computeGraph(container);
    	return graph.reachableNodes(graph.node(in)).contains(graph.node(out));
    }

	public Set getEquivalenceClassesPortLists(Actor container) throws IllegalActionException {

    	Set equivalenceClasses = new HashSet();
    	DirectedAcyclicGraph graph = _computeGraph(container);	
    	
    	Hashtable ioMapping = new Hashtable();
		for (Iterator colIt = graph.connectedComponents().iterator(); colIt.hasNext();) {
			Set ports = new HashSet();
			for (Iterator col2It = ((Collection) colIt.next()).iterator(); col2It.hasNext();) {
				Port p = (Port) ((Node)col2It.next()).getWeight();
				if (p.getContainer().equals(container) && ((IOPort)p).isInput())
					ports.add(p);
			}
			if (!ports.isEmpty())
				equivalenceClasses.add(ports);
		}
    	for (Iterator it = equivalenceClasses.iterator(); it.hasNext(); ) {
    		Set eqc = (Set) it.next();
    		System.out.print("c: ");
    		for (Iterator it2 = eqc.iterator(); it2.hasNext(); ) {
    			IOPort port = (IOPort) it2.next();
    			System.out.print(port.getName()+ " ");
    		}
    		System.out.println();
    	}
		return equivalenceClasses;
	}
	
    private void _getDisconnectedGraphs(Actor container) throws IllegalActionException {
    	Set disconnectedgraphs = new HashSet();
    	Set equivalenceClasses = new HashSet();
    	DirectedAcyclicGraph graph = _computeGraph(container);	
    	
    	Hashtable ioMapping = new Hashtable();
		for (Iterator colIt = graph.connectedComponents().iterator(); colIt.hasNext();) {
			Set actors = new HashSet();
			Set ports = new HashSet();
			for (Iterator col2It = ((Collection) colIt.next()).iterator(); col2It.hasNext();) {
				Port p = (Port) ((Node)col2It.next()).getWeight();
				if (!p.getContainer().equals(container))
					actors.add(p.getContainer());
				else // p is an input or output port of a platform
					if (((IOPort)p).isInput())
					ports.add(p);
			}
			if (!actors.isEmpty())
				disconnectedgraphs.add(actors);
			if (!ports.isEmpty())
				equivalenceClasses.add(ports);
		}
	}
    
    private double _getMinDelay(Actor container, boolean forward, Node node, DirectedAcyclicGraph graph, double minDelay, Set traversedEdges) throws IllegalActionException {
    	System.out.println("fw");
    	IOPort port = (IOPort) (node).getWeight();
    	Collection inputs;
    	if (forward)
    		inputs = graph.outputEdges(node);
    	else
    		inputs = graph.inputEdges(node);
		if (inputs.size() == 0)
			return Integer.MAX_VALUE;
		Iterator inputIt = inputs.iterator();
		while (inputIt.hasNext()) {
			double delay = 0.0;
			Edge edge = (Edge) inputIt.next();
			Node nextNode;
			if (forward)
				nextNode = edge.sink();
			else
				nextNode = edge.source();
			Actor inputActor = (Actor)((IOPort) nextNode.getWeight()).getContainer();
			if (!traversedEdges.contains(edge)) {
				traversedEdges.add(edge);
				if (inputActor instanceof TimedDelay) {
					TimedDelay delayActor = (TimedDelay) inputActor;
					delay = ((DoubleToken) (delayActor.delay.getToken())).doubleValue();
					if (forward)
						nextNode = graph.node(delayActor.output);
					else
						nextNode = graph.node(delayActor.input);
				} 
				if (PtidesGraphUtilities.isSensor(inputActor)) {
					return delay;
				} else if (inputActor instanceof Clock) { // assume periodic clock
					// TODO
					Clock clock = (Clock) inputActor;
					delay = ((DoubleToken) clock.period.getToken()).doubleValue() / ((ArrayToken) clock.offsets.getToken()).length();
				} else if (inputActor.equals(container))
					return delay;
				delay += _getMinDelay(container, forward, nextNode, graph, minDelay, traversedEdges); 
			}
			if (delay < minDelay)
				minDelay = delay;
		}
		return minDelay;
    }
    

    
    private boolean _isInputConnectedToAnyOutput(Actor container, DirectedAcyclicGraph graph, IOPort in) {
    	Collection reachableNodes = graph.reachableNodes(graph.node(in));
		for (Iterator it2 = container.outputPortList().iterator(); it2.hasNext(); ) {
			IOPort out = (IOPort) it2.next();
			if (reachableNodes.contains(graph.node(out))) {
				return true;
			}
		}
		return false;
	}
    
    public static boolean mustBeFiredAtRealTime(Actor actor) {
		return isSensor(actor) || isActuator(actor);
	}

	public static double getMinDelayTime(NamedObj actor) {
			try {
				
	//			double clockSyncError = ((EmbeddedDEDirector4Ptides)((Actor)actor.getContainer()).getDirector()).getExecutiveDirector().getClockSyncError();
	//	    	double networkDelay = ((EmbeddedDEDirector4Ptides)((Actor)actor.getContainer()).getDirector()).getExecutiveDirector().getNetworkDelay();
				
	            Parameter parameter = (Parameter) actor.getAttribute("minDelay");
	
	            if (parameter != null) {
	            	DoubleToken intToken = (DoubleToken) parameter.getToken();
	
	                return intToken.doubleValue();
	            } else {
	                return Double.MAX_VALUE;
	            }
	        } catch (ClassCastException ex) {
	            return Double.MAX_VALUE;
	        } catch (IllegalActionException ex) {
	            return Double.MAX_VALUE;
	        }
		}

	public static double getWCET(Actor actor) {
		try {
            Parameter parameter = (Parameter) ((NamedObj)actor).getAttribute("WCET");

            if (parameter != null) {
            	DoubleToken token = (DoubleToken) parameter.getToken();

                return token.doubleValue();
            } else {
            	return 0.0;
            }
        } catch (ClassCastException ex) {
            return 0.0;
        } catch (IllegalActionException ex) {
            return 0.0;
        }
	}
	
	public static boolean isActuator(Actor actor) {
		try {
			if (actor == null)
				actor = null;
            Parameter parameter = (Parameter) ((NamedObj)actor).getAttribute("isActuator");

            if (parameter != null) {
            	BooleanToken intToken = (BooleanToken) parameter.getToken();

                return intToken.booleanValue();
            } else {
                return false;
            }
        } catch (ClassCastException ex) {
            return false;
        } catch (IllegalActionException ex) {
            return false;
        }
	}
	
	public static boolean isSensor(Actor actor) {
		try {
			if (actor == null)
				actor = null;
            Parameter parameter = (Parameter) ((NamedObj)actor).getAttribute("isSensor");

            if (parameter != null) {
            	BooleanToken intToken = (BooleanToken) parameter.getToken();

                return intToken.booleanValue();
            } else {
                return false;
            }
        } catch (ClassCastException ex) {
            return false;
        } catch (IllegalActionException ex) {
            return false;
        }
	}
	
	public static void setMinDelay(IOPort out, double minDelay) throws IllegalActionException {
    	Parameter parameter = (Parameter) ((NamedObj)out).getAttribute("minDelay");
		if (parameter == null)
			try {
			parameter = new Parameter((NamedObj)out, "minDelay", new DoubleToken(minDelay));
			} catch (NameDuplicationException ex) {
				// can never happen
			}
		else 
			parameter.setToken(new DoubleToken(minDelay));
		System.out.println(out.getName() + ": " + minDelay);
	}
	
	private Actor container1;
	
	private CompositeActor topLevelContainer;
	
	private Hashtable graphs = new Hashtable();
	


	
}
