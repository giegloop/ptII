/* Interface for objects that can export themselves in MoML.

Copyright (c) 2003-2005 The Regents of the University of California.
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
package ptolemy.kernel.util;

import java.io.IOException;
import java.io.Writer;


//////////////////////////////////////////////////////////////////////////
//// MoMLExportable

/**
   This is an interface for objects that have persistent MoML representations.
   MoML is an XML schema used to represent Ptolemy II models.  The form of
   the MoML generated by an implementor of this interface should be:
   <pre>
   &lt;<i>elementName</i> name="<i>name</i>" class="<i>className</i>" source="<i>source</i>"&gt;
   <i>body, determined by the implementor</i>
   &lt;/<i>elementName</i>&gt;
   </pre>
   or
   <pre>
   &lt;class name="<i>name</i>" extends="<i>className</i>" source="<i>source</i>"&gt;
   <i>body, determined by the implementor</i>
   &lt;/class&gt;
   </pre>

   The <i>elementName</i> is the string returned by getElementName().
   The <i>name</i> is the string returned by getName().
   The <i>className</i> is the string returned by getClassName().
   The <i>source</i> is the string returned by getSource().
   If either of the last two methods returns null, then that
   XML attribute is omitted from the description.
   <p>
   If this object has no container (and for the exportMoML() methods
   that take a depth argument, if that argument is zero),
   then the exportMoML() methods prepend XML file header information,
   which is:
   <pre>
   &lt;?xml version="1.0" standalone="no"?&gt;
   &lt;!DOCTYPE entity PUBLIC "-//UC Berkeley//DTD MoML 1//EN"
   "http://ptolemy.eecs.berkeley.edu/xml/dtd/MoML_1.dtd"&gt;
   </pre>

   @author Edward A. Lee
   @version $Id$
   @since Ptolemy II 4.0
   @Pt.ProposedRating Green (eal)
   @Pt.AcceptedRating Green (neuendor)
   @see NamedObj
*/
public interface MoMLExportable extends Nameable {
    /** Return a MoML description of this object.  This might be an empty string
     *  if there is no MoML description of this object or if this object is
     *  not persistent or if an implementor has some other reason that the
     *  object has no persistent description.
     *  @return A MoML description, or an empty string if there is none.
     *  @see #isPersistent()
     */
    public String exportMoML();

    /** Return a MoML description of this object with its name replaced by
     *  the specified name.  The description might be an empty string
     *  if there is no MoML description of this object or if this object
     *  is not persistent, or if an implementor has some other reason that the
     *  object has no persistent description.
     *  @return A MoML description, or the empty string if there is none.
     *  @see #isPersistent()
     */
    public String exportMoML(String name);

    /** Write a MoML description of this object using the specified
     *  Writer.  If there is no MoML description, or if the object
     *  is not persistent, or if an implementor has some other
     *  reason that the object has no persistent description,
     *  then nothing is written. To write to standard out, do
     *  <pre>
     *      exportMoML(new OutputStreamWriter(System.out))
     *  </pre>
     *  @exception IOException If an I/O error occurs.
     *  @param output The writer to write to.
     *  @see #isPersistent()
     */
    public void exportMoML(Writer output) throws IOException;

    /** Write a MoML description of this entity with the specified
     *  depth in a hierarchy. The depth in the hierarchy determines
     *  indenting of the output, but may also be used to indicate
     *  to an implementor whether this object is being exported
     *  as part of the export of its container (in which case
     *  depth > 0), or this object is being exported independently
     *  of its container (depth == 0). If there is no MoML description,
     *  or if the object is not persistent, or if an implementor
     *  has some other reason that the object has no persistent
     *  description, then nothing is written.
     *  @param output The output writer to write to.
     *  @param depth The depth in the hierarchy.
     *  @exception IOException If an I/O error occurs.
     *  @see #isPersistent()
     */
    public void exportMoML(Writer output, int depth) throws IOException;

    /** Write a MoML description of this entity with the specified
     *  depth in a hierarchy and with the specified name substituting
     *  for the name of this object. The depth in the hierarchy determines
     *  indenting of the output, but may also be used to indicate
     *  to an implementor whether this object is being exported
     *  as part of the export of its container (in which case
     *  depth > 0), or this object is being exported independently
     *  of its container (depth == 0). If there is no MoML description,
     *  or if the object is not persistent, or if an implementor
     *  has some other reason that the object has no persistent
     *  description, then nothing is written.
     *  @param output The output writer to write to.
     *  @param depth The depth in the hierarchy, to determine indenting.
     *  @param name The name to use in the exported MoML.
     *  @exception IOException If an I/O error occurs.
     *  @see #isPersistent()
     */
    public void exportMoML(Writer output, int depth, String name)
        throws IOException;

    /** Return the class name.  This is either the name of the
     *  class of which this object is an instance, or if this
     *  object is itself a class, then the class that it extends.
     *  The class name could be a fully qualified Java class name
     *  or the name of the parent of this object if it has a parent.
     *  @return The MoML class name.
     *  @see Instantiable#getParent()
     */
    public String getClassName();

    /** Get the XML element name.  Implementors should provide the
     *  appropriate XML element name to use when exporting MoML.
     *  @return The XML element name for this object.
     */
    public String getElementName();

    /** Get the source, which gives an external URL
     *  associated with an entity (presumably from which the entity
     *  was defined).  This becomes the value in the "source"
     *  attribute of exported MoML.
     *  @return The source, or null if there is none.
     *  @see #setSource(String)
     */
    public String getSource();

    /** Return true if this object is persistent.
     *  A persistent object has a MoML description that can be stored
     *  in a file and used to re-create the object. A non-persistent
     *  object has an empty MoML description.
     *  @return True if the object is persistent.
     *  @see #setPersistent(boolean)
     */
    public boolean isPersistent();

    /** Set the persistence of this object. If the persistence is not
     *  specified with this method, then by default the object will be
     *  persistent unless it is derivable by derivation from a class.
     *  A persistent object has a non-empty MoML description that can be used
     *  to re-create the object. To make an instance non-persistent,
     *  call this method with the argument <i>false</i>. To force
     *  it to always be persistent, irrespective of its relationship
     *  to a class, then call this with argument <i>true</i>. Note
     *  that this will have the additional effect that it no longer
     *  inherits properties from the class, so in effect, calling
     *  this with <i>true</i> overrides values given by the class.
     *  @param isPersistent False to make this object non-persistent.
     *  @see #isPersistent()
     */
    public void setPersistent(boolean isPersistent);

    /** Set the source, which gives an external URL
     *  associated with an entity (presumably from which the entity
     *  was defined).  This becomes the value in the "source"
     *  attribute of exported MoML. Call this with null to prevent
     *  any source attribute from being generated.
     *  @param source The source, or null if there is none.
     *  @see #getSource()
     */
    public void setSource(String source);
}
