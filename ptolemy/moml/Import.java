/* An attribute indicating import from an external file.

 Copyright (c) 1998 The Regents of the University of California.
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
@ProposedRating Red (eal@eecs.berkeley.edu)
@AcceptedRating Red (reviewmoderator@eecs.berkeley.edu)

*/

package ptolemy.moml;

import java.io.IOException;
import java.io.Writer;
import java.net.URL;

import ptolemy.kernel.util.*;

//////////////////////////////////////////////////////////////////////////
//// Import
/**
This attribute identifies an external file that the container of the
attribute depends on.  When exported to MoML, this attribute becomes
an "import" element rather than the usual attribute.

@author  Edward A. Lee
@version $Id$
*/
public class Import extends Attribute {

    /** Construct an import reference with the specified container and name.
     *  @param container The container.
     *  @param name The name of this attribute.
     *  @exception IllegalActionException If the attribute is not of an
     *   acceptable class for the container, or if the name contains a period.
     *  @exception NameDuplicationException If the name coincides with
     *   an attribute already in the container.
     */
    public Import(NamedObj container, String name)
            throws IllegalActionException, NameDuplicationException {
        super(container, name);
    }

    ///////////////////////////////////////////////////////////////////
    ////                         public methods                    ////

    /** Write a MoML description of this object, which in this case is
     *  an import element.
     *  @param output The output stream to write to.
     *  @param depth The depth in the hierarchy, to determine indenting.
     */
    public void exportMoML(Writer output, int depth) throws IOException {
        output.write(_getIndentPrefix(depth)
               + "<import base=\""
               + _base.toExternalForm()
               + "\" source=\""
               + _source
               + "\"/>\n");
    }

    /** Get the base with respect to which this import source can be opened.
     *  @return The base with respect to which this import source
     *   can be opened.
     */
    public URL getBase() {
        return _base;
    }

    /** Get the file that this import refers to.
     *  @return The file referred to.
     */
    public String getSource() {
        return _source;
    }

    /** Set the base with respect to which this import source can be opened.
     *  @param source The base with respect to which this import source can
     *   be opened.
     */
    public void setBase(URL base) {
        _base = base;
    }

    /** Set the file that this import refers to.
     *  The argument is interpreted as a URL, relative or absolute.
     *  @param source The file that this import refers to.
     */
    public void setSource(String source) {
        _source = source;
    }

    ///////////////////////////////////////////////////////////////////
    ////                         private variables                 ////

    // The base with respect to which the source can be opened.
    private URL _base;

    // The object referred to.
    private String _source;
}
