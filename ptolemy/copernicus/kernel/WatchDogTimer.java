/* A transformer that calls exit after a certain amount of time

 Copyright (c) 2001 The Regents of the University of California.
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
@ProposedRating Red (cxh@eecs.berkeley.edu)
@AcceptedRating Red (cxh@eecs.berkeley.edu)
*/


package ptolemy.copernicus.kernel;

import soot.*;
import soot.jimple.*;
import soot.toolkits.scalar.*;
import soot.util.*;
import soot.toolkits.graph.*;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/** 
A transformer that calls System.exit() after a certain amount of time.
This transformer is useful for killing tests that are in tight loops

@author Stephen Neuendorffer, Christopher Hylands
@version $Id$
*/
public class WatchDogTimer extends SceneTransformer {
    private static WatchDogTimer instance = new WatchDogTimer();
    private WatchDogTimer() {}

    public static WatchDogTimer v() {
        return instance; 
    }

    public String getDeclaredOptions() { 
        return super.getDeclaredOptions() + " debug time"; 
    }
    
    /** Start up a watch dog timer that will call System.exit().
     *  Sample option arguments:
     *  <pre>
     *	-p wjtp.watchDogTimer time:10000
     *  </pre>
     *  means that exit will be called in 10,000 ms, or 10 seconds
     *
     *  @param phaseName The name of the phase, for example 
     *  <code>wjtp.watchDogTimer</code>.
     *  @param options The options Map.  This method uses the
     *  <code>time</code> option to specify the number of milliseconds
     *  until System.exit() should be called.
     */ 
    protected void internalTransform(String phaseName, Map options)
    {
        System.out.println("WatchDogTimer.internalTransform("
                + phaseName + ", " + options + ")");

        String timeToDieString = Options.getString(options, "time");
        if (timeToDieString == null 
                || timeToDieString.length() == 0) {
            return;
        }

        final long timeToDie = (new Long(timeToDieString)).longValue();

        // Timers are new in JDK1.3
        // For information about Timers, see
        // http://java.sun.com/docs/books/tutorial/essential/threads/timer.html

        if (timeToDie <= 0) {
            return;
        }
        Timer timer = new Timer();
        TimerTask doTimeToDie = new TimerTask() {
            public void run() {
                try {
                    System.err.println("WatchDogTimer went off after "
                            + timeToDie + "ms.");

                    // Get the root ThreadGroup
                    ThreadGroup parent, rootGroup;
                    
                    parent = Thread.currentThread().getThreadGroup();
                    do {
                        rootGroup = parent;
                        parent = parent.getParent();
                    } while (parent != null);

                    // Display all the threads
                    Thread threads[] = new Thread[rootGroup.activeCount()];
                    rootGroup.enumerate(threads);
                    for (int i = 0; i < threads.length; i++) {
                        System.err.println(i + ". " + threads[i]);
                    }

                } catch (Exception e) {
                    System.err.println(e);
                } finally {
                    // Do not pass go, do not collect $200
                    System.exit(4);
                }
            }
        };
        timer.schedule(doTimeToDie, timeToDie);
    }
}

