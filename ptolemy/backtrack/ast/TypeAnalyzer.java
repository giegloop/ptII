/* AST type analyzer.

Copyright (c) 1997-2004 The Regents of the University of California.
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

package ptolemy.backtrack.ast;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import net.sourceforge.jrefactory.ast.ASTActualTypeArgument;
import net.sourceforge.jrefactory.ast.ASTAdditiveExpression;
import net.sourceforge.jrefactory.ast.ASTAllocationExpression;
import net.sourceforge.jrefactory.ast.ASTAndExpression;
import net.sourceforge.jrefactory.ast.ASTArgumentList;
import net.sourceforge.jrefactory.ast.ASTArguments;
import net.sourceforge.jrefactory.ast.ASTArrayDimsAndInits;
import net.sourceforge.jrefactory.ast.ASTAssignmentOperator;
import net.sourceforge.jrefactory.ast.ASTBlock;
import net.sourceforge.jrefactory.ast.ASTBooleanLiteral;
import net.sourceforge.jrefactory.ast.ASTCastExpression;
import net.sourceforge.jrefactory.ast.ASTClassBody;
import net.sourceforge.jrefactory.ast.ASTClassOrInterfaceType;
import net.sourceforge.jrefactory.ast.ASTCompilationUnit;
import net.sourceforge.jrefactory.ast.ASTConditionalAndExpression;
import net.sourceforge.jrefactory.ast.ASTConditionalExpression;
import net.sourceforge.jrefactory.ast.ASTConditionalOrExpression;
import net.sourceforge.jrefactory.ast.ASTConstructorDeclaration;
import net.sourceforge.jrefactory.ast.ASTEqualityExpression;
import net.sourceforge.jrefactory.ast.ASTExclusiveOrExpression;
import net.sourceforge.jrefactory.ast.ASTExpression;
import net.sourceforge.jrefactory.ast.ASTFieldDeclaration;
import net.sourceforge.jrefactory.ast.ASTFormalParameter;
import net.sourceforge.jrefactory.ast.ASTIdentifier;
import net.sourceforge.jrefactory.ast.ASTImportDeclaration;
import net.sourceforge.jrefactory.ast.ASTInclusiveOrExpression;
import net.sourceforge.jrefactory.ast.ASTInstanceOfExpression;
import net.sourceforge.jrefactory.ast.ASTLiteral;
import net.sourceforge.jrefactory.ast.ASTLocalVariableDeclaration;
import net.sourceforge.jrefactory.ast.ASTMethodDeclaration;
import net.sourceforge.jrefactory.ast.ASTMultiplicativeExpression;
import net.sourceforge.jrefactory.ast.ASTName;
import net.sourceforge.jrefactory.ast.ASTNullLiteral;
import net.sourceforge.jrefactory.ast.ASTPackageDeclaration;
import net.sourceforge.jrefactory.ast.ASTPostfixExpression;
import net.sourceforge.jrefactory.ast.ASTPreDecrementExpression;
import net.sourceforge.jrefactory.ast.ASTPreIncrementExpression;
import net.sourceforge.jrefactory.ast.ASTPrimaryExpression;
import net.sourceforge.jrefactory.ast.ASTPrimaryPrefix;
import net.sourceforge.jrefactory.ast.ASTPrimarySuffix;
import net.sourceforge.jrefactory.ast.ASTPrimitiveType;
import net.sourceforge.jrefactory.ast.ASTReferenceType;
import net.sourceforge.jrefactory.ast.ASTReferenceTypeList;
import net.sourceforge.jrefactory.ast.ASTRelationalExpression;
import net.sourceforge.jrefactory.ast.ASTResultType;
import net.sourceforge.jrefactory.ast.ASTShiftExpression;
import net.sourceforge.jrefactory.ast.ASTStatementExpression;
import net.sourceforge.jrefactory.ast.ASTType;
import net.sourceforge.jrefactory.ast.ASTTypeArguments;
import net.sourceforge.jrefactory.ast.ASTUnaryExpression;
import net.sourceforge.jrefactory.ast.ASTUnaryExpressionNotPlusMinus;
import net.sourceforge.jrefactory.ast.ASTUnmodifiedClassDeclaration;
import net.sourceforge.jrefactory.ast.ASTUnmodifiedInterfaceDeclaration;
import net.sourceforge.jrefactory.ast.ASTVariableDeclarator;
import net.sourceforge.jrefactory.ast.ASTVariableDeclaratorId;
import net.sourceforge.jrefactory.ast.ASTVariableInitializer;
import net.sourceforge.jrefactory.ast.Node;
import net.sourceforge.jrefactory.ast.SimpleNode;
import net.sourceforge.jrefactory.parser.JavaParserConstants;

import org.acm.seguin.summary.FileSummary;
import org.acm.seguin.summary.TypeSummary;

//////////////////////////////////////////////////////////////////////////
//// TypeAnalyzer
/**
 *  This class implements type analysis for Java ASTs (Abstract Syntax
 *  Trees) generated by <a href="http://jrefactory.sourceforge.net/"
 *  target="_blank">JRefactory</a>.
 *  <p>
 *  This analyzer does a depth-first traversal on the given AST. It tries
 *  to assign a type to every expression and sub-expression node a type
 *  according to the Java 1.5 semantics (but it is also backward compatible
 *  with Java 1.4). This type information can be used later to decide the
 *  behavior of each method call and field access.
 *  <p>
 *  When other classes are referenced in the AST, this analyzer tries to
 *  load them with the Java reflection mechanism. Once loaded, their names
 *  are assigned to the corresponding nodes as types. This analyzer uses
 *  {@link LocalClassLoader} to load those classes from the viewpoint of
 *  the class being analyzed.
 *  <p>
 *  This analyzer assumes that the classes being analyzed (including those
 *  classes indirectly referred to) have been compiled with the Java
 *  compiler. It does not check for type errors in the AST, but it tries to
 *  mimic the typing behavior of the Java compiler.
 *  <p>
 *  This class overrides methods from its superclass {@link ASTVisitor}.
 *  Each of those methods handles a special kind of nodes in ASTs. Each
 *  kind of nodes corresponds to a single <em>nonterminal</em> symbol in
 *  the Java 1.5 grammar. Part of the grammar is included in the method
 *  documentation where appropriate.
 * 
 *  @author Thomas Feng
 *  @version $Id$
 *  @since Ptolemy II 4.1
 *  @Pt.ProposedRating Red (tfeng)
 *  @see LocalClassLoader
 */
public class TypeAnalyzer extends ASTVisitor {

    ///////////////////////////////////////////////////////////////////
    ////                        constructors                       ////

    /** Construct an analyzer with no explicit class path for its
     *  class loader (an instanceof {@link LocalClassLoader}). Such
     *  a class loader cannot resolve classes other than Java
     *  built-in classes.
     */
    public TypeAnalyzer() {
        this(null);
    }
    
    /** Construct an analyzer with with an array of explicit class
     *  paths for its class loader (an instanceof {@link
     *  LocalClassLoader}).
     */
    public TypeAnalyzer(String[] classPaths) {
        _loader = new LocalClassLoader(classPaths);
    }
    
    ///////////////////////////////////////////////////////////////////
    ////                       public methods                      ////
    
    /** Visit a <tt>CompilationUnit</tt> node in the AST, and compute
     *  the summary for the node. A <tt>CompilationUnit</tt> is the
     *  root node of an AST tree. The summary contains all the
     *  definitions in the AST, including all the types, and fields
     *  and methods in those types.
     *  <p>
     *  Problems of anonymous class numbering in JRefactory is fixed
     *  in this function by one extra traversal on the summary and
     *  recounting anonymous classes. 
     *  <p>
     *  Grammar: <tt>CompilationUnit ::= [PackageDeclaration] ImportDeclaration*
     *    TypeDeclaration* &lt;EOF&gt;</tt>
     *    
     *  @param node The node to be visited.
     *  @param data User-defined data passed from the parent node
     *   (not used for this function).
     *  @return The return value of the superclass.
     */
    public Object visit(ASTCompilationUnit node, Object data) {
        FileSummary summary = ASTQuery.summarize(node, null, _summaryBuilder);
        summary.accept(_anonymousNumbers, null);
        
        return super.visit(node, data);
    }

    /** Visit a <tt>PackageDeclaration</tt> node in the AST, and set
     *  the current package in the class loader to be the package
     *  declared in this node.
     *  <p>
     *  Grammar: <tt>PackageDeclaration ::= "package" Name ";"</tt>
     *  
     *  @param node The node to be visited.
     *  @param data User-defined data passed from the parent node
     *   (not used for this function).
     *  @return Always <tt>null</tt>.
     */
    public Object visit(ASTPackageDeclaration node, Object data) {
        ASTName name = (ASTName)node.jjtGetFirstChild();
        _loader.setCurrentPackage(name.getImage());
        return null;
    }
    
    /** Visit an <tt>ImportDeclaration</tt> node in the AST, and add
     *  the importation to the class loader.
     *  <p>
     *  Grammar: <tt>ImportDeclaration ::= "import" ["static"] Name [".*"] ";"*</tt>
     *  
     *  @param node The node to be visited.
     *  @param data User-defined data passed from the parent node
     *   (not used for this function).
     *  @return Always <tt>null</tt>.
     */
    public Object visit(ASTImportDeclaration node, Object data) {
        ASTName name = (ASTName)node.jjtGetFirstChild();
        if (node.isImportingPackage())
            _loader.importPackage(name.getImage());
        else {
            _loader.importClass(name.getImage());
            _importClass(name.getImage());
        }
        return null;
    }

    /** Visit an <tt>UnmodifiedClassDeclaration</tt> node in the AST,
     *  and set the current class to be this class declaration. A new
     *  scope is opened for field declarations.
     *  <p>
     *  Grammar: <tt>UnmodifiedClassDeclaration ::= "class" &lt;IDENTIFIER&gt;
     *    [TypeParameters] ["extends" ClassOrInterfaceType] ["implements" GenericNameList]
     *    ClassBody</tt>
     *  
     *  @param node The node to be visited.
     *  @param data User-defined data passed from the parent node
     *   (not used for this function).
     *  @return The return value of the superclass.
     */
    public Object visit(ASTUnmodifiedClassDeclaration node, Object data) {
        return _visitClassOrInterface(node, data);
    }
    
    /** Visit an <tt>UnmodifiedInterfaceDeclaration</tt> node in the AST,
     *  and set the current class to be this interface declaration. A new
     *  scope is opened for field declarations.
     *  <p>
     *  Grammar: <tt>UnmodifiedInterfaceDeclaration ::= "interface" &lt;IDENTIFIER&gt;
     *    [TypeParameters] ["extends" ClassOrInterfaceType]
     *    InterfaceBody</tt>
     *  
     *  @param node The node to be visited.
     *  @param data User-defined data passed from the parent node
     *   (not used for this function).
     *  @return The return value of the superclass.
     */
    public Object visit(ASTUnmodifiedInterfaceDeclaration node, Object data) {
        return _visitClassOrInterface(node, data);
    }
    
    /** Visit a <tt>MethodDeclaration</tt> node in the AST, and open
     *  a new scope for parameters.
     *  <p>
     *  Grammar: <tt>MethodDeclaration ::= ("public" | "protected" | "private" | "static" |
     *    "abstract" | "final" | "native" | "strictfp" | "synchronized" | "@" Annotation)*
     *    [TypeParameters] ResultType MethodDeclarator ["throws" NameList] (Block | ";")
     *    ";"*</tt>
     *  
     *  @param node The node to be visited.
     *  @param data User-defined data passed from the parent node
     *   (not used for this function).
     *  @return The return value of the superclass.
     */
    public Object visit(ASTMethodDeclaration node, Object data) {
        // A method declaration starts a new scope.
        _variableStack.push(new Hashtable());
        Object result = super.visit(node, data);
        _variableStack.pop();
        
        for (int i=0; i<node.jjtGetNumChildren(); i++) {
            Node child = node.jjtGetChild(i);
            if (child instanceof ASTResultType) {
                Type.propagateType(node, child);
                break;
            }
        }
        return result;
    }
    
    /** Visit a <tt>ConstructorDeclaration</tt> node in the AST, and open
     *  a new scope for parameters.
     *  <p>
     *  Grammar: <tt>ConstructorDeclaration ::= ("@" Annotation | "public" | "protected" |
     *    "private")* [TypeParameters] &lt;IDENTIFIER&gt; [TypeArguments] FormalParameters
     *    ["throws" NameList] "{" [ExplicitConstructorInvocation] BlockStatement* "}" [";"]</tt>
     *  
     *  @param node The node to be visited.
     *  @param data User-defined data passed from the parent node
     *   (not used for this function).
     *  @return The return value of the superclass.
     */
    public Object visit(ASTConstructorDeclaration node, Object data) {
        // A method declaration starts a new scope.
        _variableStack.push(new Hashtable());
        Object result = super.visit(node, data);
        _variableStack.pop();
        return result;
    }
    
    /** Visit a <tt>FormalParameter</tt> node in the AST, and add
     *  the formal parameter to the last opened scope (must be the
     *  scope created by a <tt>MethodDeclaration</tt> node).
     *  <p>
     *  Grammar: <tt>FormalParameter ::= ("@" Annotation | "final")* Type ["..."]
     *    VariableDeclaratorId</tt>
     *  
     *  @param node The node to be visited.
     *  @param data User-defined data passed from the parent node
     *   (not used for this function).
     *  @return The return value of the superclass.
     */
    public Object visit(ASTFormalParameter node, Object data) {
        Object result = super.visit(node, data);
        _recordVariableDeclaration(node);
        return result;
    }
    
    /** Visit a <tt>FieldDeclaration</tt> node in the AST, and add
     *  the declared field to the last opened scope (must be the
     *  scope created by a <tt>UnmodifiedClassDeclaration</tt> node
     *  or a <tt>UnmodifiedInterfaceDeclaration</tt> node).
     *  <p>
     *  Grammar: <tt>FieldDeclaration ::= ("static" | "transient" | "volatile" |
     *    "strictfp" | "final" | "public" | "protected" | "private" | "@" Annotation)*
     *    Type VariableDeclarator ("," VariableDeclarator)* ";" ";"*</tt>
     *  
     *  @param node The node to be visited.
     *  @param data User-defined data passed from the parent node
     *   (not used for this function).
     *  @return The return value of the superclass.
     */
    public Object visit(ASTFieldDeclaration node, Object data) {
        Object result = super.visit(node, data);
        // _recordVariableDeclaration(node);
        return result;
    }

    /** Visit a <tt>LocalVariableDeclaration</tt> node in the AST, and add
     *  the declared variable to the last opened scope (must be the
     *  scope created by a <tt>Block</tt> node).
     *  <p>
     *  Grammar: <tt>LocalVariableDeclaration ::= ("@" Annotation | "final")* Type
     *    VariableDeclarator ("," VariableDeclarator)*</tt>
     *  
     *  @param node The node to be visited.
     *  @param data User-defined data passed from the parent node
     *   (not used for this function).
     *  @return The return value of the superclass.
     */
    public Object visit(ASTLocalVariableDeclaration node, Object data) {
        Object result = super.visit(node, data);
        _recordVariableDeclaration(node);
        return result;
    }
    
    /** Visit a <tt>ResultType</tt> node in the AST, and set the type
     *  of the node to be its single child (if any).
     *  <p>
     *  Grammar: <tt>ResultType ::= "void" | Type</tt>
     *  
     *  @param node The node to be visited.
     *  @param data User-defined data passed from the parent node
     *   (not used for this function).
     *  @return The return value of the superclass.
     */
    public Object visit(ASTResultType node, Object data) {
        Object result = super.visit(node, data);
        if (node.jjtGetNumChildren() == 0)
            Type.setType(node, Type.NULL);
        else if (node.jjtGetNumChildren() == 1)
            Type.propagateType(node, node.jjtGetFirstChild());
        else
            throw new UnknownASTException();
        return result;
    }
    
    /** Visit a <tt>Block</tt> node in the AST, and open a new
     *  scope for variable declaration.
     *  <p>
     *  Grammar: <tt>Block ::= "{" BlockStatement "}"</tt>
     *  
     *  @param node The node to be visited.
     *  @param data User-defined data passed from the parent node
     *   (not used for this function).
     *  @return The return value of the superclass.
     */
    public Object visit(ASTBlock node, Object data) {
        _variableStack.push(new Hashtable());
        Object result = super.visit(node, data);
        _variableStack.pop();
        return result;
    }
    
    /** Visit a <tt>PrimitiveType</tt> node in the AST, and create
     *  a {@link Type} object for this primitive type.
     *  <p>
     *  Grammar: <tt>PrimitiveType ::= "boolean" | "byte" | "char" | "double" |
     *    "float" | "int" | "long" | "short"</tt>
     *  
     *  @param node The node to be visited.
     *  @param data User-defined data passed from the parent node
     *   (not used for this function).
     *  @return Always <tt>null</tt>
     */
    public Object visit(ASTPrimitiveType node, Object data) {
        Type type = Type.createType(node.getImage());
        Type.setType(node, type);
        return null;
    }
    
    /** Visit a <tt>ReferenceType</tt> node in the AST, and create
     *  a {@link Type} object for this reference type.
     *  <p>
     *  Grammar: <tt>ReferenceType ::= (ClassOrInterfaceType "[]"*) |
     *    (PrimitiveType "[]"*)</tt>
     *  
     *  @param node The node to be visited.
     *  @param data User-defined data passed from the parent node
     *   (not used for this function).
     *  @return Always <tt>null</tt>
     */
    public Object visit(ASTReferenceType node, Object data) {
        if (node.jjtGetNumChildren() == 1) {
            Node child = node.jjtGetFirstChild();
            child.jjtAccept(this, data);
            if (node.getArrayCount() == 0)
                Type.propagateType(node, child);
            else {
                StringBuffer type = new StringBuffer(Type.getType(child).getName());
                for (int i=0; i<node.getArrayCount(); i++)
                    type.append("[]");
                Type.setType(node, Type.createType(type.toString()));
            }
            return null;
        } else
            throw new UnknownASTException();
    }
    
    /** Visit a <tt>ClassOrInterfaceType</tt> node in the AST, and create
     *  a {@link Type} object for this type.
     *  <p>
     *  Grammar: <tt>ClassOrInterfaceType ::= Identifier [TypeArguments] ("." Identifier
     *    [TypeArguments])*</tt>
     *  
     *  @param node The node to be visited.
     *  @param data User-defined data passed from the parent node
     *   (not used for this function).
     *  @return The return value of the superclass.
     */
    public Object visit(ASTClassOrInterfaceType node, Object data)
            throws ASTClassNotFoundException {
        Object result = super.visit(node, data);
        
        // Do not load the actual class.
        String className = node.getImage();
        Class c = _lookupClass(className);
        if (c == null)
            throw new ASTClassNotFoundException(className);
        Type.setType(node, Type.createType(c.getName()));
        
        return result;
    }
    
    /** Visit a <tt>ClassBody</tt> node in the AST, and set the
     *  current class to be this declaration. A new scope is opened
     *  for field declaration. If <tt>data</tt> is not <tt>null</tt>,
     *  it represents the type in an anonymous class instantiation.
     *  <p>
     *  Example: The <tt>data</tt> for the following class body
     *  is considered a {@link Type} object with the name
     *  "<tt>java.util.Hashtable</tt>".
     *  <pre>    Object obj = new java.util.Hashtable() { ... };</pre>
     *  <p>
     *  Grammar: <tt>ClassBody ::= "{" ClassBodyDeclaration "}"</tt>
     *  
     *  @param node The node to be visited.
     *  @param data User-defined data passed from the parent node.
     *   When it is not <tt>null</tt>,
     *   it represents the type in an anonymous class instantiation.
     *  @return The return value of the superclass.
     */
    public Object visit(ASTClassBody node, Object data) {
        if (data != null) { // Anonymous class.
            // Create a new class declaration.
            _previousClasses.push(_currentClass);
            String currentName = _currentClass.getName();
            int dollarPos = currentName.indexOf('$');
            if (dollarPos >= 0)
                currentName = currentName.substring(0, dollarPos);
            TypeSummary summary = _summaryBuilder.getSummary(node);
            String newName = currentName + "$" + _anonymousNumbers.getSummaryNumber(summary);//_anonymousNumbers.next();
            try {
                _currentClass = _loader.searchForClass(newName);
            } catch (ClassNotFoundException e) {
                throw new ASTClassNotFoundException(newName);
            }
            Type.setType(node, Type.createType(_currentClass.getName()));
            _loader.setCurrentClass(_currentClass);
            
            // A class body starts a new scope.
            _variableStack.push(new Hashtable());
            _recordFields();
            Object result = super.visit(node, null);
            _variableStack.pop();
            
            _currentClass = (Class)_previousClasses.pop();
            _loader.setCurrentClass(_currentClass, false);
            return result;
        } else
            return super.visit(node, null);
    }
    
    /** Visit a <tt>PrimaryPrefix</tt> node in the AST, and set
     *  its type depending on the number and types of its children.
     *  <p>
     *  If <tt>data</tt> is not null, it is an Object[2] tuple with
     *  the type of the previous node as its first element and the
     *  arguments as the second.
     *  <p>
     *  Grammar: <tt>PrimaryPrefix ::= Literal | "this" | (&lt;IDENTIFIER&gt; ".")*
     *    "super" "." &lt;IDENTIFIER&gt; | "(" Expression ")" | AllocationExpression |
     *    ResultType ".class"</tt>
     *  
     *  @param node The node to be visited.
     *  @param data User-defined data passed from the parent node.
     *   If it is not null, it is an Object[2] tuple with
     *   the type of the previous node as its first element and the
     *   arguments as the second.
     *  @return The return value of the superclass.
     *  @see #visit(ASTPrimaryExpression, Object)
     *  @see #visit(ASTPrimarySuffix, Object)
     */
    public Object visit(ASTPrimaryPrefix node, Object data) {
        int nChildren = node.jjtGetNumChildren();
        String image = node.getImage();
        
        // Organize possible arguments in data.
        Object[] dataTuple = (Object[])data;
        Type lastType = null;
        ASTArguments args = null;
        if (dataTuple != null) {    // Must be a tuple if not null.
            lastType = (Type)dataTuple[0];
            args = (ASTArguments)dataTuple[1];
            if (args != null)       // Type-check the arguments first.
                visit(args, null);
        }
        
        if (nChildren == 0) {
            ASTName name = _String2ASTName(image);
            _resolveName(name, lastType, args);
            Type.propagateType(node, name);
            return null;
        } else if (nChildren == 1) {
            Node firstChild = node.jjtGetFirstChild();
            if (firstChild instanceof ASTResultType) {
                // ASTPrimaryPrefix followed by ASTResultType is a ".class" expression.
                Object result = _visitSingleChild(node, data);
                Type.setType(node, Type.createType("java.lang.Class"));
                return result;
            } else if (firstChild instanceof ASTName) {
                ASTName name = (ASTName)firstChild;
                _resolveName(name, lastType, args);
                Type.propagateType(node, name);
                return null;
            } else if (firstChild instanceof ASTLiteral ||
                    firstChild instanceof ASTAllocationExpression ||
                    firstChild instanceof ASTExpression)
                return _visitSingleChild(node, data);
        }
        throw new UnknownASTException();
    }
    
    /** Visit a <tt>PrimarySuffix</tt> node in the AST, and set
     *  its type depending on the number and types of its children.
     *  The type of the first part of this prefix is resolved from
     *  the data given by the last <tt>PrimaryPrefix</tt> node.
     *  <p>
     *  If <tt>data</tt> is not null, it is an Object[2] tuple with
     *  the type of the previous node as its first element and the
     *  arguments as the second.
     *  <p>
     *  Grammar: <tt>PrimarySuffix ::= "." ("this" | "super" | AllocationExpression |
     *    [ReferenceTypeList] &lt;IDENTIFIER&gt;) | "[" Expression "]" | Arguments |
     *    ReferenceTypeList</tt>
     *  
     *  @param node The node to be visited.
     *  @param data User-defined data passed from the parent node.
     *   If it is not null, it is an Object[2] tuple with
     *   the type of the previous node as its first element and the
     *   arguments as the second.
     *  @return The return value of the superclass.
     *  @see #visit(ASTPrimaryExpression, Object)
     *  @see #visit(ASTPrimaryPrefix, Object)
     */
    public Object visit(ASTPrimarySuffix node, Object data)
            throws ASTClassNotFoundException {
        int nChildren = node.jjtGetNumChildren();
        if (nChildren == 0) {
            ASTName name = new ASTName();
            ASTIdentifier id = new ASTIdentifier(JavaParserConstants.IDENTIFIER);
            id.setName(node.getImage());
            name.jjtAddFirstChild(id);
            
            Object[] dataTuple = (Object[])data;
            Type lastType = (Type)dataTuple[0]; // It shouldn't be null.
            ASTArguments args = (ASTArguments)dataTuple[1];
            if (args != null)   // Type-check the arguments first.
                visit(args, null);
            _resolveName(name, lastType, args);
            Type.propagateType(node, name);
            return null;
        } else if (nChildren == 1) {
            Node child = node.jjtGetFirstChild();
            if (child instanceof ASTAllocationExpression) {
                Object[] dataTuple = (Object[])data;
                Type lastType = (Type)dataTuple[0]; // It shouldn't be null.
                ASTAllocationExpression alloc = (ASTAllocationExpression)child;
                ASTClassOrInterfaceType coi = (ASTClassOrInterfaceType)alloc.jjtGetFirstChild();
                String className = lastType.getName();
                for (int i=0; i<coi.jjtGetNumChildren(); i++)
                    className += "." + ((ASTIdentifier)coi.jjtGetChild(i)).getImage();
                coi.setImage(className);
                return _visitSingleChild(node, data);
            } else if (child instanceof ASTReferenceTypeList)
                return super.visit(node, data);
            else if (child instanceof ASTExpression) {
                // Array indexing.
                Object[] dataTuple = (Object[])data;
                Type lastType = (Type)dataTuple[0]; // It shouldn't be null.
                ASTArguments args = (ASTArguments)dataTuple[1];
            
                if (lastType.isArray()) {
                    try {
                        Type.setType(node, lastType.removeOneDimension());
                    } catch (ClassNotFoundException e) {
                        // Not exact.
                        throw new ASTClassNotFoundException(lastType.getName());
                    }
                    return null;
                } else
                    throw new UnknownASTException();
            } else
                throw new UnknownASTException();
        } else
            throw new UnknownASTException();
    }
    
    
    /** Visit an <tt>Arguments</tt> node in the AST, and also
     *  propagate the type of its single child (an
     *  <tt>ArgumentList</tt> node), if any, to this node.
     *  <p>
     *  Grammar: <tt>Arguments ::= "(" [ArgumentList] ")"</tt>
     *  
     *  @param node The node to be visited.
     *  @param data User-defined data passed from the parent node
     *   (not used for this function).
     *  @return The return value of the superclass.
     */
    public Object visit(ASTArguments node, Object data) {
        if (node.jjtGetNumChildren() == 0) { // Has no argument.
            Type.setType(node, Type.NULL);
            return null;
        } else
            return _visitSingleChild(node, data);
    }
    
    /** Visit a <tt>Literal</tt> node in the AST, and set the type
     *  of this node to be the type resolved from the literal. A
     *  literal can be a number, a boolean constant, a character,
     *  a string, or "<tt>null</tt>".
     *  <p>
     *  Grammar: <tt>Literal ::= &lt;INTEGER_LITERAL&gt; |
     *    &lt;FLOATING_POINT_LITERAL&gt; | &lt;CHARACTER_LITERAL&gt; |
     *    &lt;STRING_LITERAL&gt; | BooleanLiteral | NullLiteral</tt>
     *  
     *  @param node The node to be visited.
     *  @param data User-defined data passed from the parent node
     *   (not used for this function).
     *  @return Always <tt>null</tt>.
     */
    public Object visit(ASTLiteral node, Object data) {
        if (node.jjtGetNumChildren() == 1) {
            Node child = node.jjtGetFirstChild();
            if (child instanceof ASTBooleanLiteral) {
                Type.setType(child, Type.BOOLEAN);
                Type.setType(node, Type.BOOLEAN);
            } else if (child instanceof ASTNullLiteral) {
                Type.setType(child, Type.NULL);
                Type.setType(node, Type.NULL);
            } else
                // Unreachable.
                throw new UnknownASTException();
        } else if (node.jjtGetNumChildren() == 0) {
            String image = node.getImage();
            if (image.startsWith("\"") && image.endsWith("\""))
                Type.setType(node, Type.createType("java.lang.String"));
            else if (image.startsWith("\'") && image.endsWith("\'"))
                Type.setType(node, Type.CHAR);
            else {  // Either int/long/short or double/float.
                if (!image.startsWith("0x") && (image.indexOf('.') != -1 || image.endsWith("d") || image.endsWith("f"))) {  // double/float
                    if (image.endsWith("f"))
                        Type.setType(node, Type.FLOAT);
                    else
                        Type.setType(node, Type.DOUBLE);
                } else {    // int/long/short
                    if (image.endsWith("l"))
                        Type.setType(node, Type.LONG);
                    else
                        Type.setType(node, Type.INT);
                }
            }
        } else
            throw new UnknownASTException();
        return null;
    }
    
    /** Visit a <tt>PrimaryExpression</tt> node in the AST, and set the type
     *  of this node to be the type of its last resolved children (excluding
     *  arguments for <tt>PrimaryPrefix</tt> or <tt>PrimarySuffix</tt>).
     *  <p>
     *  Grammar: <tt>PrimaryExpression ::= PrimaryPrefix PrimarySuffix*</tt>
     *  
     *  @param node The node to be visited.
     *  @param data User-defined data passed from the parent node
     *   (not used for this function).
     *  @return Always <tt>null</tt>.
     *  @see #visit(ASTPrimaryPrefix, Object)
     *  @see #visit(ASTPrimarySuffix, Object)
     */
    public Object visit(ASTPrimaryExpression node, Object data) {
        int nChildren = node.jjtGetNumChildren();
        boolean lastArgument = false;
        Type lastType = null;
        for (int i=0; i<nChildren; i++) {
            lastArgument = false;
            Node child = node.jjtGetChild(i);
            Node arguments = null;
            if (i < nChildren - 1) {
                 // Test if the next child is argument list.
                 Node nextChild = node.jjtGetChild(i + 1);
                 if ((nextChild instanceof ASTPrimarySuffix) &&
                     ((ASTPrimarySuffix)nextChild).isArguments()) {
                     i++;   // Skip arguments
                     arguments = nextChild.jjtGetFirstChild();
                     lastArgument = true;
                 }
            }
            child.jjtAccept(this, new Object[]{lastType, arguments});
            lastType = Type.getType(child);
        }
        
        if (lastArgument)
            Type.propagateType(node, node.jjtGetChild(nChildren - 2));
        else
            Type.propagateType(node, node.jjtGetChild(nChildren - 1));
        return null;
    }
    
    /** Visit an <tt>AllocationExpression</tt> node in the AST, and set the type
     *  of this node to be the class that is instantiated.
     *  <p>
     *  Grammar: <tt>AllocationExpression ::= "new" (ClassOrInterfaceType (ArrayDimsAndInits |
     *    Arguments [ClassBody]) | PrimitiveType ArrayDimsAndInits)</tt>
     *  
     *  @param node The node to be visited.
     *  @param data User-defined data passed from the parent node
     *   (not used for this function).
     *  @return The return value of the superclass.
     */
    public Object visit(ASTAllocationExpression node, Object data) {
        int nChildren = node.jjtGetNumChildren();
        if (nChildren == 1)
            return _visitSingleChild(node, data);
        else if (nChildren == 2) {
            Object result = super.visit(node, data);
            Type type = Type.getType(node.jjtGetFirstChild());
            if (node.jjtGetChild(1) instanceof ASTArrayDimsAndInits) {
                ASTArrayDimsAndInits dims = (ASTArrayDimsAndInits)node.jjtGetChild(1);
                for (int i=0; i<dims.getArrayCount(); i++)
                    type = type.addOneDimension();
            }
            Type.setType(node, type);
            return result;
        } else if (nChildren == 3) {
            visit((ASTClassOrInterfaceType)node.jjtGetChild(0), data);
            visit((ASTArguments)node.jjtGetChild(1), data);
            Type type = Type.getType(node.jjtGetFirstChild());
            visit((ASTClassBody)node.jjtGetChild(2), type);
            Type.setType(node, type);
            return null;
        }
        throw new UnknownASTException();
    }

    //-------------------------------------------------------------
    // The following nodes preserve types.
    
    /** Visit a <tt>Type</tt> node in the AST, and set the type
     *  of this node to be the same as its single child.
     *  <p>
     *  Grammar: <tt>Type ::= ReferenceType | PrimitiveType</tt>
     *  
     *  @param node The node to be visited.
     *  @param data User-defined data passed from the parent node
     *   (not used for this function).
     *  @return The return value of the superclass.
     */
    public Object visit(ASTType node, Object data) {
        return _visitSingleChild(node, data);
    }

    /** Visit a <tt>PostfixExpression</tt> node in the AST, and set the type
     *  of this node to be the same as its single child.
     *  <p>
     *  Grammar: <tt>PostfixExpression ::= PrimaryExpression ["++" | "--"]</tt>
     *  
     *  @param node The node to be visited.
     *  @param data User-defined data passed from the parent node
     *   (not used for this function).
     *  @return The return value of the superclass.
     */
    public Object visit(ASTPostfixExpression node, Object data) {
        return _visitSingleChild(node, data);
    }

    /** Visit a <tt>UnaryExpressionNotPlusMinus</tt> node in the AST, and set the type
     *  of this node to be the same as its single child.
     *  <p>
     *  Grammar: <tt>UnaryExpressionNotPlusMinus ::= CastExpression | ("~" | "!") UnaryExpression |
     *    PostfixExpression</tt>
     *  
     *  @param node The node to be visited.
     *  @param data User-defined data passed from the parent node
     *   (not used for this function).
     *  @return The return value of the superclass.
     */
    public Object visit(ASTUnaryExpressionNotPlusMinus node, Object data) {
        return _visitSingleChild(node, data);
    }

    /** Visit a <tt>UnaryExpression</tt> node in the AST, and set the type
     *  of this node to be the same as its single child.
     *  <p>
     *  Grammar: <tt>UnaryExpression ::= ("+" | "-") UnaryExpression | PreIncrementExpression |
     *    PreDecrementExpression | UnaryExpressionNotPlusMinus</tt>
     *  
     *  @param node The node to be visited.
     *  @param data User-defined data passed from the parent node
     *   (not used for this function).
     *  @return The return value of the superclass.
     */
    public Object visit(ASTUnaryExpression node, Object data) {
        return _visitSingleChild(node, data);
    }
    
    /** Visit a <tt>PreIncrementExpression</tt> node in the AST, and set the type
     *  of this node to be the same as its single child.
     *  <p>
     *  Grammar: <tt>PreIncrementExpression ::= "++" PrimaryExpression</tt>
     *  
     *  @param node The node to be visited.
     *  @param data User-defined data passed from the parent node
     *   (not used for this function).
     *  @return The return value of the superclass.
     */
    public Object visit(ASTPreIncrementExpression node, Object data) {
        return _visitSingleChild(node, data);
    }
    
    /** Visit a <tt>PreDecrementExpression</tt> node in the AST, and set the type
     *  of this node to be the same as its single child.
     *  <p>
     *  Grammar: <tt>PreDecrementExpression ::= "--" PrimaryExpression</tt>
     *  
     *  @param node The node to be visited.
     *  @param data User-defined data passed from the parent node
     *   (not used for this function).
     *  @return The return value of the superclass.
     */
    public Object visit(ASTPreDecrementExpression node, Object data) {
        return _visitSingleChild(node, data);
    }
    
    /** Visit a <tt>VariableInitializer</tt> node in the AST, and set the type
     *  of this node to be the same as its single child.
     *  <p>
     *  Grammar: <tt>VariableInitializer ::= ArrayInitializer | Expression</tt>
     *  
     *  @param node The node to be visited.
     *  @param data User-defined data passed from the parent node
     *   (not used for this function).
     *  @return The return value of the superclass.
     */
    public Object visit(ASTVariableInitializer node, Object data) {
        return _visitSingleChild(node, data);
    }
    
    /** Visit a <tt>MultiplicativeExpression</tt> node in the AST, and set the type
     *  of this node to be the least type that is compatible with all its children.
     *  <p>
     *  Grammar: <tt>MultiplicativeExpression ::= UnaryExpression (("*" | "/" | "%") UnaryExpression)*</tt>
     *  
     *  @param node The node to be visited.
     *  @param data User-defined data passed from the parent node
     *   (not used for this function).
     *  @return The return value of the superclass.
     */
    public Object visit(ASTMultiplicativeExpression node, Object data) {
        return _visitExpressionWithChildren(node, data);
    }
    
    /** Visit an <tt>AdditiveExpression</tt> node in the AST, and set the type
     *  of this node to be the least type that is compatible with all its children.
     *  <p>
     *  Grammar: <tt>AdditiveExpression ::= MultiplicativeExpression (("+" | "-") MultiplicativeExpression)*</tt>
     *  
     *  @param node The node to be visited.
     *  @param data User-defined data passed from the parent node
     *   (not used for this function).
     *  @return The return value of the superclass.
     */
    public Object visit(ASTAdditiveExpression node, Object data) {
        return _visitExpressionWithChildren(node, data);
    }
    
    /** Visit a <tt>ShiftExpression</tt> node in the AST, and set the type
     *  of this node to be the least type that is compatible with all its children.
     *  <p>
     *  Grammar: <tt>ShiftExpression ::= AdditiveExpression (("<<" | ">>" | ">>>") AdditiveExpression)*</tt>
     *  
     *  @param node The node to be visited.
     *  @param data User-defined data passed from the parent node
     *   (not used for this function).
     *  @return The return value of the superclass.
     */
    public Object visit(ASTShiftExpression node, Object data) {
        return _visitExpressionWithChildren(node, data);
    }
    
    /** Visit an <tt>AndExpression</tt> node in the AST, and set the type
     *  of this node to be the least type that is compatible with all its children.
     *  <p>
     *  Grammar: <tt>AndExpression ::= EqualityExpression ("&" EqualityExpression)*</tt>
     *  
     *  @param node The node to be visited.
     *  @param data User-defined data passed from the parent node
     *   (not used for this function).
     *  @return The return value of the superclass.
     */
    public Object visit(ASTAndExpression node, Object data) {
        return _visitExpressionWithChildren(node, data);
    }
    
    /** Visit an <tt>ExclusiveOrExpression</tt> node in the AST, and set the type
     *  of this node to be the least type that is compatible with all its children.
     *  <p>
     *  Grammar: <tt>ExclusiveOrExpression ::= AndExpression ("^" AndExpression)*</tt>
     *  
     *  @param node The node to be visited.
     *  @param data User-defined data passed from the parent node
     *   (not used for this function).
     *  @return The return value of the superclass.
     */
    public Object visit(ASTExclusiveOrExpression node, Object data) {
        return _visitExpressionWithChildren(node, data);
    }
    
    /** Visit an <tt>InclusiveOrExpression</tt> node in the AST, and set the type
     *  of this node to be the least type that is compatible with all its children.
     *  <p>
     *  Grammar: <tt>InclusiveOrExpression ::= ExclusiveOrExpression ("|" ExclusiveOrExpression)*</tt>
     *  
     *  @param node The node to be visited.
     *  @param data User-defined data passed from the parent node
     *   (not used for this function).
     *  @return The return value of the superclass.
     */
    public Object visit(ASTInclusiveOrExpression node, Object data) {
        return _visitExpressionWithChildren(node, data);
    }
    
    /** Visit a <tt>ConditionalAndExpression</tt> node in the AST, and set the type
     *  of this node to be the least type that is compatible with all its children.
     *  <p>
     *  Grammar: <tt>ConditionalAndExpression ::= InclusiveOrExpression ("&&" InclusiveOrExpression)*</tt>
     *  
     *  @param node The node to be visited.
     *  @param data User-defined data passed from the parent node
     *   (not used for this function).
     *  @return The return value of the superclass.
     */
    public Object visit(ASTConditionalAndExpression node, Object data) {
        return _visitExpressionWithChildren(node, data);
    }
    
    /** Visit a <tt>ConditionalOrExpression</tt> node in the AST, and set the type
     *  of this node to be the least type that is compatible with all its children.
     *  <p>
     *  Grammar: <tt>ConditionalOrExpression ::= ConditionalAndExpression ("||" ConditionalAndExpression)*</tt>
     *  
     *  @param node The node to be visited.
     *  @param data User-defined data passed from the parent node
     *   (not used for this function).
     *  @return The return value of the superclass.
     */
    public Object visit(ASTConditionalOrExpression node, Object data) {
        return _visitExpressionWithChildren(node, data);
    }
    
    /** Visit an <tt>Expression</tt> node in the AST, and set the type
     *  of this node to be the same as that of its first child.
     *  <p>
     *  If the expression is an assignment, this function check the
     *  type compatibility for the assignment to detect if any error
     *  occur in a previous step in type analysis.
     *  <p>
     *  Grammar: <tt>Expression ::= ConditionalExpression [AssignmentOperator Expression]</tt>
     *  
     *  @param node The node to be visited.
     *  @param data User-defined data passed from the parent node
     *   (not used for this function).
     *  @return The return value of the superclass.
     */
    public Object visit(ASTExpression node, Object data) {
        int nChildren = node.jjtGetNumChildren();
        if (nChildren == 1)
            // Propagate the type if only one child (ConditionalExpression).
            return _visitSingleChild(node, data);
        else if (nChildren == 3) {
            ASTConditionalExpression child1 =
                (ASTConditionalExpression)node.jjtGetFirstChild();
            ASTAssignmentOperator child2 =
                (ASTAssignmentOperator)node.jjtGetChild(1);
            ASTExpression child3 =
                (ASTExpression)node.jjtGetChild(2);
            
            visit(child1, data);
            visit(child3, data);
            
            Type type1 = Type.getType(child1);
            Type type3 = Type.getType(child3);
            
            try {
                if (type3.compatibility(type1, _loader) == -1)
                    // Incompatible types in an assignments means something goes
                    // wrong in some previous steps.
                    throw new UnknownASTException();
            } catch (ClassNotFoundException e) {
                throw new UnknownASTException();
            }
            
            Type.setType(node, type1);
            return null;
        } else
            throw new UnknownASTException();
    }
    
    /** Visit a <tt>StatementExpression</tt> node in the AST, and set the type
     *  of this node to be the same as that of its first child.
     *  <p>
     *  If the expression is an assignment, this function check the
     *  type compatibility for the assignment to detect if any error
     *  occur in a previous step in type analysis.
     *  <p>
     *  Grammar: <tt>StatementExpression ::= PreIncrementExpression | PreDecrementExpression |
     *    PrimaryExpression ["++" | "--" | AssignmentOperator Expression]</tt>
     *  
     *  @param node The node to be visited.
     *  @param data User-defined data passed from the parent node
     *   (not used for this function).
     *  @return The return value of the superclass.
     */
    public Object visit(ASTStatementExpression node, Object data) {
        int nChildren = node.jjtGetNumChildren();
        if (nChildren == 1)
            // Propagate the type if only one child (ConditionalExpression).
            return _visitSingleChild(node, data);
        else if (nChildren == 3) {
            ASTPrimaryExpression child1 =
                (ASTPrimaryExpression)node.jjtGetFirstChild();
            ASTAssignmentOperator child2 =
                (ASTAssignmentOperator)node.jjtGetChild(1);
            ASTExpression child3 =
                (ASTExpression)node.jjtGetChild(2);
            
            visit(child1, data);
            visit(child3, data);
            
            Type type1 = Type.getType(child1);
            Type type3 = Type.getType(child3);
            
            Type.setType(node, type1);
            return null;
        } else
            throw new UnknownASTException();
    }
    
    /** Visit a <tt>CastExpression</tt> node in the AST, and set the type
     *  of this node to be the same as that of its first child.
     *  <p>
     *  Grammar: <tt>CastExpression ::= "(" Type ")" UnaryExpression</tt>
     *  
     *  @param node The node to be visited.
     *  @param data User-defined data passed from the parent node
     *   (not used for this function).
     *  @return The return value of the superclass.
     */
    public Object visit(ASTCastExpression node, Object data) {
        return _visitExpressionWithChildren(node, data, false);
    }
    
    /** Visit a <tt>TypeArguments</tt> node in the AST, and set the type
     *  of this node to be the same as that of its first child. (A Java
     *  1.5 feature.)
     *  <p>
     *  Grammar: <tt>TypeArguments ::= "<" ActualTypeArgument ("," ActualTypeArgument)* (">" | ">>" | ">>>")</tt>
     *  
     *  @param node The node to be visited.
     *  @param data User-defined data passed from the parent node
     *   (not used for this function).
     *  @return The return value of the superclass.
     */
    public Object visit(ASTTypeArguments node, Object data) {
        return _visitExpressionWithChildren(node, data, false);
    }
    
    /** Visit a <tt>ReferenceTypeList</tt> node in the AST, and set the type
     *  of this node to be the same as that of its first child. (A Java
     *  1.5 feature.)
     *  <p>
     *  Grammar: <tt>ReferenceTypeList ::= "<" ActualTypeArgument ("," ActualTypeArgument)* (">" | ">>" | ">>>")</tt>
     *  
     *  @param node The node to be visited.
     *  @param data User-defined data passed from the parent node
     *   (not used for this function).
     *  @return The return value of the superclass.
     */
    public Object visit(ASTReferenceTypeList node, Object data) {
        return _visitExpressionWithChildren(node, data, false);
    }
    
    /** Visit an <tt>ActualTypeArgument</tt> node in the AST, and set the type
     *  of this node to be the same as that of its first child. (A Java
     *  1.5 feature.)
     *  <p>
     *  Grammar: <tt>ActualTypeArgument ::= "?" [("extends" | "super") ReferenceType] | ReferenceType</tt>
     *  
     *  @param node The node to be visited.
     *  @param data User-defined data passed from the parent node
     *   (not used for this function).
     *  @return The return value of the superclass.
     */
    public Object visit(ASTActualTypeArgument node, Object data) {
        return _visitExpressionWithChildren(node, data, false);
    }
    
    //-------------------------------------------------------------
    // The following nodes yield boolean types if has more than 1
    // child.
    
    /** Visit an <tt>RelationalExpression</tt> node in the AST. If
     *  it has only one child, its type is set to the type of that
     *  child; if it has more than one child, its type is boolean.
     *  <p>
     *  Grammar: <tt>RelationalExpression ::= ShiftExpression (("<" | ">" | "<=" | ">=") ShiftExpression)*</tt>
     *  
     *  @param node The node to be visited.
     *  @param data User-defined data passed from the parent node
     *   (not used for this function).
     *  @return The return value of the superclass.
     */
    public Object visit(ASTRelationalExpression node, Object data) {
        Object result = super.visit(node, data);
        if (node.jjtGetNumChildren() > 1)
            Type.setType(node, Type.BOOLEAN);
        else
            Type.propagateType(node, node.jjtGetFirstChild());
        return result;
    }
    
    /** Visit an <tt>InstanceOfExpression</tt> node in the AST. If
     *  it has only one child, its type is set to the type of that
     *  child; if it has more than one child, its type is boolean.
     *  <p>
     *  Grammar: <tt>InstanceOfExpression ::= RelationalExpression ["instanceof" RelationalExpression]</tt>
     *  
     *  @param node The node to be visited.
     *  @param data User-defined data passed from the parent node
     *   (not used for this function).
     *  @return The return value of the superclass.
     */
    public Object visit(ASTInstanceOfExpression node, Object data) {
        Object result = super.visit(node, data);
        if (node.jjtGetNumChildren() > 1)
            Type.setType(node, Type.BOOLEAN);
        else
            Type.propagateType(node, node.jjtGetFirstChild());
        return result;
    }
    
    /** Visit an <tt>EqualityExpression</tt> node in the AST. If
     *  it has only one child, its type is set to the type of that
     *  child; if it has more than one child, its type is boolean.
     *  <p>
     *  Grammar: <tt>EqualityExpression ::= InstanceOfExpression (("==" | "!=") InstanceOfExpression)*</tt>
     *  
     *  @param node The node to be visited.
     *  @param data User-defined data passed from the parent node
     *   (not used for this function).
     *  @return The return value of the superclass.
     */
    public Object visit(ASTEqualityExpression node, Object data) {
        Object result = super.visit(node, data);
        if (node.jjtGetNumChildren() > 1)
            Type.setType(node, Type.BOOLEAN);
        else
            Type.propagateType(node, node.jjtGetFirstChild());
        return result;
    }
    
    //-------------------------------------------------------------
    // Conditional expression (1 == 2 ? 0 : 1).
    
    /** Visit an <tt>ConditionalExpression</tt> node in the AST. If
     *  it has only one child, its type is set to the type of that
     *  child; if it has three children, its type is the least type
     *  that is compatible with both the types of its last two
     *  children.
     *  <p>
     *  Grammar: <tt>ConditionalExpression ::= ConditionalOrExpression ["?" Expression ":" ConditionalExpression]</tt>
     *  
     *  @param node The node to be visited.
     *  @param data User-defined data passed from the parent node
     *   (not used for this function).
     *  @return The return value of the superclass.
     */
    public Object visit(ASTConditionalExpression node, Object data) {
        Object result = super.visit(node, data);
        int nChildren = node.jjtGetNumChildren();
        if (nChildren == 3) {
            Type type1 = Type.getType(node.jjtGetChild(1));
            Type type2 = Type.getType(node.jjtGetChild(2));
            
            Type commonType = Type.getCommonType(type1, type2);
            try {
                if (commonType == null && type1.compatibility(type2, _loader) >= 0)
                    commonType = type2;
            } catch (ClassNotFoundException e) {
            }
            
            try {
                if (commonType == null && type2.compatibility(type1, _loader) >= 0)
                    commonType = type1;
            } catch (ClassNotFoundException e) {
            }
            
            if (commonType == null)
                throw new UnknownASTException();
            
            Type.setType(node, commonType);
        } else if (nChildren == 1)
            Type.propagateType(node, node.jjtGetFirstChild());
        else
            throw new UnknownASTException();
        return result;
    }
    
    ///////////////////////////////////////////////////////////////////
    ////                      protected methods                    ////

    /** Look up all the currently visible scopes and search for the
     *  most recently defined variable (or field) with a name.
     *  
     *  @param name The name of the variable.
     *  @return The type of that variable if found; otherwise,
     *   <tt>null</tt>.
     */
    protected Type findVariable(String name) {
        return _findVariable(name, _variableStack.iterator());
    }
    
    /** Get the type of a field in a class by its name. If not found
     *  in the class definition, this function also searches the
     *  superclasses of that class, as well as the interfaces that
     *  the class and its superclasses implement.
     *  
     *  @param c The class from which the field name is resolved.
     *  @param name The name of the field.
     *  @return The type of the field if found; otherwise, <tt>null</tt>.
     *  @see #getMethodType(Class, String, Type[])
     */
    protected Type getFieldType(Class c, String name) {
        Field field;
        List workList = new LinkedList();
        Set handledSet = new HashSet();
        workList.add(c);
        
        while (!workList.isEmpty()) {
            Class topClass = (Class)workList.remove(0);
            
            try {
                field = topClass.getDeclaredField(name);
                return Type.createType(field.getType().getName());
            } catch (NoSuchFieldException e1) {
            }
            
            handledSet.add(topClass);
            Class superClass = topClass.getSuperclass();
            if (superClass == null && !topClass.getName().equals("java.lang.Object"))
                superClass = Object.class;
            if (superClass != null)
                workList.add(superClass);
            Class[] interfaces = topClass.getInterfaces();
            for (int i=0; i<interfaces.length; i++)
                if (!handledSet.contains(interfaces[i]))
                    workList.add(interfaces[i]);
        }
        return null;
    }
    
    /** Get the type of a method in a class by its name and types of
     *  actural arguments. If not found in the class definition, this
     *  function also searches the superclasses of that class, as well
     *  as the interfaces that the class and its superclasses implement.
     *  <p>
     *  This function always tries to find the best match if multiple
     *  methods with the same name and the same number of arguments are
     *  defined in the class and interface hierarchy. This is
     *  accomplished by computing the compatibility rating between each
     *  pair of formal argument and actural argument, and sum those
     *  numbers together.
     *  
     *  @param c The class from which the method is resolved.
     *  @param name The name of the field.
     *  @param args The types of actural arguments for a call.
     *  @return The return type of the method if found; otherwise,
     *   <tt>null</tt>.
     *  @see #getFieldType(Class, String)
     *  @see Type#compatibility(Type, ClassLoader)
     */
    protected Type getMethodType(Class c, String name, Type[] args)
            throws ASTClassNotFoundException {
        Method[] methods = null;
        int best_compatibility = -1;
        Method best_method = null;
        List workList = new LinkedList();
        Set handledSet = new HashSet();
        workList.add(c);
        
        while (!workList.isEmpty()) {
            Class topClass = (Class)workList.remove(0);
            methods = topClass.getDeclaredMethods();
            
            for (int i=0; i<methods.length; i++) {
                Method method = methods[i];
                
                // FIXME: Ignore volatile methods.
                if (Modifier.isVolatile(method.getModifiers()))
                    continue;
                
                if (method.getName().equals(name)) {
                    Class[] formalParams = method.getParameterTypes();
                    if (formalParams.length == args.length) {
                        int compatibility = 0;
                        for (int j=0; j<formalParams.length; j++)
                            try {
                                Type formalType = Type.createType(formalParams[j].getName());
                                int comp = args[j].compatibility(formalType, _loader);
                                if (comp == -1) {
                                    compatibility = -1;
                                    break;
                                } else
                                    compatibility += comp;
                            } catch (ClassNotFoundException e) {
                                // Not exact.
                                throw new ASTClassNotFoundException(args[j].getName());
                            }
                        if (compatibility == -1)
                            continue;
                        else if (best_compatibility == -1 || best_compatibility > compatibility) {
                            best_compatibility = compatibility;
                            best_method = method;
                            if (best_compatibility == 0)    // The best found.
                                break;
                        }
                    }
                }
            }
            
            if (best_compatibility == 0)    // The best found.
                break;
            
            handledSet.add(topClass);
            Class superClass = topClass.getSuperclass();
            if (superClass == null && !topClass.getName().equals("java.lang.Object"))
                superClass = Object.class;
            if (superClass != null && !handledSet.contains(superClass))
                workList.add(superClass);
            Class[] interfaces = topClass.getInterfaces();
            for (int i=0; i<interfaces.length; i++)
                if (!handledSet.contains(interfaces[i]))
                    workList.add(interfaces[i]);
        }
        
        if (best_compatibility != -1)
            return Type.createType(best_method.getReturnType().getName());
        else
            return null;
    }
    
    ///////////////////////////////////////////////////////////////////
    ////                       private methods                     ////

    /** Look up all the currently visible scopes and search for the
     *  most recently defined variable (or field) with a name.
     *  
     *  @param name The name of the variable.
     *  @param variableStackIter The iterator of the stack of scopes.
     *  @return The type of that variable if found; otherwise,
     *   <tt>null</tt>.
     *  @see #findVariable(String)
     */
    private Type _findVariable(String name, Iterator variableStackIter) {
        if (variableStackIter.hasNext()) {
            Hashtable table = (Hashtable)variableStackIter.next();
            Type t = _findVariable(name, variableStackIter);
            if (t != null)
                return t;
            else if (table.containsKey(name))
                return (Type)table.get(name);
            else
                return null;
        } else
            return null;
    }

    /** Visit a node with a single child, and set the type of that node
     *  to be the same as the type of that child.
     *  
     *  @param node The node to be visited.
     *  @param data User-defined data passed from the parent node
     *   (not used for this function)
     *  @return The return value of the superclass.
     */
    private Object _visitSingleChild(SimpleNode node, Object data) {
        if (node.jjtGetNumChildren() != 1)
            throw new UnknownASTException();
        return _visitExpressionWithChildren(node, data, false);
    }
    
    /** Visit a node with a single child, and set the type of that node
     *  to be the least type that is compatible with the types of all
     *  its children. This is the same as calling
     *  <tt>visitExpressionWithChildren(node, data, true)</tt>.
     *  
     *  @param node The node to be visited.
     *  @param data User-defined data passed from the parent node
     *   (not used for this function)
     *  @return The return value of the superclass.
     *  @see #_visitExpressionWithChildren(SimpleNode, Object, boolean)
     */
    private Object _visitExpressionWithChildren(SimpleNode node, Object data) {
        return _visitExpressionWithChildren(node, data, true);
    }
    
    /** Visit a node with a single child, and set the type of that node
     *  to be the type of its first child or the least type that is compatible
     *  with the types of all its children, depending on the flag
     *  <tt>checkCommonTypes</tt>.
     *  
     *  @param node The node to be visited.
     *  @param data User-defined data passed from the parent node
     *   (not used for this function)
     *  @param checkCommonTypes If <tt>true</tt>, set the type of the node
     *   to be the least type that is compatible with the types of all its
     *   children; If <tt>false</tt>, simply propagate the type of the first
     *   child to the node.
     *  @return The return value of the superclass.
     */
    private Object _visitExpressionWithChildren(SimpleNode node, Object data, boolean checkCommonTypes) {
        Object result = super.visit(node, data);
        int nChildren = node.jjtGetNumChildren();
        if (nChildren == 1 || !checkCommonTypes)
            Type.propagateType(node, node.jjtGetFirstChild());
        else {
            Type commonType = Type.getType(node.jjtGetFirstChild());
            if (commonType == null)
                throw new UnknownASTException();
            for (int i=1; i<nChildren; i++) {
                Type childType = Type.getType(node.jjtGetChild(i));
                if (childType == null)
                    throw new UnknownASTException();
                commonType = Type.getCommonType(childType, commonType);
            }
            Type.setType(node, commonType);
        }
        return result;
    }
    
    /** Visit the node and record the variables declared by that node.
     *  The node must be an instance of {@link ASTFormalParameter},
     *  {@link ASTFieldDeclaration}, or {@link ASTLocalVariableDeclaration}.
     * 
     *  @param node The node with variable declaration.
     */
    private void _recordVariableDeclaration(SimpleNode node) {
        Hashtable variableTable = (Hashtable)_variableStack.peek();
        Type type = Type.getType(node.jjtGetFirstChild());  // The first child is an ASTType.
        Type.setType(node, type);
        for (int i=1; i<node.jjtGetNumChildren(); i++) {
            Node child = node.jjtGetChild(i);
            ASTVariableDeclaratorId id;
            if (child instanceof ASTVariableDeclarator) {
                ASTVariableDeclarator decl = (ASTVariableDeclarator)child;
                id = (ASTVariableDeclaratorId)decl.jjtGetFirstChild();
            } else if (child instanceof ASTVariableDeclaratorId)
                id = (ASTVariableDeclaratorId)child;
            else
                throw new UnknownASTException();
            Type arrayType = type;
            for (int j=0; j<id.getArrayCount(); j++)
                arrayType = arrayType.addOneDimension();
            variableTable.put(id.getImage(), arrayType);
            Type.setType(id, arrayType);
            if (id != child)
                Type.setType(child, arrayType);
        }
    }
        
    /** Convert an <tt>Arguments</tt> node into an array of
     *  types. It assumes the <tt>Arguments</tt> node has already
     *  been visited by this visitor so that a type is properly
     *  assigned to each of the arguments in it.
     *  
     *  @param args The <tt>Arguments</tt> node.
     *  @return The array of types of all the arguments in the
     *   node.
     */
    private Type[] _ASTArguments2Types(ASTArguments args) {
        Type[] arguments = null;
        if (args != null) {
            if (args.jjtGetNumChildren() > 0) {
                ASTArgumentList argList = (ASTArgumentList)args.jjtGetFirstChild();
                int length = argList.jjtGetNumChildren();
                arguments = new Type[length];
                for (int i=0; i<length; i++)
                    arguments[i] = Type.getType(argList.jjtGetChild(i));
            } else
                arguments = new Type[0];
        }
        return arguments;
    }
    
    /** Convert a name with "."s separating parts of it into a
     *  <tt>Name</tt> node. Each part of the name becomes an
     *  <tt>Identifier</tt> node as a child of the <tt>Name</tt>
     *  node returned.
     *  
     *  @param sName The name to be converted.
     *  @return A <tt>Name</tt> node that represents the same
     *   name with one or more children of <tt>Identifier</tt>
     */
    private ASTName _String2ASTName(String sName) {
        ASTName name = new ASTName();
        int dotPos = 0;
        int i = 0;
        while (dotPos >= 0) {
            int lastDotPos = dotPos;
            dotPos = sName.indexOf(".", lastDotPos);
            String currentName;
            if (dotPos >= 0) {
                currentName = sName.substring(lastDotPos, dotPos);
                dotPos++;
            } else
                currentName = sName.substring(lastDotPos);
            ASTIdentifier id = new ASTIdentifier(JavaParserConstants.IDENTIFIER);
            id.setName(currentName);
            name.jjtAddChild(id, i++);
        }
        return name;
    }
    
    /** Resolve a name within the scope of the given type. The name can
     *  be a variable name, a field name, "this" or "super", a method
     *  name, or a class relative to that type. If the name is a method
     *  name, arguments of the method must be given.
     *  
     *  @param name The name to be resolved.
     *  @param lastType The type from which the name is resolved.
     *  @param args Not <tt>null</tt> if a method is to be resolved, in
     *   which case it is the arguments for the method.
     *  @throws ASTClassNotFoundException A class cannot be loaded when
     *   resolving the type of a name.
     *  @throws ASTResolutionException The name cannot be resolved from
     *   the given type.
     */
    private void _resolveName(ASTName name, Type lastType, ASTArguments args)
            throws ASTClassNotFoundException, ASTResolutionException {
        // Handle arguments.
        Type[] arguments = _ASTArguments2Types(args);
        
        // Resolve each part of the name.
        Class owner;
        try {
            owner = lastType == null ? _currentClass : lastType.toClass(_loader);
        } catch (ClassNotFoundException e) {
            throw new ASTClassNotFoundException(lastType.getName());
        }
        
        int length = name.jjtGetNumChildren();
        String remainingName = "";
        for (int i=0; i<length; i++) {
            ASTIdentifier id = (ASTIdentifier)name.jjtGetChild(i);
            String currentName = remainingName + id.getImage();
            
            if (currentName.equals("this")) {
                if (owner instanceof Class)
                    Type.setType(id, Type.createType(((Class)owner).getName()));
                continue;
            } else if (currentName.equals("super")) {
                Type.setType(id, Type.createType(((Class)owner).getSuperclass().getName()));
                continue;
            }
            
            if (i == 0 && lastType == null && !(i == length - 1 && arguments != null)) {
                // Look for variables or fields in the current class/method.
                Type vtype = findVariable(currentName);
                if (vtype != null) {
                    Type.setType(id, vtype);
                    if (vtype.isPrimitive())
                        owner = null;
                    else
                        try {
                            owner = vtype.toClass(_loader);
                        } catch (ClassNotFoundException e) {
                            throw new ASTClassNotFoundException(vtype.getName());
                        }
                    continue;
                }
                
                // Look for nested classes declared in _currentClass.
                Class c = _lookupClass(currentName);
                if (c != null) {
                    owner = c;
                    Type.setType(id, Type.createType(c.getName()));
                    continue;
                }
            }
            
            // Lookup enclosing classes.
            Type idType;
            Type[] argumentsForResolution = i < length - 1 ? null : arguments;
            idType = _resolveNameFromClass(owner, currentName, argumentsForResolution);
            
            if (idType == null && i == 0 && lastType == null) {
                int previousNumber = _previousClasses.size() - 1;
                while (idType == null && previousNumber >= 0) {
                    Class previousClass = (Class)_previousClasses.get(previousNumber--);
                    if (previousClass != null)
                        idType = _resolveNameFromClass(previousClass, currentName, argumentsForResolution);
                }
            }

            Type.setType(id, idType);
            if (idType != null) {
                remainingName = "";
                if (idType.isPrimitive())
                    owner = null;
                else
                    try {
                        owner = idType.toClass(_loader);
                    } catch (ClassNotFoundException e) {
                        throw new ASTClassNotFoundException(idType.getName());
                    }
            } else if (i < length - 1)  // Keep the unresolved part for next round.
                remainingName = currentName + ".";
            else
                throw new ASTResolutionException(owner.getName(), currentName);
        }
        Type.propagateType(name, name.jjtGetChild(length - 1));
    }
    
    /** Lookup a class with a partially given name as may
     *  appear in Java source code. The name may be relative
     *  to the current class and its enclosing classes. It
     *  may also be a full name.
     *  
     *  @param partialSimpleName The partially given class
     *   name.
     *  @return The class; <tt>null</tt> is returned if the
     *   class cannot be found.
     */
    private Class _lookupClass(String partialSimpleName) {
        int dotPos = partialSimpleName.indexOf('.');
        String simpleName;
        if (dotPos == -1)
            simpleName = partialSimpleName;
        else
            simpleName = partialSimpleName.substring(0, dotPos);
        Class result = null;
        
        int previousNumber = _previousClasses.size();
        for (int i=previousNumber; i>=0; i--) {
            Class workingClass =
                i == previousNumber ? _currentClass : (Class)_previousClasses.get(i);
            if (workingClass != null) {
                if (_getSimpleClassName(workingClass).equals(simpleName)) {
                    result = workingClass;
                    break;
                }
                Class[] declaredClasses = workingClass.getDeclaredClasses();
                for (int j=0; j<declaredClasses.length; j++)
                    if (_getSimpleClassName(declaredClasses[j]).equals(simpleName)) {
                        result = declaredClasses[j];
                        break;
                    }
                if (result != null)
                    break;
            }
        }
        
        // Look for imported classes.
        if (result == null && _importedClasses.containsKey(simpleName))
            result = (Class)_importedClasses.get(simpleName);
        
        // A result is found for simpleName.
        if (result != null && dotPos >= 0)
            try {
                result = _loader.loadClass(result.getName() + partialSimpleName.substring(dotPos));
            } catch (ClassNotFoundException e) {
                result = null;
            }

        // Fall back to simple class loader.
        if (result == null)
            try {
                result = _loader.searchForClass(partialSimpleName);
            } catch (ClassNotFoundException e) {
            }
        
        return result;
    }
    
    /** Resolve a name within the scope of the given class. The name can
     *  be a field name, a method name, or a class relative to that class.
     *  If the name is a method name, arguments of the method must be given.
     *  
     *  @param owner The class from which the name is resolved.
     *  @param name The name to be resolved.
     *  @param args Not <tt>null</tt> if a method is to be resolved, in
     *   which case it is the array of formal argument types for that
     *   method.
     *  @return The type of that name if found.
     *  @see #_resolveName(ASTName, Type, ASTArguments)
     */
    private Type _resolveNameFromClass(Class owner, String name, Type[] args) {
        // Try to resolve special members of arrays.
        if (owner.isArray()) {
            if (name.equals("length") && args == null)
                return Type.INT;
            else if (name.equals("clone") && args != null && args.length == 0)
                return Type.createType("java.lang.Object");
        }
        
        // Try dynamic resolution.
        if (args == null) {   // A field.
            Type fieldType = getFieldType(owner, name);
            if (fieldType != null)
                return fieldType;
        } else {
            Type methodType = getMethodType(owner, name, args);
            if (methodType != null)
                return methodType;
        }
        
        // Try class name resolution.
        try {
            Class c = _loader.searchForClass(new StringBuffer(name), (Class)owner);
            return Type.createType(c.getName());
        } catch (ClassNotFoundException e) {
        }
        
        Class superclass = owner.getSuperclass();
        if (superclass == null && !owner.getName().equals("java.lang.Object"))
            superclass = Object.class;
        if (superclass != null) {
            Type result = _resolveNameFromClass(superclass, name, args);
            if (result != null)
                return result;
        }
        return null;
    }
    
    /** Visit an <tt>UnmodifiedClassDeclaration</tt> or
     *  <tt>UnmodifiedInterfaceDeclaration</tt>, and set the current
     *  class to be the declaration.
     *  
     *  @param node A node of {@link ASTUnmodifiedClassDeclaration}
     *   or {@link ASTUnmodifiedInterfaceDeclaration}.
     *  @param data User-defined data passed from the parent node
     *   (not used for this function)
     *  @return The return value of the superclass.
     *  @see #visit(ASTUnmodifiedClassDeclaration, Object)
     *  @see #visit(ASTUnmodifiedInterfaceDeclaration, Object)
     */
    private Object _visitClassOrInterface(SimpleNode node, Object data) {
        String className = node.getImage();
        _previousClasses.push(_currentClass);
        try {
            if (_currentClass != null) {   // A inner class is found.
                className = _currentClass.getName() + "$" + className;
                _currentClass = _loader.searchForClass(className);
            } else
                _currentClass = _loader.searchForClass(className);
            Type.setType(node, Type.createType(_currentClass.getName()));
            _loader.setCurrentClass(_currentClass);
        } catch (ClassNotFoundException e) {
            throw new ASTClassNotFoundException(className);
        }
        
        // A class declaration starts a new scope.
        _variableStack.push(new Hashtable());
        _recordFields();
        Object result = super.visit(node, data);
        _variableStack.pop();
        
        _currentClass = (Class)_previousClasses.pop();
        _loader.setCurrentClass(_currentClass, false);
        return result;
    }
    
    /** Get the part of package name from a class name.
     * 
     *  @param classFullName The full name of a class.
     *  @return The name of the package which the class belongs
     *   to, or empty string if the class is not in any package.
     */
    private String _getPackageName(String classFullName) {
        int dotPos = classFullName.lastIndexOf('.');
        return dotPos < 0 ? "" : classFullName.substring(0, dotPos);
    }
    
    /** Get the simple class name of a class (not including any
     *  "." or "$"). The same function is provided in {@link
     *  Class} class in Java 1.5.
     *  
     *  @param c The class object.
     *  @return The simple name of the class object.
     */
    private String _getSimpleClassName(Class c) {
        String name = c.getName();
        int lastSeparator1 = name.lastIndexOf('.');
        int lastSeparator2 = name.lastIndexOf('$');
        int lastSeparator =
            lastSeparator1 >= lastSeparator2 ? lastSeparator1 : lastSeparator2;
        return name.substring(lastSeparator + 1);
    }
    
    /** Import a class with its full name, using "." instead
     *  of "$" to separate names of nested classes when they
     *  are refered to. The imported class is added to a
     *  {@link Hashtable} to be looked up in name resolving.
     *  
     *  @param classFullName The full name of the class to be
     *   imported.
     */
    private void _importClass(String classFullName) {
        int lastDotPos = classFullName.lastIndexOf('.');
        String simpleName = classFullName.substring(lastDotPos + 1);
        try {
            _importedClasses.put(simpleName, _loader.loadClass(classFullName));
        } catch (ClassNotFoundException e) {
            throw new ASTClassNotFoundException(classFullName);
        }
    }
    
    /** Record all the fields of the currently inspecting class
     *  ({@link #_currentClass}) in the variable table on the
     *  top of the variable stack ({@link #_variableStack}). 
     */
    private void _recordFields() {
        Class c = _currentClass;
        Hashtable table = (Hashtable)_variableStack.peek();
        while (c != null) {
            Field[] fields = c.getDeclaredFields();
            for (int i=0; i<fields.length; i++) {
                Field field = fields[i];
                
                // FIXME: Ignore volatile fields.
                if (Modifier.isVolatile(field.getModifiers()))
                    continue;
                
                String fieldName = field.getName();
                if (!table.containsKey(fieldName)) {
                    Class fieldType = field.getType();
                    table.put(fieldName, Type.createType(fieldType.getName()));
                }
            }
            
            Class[] interfaces = c.getInterfaces();
            for (int i=0; i<interfaces.length; i++) {
                fields = interfaces[i].getDeclaredFields();
                for (int j=0; j<fields.length; j++) {
                    String fieldName = fields[j].getName();
                    if (!table.containsKey(fieldName)) {
                        Class fieldType = fields[j].getType();
                        table.put(fieldName, Type.createType(fieldType.getName()));
                    }
                }
            }
            
            Class superclass = c.getSuperclass();
            if (superclass == null && !c.getName().equals("java.lang.Object"))
                superclass = Object.class;
            c = superclass;
        }
    }
    
    /** Remove the part of package name from a class name.
     * 
     *  @param classFullName The full name of a class.
     *  @return The string with package name removed from the
     *   class name. If the class is not in any package, its
     *   name is returned unmodified.
     */
    private String _removePackageName(String classFullName) {
        int dotPos = classFullName.lastIndexOf(".");
        return dotPos < 0 ? classFullName : classFullName.substring(dotPos + 1);
    }

    ///////////////////////////////////////////////////////////////////
    ////                        private fields                     ////

    /** The stack of currently opened scopes for variable
     *  declaration. Each element is a {@link Hashtable}. In each table,
     *  keys are variable names while values are {@link Type}'s of the
     *  corresponding variables.
     */
    private Stack _variableStack = new Stack();
    
    /** The class loader used to load classes.
     */
    private LocalClassLoader _loader;
    
    /** The class currently being analyzed, whose declaration is opened
     *  most recently. <tt>null</tt> only when the analyzer is analyzing
     *  the part of source code before any class definition ("package"
     *  and "import").
     */
    private Class _currentClass;
    
    /** The stack of previously opened class, which has not been closed
     *  yet.
     */
    private Stack _previousClasses = new Stack();
    
    /** Table of all the explicitly imported classes (not including
     *  package importations). Keys are class names; values are {@link
     *  Class} objects.
     */
    private Hashtable _importedClasses = new Hashtable();
    
    /** The {@link SummaryBuilder} object used to build the summary of
     *  the root AST node (<tt>CompilationUnit</tt>).
     */
    private SummaryBuilder _summaryBuilder = new SummaryBuilder();
    
    /** The visitor used to visit the top level summary ({@link
     *  FileSummary}) and fix anonymous class numbers.
     */
    private AnonymousClassVisitor _anonymousNumbers = new AnonymousClassVisitor();
}
