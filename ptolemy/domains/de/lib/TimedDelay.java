/* An actor that delays the input by the specified amount.

Copyright (c) 1998-2004 The Regents of the University of California.
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

*/

package ptolemy.domains.de.lib;

import java.util.HashMap;

import ptolemy.actor.util.Time;
import ptolemy.data.DoubleToken;
import ptolemy.data.Token;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Workspace;

//////////////////////////////////////////////////////////////////////////
//// TimedDelay
/**
   This actor delays the input by a specified amount of time. The amount 
   of the time is required to be non-negative and has a default value 1.0.
   The input and output types are unconstrained, except that the
   output type must be at least that of the input.
   <p>
   The behavior of this actor on each firing is to read a token from the input,
   if there is one, and to produce the token on the corresponding output
   channel after the appropriate time delay. Note that if the value of delay
   is 0.0, the output is procduced immediately. If there is no input token, 
   then no output token is produced. NOTE: The output is produced in the fire() 
   method. 
   <p>
   Occasionally, this actor is used inside a feedback loop just for scheduling
   perpose, where the delay parameter is set to zero. This implies that no
   output token is produced earlier than the time its trigger input arrives.
   Therefore the actor declares that there is a delay between the input 
   and the output, and the DE director will leverage this when 
   determining the precedences of the actors. It is sometimes useful to think 
   of this zero-valued delay as an infinitesimal delay.
   <p> 
   The output may have the same microstep with the input, if there is
   no queued event scheduled to produce at the same time the input arrives. 
   Otherwise, the output is produced one microstep later. This guarantees that
   a DE signal is functional in the sense that there for any tag, there is 
   at most one value. 
   
   @see ptolemy.actor.util.FunctionDependencyOfAtomicActor
   @see ptolemy.domains.de.lib.VariableDelay
   @see ptolemy.domains.de.lib.Server
   @see ptolemy.domains.sdf.lib.SampleDelay

   @author Edward A. Lee, Lukito Muliadi, Haiyang Zheng
   @version $Id$
   @since Ptolemy II 1.0
   @Pt.ProposedRating Red (hyzheng)
   @Pt.AcceptedRating Red (hyzheng)
*/
public class TimedDelay extends DETransformer {

    /** Construct an actor with the specified container and name.
     *  @param container The composite actor to contain this one.
     *  @param name The name of this actor.
     *  @exception IllegalActionException If the entity cannot be contained
     *   by the proposed container.
     *  @exception NameDuplicationException If the container already has an
     *   actor with this name.
     */
    public TimedDelay(CompositeEntity container, String name)
            throws NameDuplicationException, IllegalActionException  {
        super(container, name);
        _init();
        
        _attachText("_iconDescription", "<svg>\n" +
                "<rect x=\"0\" y=\"0\" "
                + "width=\"60\" height=\"20\" "
                + "style=\"fill:white\"/>\n" +
                "</svg>\n");
    }

    ///////////////////////////////////////////////////////////////////
    ////                       ports and parameters                ////

    /** The amount of delay.  This parameter must contain a DoubleToken
     *  with a non-negative value, or an exception will be thrown when
     *  it is set.
     */
    public Parameter delay;

    ///////////////////////////////////////////////////////////////////
    ////                         public methods                    ////

    /** If the attribute is <i>delay</i>, then check that the value
     *  is non-negative.
     *  NOTE: the newDelay may be 0.0, which may change the causality 
     *  property of the model. We leave the model designers to decide 
     *  whether the zero delay is really what they want. 
     *  @param attribute The attribute that changed.
     *  @exception IllegalActionException If the delay is negative.
     */
    public void attributeChanged(Attribute attribute)
            throws IllegalActionException {
        if (attribute == delay) {
            double newDelay = ((DoubleToken)(delay.getToken())).doubleValue();
            if (newDelay < 0.0) {
                throw new IllegalActionException(this,
                        "Cannot have negative delay: "
                        + newDelay);
            } else {
                // NOTE: the newDelay may be 0.0, which may change
                // the causality property of the model. 
                // We leave the model designers to decide whether the
                // zero delay is really what they want. 
                _delay = newDelay;
            }
        } else {
            super.attributeChanged(attribute);
        }
    }

    /** Clone the actor into the specified workspace. This calls the
     *  base class and then sets the delayTo relation between the input
     *  and the output of the new actor.
     *  @param workspace The workspace for the new object.
     *  @return A new actor.
     *  @exception CloneNotSupportedException If a derived class has
     *   has an attribute that cannot be cloned.
     */
    public Object clone(Workspace workspace)
            throws CloneNotSupportedException {
        TimedDelay newObject = (TimedDelay)super.clone(workspace);
        newObject.output.setTypeSameAs(newObject.input);
        return newObject;
    }

    /** Read one token from the input and send one output
     *  that is scheduled to produce at the current time. 
     *  @exception IllegalActionException If there is no director, or the
     *  input can not be read, or the output can not be sent.
     */
    public void fire() throws IllegalActionException {
        // consume input
        if (input.hasToken(0)) {
            _currentInput = input.get(0);
        } else {
            _currentInput = null;
        }
        // produce output
        // NOTE: the delay may be zero. however, if there is some other event 
        // scheduled to produce at current time before the current firing, 
        // the current input is delayed to the next firing to produce.
        Time currentTime = getDirector().getModelTime();
        _currentOutput = null;
        if (_delayedTokens.size() > 0) {
            _currentOutput = (Token)_delayedTokens.get(currentTime);
            if (_currentOutput != null) {
                output.send(0, _currentOutput);
                return;
            } else {
                // no tokens to be produced at the current time.
            }
        }
        if (_delay == 0 && _currentInput != null) {
            output.send(0, _currentInput);
            _currentInput = null;
        }
    }

    /** Initialize the states of this actor.
     *  @exception IllegalActionException If a derived class throws it.
     */
    public void initialize() throws IllegalActionException {
        super.initialize();
        _currentInput = null;
        _currentOutput = null;
        _delayedTokens = new HashMap();
    }

    /** If the current input is scheduled to produce in a future time,
     *  schedule a refiring of this actor at that time.
     *  @exception IllegalActionException If scheduling to refire cannot
     *  be performed or the superclass throws it.
     */
    public boolean postfire() throws IllegalActionException {
       Time currentTime = getDirector().getModelTime();
       Time delayToTime = currentTime.add(_delay);
       // Remove the token that is already sent 
       // at the current time.
       if (_delayedTokens.size() > 0 && 
           _currentOutput != null) {
           _delayedTokens.remove(currentTime);
           // Schedule refiring at current time if the multiple tokens
           // are scheduled to produce at the current time.
           if (_delayedTokens.get(currentTime) != null) {
               getDirector().fireAtCurrentTime(this);
           }
       }
       // Schedule the not handled current input for future firing.
       if (_currentInput != null) {
           _delayedTokens.put(delayToTime, _currentInput);
           getDirector().fireAt(this, delayToTime);
       }
        return super.postfire();
    }

    /** Override the base class to declare that the <i>output</i>
     *  does not depend on the <i>input</i> in a firing.
     */
    public void pruneDependencies() {
        super.pruneDependencies();
        removeDependency(input, output);
    }

    ///////////////////////////////////////////////////////////////////
    ////                       protected method                    ////

    /** Initialize the delay parameter.
     */
    protected void _init() 
        throws NameDuplicationException, IllegalActionException  {
        delay = new Parameter(this, "delay", new DoubleToken(1.0));
        delay.setTypeEquals(BaseType.DOUBLE);
        _delay = ((DoubleToken)delay.getToken()).doubleValue();
    }

    ///////////////////////////////////////////////////////////////////
    ////                       protected variables                 ////

    /** The amount of delay.
     */ 
    protected double _delay;

    /** A hash map to store the delayed tokens.
    */
    protected HashMap _delayedTokens;

    /** Current input.
     */
    protected Token _currentInput;
    
    /** Current output.
     */
    protected Token _currentOutput;    


}
