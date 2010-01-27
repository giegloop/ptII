/* Exception for graph action errors.

 Copyright (c) 2002-2008 The University of Maryland.
 All rights reserved.
 Permission is hereby granted, without written agreement and without
 license or royalty fees, to use, copy, modify, and distribute this
 software and its documentation for any purpose, provided that the above
 copyright notice and the following two paragraphs appear in all copies
 of this software.

 IN NO EVENT SHALL THE UNIVERSITY OF MARYLAND BE LIABLE TO ANY PARTY
 FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
 ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
 THE UNIVERSITY OF MARYLAND HAS BEEN ADVISED OF THE POSSIBILITY OF
 SUCH DAMAGE.

 THE UNIVERSITY OF MARYLAND SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
 PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
 MARYLAND HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
 ENHANCEMENTS, OR MODIFICATIONS.

 */
package ptolemy.graph;

import ptolemy.kernel.util.IllegalActionException;

//////////////////////////////////////////////////////////////////////////
//// GraphActionException

/**
 Exception for graph action errors. This is a non-RuntimeException and
 should be caught by calling methods.

 @author Mingyung Ko
 @version $Id$
 @since Ptolemy II 2.1
 @Pt.ProposedRating Red (myko)
 @Pt.AcceptedRating Red (ssb)
 */
public class GraphActionException extends IllegalActionException {

    // This class extends IllegalActionException so that we have
    // the dependency from graph to kernel.util in as few places
    // as possible.  However, we must extend IllegalActionException
    // because that is the exception that is caught in the UI,
    // which indicates which actor has a problem.  A better design
    // would be to have base class exceptions that do not
    // refer to NamedObj in a separate package and then have
    // kernel.util exceptions extend those exceptions.

    /** Constructor with an argument of text description.
     *  @param message Detailed description of the error.
     */
    public GraphActionException(String message) {
        super(message);
    }

    /** Constructor with an argument of text description.
     *  @param message Detailed description of the error.
     *  @param cause The cause of this exception, or null if the cause
     *  is not known or nonexistent.
     */
    public GraphActionException(Throwable cause, String message) {
        super(null, cause, message);
    }
}
