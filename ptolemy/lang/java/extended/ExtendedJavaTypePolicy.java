/*
An object that encapsulates the type policy for Extended Java.

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

@ProposedRating Red (ctsay@eecs.berkeley.edu)
@AcceptedRating Red (ctsay@eecs.berkeley.edu)
*/


package ptolemy.lang.java.extended;

import ptolemy.lang.*;
import ptolemy.lang.java.*;
import ptolemy.lang.java.nodetypes.*;

/** An object that encapsulates the type policy for Extended Java.
 *
 *  @author Jeff Tsay
 */
public class ExtendedJavaTypePolicy extends TypePolicy {
    public ExtendedJavaTypePolicy() {
        super(new ExtendedJavaTypeIdentifier());
    }

    public ExtendedJavaTypePolicy(TypeIdentifier typeID) {
        super(typeID);
    }

    public TypeNode arithPromoteType(final TypeNode type1, final TypeNode type2) {
        int kind1 = _typeID.kind(type1);
        int kind2 = _typeID.kind(type2);

        if (_debug) {
            System.out.println("ExtendedJavaTypePolicy."
			       + "arithPromotType(): "
			       + " kind1: " + kind1
			       + " kind2: " + kind2
			       );
	}

	// FIXME: These should be reordered to match the order 
	// TYPE_KIND_FIX_POINT_MATRIX etc. are declared in
	// ExtendedJavaTypeIdentifier.java

	// Matrices
        if ((kind1 == ExtendedJavaTypeIdentifier.TYPE_KIND_FIX_POINT_MATRIX) ||
                (kind2 == ExtendedJavaTypeIdentifier.TYPE_KIND_FIX_POINT_MATRIX)) {
            return TypeUtility.makeArrayType((TypeNode)
                    ExtendedJavaTypeIdentifier.FIX_POINT_TYPE.clone(), 2);
        }

        if ((kind1 == ExtendedJavaTypeIdentifier.TYPE_KIND_COMPLEX_MATRIX) ||
                (kind2 == ExtendedJavaTypeIdentifier.TYPE_KIND_COMPLEX_MATRIX)) {
            return TypeUtility.makeArrayType((TypeNode)
                    ExtendedJavaTypeIdentifier.COMPLEX_TYPE.clone(), 2);
        }

        if ((kind1 == ExtendedJavaTypeIdentifier.TYPE_KIND_DOUBLE_MATRIX) ||
                (kind2 == ExtendedJavaTypeIdentifier.TYPE_KIND_DOUBLE_MATRIX)) {
            return TypeUtility.makeArrayType(DoubleTypeNode.instance, 2);
        }

        if ((kind1 == ExtendedJavaTypeIdentifier.TYPE_KIND_LONG_MATRIX) ||
                (kind2 == ExtendedJavaTypeIdentifier.TYPE_KIND_LONG_MATRIX)) {
            return TypeUtility.makeArrayType(LongTypeNode.instance, 2);
        }

        if ((kind1 == ExtendedJavaTypeIdentifier.TYPE_KIND_INT_MATRIX) ||
                (kind2 == ExtendedJavaTypeIdentifier.TYPE_KIND_INT_MATRIX)) {
            return TypeUtility.makeArrayType(IntTypeNode.instance, 2);
        }

        if ((kind1 == ExtendedJavaTypeIdentifier.TYPE_KIND_BOOLEAN_MATRIX) ||
                (kind2 == ExtendedJavaTypeIdentifier.TYPE_KIND_BOOLEAN_MATRIX)) {
            return TypeUtility.makeArrayType(BoolTypeNode.instance, 2);
        }

	// Arrays
        if ((kind1 == ExtendedJavaTypeIdentifier.TYPE_KIND_BOOLEAN_ARRAY) ||
                (kind2 == ExtendedJavaTypeIdentifier.TYPE_KIND_BOOLEAN_ARRAY)) {
            return TypeUtility.makeArrayType(BoolTypeNode.instance, 1);
        }

        if ((kind1 == ExtendedJavaTypeIdentifier.TYPE_KIND_INT_ARRAY) ||
                (kind2 == ExtendedJavaTypeIdentifier.TYPE_KIND_INT_ARRAY)) {
            return TypeUtility.makeArrayType(IntTypeNode.instance, 1);
        }

        if ((kind1 == ExtendedJavaTypeIdentifier.TYPE_KIND_LONG_ARRAY) ||
                (kind2 == ExtendedJavaTypeIdentifier.TYPE_KIND_LONG_ARRAY)) {
            return TypeUtility.makeArrayType(LongTypeNode.instance, 1);
        }

        if ((kind1 == ExtendedJavaTypeIdentifier.TYPE_KIND_DOUBLE_ARRAY) ||
                (kind2 == ExtendedJavaTypeIdentifier.TYPE_KIND_DOUBLE_ARRAY)) {
            return TypeUtility.makeArrayType(DoubleTypeNode.instance, 1);
        }

        if ((kind1 == ExtendedJavaTypeIdentifier.TYPE_KIND_COMPLEX_ARRAY) ||
                (kind2 == ExtendedJavaTypeIdentifier.TYPE_KIND_COMPLEX_ARRAY)) {
            return TypeUtility.makeArrayType((TypeNode)
                    ExtendedJavaTypeIdentifier.COMPLEX_TYPE.clone(), 1);
        }

        if ((kind1 == ExtendedJavaTypeIdentifier.TYPE_KIND_FIX_POINT_ARRAY) ||
                (kind2 == ExtendedJavaTypeIdentifier.TYPE_KIND_FIX_POINT_ARRAY)) {
            return TypeUtility.makeArrayType((TypeNode)
                    ExtendedJavaTypeIdentifier.FIX_POINT_TYPE.clone(), 1);
        }

	// Others
        if ((kind1 == ExtendedJavaTypeIdentifier.TYPE_KIND_FIX_POINT) ||
                (kind2 == ExtendedJavaTypeIdentifier.TYPE_KIND_FIX_POINT)) {
            return (TypeNode) ExtendedJavaTypeIdentifier.FIX_POINT_TYPE.clone();
        }

        if ((kind1 == ExtendedJavaTypeIdentifier.TYPE_KIND_COMPLEX) ||
                (kind2 == ExtendedJavaTypeIdentifier.TYPE_KIND_COMPLEX)) {
            return (TypeNode) ExtendedJavaTypeIdentifier.COMPLEX_TYPE.clone();
        }

        if ((kind1 == ExtendedJavaTypeIdentifier.TYPE_KIND_TOKEN) ||
                (kind2 == ExtendedJavaTypeIdentifier.TYPE_KIND_TOKEN)) {
            return (TypeNode) ExtendedJavaTypeIdentifier.TOKEN_TYPE.clone();
        }

        if ((kind1 == TypeIdentifier.TYPE_KIND_NULL) &&
                (kind2 == TypeIdentifier.TYPE_KIND_NULL)) {
            return NullTypeNode.instance;
        }

        if (_debug) {
            System.out.println("ExtendedJavaTypePolicy."
			       + "arithPromotType(): "
			       + " calling super.arithPromoteType");
	}

        return super.arithPromoteType(type1, type2);
    }

    ///////////////////////////////////////////////////////////////////
    ////                         private variables                 ////

    private final static boolean _debug = false;
}
