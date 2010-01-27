/*
 * The base class of a property lattice.
 * 
 * Copyright (c) 2007-2010 The Regents of the University of California. All
 * rights reserved. Permission is hereby granted, without written agreement and
 * without license or royalty fees, to use, copy, modify, and distribute this
 * software and its documentation for any purpose, provided that the above
 * copyright notice and the following two paragraphs appear in all copies of
 * this software.
 * 
 * IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY FOR
 * DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES ARISING OUT
 * OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF THE UNIVERSITY OF
 * CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE PROVIDED HEREUNDER IS ON AN
 * "AS IS" BASIS, AND THE UNIVERSITY OF CALIFORNIA HAS NO OBLIGATION TO PROVIDE
 * MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 * 
 * PT_COPYRIGHT_VERSION_2 COPYRIGHTENDKEY
 * 
 */
package ptolemy.data.properties.lattice;

import java.util.HashMap;

import ptolemy.data.properties.Property;
import ptolemy.graph.CPO;
import ptolemy.graph.DirectedAcyclicGraph;
import ptolemy.graph.Node;
import ptolemy.kernel.util.IllegalActionException;

////PropertyLattice

/**
 * The property lattice base class. There are more than one instances of a
 * property lattice. The property lattice is constructed once and then typically
 * does not change during execution.
 * 
 * @author Thomas Mandl, Man-Kit Leung, Edward A. Lee
 * @version $Id$
 * @since Ptolemy II 7.1
 * @Pt.ProposedRating Red (mankit)
 * @Pt.AcceptedRating Red (mankit)
 * @see ptolemy.graph.CPO
 */
public class PropertyLattice extends DirectedAcyclicGraph {

    /**
     * Construct a new property lattice.
     */
    public PropertyLattice() {
    }

    ///////////////////////////////////////////////////////////////////
    ////                         public methods                    ////

    /**
     * Add the specified element to this lattice. The element is assumed to be a
     * LatticeProperty. Otherwise, a runtime exception would occur.
     * @param weight The element.
     * @return The constructed graph node in the lattice.
     */
    public Node addNodeWeight(Object weight) {
        // The weight argument must be Object or else we get test failures
        _propertyMap.put(weight.toString().toUpperCase(),
                (LatticeProperty) weight);
        return super.addNodeWeight(weight);
    }

    /**
     * Add structured properties. The parameter flag is a bit-wise OR union of
     * the structured properties desired to add. The class provides a set of
     * symbolic constants for these structured property types. For example,
     * invoke addStructuredProperties(RECORD) to add the RecordProperty. This
     * method should be called after all base elements have been added. The user
     * is responsible for ensuring a lattice structure before calling this
     * method. The lattice structure should be preserved after calling this
     * method, so there is no need to do another check.
     * @param structuredPropertiesToAdd The bit-wise OR union of the structured
     * properties desired to add.
     * @exception IllegalActionException
     */
    public void addStructuredProperties(int structuredPropertiesToAdd)
            throws IllegalActionException {

        if ((structuredPropertiesToAdd & RECORD) != 0) {
            Property record = new RecordProperty(this, new String[0],
                    new LatticeProperty[0]).getRepresentative();

            if (!isLattice()) {
                throw new IllegalActionException(
                        "This ontology needs to be a lattice "
                                + "before adding structured types.");
            }

            Object bottom = bottom();
            Object top = top();

            addNodeWeight(record);
            addEdge(bottom, record);
            addEdge(record, top);
        }

        if ((structuredPropertiesToAdd & ARRAY) != 0) {
            // FIXME: add Array structure.
        }
    }

    /**
     * Compare two properties in the property lattice. The arguments must be
     * instances of Property, otherwise an exception will be thrown. This method
     * returns one of ptolemy.graph.CPO.LOWER, ptolemy.graph.CPO.SAME,
     * ptolemy.graph.CPO.HIGHER, ptolemy.graph.CPO.INCOMPARABLE, indicating the
     * first argument is lower than, equal to, higher than, or incomparable with
     * the second argument in the property hierarchy, respectively.
     * @param t1 an instance of Property.
     * @param t2 an instance of Property.
     * @return An integer.
     * @exception IllegalArgumentException If one or both arguments are not
     * instances of Property.
     */
    public int compare(Object t1, Object t2) {
        synchronized (PropertyLattice.class) {
            if (!(t1 instanceof Property) || !(t2 instanceof Property)) {
                throw new IllegalArgumentException("PropertyLattice.compare: "
                        + "Arguments are not instances of Property: "
                        + " property1 = " + t1 + ", property2 = " + t2);
            }

            Property t1Rep = _toRepresentative((Property) t1);
            Property t2Rep = _toRepresentative((Property) t2);

            if (t1Rep.equals(t2Rep) && t1Rep instanceof StructuredProperty) {
                return ((StructuredProperty) t1)
                        ._compare((StructuredProperty) t2);
            }

            return super.compare(t1Rep, t2Rep);
        }
    }

    /**
     * Throw an exception. This operation is not supported since we don't need
     * it.
     * @exception UnsupportedOperationException Always thrown.
     */

    public Object[] downSet(Object e) {
        throw new UnsupportedOperationException(
                "PropertyLattice.downSet(): operation not supported for "
                        + "the property lattice.");
    }

    /**
     * Return the element associated with the specified name.
     * @param elementName The specified name.
     * @return The element.
     * @exception IllegalActionException Thrown if this lattice does not contain
     * the element with the specified name.
     */
    public LatticeProperty getElement(String elementName)
            throws IllegalActionException {

        LatticeProperty property = _propertyMap.get(elementName.toUpperCase());
        if (property == null) {
            throw new IllegalActionException("No lattice element named \""
                    + elementName + "\".");
        }
        return property;
    }

    /**
     * Return the name of this lattice.
     * @return The name of this lattice, which is the name of the package.
     */
    public String getName() {
        return toString();
    }

    /**
     * Return the greatest property of a set of properties, or null if the
     * greatest one does not exist.
     * @param subset an array of properties.
     * @return A Property or null.
     */

    public Object greatestElement(Object[] subset) {
        synchronized (PropertyLattice.class) {
            // Compare each element with all of the other elements to search
            // for the greatest one. This is a simple, brute force algorithm,
            // but may be inefficient. A more efficient one is used in
            // the graph package, but more complex.
            for (Object element : subset) {
                boolean isGreatest = true;

                for (Object element2 : subset) {
                    int result = compare(element, element2);

                    if (result == CPO.LOWER || result == CPO.INCOMPARABLE) {
                        isGreatest = false;
                        break;
                    }
                }

                if (isGreatest == true) {
                    return element;
                }
            }
            // FIXME: Shouldn't this return GENERAL?
            return null;
        }
    }

    /**
     * Return the greatest lower bound of two properties.
     * @param t1 an instance of Property.
     * @param t2 an instance of Property.
     * @return an instance of Property.
     * @exception IllegalArgumentException If one or both of the specified
     * arguments are not instances of Property.
     */

    public Object greatestLowerBound(Object t1, Object t2) {
        synchronized (PropertyLattice.class) {
            if (t1 == null || t2 == null) {
                return null;
            }

            if (!(t1 instanceof Property) || !(t2 instanceof Property)) {
                throw new IllegalArgumentException(
                        "PropertyLattice.greatestLowerBound: "
                                + "Arguments are not instances of Property.");
            }

            Property t1Rep = _toRepresentative((Property) t1);
            Property t2Rep = _toRepresentative((Property) t2);

            if (t1Rep.equals(t2Rep) && t1Rep instanceof StructuredProperty) {
                return ((StructuredProperty) t1)
                        ._greatestLowerBound((StructuredProperty) t2);
            }

            if (!super.containsNodeWeight(t1)) {
                throw new IllegalArgumentException(
                        "PropertyLattice does not contain " + t1);
            }
            if (!super.containsNodeWeight(t2)) {
                throw new IllegalArgumentException(
                        "PropertyLattice does not contain " + t2);
            }
            int relation = super.compare(t1Rep, t2Rep);
            if (relation == SAME) {
                return t1;
            } else if (relation == LOWER) {
                return t1;
            } else if (relation == HIGHER) {
                return t2;
            } else { // INCOMPARABLE
                // FIXME: this case shouldn't happen, right?
                return super.greatestLowerBound(t1, t2);
            }
        }
    }

    /**
     * Return the greatest lower bound of a subset.
     * @param subset an array of properties.
     * @return an instance of Property.
     */

    public Object greatestLowerBound(Object[] subset) {
        throw new UnsupportedOperationException(
                "PropertyLattice.greatestUpperBound(Object[]) :"
                        + " operation not supported for the property lattice.");
    }

    /**
     * Return the greatest lower bound of the two given properties.
     * @param property1 The first given property.
     * @param property2 The second given property.
     * @return The greatest lower bound of property1 and property2.
     */
    public synchronized Property greatestLowerBound(Property property1,
            Property property2) {
        return (Property) super.greatestLowerBound(property1, property2);
    }

    /**
     * Return true.
     * @return true.
     */
    public boolean isLattice() {
        return true;
    }

    /**
     * Return the least property of a set of properties, or null if the least
     * one does not exist.
     * @param subset an array of properties.
     * @return A Property or null.
     */
    public Object leastElement(Object[] subset) {
        synchronized (PropertyLattice.class) {
            // Compare each element with all of the other elements to search
            // for the least one. This is a simple, brute force algorithm,
            // but may be inefficient. A more efficient one is used in
            // the graph package, but more complex.
            for (Object element : subset) {
                boolean isLeast = true;

                for (Object element2 : subset) {
                    int result = compare(element, element2);

                    if (result == CPO.HIGHER || result == CPO.INCOMPARABLE) {
                        isLeast = false;
                        break;
                    }
                }

                if (isLeast == true) {
                    return element;
                }
            }
            // FIXME: Shouldn't this return bottom?
            return null;
        }
    }

    /**
     * Return the least upper bound of two properties.
     * @param t1 an instance of Property.
     * @param t2 an instance of Property.
     * @return an instance of Property.
     */
    public Object leastUpperBound(Object t1, Object t2) {
        synchronized (PropertyLattice.class) {
            if (t1 == null) {
                return t2;
            }
            if (t2 == null) {
                return t1;
            }

            if (!(t1 instanceof Property) || !(t2 instanceof Property)) {
                throw new IllegalArgumentException(
                        "PropertyLattice.leastUpperBound: "
                                + "Arguments are not instances of Property.");
            }

            Property t1Rep = _toRepresentative((Property) t1);
            Property t2Rep = _toRepresentative((Property) t2);

            if (t1Rep.equals(t2Rep) && t1Rep instanceof StructuredProperty) {
                return ((StructuredProperty) t1)
                        ._leastUpperBound((StructuredProperty) t2);
            }

            // Both are neither the same structured property, nor an array
            // and non-array pair, so their property relation is defined
            // by the basic lattice.
            int relation = super.compare(t1Rep, t2Rep);

            if (relation == SAME) {
                return t1;
            } else if (relation == LOWER) {
                return t2;
            } else if (relation == HIGHER) {
                return t1;
            } else { // INCOMPARABLE
                // FIXME: this case shouldn't happen, right?
                return super.leastUpperBound(t1, t2);
            }
        }
    }

    /**
     * Return the least upper bound of a subset.
     * @param subset an array of properties.
     * @return an instance of Property.
     */
    public Object leastUpperBound(Object[] subset) {
        throw new UnsupportedOperationException(
                "PropertyLattice.leastUpperBound(Object[]) :"
                        + " operation not supported for the property lattice.");
    }

    /**
     * Return the least upper bound of the two given properties.
     * @param property1 The first given property.
     * @param property2 The second given property.
     * @return The least upper bound of property1 and property2.
     */
    public synchronized Property leastUpperBound(Property property1,
            Property property2) {
        return (Property) leastUpperBound((Object) property1,
                (Object) property2);
    }

    /**
     * Throw an exception. This operation is not supported since the property
     * lattice is infinite, this operation is not supported.
     * @exception UnsupportedOperationException Always thrown.
     */
    public Object[] upSet(Object e) {
        throw new UnsupportedOperationException(
                "PropertyLattice.upSet(): operation not supported for "
                        + "the property lattice.");
    }

    /**
     * Return the property lattice described by the given lattice description
     * file. If the lattice was not created already, a new property lattice is
     * instantiated.
     * @param latticeName The given lattice name.
     * @return The property lattice described by the given file.
     */
    public static PropertyLattice getPropertyLattice(String latticeName) {

        // Check whether the lattice is included in the model.
        if (latticeName.startsWith(PropertyConstraintSolver._USER_DEFINED_LATTICE)) {
            // In this case, we don't want to look
            // in the predefined lattices.
            latticeName = latticeName.replace(
                    PropertyConstraintSolver._USER_DEFINED_LATTICE, "");
            return _lattices.get(latticeName);
        }

        if (!_lattices.containsKey(latticeName)) {

            try {

                Class latticeClass = Class
                        .forName("ptolemy.data.properties.lattice."
                                + latticeName + ".Lattice");

                // Create a new instance of PropertyLattice.
                PropertyLattice newLattice = (PropertyLattice) latticeClass
                        .getConstructor(new Class[0])
                        .newInstance(new Object[0]);

                _lattices.put(latticeName, newLattice);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        PropertyLattice lattice = _lattices.get(latticeName);

        return lattice;
    }

    /**
     * Globally reset all lattices in the model. After calling this method,
     * future calls to {@link #getPropertyLattice(String)} would have no memory
     * of previously created lattices, and thus instantiate new instances.
     */
    public static void resetAll() {
        _lattices.clear();
    }

    /**
     * Store the specified lattice with the specified name.
     * {@link #getPropertyLattice(String)} first searches from the set of stored
     * lattices before instantiating a new instant.
     */
    public static void storeLattice(PropertyLattice lattice, String name) {
        _lattices.put(name, lattice);
    }

    /** Return the name of the package that contains this class.
     *  @return the name of the package that contains this class.
     */
    public String toString() {
         String name = getClass().getPackage().getName();
         return name.substring(name.lastIndexOf('.') + 1);
    }

    ///////////////////////////////////////////////////////////////////
    ////                          public fields                    ////

    /**
     * Public symbolic constant value for the RecordProperty.
     */
    public final int RECORD = 0x1;

    /**
     * Public symbolic constant value for the ArrayProperty.
     */
    public final int ARRAY = 0x10;

    ///////////////////////////////////////////////////////////////////
    ////                         private methods                   ////

    // If the argument is a structured type, return its representative;
    // otherwise, return the argument. In the latter case, the argument
    // is either a base type or a user defined type that is not a
    // structured type.
    private Property _toRepresentative(Property p) {
        if (p instanceof StructuredProperty) {
            return ((StructuredProperty) p).getRepresentative();
        } else {
            return p;
        }
    }

    ///////////////////////////////////////////////////////////////////
    ////                         private fields                    ////

    private final HashMap<String, LatticeProperty> _propertyMap = new HashMap<String, LatticeProperty>();

    /**
     * A HashMap that contains all property lattices with unique lattice files
     * as keys.
     */
    private static HashMap<String, PropertyLattice> _lattices = new HashMap<String, PropertyLattice>();

}
