/* Generated By:JavaCC: Do not edit this line. UParserConstants.java */
/*

 Copyright (c) 1998-2005 The Regents of the University of California.
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

                                        PT_COPYRIGHT_VERSION_3
                                        COPYRIGHTENDKEY
*/
package ptolemy.data.unit;

public interface UParserConstants {
    int EOF = 0;
    int PLUS = 5;
    int MINUS = 6;
    int MULTIPLY = 7;
    int DIVIDE = 8;
    int POWER = 9;
    int EQUALS = 10;
    int LT = 11;
    int DOLLAR = 12;
    int SEMICOLON = 13;
    int INTEGER = 14;
    int EXPONENT = 15;
    int DOUBLE = 16;
    int UNITLABEL = 17;
    int LETTER = 18;
    int PORT = 19;
    int LPAREN = 20;
    int RPAREN = 21;
    int DEFAULT = 0;
    String[] tokenImage = {
            "<EOF>", "\" \"", "\"\\r\"", "\"\\t\"", "\"\\n\"", "\"+\"", "\"-\"",
            "\"*\"", "\"/\"", "\"^\"", "\"=\"", "\"<\"", "\"$\"", "\";\"",
            "<INTEGER>", "<EXPONENT>", "<DOUBLE>", "<UNITLABEL>", "<LETTER>",
            "<PORT>", "\"(\"", "\")\"",
        };
}
