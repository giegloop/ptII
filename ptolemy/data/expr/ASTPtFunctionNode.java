/* Generated By:JJTree: Do not edit this line. ASTPtFunctionNode.java */

/* ASTPtFunctionNode represent function nodes in the parse tree

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

@ProposedRating Red (nsmyth@eecs.berkeley.edu)

Created : May 1998

*/

//////////////////////////////////////////////////////////////////////////
//// ASTPtFunctionNode
/**
 * The parse tree created from the expression string consists of a 
 * hierarchy of node objects. This class represents function nodes in 
 * the parse tree. Currently the functions supported are precisely those 
 * in the java.lang.Math package. In the future the ability to deal with
 * other math packages can be added, as well as calls to tcl & matlab.
 * 
 * @author Neil Smyth
 * @version $Id$
 * @see pt.data.parser.ASTPtSimpleNode
 * @see pt.data.parser.PtParser 
 * @see pt.data.Token 
*/

package pt.data.parser;

import pt.kernel.IllegalActionException;
import java.lang.reflect.*;

public class ASTPtFunctionNode extends ASTPtSimpleNode {
    protected String funcName;

    protected pt.data.Token _resolveNode() throws IllegalActionException {
        int args = jjtGetNumChildren();
        Class[] argTypes = new Class[args];
        Double[] argValues = new Double[args];
        try {
            for (int i = 0; i<args; i++) {
                argValues[i] = new Double(((pt.data.DoubleToken)childTokens[i]).doubleValue()); 
                argTypes[i] = argValues[i].TYPE;
            }
            // Note: Java makes a dintinction between the class objects
            // for double & Double...
            Class tmp = Class.forName("java.lang.Math");
            Method m = tmp.getMethod(funcName, argTypes);
            Double result = (Double)m.invoke(tmp, argValues);
            return new pt.data.DoubleToken(result.doubleValue());
        } catch (Exception ex) {
            StringBuffer sb = new StringBuffer();
            for (int i=0; i<args; i++) {
                if (i==0) {
            sb.append(argValues[i].doubleValue());
                } else {
                    sb.append(", " + argValues[i].doubleValue());
                }
            }  
            String str = "Function " + funcName + "(" + sb;
            str = str + ") cannot be executed with given arguments";
            throw new IllegalActionException(str + ": " + ex.getMessage());
        } 
    }


  public ASTPtFunctionNode(int id) {
    super(id);
  }

  public ASTPtFunctionNode(PtParser p, int id) {
    super(p, id);
  }

  public static Node jjtCreate(int id) {
      return new ASTPtFunctionNode(id);
  }

  public static Node jjtCreate(PtParser p, int id) {
      return new ASTPtFunctionNode(p, id);
  }
}
