/*

A C code generator for generating the c file containing the wrapper "main"
method. This simply does some initialization and calls the main method of
the class.

Copyright (c) 2002-2003 The University of Maryland.
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

                                        PT_COPYRIGHT_VERSION_2
                                        COPYRIGHTENDKEY

@ProposedRating Red (ankush@eng.umd.edu)
@AcceptedRating Red (ankush@eng.umd.edu)
*/

package ptolemy.copernicus.c;

import soot.SootClass;
import soot.SootMethod;

import java.util.Iterator;

/** A C code generator for generating the c file containing the wrapper "main"
    method. This simply does some initialization and calls the main method of
    the appropriate class, if such a method exists.

    @author Ankush Varma
    @version $Id$
    @since Ptolemy II 2.0
*/

public class MainFileGenerator extends CodeGenerator {

    /** Default constructor.
     */
    public MainFileGenerator() {
        super();
    }
    /////////////////////////////////////////////////////
    //////////        public methods      ///////////////

    /** Generate the code for a the wrapper "main" C function that calls
     *  the main method of the appropriate class. It also generates other
     *  functions required for correct initialization.
     *  @param source The main class.
     */
    public String generate(SootClass source) {
        // Initialize the structures.
        StringBuffer headerCode = new StringBuffer();
        StringBuffer bodyCode   = new StringBuffer();
        StringBuffer footerCode = new StringBuffer();



        boolean mainExists = source.declaresMethodByName("main");

        // Generate the head of the file, include the "#include"
        // preprocessor directives.
        headerCode.append("/* Automatically generated by the"
                + "Ptolemy C Code Generator. */\n\n");
        headerCode.append("/*Required include files. */\n");
        headerCode.append("#include <stdlib.h>\n");
        headerCode.append("#include <setjmp.h>\n");
        headerCode.append("#include \"name_defs.h\"\n");
        headerCode.append("#include \"strings.h\"\n");
        headerCode.append("#include \""
                + CNames.classNameToFileName(source.getName())
                + ".h\"\n");

        Iterator requiredClasses = RequiredFileGenerator
                .getStrictlyRequiredClasses().iterator();

        // Call the Class Structure Initializations for all required
        // classes.
        headerCode.append("\n");
        while (requiredClasses.hasNext()){
            SootClass nextClass = (SootClass)requiredClasses.next();
            headerCode.append("#include \""
                    + CNames.includeFileNameOf(nextClass)
                    + "\"\n");
        }
        headerCode.append("\n");

        headerCode.append("#ifndef A_DEF_iA1_i1195259493_String\n"
                + "#define A_DEF_iA1_i1195259493_String\n"
                + "typedef PCCG_ARRAY_INSTANCE_PTR iA1_i1195259493_String;\n"
                + "#endif\n");

        // Forward declaration of methods.
        headerCode.append("void classStructInit();\n");
        headerCode.append("void staticInit();\n");
        headerCode.append("iA1_i1195259493_String "
            + "commandLineArgs(int, char**);\n");

        headerCode.append("\nvoid _INITIALIZE_SYSTEM_CLASS();\n");

        // Call the actual main method of the class, if it exists.
        if (mainExists){
            bodyCode.append(_generateMain(source) + "\n");
            bodyCode.append(_generateClassStructInit() + "\n");
            bodyCode.append(_generateStaticInit() + "\n");
            bodyCode.append(_generateCommandLineArgs() + "\n");
        }

        return (headerCode.append(bodyCode.append(footerCode))).toString();
    }

    /** Generate a function that initializes the required class structures.
     *
     *  @return The code for the "classStructInit" method that initializes
     *  the class structures.
     */
    private String _generateClassStructInit() {
        StringBuffer code = new StringBuffer();

        // Generate the method header.
        code.append("void classStructInit()\n{\n");

        // Generate the method body
        code.append(_indent(1)
                + "/* Class Structure initializations*/\n");

        Iterator requiredClasses = RequiredFileGenerator
                .getStrictlyRequiredClasses().iterator();

        // Call the Class Structure Initializations for all required
        // classes.
        while (requiredClasses.hasNext()){
            SootClass nextClass = (SootClass)requiredClasses.next();

            code.append("\n" + _indent(1) + "/* " + nextClass.toString()
                        + " */\n");

            code.append(_indent(1) +  CNames.initializerNameOf(nextClass)
                        + "(&" + CNames.classStructureNameOf(nextClass)
                        + ");\n");
        }

        code.append("}\n");
        return code.toString();
    }


    /** Generate the code for a the wrapper "main" C function that calls
     *  the main method of the appropriate class.
     *  @param source The main class.
     *  @return The code for the wrapper main method.
     */
    private String _generateMain(SootClass source) {
        StringBuffer bodyCode = new StringBuffer();
        String instanceName = CNames.instanceNameOf(source);
        // Function that initializes the class structure.
        String structInitFunction = CNames.initializerNameOf(source);
        SootMethod mainMethod = source.getMethodByName("main");

        // Generate the actual C "main" method.
        bodyCode.append("\nint main(int argc, char** argv)\n{\n");

        // Variable declarations.
        bodyCode.append(_indent(1) + instanceName + " instance;\n");

        // Initialize required class structures.
        bodyCode.append("\n" + _indent(1) + "classStructInit();\n");

        // Call static initializers for required classes.
        bodyCode.append(_indent(1) + "staticInit();\n");

        // Initialize java.lang.System()
        bodyCode.append(_indent(1) + "_INITIALIZE_SYSTEM_CLASS();\n");

        // Initialize the instance of the source class.
        bodyCode.append("\n" + _indent(1)
                + "/* Initialize the instance of the main class. */\n");
        bodyCode.append(_indent(1)
                + "instance = malloc(sizeof(instance));\n");
        bodyCode.append(_indent(1) + "instance->class = &"
                + CNames.classStructureNameOf(source) + ";\n");
        bodyCode.append(_indent(1) + structInitFunction
                + "(&" + CNames.classStructureNameOf(source)+ ");\n");

        // Call the main method for the source class with the appropriate
        // parameters.
        bodyCode.append("\n" + _indent(1)
                + "/* Call the function corresponding to the main java "
                + "method. */\n");
        bodyCode.append(_indent(1)
                + CNames.functionNameOf(mainMethod) + "(" );
        // If the method is non-static, put the name of the instance as
        // the first argument.
        if (!mainMethod.isStatic()) {
            bodyCode.append("instance");
            if (mainMethod.getParameterCount() == 1) {
                // Assume that the only possible argument is String[]
                // args.
                bodyCode.append(", commandLineArgs(argc, argv)");
            }
        }
        else {
            if (mainMethod.getParameterCount() == 1) {
                // Assume that the only possible argument is String[]
                // args.
                bodyCode.append("commandLineArgs(argc, argv)");
            }
        }

        bodyCode.append(");\n");
        bodyCode.append(_indent(1) + "return(0);\n");
        bodyCode.append("}\n");

        return bodyCode.toString();
    }

    /** Generate a function that calls the static initialization methods
     * for all required classes.
     *
     * @return The code for the staticInit() method in the main file.
     */
    private String _generateStaticInit() {
        StringBuffer code = new StringBuffer();

        // Generate the method header.
        code.append("void staticInit()\n{\n");

        // Generate the method body
        code.append(_indent(1)
                + "/* Static initialization methods. */\n");

        Iterator requiredClasses = RequiredFileGenerator
                .getStrictlyRequiredClasses().iterator();

        while (requiredClasses.hasNext()){
            // Invoke the static initializer method (clinit) for the class if it
            // exists.
            SootClass nextClass = (SootClass)requiredClasses.next();

            // FIXME: Does not initialize inner classes. Inner Classes have
            // a "$" in their name.
            SootMethod initializer = MethodListGenerator
                    .getClassInitializer(nextClass);

            if ((initializer!= null)
                    &&(nextClass.toString().indexOf("$") == -1)
                    &&(!OverriddenMethodGenerator.isOverridden(initializer))){

                code.append("\n" + _indent(1)
                        + _comment("Static initializer method for "
                                + nextClass.toString()));

                code.append(_indent(1) +
                                CNames.functionNameOf(initializer) + "();\n");
            }
        }

        code.append("}\n");
        return code.toString();
    }

    /** Generates a function that converts the C command-line
     * arguments(argc, argv) into the String array that java wants.
     *
     * @return The code for the commandLineArgs function that makes this
     * conversion.
     */
    private String _generateCommandLineArgs() {
        StringBuffer code = new StringBuffer();

        code.append("iA1_i1195259493_String "
            + "commandLineArgs(int argc, char** argv) {\n");
        code.append(_indent(1) + "int i;\n");
        code.append(_indent(1) + "iA1_i1195259493_String string_array;\n\n");

        // Allocate memory for the string array.
        code.append(_indent(1) + "string_array = "
                +
                "pccg_array_allocate((PCCG_CLASS_PTR)"
                + "malloc(sizeof(PCCG_ARRAY_CLASS)), "
                + "sizeof(_STRING_INSTANCE_STRUCT), "
                + "1, 0, argc - 1);\n");
        // Set the elements of the String array correctly.
        code.append(_indent(1)
                + "for(i = 1; i < argc; i++) {\n");
        code.append(_indent(2)
                + CNames.arrayReferenceFunction
                + "(string_array, _STRING_INSTANCE_STRUCT, i - 1)"
                + " = charArrayToString(argv[i]);\n");
        code.append(_indent(1) + "}\n\n");

        code.append(_indent(1) + "return string_array;\n");
        code.append("}\n");


        return code.toString();
    }
}
