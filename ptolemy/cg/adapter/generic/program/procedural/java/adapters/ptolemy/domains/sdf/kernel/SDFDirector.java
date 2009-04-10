/* Code generator adapter class associated with the SDFDirector class.

 Copyright (c) 2005-2006 The Regents of the University of California.
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
package ptolemy.cg.adapter.generic.program.procedural.java.adapters.ptolemy.domains.sdf.kernel;

import java.util.Iterator;

import ptolemy.actor.Actor;
import ptolemy.actor.CompositeActor;
import ptolemy.actor.IOPort;
import ptolemy.actor.TypedIOPort;
import ptolemy.actor.util.DFUtilities;
import ptolemy.cg.adapter.generic.program.procedural.adapters.ptolemy.actor.TypedCompositeActor;
import ptolemy.cg.kernel.generic.CodeGeneratorAdapterStrategy;
import ptolemy.cg.kernel.generic.CodeStream;
import ptolemy.cg.kernel.generic.GenericCodeGenerator;
import ptolemy.cg.kernel.generic.program.procedural.java.JavaCodeGenerator;
import ptolemy.cg.lib.CompiledCompositeActor;
import ptolemy.data.BooleanToken;
import ptolemy.data.type.BaseType;
import ptolemy.data.type.Type;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.InternalErrorException;

//////////////////////////////////////////////////////////////////
////SDFDirector

/**
 Code generator adapter associated with the SDFDirector class. This class
 is also associated with a code generator.

 @author Ye Zhou, Gang Zhou
 @version $Id$
 @since Ptolemy II 7.1
 @Pt.ProposedRating Yellow (zgang)
 @Pt.AcceptedRating Red (eal)
 */
public class SDFDirector extends ptolemy.cg.adapter.generic.program.procedural.adapters.ptolemy.domains.sdf.kernel.SDFDirector {

    /** Construct the code generator adapter associated with the given
     *  SDFDirector.
     *  @param sdfDirector The associated
     *  ptolemy.domains.sdf.kernel.SDFDirector
     */
    public SDFDirector(ptolemy.domains.sdf.kernel.SDFDirector sdfDirector) {
        super(sdfDirector);
    }

    ////////////////////////////////////////////////////////////////////////
    ////                         public methods                         ////


    /** Generate input variable declarations.
     *  @return a String that declares input variables.
     *  @exception IllegalActionException If thrown while
     *  getting port information.
     */
    @Override
    public String generateInputVariableDeclaration()
        throws IllegalActionException {

        StringBuffer code = new StringBuffer();

        Iterator<?> inputPorts = ((Actor) getComponent().getContainer()).inputPortList()
        .iterator();

        while (inputPorts.hasNext()) {
            TypedIOPort inputPort = (TypedIOPort) inputPorts.next();

            if (!inputPort.isOutsideConnected()) {
                continue;
            }

            _portVariableDeclaration(code, inputPort);
        }

        return code.toString();
    }

    /** Generate output variable declarations.
     *  @return a String that declares output variables.
     *  @exception IllegalActionException If thrown while
     *  getting port information.
     */
    @Override
    public String generateOutputVariableDeclaration()
    throws IllegalActionException {
        StringBuffer code = new StringBuffer();

        Iterator<?> outputPorts = ((Actor) getComponent().getContainer()).outputPortList()
        .iterator();

        while (outputPorts.hasNext()) {
            TypedIOPort outputPort = (TypedIOPort) outputPorts.next();

            // If either the output port is a dangling port or
            // the output port has inside receivers.
            if (!outputPort.isOutsideConnected() || outputPort.isInsideConnected()) {
                _portVariableDeclaration(code, outputPort);
            }
        }

        return code.toString();
    }


    /** Generate the preinitialize code for this director.
     *  The preinitialize code for the director is generated by appending
     *  the preinitialize code for each actor.
     *  @return The generated preinitialize code.
     *  @exception IllegalActionException If getting the adapter fails,
     *   or if generating the preinitialize code for a adapter fails,
     *   or if there is a problem getting the buffer size of a port.
     */
    public String generatePreinitializeCode() throws IllegalActionException {
        StringBuffer code = new StringBuffer();
        code.append(super.generatePreinitializeCode());

        _updatePortBufferSize();
        _portNumber = 0;

        return code.toString();
    }

    /** Generate code for transferring enough tokens to complete an internal
     *  iteration.
     *  @param inputPort The port to transfer tokens.
     *  @param code The string buffer that the generated code is appended to.
     *  @exception IllegalActionException If thrown while transferring tokens.
     */
    public void generateTransferInputsCode(IOPort inputPort, StringBuffer code)
            throws IllegalActionException {
        code.append(CodeStream.indent(getCodeGenerator().comment("SDFDirector: "
                + "Transfer tokens to the inside.")));
        int rate = DFUtilities.getTokenConsumptionRate(inputPort);

        CompositeActor container = (CompositeActor) getComponent().getContainer();
        TypedCompositeActor compositeActorAdapter = (TypedCompositeActor) getCodeGenerator().getAdapter(container);

        if (container instanceof CompiledCompositeActor
                && ((BooleanToken) getCodeGenerator().generateEmbeddedCode.getToken())
                        .booleanValue()) {

            // FindBugs wants this instanceof check.
            if (!(inputPort instanceof TypedIOPort)) {
                throw new InternalErrorException(inputPort, null,
                        " is not an instance of TypedIOPort.");
            }
            Type type = ((TypedIOPort) inputPort).getType();
            String portName = inputPort.getName();

            for (int i = 0; i < inputPort.getWidth(); i++) {
                if (i < inputPort.getWidthInside()) {

                    String tokensFromOneChannel = "tokensFromOneChannelOf"
                            + portName + i;

                    if (type == BaseType.INT) {
                        code.append("int[] "
                                + tokensFromOneChannel
                                + " = (int[])(" + portName + "[" + String.valueOf(i)
                                + "]);" + _eol);
                    } else if (type == BaseType.DOUBLE) {
                        code.append("double[] "
                                + tokensFromOneChannel
                                + " = (double[])" + portName + "[" + String.valueOf(i)
                                + "];" + _eol);
                    } else if (type == BaseType.BOOLEAN) {
                        code.append("boolean[] "
                                + tokensFromOneChannel
                                + " = (boolean[])" + portName + "[" + String.valueOf(i)
                                + "];" + _eol);
                    } else {
                        // FIXME: need to deal with other types
                    }

                    String portNameWithChannelNumber = portName;
                    if (inputPort.isMultiport()) {
                        portNameWithChannelNumber = portName + '#' + i;
                    }
                    for (int k = 0; k < rate; k++) {
                        code.append(compositeActorAdapter.getReference("@"
                                + portNameWithChannelNumber + "," + k));
                        /*if (type == PointerToken.POINTER) {
                            code.append(" = (void *) "
                                    + pointerToTokensFromOneChannel + "[" + k
                                    + "];" + _eol);
                        } else {*/
                            code.append(" = " + tokensFromOneChannel
                                    + "[" + k + "];" + _eol);
                        //}
                    }
                }
            }

        } else {
            for (int i = 0; i < inputPort.getWidth(); i++) {
                if (i < inputPort.getWidthInside()) {
                    String name = inputPort.getName();

                    if (inputPort.isMultiport()) {
                        name = name + '#' + i;
                    }

                    for (int k = 0; k < rate; k++) {
                        code.append(compositeActorAdapter.getReference(
                                "@" + name + "," + k));
                        code.append(" = " + _eol);
                        code.append(compositeActorAdapter.getReference(
                                name + "," + k));
                        code.append(";" + _eol);
                    }
                }
            }
        }

        // Generate the type conversion code before fire code.
        code.append(compositeActorAdapter.generateTypeConvertFireCode(true));

        // The offset of the input port itself is updated by outside director.
        _updateConnectedPortsOffset(inputPort, code, rate);
    }


    /** Generate code for transferring enough tokens to fulfill the output
     *  production rate.
     *  @param outputPort The port to transfer tokens.
     *  @param code The string buffer that the generated code is appended to.
     *  @exception IllegalActionException If thrown while transferring tokens.
     */
    public void generateTransferOutputsCode(IOPort outputPort, StringBuffer code)
            throws IllegalActionException {
        code.append(CodeStream.indent(getCodeGenerator().comment("SDFDirector: "
                + "Transfer tokens to the outside.")));

        int rate = DFUtilities.getTokenProductionRate(outputPort);

        CompositeActor container = (CompositeActor) getComponent()
                .getContainer();
        TypedCompositeActor compositeActorAdapter = (TypedCompositeActor) getCodeGenerator().getAdapter(container);

        if (container instanceof CompiledCompositeActor
                && ((BooleanToken) getCodeGenerator().generateEmbeddedCode.getToken())
                        .booleanValue()) {

            if (_portNumber == 0) {
                int numberOfOutputPorts = container.outputPortList().size();

                code.append("Object[] tokensToAllOutputPorts = "
                        + " new Object["
                        + String.valueOf(numberOfOutputPorts) + "];" + _eol);
            }

            String portName = outputPort.getName();
            String tokensToThisPort = "tokensTo" + portName;

            // FindBugs wants this instanceof check.
            if (!(outputPort instanceof TypedIOPort)) {
                throw new InternalErrorException(outputPort, null,
                        " is not an instance of TypedIOPort.");
            }

            Type type = ((TypedIOPort) outputPort).getType();

            int numberOfChannels = outputPort.getWidthInside();

            // Find construct correct array type.
            if (type == BaseType.INT) {
                code.append("int[][] " + tokensToThisPort + " ="
                        + " new int[ "+ String.valueOf(numberOfChannels) + "][" + rate + "];" + _eol);

            } else if (type == BaseType.DOUBLE) {
                code.append("double[][] " + tokensToThisPort + " ="
                        + " new double[ "+ String.valueOf(numberOfChannels) + "][" + rate + "];" + _eol);
            } else if (type == BaseType.BOOLEAN) {
                code.append("boolean[][] " + tokensToThisPort + " ="
                        + " new boolean[ "+ String.valueOf(numberOfChannels) + "][" + rate + "];" + _eol);

            } else {
                // FIXME: need to deal with other types
            }
            for (int i = 0; i < outputPort.getWidthInside(); i++) {
                String portNameWithChannelNumber = portName;
                if (outputPort.isMultiport()) {
                    portNameWithChannelNumber = portName + '#' + i;
                }

                for (int k = 0; k < rate; k++) {
                    String portReference = compositeActorAdapter
                            .getReference("@" + portNameWithChannelNumber + ","
                                    + k);
                    /*if (type == PointerToken.POINTER) {
                        code.append(tokensToOneChannel + "[" + k
                                + "] = " + "(int) " + portReference + ";"
                                + _eol);
                    } else {*/
                        code.append(tokensToThisPort + "[" + i + "][" + k
                                + "] = " + portReference + ";" + _eol);
                    //}
                }
            }
            code.append("tokensToAllOutputPorts [" + String.valueOf(_portNumber) + "] = "
                    + tokensToThisPort + ";" + _eol);

            _portNumber++;

        } else {
            for (int i = 0; i < outputPort.getWidthInside(); i++) {
                if (i < outputPort.getWidth()) {
                    String name = outputPort.getName();

                    if (outputPort.isMultiport()) {
                        name = name + '#' + i;
                    }

                    for (int k = 0; k < rate; k++) {
                        code.append(CodeStream.indent(compositeActorAdapter
                                .getReference(name + "," + k)));
                        code.append(" =" + _eol);
                        code.append(CodeStream.indent(compositeActorAdapter.getReference("@" + name
                                        + "," + k)));
                        code.append(";" + _eol);
                    }
                }
            }
        }

        // The offset of the ports connected to the output port is
        // updated by outside director.
        _updatePortOffset(outputPort, code, rate);
    }

    /** Get the code generator associated with this adapter class.
     *  @return The code generator associated with this adapter class.
     *  @see #setCodeGenerator(GenericCodeGenerator)
     */
    public JavaCodeGenerator getCodeGenerator() {
        return (JavaCodeGenerator) super.getCodeGenerator();
    }

    ////////////////////////////////////////////////////////////////////////
    ////                         protected methods                      ////


    ////////////////////////////////////////////////////////////////////////
    ////                         private methods                      ////


    private void _portVariableDeclaration(StringBuffer code, TypedIOPort port)
    throws IllegalActionException {

        code.append("static " + targetType(port.getType()) + " "
                + CodeGeneratorAdapterStrategy.generateName(port));

        int bufferSize = _ports.getBufferSize(port);

        if (port.isMultiport()) {
            code.append("[]");
            if (bufferSize > 1) {
                code.append("[]");
            }
            code.append(" = new " + targetType(port.getType()));
        } else {
            if (bufferSize > 1) {
                code.append("[]");
                code.append(" = new " + targetType(port.getType()));
            } else {
                //code.append(" = ");
            }
        }

        if (port.isMultiport()) {
            code.append("[" + port.getWidth() + "]");
        }

        if (bufferSize > 1) {
            code.append("[" + bufferSize + "]");
        } else {
            //code.append("0");
        }
        code.append(";" + _eol);
    }

    ////////////////////////////////////////////////////////////////////////
    ////                         private methods                        ////

    private int _portNumber = 0;

}
