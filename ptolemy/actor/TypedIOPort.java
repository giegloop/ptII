/* An IOPort with a type.

 Copyright (c) 1997-1998 The Regents of the University of California.
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

package ptolemy.actor;
import ptolemy.kernel.*;
import ptolemy.kernel.util.*;
import ptolemy.data.*;
import ptolemy.graph.*;

//////////////////////////////////////////////////////////////////////////
//// TypedIOPort
/**
An IOPort with a type.  The type is one of the token types in the data
package.  A TypedIOPort has a declared type which can be set by calling
setDeclaredType().  If this method is not called, the declared type is
null, and the type is said to be undeclared.  In this case, the port has
more than one types and the acceptable types are defined by a separate set of
constraints.<p>

In addition to the declared type, a TypedIOPort also has a resolved type,
set by the type resolution algorithm.  If the declared type is not null,
the resolved type will be the same as the declared type; otherwise, the
type resolution algorithm will assign a resolved type according to the
type constraints.<p>

The TypedIOPort implements the InequalityTerm interface for the purpose
of type resolution.<p>

The type resolution algorithm and run-time type checking will guaranttee
that only tokens with acceptable types are allowed to transport through
a TypedIOPort.<p>

An IOPort can only be contained by a class derived from ComponentEntity
and implementing the TypedActor interface.  Subclasses may further
constrain the containers by overriding setContainer().

@author Yuhong Xiong
$Id$
*/

public class TypedIOPort extends IOPort implements InequalityTerm {

    // all the constructors are wrappers of the super class constructors.

    /** Construct a TypedIOPort with no container and no name that is
     *  neither an input nor an output.
     */
    public TypedIOPort() {
        super();
    }

    /** Construct a TypedIOPort with a containing actor and a name
     *  that is neither an input nor an output.  The specified container
     *  must implement the Actor interface, or an exception will be thrown.
     *
     *  @param container The container actor.
     *  @param name The name of the port.
     *  @exception IllegalActionException If the port is not of an acceptable
     *   class for the container, or if the container does not implement the
     *   Actor interface.
     *  @exception NameDuplicationException If the name coincides with
     *   a port already in the container.
     */
    public TypedIOPort(ComponentEntity container, String name)
            throws IllegalActionException, NameDuplicationException {
	super(container, name);
    }

    /** Construct a TypedIOPort with a container and a name that is
     *  either an input, an output, or both, depending on the third
     *  and fourth arguments. The specified container must implement
     *  the Actor interface or an exception will be thrown.
     *
     *  @param container The container actor.
     *  @param name The name of the port.
     *  @param isinput True if this is to be an input port.
     *  @param isoutput True if this is to be an output port.
     *  @exception IllegalActionException If the port is not of an acceptable
     *   class for the container, or if the container does not implement the
     *   Actor interface.
     *  @exception NameDuplicationException If the name coincides with
     *   a port already in the container.
     */
    public TypedIOPort(ComponentEntity container, String name,
            boolean isinput, boolean isoutput)
            throws IllegalActionException, NameDuplicationException {
	super(container, name, isinput, isoutput);
    }


    ///////////////////////////////////////////////////////////////////
    ////                         public methods                    ////

    /** Returns the declared type of this port.  The type is represented
     *  by an instance of a token of the correct type.  If the type is
     *  undeclared, returns null.
     *  This method is read-synchronized on the workspace.
     *  @return a token of the declared type, or null if the type is
     *   undeclared.
     */
    public Token declaredType() {
	try {
	    workspace().getReadAccess();
	    return _declaredType;
	} finally {
	    workspace().doneReading();
	}
    }

    /** Returns the resolved type of this object.  The type is represented
     *  by an instance of a token of the correct type. 
     *  NOTE: The reason that the resolved type is represented by an
     *  instance of Token instead of an instance of Class is to be able to
     *  call the static convert() method of the Token class to
     *  do lossless conversion.  The declared type is also a token to
     *  be consistent with the resolved type.
     *  This method is read-synchronized on the workspace.
     *  @return a token of the resolved type.
     */
    public Token resolvedType() {
	try {
	    workspace().getReadAccess();
	    return _resolvedType;
	} finally {
	    workspace().doneReading();
	}
    }

    /** Override the method in the super class to do type checking.
     *  If the specified token has the resolved type of this IOPort, or
     *  the token can be converted to the resolved type losslessly, the
     *  token is allowed to be sent to all receivers connected to the
     *  specified channel. Otherwise, IllegalActionException is thrown.
     *  Before putting the token into the destination receivers, this
     *  method also finds the resolved type of the input IOPort containing
     *  the receivers, and tests if the token is an instance of that type.
     *  If not, this method will convert the token to the type of the
     *  input IOPort. The conversion is done by calling the convert()
     *  method of the token representing the resolved type of the input
     *  IOport.
     *  This method is read-synchronized on the workspace.
     *
     *  @param channelindex The index of the channel, from 0 to width-1
     *  @param token The token to send
     *  @exception CloneNotSupportedException If the token cannot be cloned
     *   and there is more than one destination.
     *  @exception IllegalActionException If the port is not an output,
     *   or if the index is out of range, or the specified token cannot be
     *   converted to the resolved type of this IOPort.
     */
    public void send(int channelindex, Token token)
            throws CloneNotSupportedException, IllegalActionException {
        try {
            workspace().getReadAccess();
            if (!isOutput()) {
                throw new IllegalActionException(this,
                        "send: Tokens can only be sent from an output port.");
            }
            if (channelindex >= getWidth() || channelindex < 0) {
                throw new IllegalActionException(this,
                        "send: channel index is out of range.");
            }
	    int compare = TypeCPO.compare(token, _resolvedType);
	    if (compare == CPO.STRICT_GREATER ||
		compare == CPO.INCOMPARABLE) {
		throw new IllegalActionException(this,
			"send: token has wrong type.");
	    }

            Receiver[][] fr = getRemoteReceivers();
            if (fr == null || fr[channelindex] == null) return;
            boolean first = true;
            for (int j = 0; j < fr[channelindex].length; j++) {
		TypedIOPort port =
			(TypedIOPort)fr[channelindex][j].getContainer();
		Token desttype = port.resolvedType();
		if (desttype.getClass().isInstance(token)) {
                    if (first) {
                    	fr[channelindex][j].put(token);
                    	first = false;
                    } else {
                    	fr[channelindex][j].put((Token)(token.clone()));
                    }
		} else {
		    Token newtoken = desttype.convert(token);
		    // since token is not an instance of the destination
		    // type, convert will always return a new instance.
		    // So no clone needed.
                    fr[channelindex][j].put(newtoken);
		}
            }
        } finally {
            workspace().doneReading();
        }
    }

    /** Sets the resolved type.
     *  This is a method in the InequalityTerm interface.
     *  This method is write-synchronized on the workspace.
     *  @param e resolved type.
     *  @exception IllegalActionException this port has a non-null
     *   declared type, so the resolved type cannot be set here.
     *  @exception IllegalArgumentException the parameter e is not an
     *   instance of a Token.
     */
    public void set(Object e)
	    throws IllegalActionException {
	try {
	    workspace().getWriteAccess();
	    if (_declaredType != null) {
	        throw new IllegalActionException("TypedIOPort.set: The port " +
			"has a non-null declared type.");
	    }

	    if ( !(e instanceof Class)) {
	        throw new IllegalArgumentException("TypedIOPort.set: " +
			"the parameter is not an instance of Class.");
	    }

	    try {
	        _resolvedType = (Token)(((Class)e).newInstance());
	    } catch (InstantiationException instan) {
	        throw new InternalErrorException("TypedIOPort.set: Can't " +
			"instantiate a token class. " + instan.getMessage());
	    } catch (IllegalAccessException illegal) {
	        throw new InternalErrorException("TypedIOPort.set: Internal " +
			"error: " + illegal.getMessage());
	    }
        } finally {
	    workspace().doneWriting();
	}
    }

    /** Override the base class to ensure that the proposed container
     *  implements the TypedActor interface (the base class ensures that
     *  the container implements the Actor interface).
     *
     *  @param container The proposed container.
     *  @exception IllegalActionException If the proposed container is not a
     *   ComponentEntity, doesn't implement Actor, or has no name,
     *   or the port and container are not in the same workspace.
     *  @exception NameDuplicationException If the container already has
     *   a port with the name of this port.
     */
    public void setContainer(Entity container)
            throws IllegalActionException, NameDuplicationException {
        if (!(container instanceof TypedActor)) {
            throw new IllegalActionException(container, this,
                    "TypedIOPort can only be contained by objects " +
		    "implementing the TypedActor interface.");
        }
        super.setContainer(container);
    }

    /** Sets the declared type of this object.  The type is represented
     *  by an instance of a token of the correct type.
     *  This method is write-synchronized on the workspace.
     *  @param t an instance of a token representing the declared type.
     */
    // FIXME: this method may want to inform its director about this
    // change.
    public void setDeclaredType(Token t) {
	try {
	    workspace().getWriteAccess();
	    _declaredType = t;

	    // also set the resolved type,  If _declaredType==null, i.e.,
	    // undeclared, the type resolution algorithm will reset the
	    // _resolvedType.
	    _resolvedType = _declaredType;
	} finally {
	    workspace().doneWriting();
	}
    }

    /** Checks if the type of this port is undeclared.  If this call
     *  returns true, set() can be used to set the resolved type.
     *  This is a method in the InequalityTerm interface.
     *  This method is read-synchronized on the workspace.
     *  @return true if the type of this port is undeclared; false
     *   otherwise.
     */
    public boolean settable() {
	try {
	    workspace().getReadAccess();
	    return _declaredType==null;
	} finally {
	    workspace().doneReading();
	}
    }

    /** Returns the resolved type of this term.
     *  This method is read-synchronized on the workspace.
     *  @return a token whose type is the resolved type.
     */
    public Object value() {
	try {
	    workspace().getReadAccess();
	    return _resolvedType.getClass();
	} finally {
	    workspace().doneReading();
	}
    }

    ///////////////////////////////////////////////////////////////////
    ////                         public variables                  ////

    /** Indicate that the description(int) method should include information
     *  about the type of this port.
     */
    public static final int TYPE = 4096;

    ///////////////////////////////////////////////////////////////////
    ////                         protected methods                 ////

    /** Return a description of the object.  The level of detail depends
     *  on the argument, which is an or-ing of the static final constants
     *  defined in the NamedObj class and in this class.
     *  Lines are indented according to
     *  to the level argument using the protected method _getIndentPrefix().
     *  Zero, one or two brackets can be specified to surround the returned
     *  description.  If one is specified it is the leading bracket.
     *  This is used by derived classes that will append to the description.
     *  Those derived classes are responsible for the closing bracket.
     *  An argument other than 0, 1, or 2 is taken to be equivalent to 0.
     *  <p>
     *  If the detail argument sets the bit defined by the constant
     *  TYPE, then append to the description a field of the form
     *  "type {declared <i>declaredType</i> resolved <i>resolvedType</i>}".
     *  The declaredType and resolvedType are the names of the tokens
     *  representing the types.
     *  <p>
     *
     *  This method is read-synchronized on the workspace.
     *  @param detail The level of detail.
     *  @param indent The amount of indenting.
     *  @param bracket The number of surrounding brackets (0, 1, or 2).
     *  @return A description of the object.
     */
    protected String _description(int detail, int indent, int bracket) {
        try {
            workspace().getReadAccess();
            String result;
            if (bracket == 1 || bracket == 2) {
                result = super._description(detail, indent, 1);
            } else {
                result = super._description(detail, indent, 0);
            }

            if ((detail & TYPE) != 0) {
                if (result.trim().length() > 0) {
                    result += " ";
                }
                result += "type {declared ";
		if (declaredType() == null) {
		    result += "null";
		} else {
		    result += declaredType().getClass().getName();
		}
		result += " resolved ";
		if (resolvedType() == null) {
		    result += "null";
		} else {
		    result += resolvedType().getClass().getName();
		}
		result += "}";
            }
            if (bracket == 2) result += "}";
            return result;
        } finally {
            workspace().doneReading();
        }
    }

    ///////////////////////////////////////////////////////////////////
    ////                         private variables                 ////

    private Token _declaredType = null;
    private Token _resolvedType = null;
}

