/* An actor that translates the input 3D shape

 Copyright (c) 1998-2001 The Regents of the University of California.
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

@ProposedRating Red (chf@eecs.berkeley.edu)
@AcceptedRating Red (chf@eecs.berkeley.edu)
*/

package ptolemy.domains.gr.lib;

import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.*;
import ptolemy.data.*;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.BaseType;
import ptolemy.actor.*;
import ptolemy.domains.gr.kernel.*;

import javax.media.j3d.*;
import javax.vecmath.*;

//////////////////////////////////////////////////////////////////////////
//// Translate3D

/** Conceptually, this actor takes 3D geometry in its input and produces a
translated version in its output. In reality, this actor encapsulates a Java3D
TransformGroup which is converted into a node in the resulting Java3D scene
graph. This actor will only have meaning in the GR domain.
@author C. Fong
@version $Id$
*/
public class Translate3D extends GRTransform {

    /** Construct an actor with the given container and name.
     *  @param container The container.
     *  @param name The name of this actor.
     *  @exception IllegalActionException If the actor cannot be contained
     *   by the proposed container.
     *  @exception NameDuplicationException If the container already has an
     *   actor with this name.
     */
    public Translate3D(CompositeEntity container, String name)
            throws IllegalActionException, NameDuplicationException {
        super(container, name);

        xTranslate = new TypedIOPort(this, "xTranslate", true, false);
        xTranslate.setTypeEquals(BaseType.DOUBLE);
        yTranslate = new TypedIOPort(this, "yTranslate", true, false);
        yTranslate.setTypeEquals(BaseType.DOUBLE);
        zTranslate = new TypedIOPort(this, "zTranslate", true, false);
        zTranslate.setTypeEquals(BaseType.DOUBLE);


        initialXTranslation = new Parameter(this,
                                  "initialXTranslation", new DoubleToken(0.0));
        initialYTranslation = new Parameter(this,
                                  "initialYTranslation", new DoubleToken(0.0));
        initialZTranslation = new Parameter(this,
                                  "initialZTranslation", new DoubleToken(0.0));

    }

    ///////////////////////////////////////////////////////////////////
    ////                     ports and parameters                  ////

    /** The amount of translation in the x-axis during firing. If this
     *  transform is in accumulate mode, the translation value is
     *  accumulated for each firing.
     */
    public TypedIOPort xTranslate;

    /** The amount of translation in the y-axis during firing. If this
     *  transform is in accumulate mode, the translation value is
     *  accumulated for each firing.
     */
    public TypedIOPort yTranslate;

    /** The amount of translation in the z-axis during firing. If this
     *  transform is in accumulate mode, the translation value is
     *  accumulated for each firing.
     */
    public TypedIOPort zTranslate;

    /** The initial translation in the x-axis
     *  This parameter should contain a DoubleToken.
     *  The default value of this parameter is 0.0.
     */
    public Parameter initialXTranslation;

    /** The initial translation in the y-axis
     *  This parameter should contain a DoubleToken.
     *  The default value of this parameter is 0.0.
     */
    public Parameter initialYTranslation;

    /** The initial translation in the z-axis
     *  This parameter should contain a DoubleToken.
     *  The default value of this parameter is 0.0.
     */
    public Parameter initialZTranslation;


    ///////////////////////////////////////////////////////////////////
    ////                         public methods                    ////

    /** Check the input ports for translation inputs.  Convert the translation
     *  tokens into a Java3D transformation.
     *  @exception IllegalActionException If the value of some parameters
     *   can't be obtained
     */
    public void fire() throws IllegalActionException {
    //  all state changes must be done in postfire()
        super.fire();
        boolean applyTransform = false;
        double xOffset = _initialXTranslation;
        double yOffset = _initialYTranslation;
        double zOffset = _initialZTranslation;
        boolean isAccumulating = _isAccumulating();

        if (xTranslate.getWidth() != 0) {
            if (xTranslate.hasToken(0)) {
                double in = ((DoubleToken) xTranslate.get(0)).doubleValue();
                applyTransform = true;
                xOffset = xOffset + in;
            }
        }

        if (yTranslate.getWidth() != 0) {
            if (yTranslate.hasToken(0)) {
                double in = ((DoubleToken) yTranslate.get(0)).doubleValue();
                applyTransform = true;
                yOffset = yOffset + in;
            }
        }

        if (zTranslate.getWidth() != 0) {
            if (zTranslate.hasToken(0)) {
                double in = ((DoubleToken) zTranslate.get(0)).doubleValue();
                applyTransform = true;
                zOffset = zOffset + in;
            }
        }

        if (isAccumulating) {
            xOffset = xOffset + _accumulatedX - _initialXTranslation;
            _accumulatedX = xOffset;
            yOffset = yOffset + _accumulatedY - _initialYTranslation;
            _accumulatedY = yOffset;
            zOffset = zOffset + _accumulatedZ - _initialZTranslation;
            _accumulatedZ = zOffset;
        }


        if (applyTransform) {
            Transform3D transform = new Transform3D();
    	    transform.setTranslation(new Vector3d(xOffset, yOffset, zOffset));
    	    transformNode.setTransform(transform);
        }

    }

    /** Setup the initial translation.
     *  @exception IllegalActionException If the value of some parameters can't
     *   be obtained
     */
    public void initialize() throws IllegalActionException {
        super.initialize();
        _initialXTranslation = ((DoubleToken)
                                  initialXTranslation.getToken()).doubleValue();
        _initialYTranslation = ((DoubleToken)
                                  initialYTranslation.getToken()).doubleValue();
        _initialZTranslation = ((DoubleToken)
                                  initialZTranslation.getToken()).doubleValue();

        transformNode = new TransformGroup();
        transformNode.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);

        Transform3D transform = new Transform3D();
        transform.setTranslation(new Vector3d(_initialXTranslation,
                                              _initialYTranslation,
                                              _initialZTranslation));
        transformNode.setTransform(transform);

        _accumulatedX = 0.0;
        _accumulatedY = 0.0;
        _accumulatedZ = 0.0;
    }


    /** Return the encapsulated Java3D node of this 3D actor. The encapsulated
     *  node for this actor TransformGroup
     *
     *  @return the Java3D TransformGroup
     */
    protected Node _getNodeObject() {
        return (Node) transformNode;
    }


    /** Connect other Java3D nodes as children of the encapsulated node in
     *  this actor
     *
     *  @param node The child Java3D node.
     */
    protected void _addChild(Node node) {
        transformNode.addChild(node);
    }

    ///////////////////////////////////////////////////////////////////
    ////                         private variables                 ////

    private double _initialXTranslation;
    private double _initialYTranslation;
    private double _initialZTranslation;
    private double _accumulatedX;
    private double _accumulatedY;
    private double _accumulatedZ;

    ///////////////////////////////////////////////////////////////////
    ////                         protected variables               ////

    protected TransformGroup transformNode;

}
