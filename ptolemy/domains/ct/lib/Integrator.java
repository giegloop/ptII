/* The integrator in the CT domain.

 Copyright (c) 1998-2000 The Regents of the University of California.
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

@ProposedRating Red (liuj@eecs.berkeley.edu)
@AcceptedRating Red (cxh@eecs.berkeley.edu)
*/

package ptolemy.domains.ct.lib;
import ptolemy.domains.ct.kernel.*;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.*;
import ptolemy.actor.*;

//////////////////////////////////////////////////////////////////////////
//// Integrator
/**
A wrapper of CTBaseIntegrator. The only purpose of this actor is
that it is in the ct.lib package.
@author Jie Liu
@version $Id$
@see ptolemy.domains.ct.kernel.CTBaseIntegrator
*/
public class Integrator extends CTBaseIntegrator {

    /** Construct an integrator.
     * @see ptolemy.domains.ct.kernel.CTBaseIntegrator
     * @param container The container.
     * @param name The name.
     * @exception NameDuplicationException If another star already had
     * this name.
     * @exception IllegalActionException If there was an internal problem.
     */
    public Integrator(CompositeEntity container, String name)
            throws NameDuplicationException, IllegalActionException {
        super(container, name);

	ProcessedString icon;
	icon = (ProcessedString)getAttribute("iconDescription");
	if(icon == null) {
	    icon = new NonpersistentProcessedString(this, "iconDescription");
	}
	icon.setInstruction("graphml");
	icon.setString("<xmlgraphic>\n" + 
	    "<rectangle coords=\"0 0 60 40\" fill=\"white\"/>\n" +
	    "<line coords=\"33 10 29 13 27 15 30 20 33 25 31 27 27 30\"/>\n" +
	    "</xmlgraphic>\n");
    }
}
