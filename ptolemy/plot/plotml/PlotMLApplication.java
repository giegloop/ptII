/* Plotter apllication that is capable of reading PlotML files.

@Author: Edward A. Lee

@Version: $Id$

@Copyright (c) 1997-1999 The Regents of the University of California.
All rights reserved.

Permission is hereby granted, without written agreement and without
license or royalty fees, to use, copy, modify, and distribute this
software and its documentation for any purpose, provided that the
above copyright notice and the following two paragraphs appear in all
copies of this software.

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
@ProposedRating red (eal@eecs.berkeley.edu)
@AcceptedRating red (cxh@eecs.berkeley.edu)
*/
package ptolemy.plot.plotml;

import ptolemy.plot.Message;
import ptolemy.plot.Plot;
import ptolemy.plot.PlotApplication;

import com.microstar.xml.XmlException;

import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;

//////////////////////////////////////////////////////////////////////////
//// PlotMLApplication

/**
An application that can plot data in PlotML format from a URL.
To compile and run this application, do the following (in Unix):
<pre>
    setenv CLASSPATH ../..
    javac PlotApplication.java
    java ptolemy.plot.PlotApplication
</pre>
or in a bash shell in Windows NT:
<pre>
    CLASSPATH=../..
    export CLASSPATH
    javac PlotApplication.java
    java ptolemy.plot.PlotApplication
</pre>

@author Edward A. Lee
@version $Id$
@see PlotBox
@see Plot
*/
public class PlotMLApplication extends PlotApplication {

    /** Construct a plot with no command-line arguments.
     *  It initially displays a sample plot.
     */
    public PlotMLApplication() {
        this(null);
    }

    /** Construct a plot with the specified command-line arguments.
     *  @param args The command-line arguments.
     */
    public PlotMLApplication(String args[]) {
        this(new Plot(), args);
    }

    /** Construct a plot with the specified command-line arguments
     *  and instance of plot.
     *  @param args The command-line arguments.
     */
    public PlotMLApplication(Plot plot, String args[]) {
        super(plot, args);
    }

    ///////////////////////////////////////////////////////////////////
    ////                         public methods                    ////

    /** Return a string describing this applet.
     */
    public String getAppletInfo() {
        return "PlotMLApplet 2.0: A data plotter.\n" +
            "By: Edward A. Lee, eal@eecs.berkeley.edu and\n " +
            "Christopher Hylands, cxh@eecs.berkeley.edu\n" +
            "($Id$)";
    }

    /** Create a new plot window and map it to the screen.
     */
    public static void main(String args[]) {
        PlotApplication plot = new PlotMLApplication(new Plot(), args);

        // If the -test arg was set, then exit after 2 seconds.
        if (_test) {
            try {
                Thread.currentThread().sleep(2000);
            }
            catch (InterruptedException e) {
            }
            System.exit(0);
        }
    }

    ///////////////////////////////////////////////////////////////////
    ////                         protected methods                 ////

    /** Display basic information about the application.
     */
    protected void _about() {
        Message message = new Message(
                "ptplot\n" +
                "By: Edward A. Lee, eal@eecs.berkeley.edu\n" +
                "and Christopher Hylands, cxh@eecs.berkeley.edu\n" +
                "Version 3.0, Build: $Id$\n\n"+
                "For more information, see\n" +
                "http://ptolemy.eecs.berkeley.edu/java/ptplot\n" +
                "Copyright (c) 1997-1999,\n" +
                "The Regents of the University of California.");
        message.setTitle("About Ptolemy Plot");
    }

    /** Read the specified stream.  This method checks to see whether
     *  the data is PlotML data, and if so, creates a parser to read it.
     *  If not, it defers to the parent class to read it.
     *  @param in The input stream.
     *  @exception IOException If the stream cannot be read.
     */
    protected void _read(InputStream in) throws IOException {
        // Create a buffered input stream so that mark and reset
        // are supported.
        BufferedInputStream bin = new BufferedInputStream(in);
        // Peek at the file...
        bin.mark(9);
        // Read 8 bytes in case 16-bit encoding is being used.
        byte[] peek = new byte[8];
        bin.read(peek);
        bin.reset();
        if ((new String(peek)).startsWith("<?xm")) {
            // file is an XML file.
            PlotBoxMLParser parser;
            if (plot instanceof Plot) {
                parser = new PlotMLParser((Plot)plot);
            } else {
                parser = new PlotBoxMLParser(plot);
            }
            try {
                String pwd;
                if (_directory != null) {
                    pwd = _directory;
                } else {
                    pwd = System.getProperty("user.dir");
                }
                URL docBase = new URL("file", null, pwd);
                parser.parse(docBase, bin);
            } catch (Exception ex) {
                String msg;
                if (ex instanceof XmlException) {
                    XmlException xmlex = (XmlException)ex;
                    msg =
                        "PlotMLApplication: failed to parse PlotML data:\n"
                        + "line: " + xmlex.getLine()
                        + ", column: " + xmlex.getColumn()
                        + "\nIn entity: " + xmlex.getSystemId()
                        + "\n";
                } else {
                    msg = "PlotMLApplication: failed to parse PlotML data:\n";
                }
                System.err.println(msg + ex.toString());
                ex.printStackTrace();
            }
        } else {
            super._read(bin);
        }
    }
}
