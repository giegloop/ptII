/* An attribute that provides connectivity through an XMPP server.

 Copyright (c) 1997-2012 The Regents of the University of California.
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

package org.ptolemy.ptango.lib.xmpp;

import java.util.Iterator;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.pubsub.LeafNode;
import org.jivesoftware.smackx.pubsub.Node;
import org.jivesoftware.smackx.pubsub.PayloadItem;
import org.jivesoftware.smackx.pubsub.PubSubManager;
import org.jivesoftware.smackx.pubsub.SimplePayload;
import org.jivesoftware.smackx.pubsub.Subscription;
import org.jivesoftware.smackx.pubsub.Subscription.State;

import ptolemy.actor.AbstractInitializableAttribute;
import ptolemy.actor.Executable;
import ptolemy.data.IntToken;
import ptolemy.data.StringToken;
import ptolemy.data.expr.Parameter;
import ptolemy.data.expr.StringParameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;

///////////////////////////////////////////////////////////////////
//// XMPPGateway

/** FIXME: class description
 *  FIXME: add XMPP icon
 *  @see XMPPGateway
 *  @author Marten Lohstroh
 *  @version $Id: XMPPGateway.java 64744 2012-10-24 22:51:43Z marten $
 *  @since Ptolemy II 9.0
 *  @Pt.ProposedRating Red (marten)
 *  @Pt.AcceptedRating Red (marten)
 */
public class XMPPGateway extends AbstractInitializableAttribute implements
        Executable {

    /** Construct an instance of the XMPPGateway attribute.
     *  @param container The container.
     *  @param name The name.
     *  @exception IllegalActionException If the superclass throws it.
     *  @exception NameDuplicationException If the superclass throws it.
     */
    public XMPPGateway(NamedObj container, String name)
            throws IllegalActionException, NameDuplicationException {
        super(container, name);

        server = new StringParameter(this, "server");
        server.setExpression("localhost");

        port = new Parameter(this, "port");
        port.setTypeEquals(BaseType.INT);
        port.setExpression("5222");

        username = new StringParameter(this, "username");
        username.setExpression("ptolemy"); // FIXME: use password file (see database attribute)

        password = new StringParameter(this, "password");
        password.setExpression("tUkM6Prj");

    }

    ///////////////////////////////////////////////////////////////////
    ////                         parameters                        ////

    /** Password to authenticate with. This is a string that
     *  defaults to "guest".
     */
    public Parameter password;

    /** Port number to connect to. This is a integer that
     *  defaults to 5222.
     */
    public Parameter port;

    /** Server to connect to. This is a string that
     *  defaults to "localhost".
     */
    public Parameter server;

    /** User name to authenticate with. This is a string that
     *  defaults to "guest".
     */
    public Parameter username;

    ///////////////////////////////////////////////////////////////////
    ////                       public methods                      ////

    /** Update the local variable associated with the changed attribute
     *  and disconnect from the server. 
     *  @param attribute The changed attribute.
     */
    public void attributeChanged(Attribute attribute)
            throws IllegalActionException {

        if (attribute == port) {
            _portNumber = ((IntToken) port.getToken()).intValue();
            _disconnect();

        } else if (attribute == server) {
            _serverName = ((StringToken) server.getToken()).stringValue();
            _disconnect();
        } else if (attribute == username) {
            _userName = ((StringToken) username.getToken()).stringValue();
            _disconnect();
        } else {
            super.attributeChanged(attribute);
        }
    }

    /** Return immediately. */
    @Override
    public void fire() throws IllegalActionException {
        return;
    }

    /** Attempt to connect to the server and login. Discover subscribers 
     *  and establish a subscriptions by registering the actors as listeners. 
     *  If a subscriber wants to subscribe to a non-existent node, create it.
     *  Also discover publishers and give them a reference to this attribute.
     *  This might change in the future, as changes to running models require
     *  actors to register themselves instead of being discovered.
     *  @throws IllegalActionException If unable to login, create a node, 
     *  find a node, or subscribe to a node.
     * 
     */
    public void initialize() throws IllegalActionException {
        // FIXME: if the server runs on localhost, it doesn't accept 'localhost', 
        // but requires 127.0.0.1, look into this
        String jid = _userName + '@' + _serverName;
        //"ptolemy@dhcp-45-24.eecs.berkeley.edu";//127.0.0.1"; 
        //_userName + '@' + _serverName + "/ptolemy";
        //System.out.println(_connection.getHost());

        System.setProperty("smack.debugEnabled", "true");
        XMPPConnection.DEBUG_ENABLED = true;

        _connectAndLogin();

        _manager = new PubSubManager(_connection);
        // discover XMPPSubscribers FIXME: how deep is this search?
        Iterator<?> objects = toplevel().containedObjectsIterator();
        while (objects.hasNext()) {
            Object object = objects.next();
            // XMPPSubscriber found
            if (object instanceof XMPPSubscriber) {
                LeafNode node;
                XMPPSubscriber subscriber = (XMPPSubscriber) object;
                String nodeId = subscriber.getNodeId();
                try {
                    //_manager.deleteNode(nodeId);
                    node = (LeafNode) _manager.getNode(nodeId); // FIXME: cast problem here
                    // FIXME: get rid of duplicate subscriptions here
                } catch (Exception e) {
                    try {
                        /* ConfigureForm form = new ConfigureForm(FormType.submit); 
                         // FIXME: figure out configuration options
                         form.setAccessModel(AccessModel.open);
                         form.setDeliverPayloads(false);
                         form.setNotifyRetract(true);
                         form.setPersistentItems(true);
                         form.setPublishModel(PublishModel.open); */
                        node = _manager.createNode(nodeId);

                    } catch (XMPPException e1) {
                        throw new IllegalActionException(
                                "Unable find or create node: " + nodeId + ".");
                    }
                }
                if (node != null) {
                    // register listener
                    node.addItemEventListener(subscriber);
                    try {
                        Subscription subscription = null;
                        for (Subscription s : node.getSubscriptions()) {
                            if (s.getJid().equals(jid)) {
                                // check if state is OK
                                if (s.getState().equals(State.subscribed)) {
                                    subscription = s;
                                    break;
                                } else {
                                    node.unsubscribe(jid, s.getId());
                                }
                            }
                        }
                        // subscribe to node if not exists
                        if (subscription == null) {
                            subscription = node.subscribe(jid); // FIXME: subscription fails here
                        }
                        // FIXME: check subscription.state here too
                    } catch (XMPPException e) {
                        throw new IllegalActionException(
                                "Unable subscribe to node: " + nodeId + " ("
                                        + e.getMessage() + ").");
                    }
                } else {
                    throw new IllegalActionException(
                            "Unable find or create node: " + nodeId + ".");
                }
            } else if (object instanceof XMPPPublisher) {
                ((XMPPPublisher) object).setGateway(this);
            }
        }

    }

    /** Return false. */
    @Override
    public boolean isFireFunctional() {
        return false;
    }

    /** Return true. */
    @Override
    public boolean isStrict() throws IllegalActionException {
        return true;
    }

    /** Return immediately. */
    @Override
    public int iterate(int count) throws IllegalActionException {
        return Executable.COMPLETED;
    }

    /** Check the connection, reconnect and login if required. */
    @Override
    public boolean prefire() throws IllegalActionException {
        if (_connection == null) {
            initialize();
        } else if (!_connection.isConnected() || !_connection.isAuthenticated()) {
            _connectAndLogin();
        }
        return true;
    }

    /** Return immediately. */
    @Override
    public boolean postfire() throws IllegalActionException {
        return true;
    }

    /** Publish a value to a node. The value is wrapped into a message stanza.
     *  @param nodeId The node to publish to.
     *  @param value The value to publish.
     *  @throws IllegalActionException If publishing failed.
     */
    public void publish(String nodeId, String value)
            throws IllegalActionException {

        Node n;
        LeafNode ln;

        try {
            if ((n = _manager.getNode(nodeId)) instanceof LeafNode) {
                ln = (LeafNode) n;
            } else {
                throw new IllegalActionException(
                        "Unable to publish a node that is not a leaf.");
            }
        } catch (XMPPException e) {
            try {
                ln = _manager.createNode(nodeId);
            } catch (XMPPException e1) {
                throw new IllegalActionException(
                        "Unable to create node with id: " + nodeId + ".");
            }
        }

        SimplePayload payload = new SimplePayload("message", "null",
                "<message>" + value + "</message>");
        PayloadItem<SimplePayload> item = new PayloadItem<SimplePayload>(null,
                payload);
        ln.publish(item);
    }

    /** Remove a node from the server configuration. 
     *  @param nodeId The node to remove.
     *  @throws IllegalActionException If unable to remove the node.
     */
    public void removeNode(String nodeId) throws IllegalActionException {
        try {
            _manager.deleteNode(nodeId);
        } catch (XMPPException e) {
            throw new IllegalActionException("Unable to remove node with id: "
                    + nodeId + ".");
        }
    }

    /** Return immediately. */
    @Override
    public void stop() {
        return;
    }

    /** Return immediately. */
    @Override
    public void stopFire() {
        return;
    }

    /** Return immediately. */
    @Override
    public void terminate() {
        return;
    }

    /** Disconnect from the server. */
    @Override
    public void wrapup() {
        _disconnect();
    }

    ///////////////////////////////////////////////////////////////////
    ////                      private methods                      ////

    /** Disconnect and leave old connection for the garbage collector. 
     *  Note that once disconnected, a Connection cannot be 
     *  reused immediately, therefore it is discarded. A new Connection
     *  object shall be instantiated for reconnecting.
     *  @see Connection
     *  @see _connectAndLogin()
     */
    private void _disconnect() {
        if (_connection != null) {
            _connection.disconnect();
        }
        _connection = null;
    }

    /** Connect to the server and login using the provided credentials.
     *  @throws IllegalActionException If connection or authentication 
     *  process fails.
     */
    private void _connectAndLogin() throws IllegalActionException {

        // already connected
        if (_connection != null && _connection.isConnected()
                && _connection.isAuthenticated()) {
            return;
        }

        // no active connection, establish one
        if (_connection == null) {
            ConnectionConfiguration config = new ConnectionConfiguration(
                    _serverName, _portNumber);
            config.setSecurityMode(ConnectionConfiguration.SecurityMode.enabled);
            config.setCompressionEnabled(true);
            config.setSASLAuthenticationEnabled(true);
            _connection = new XMPPConnection(config);
        }
        try {
            // connect to the server
            if (!_connection.isConnected()) {
                _connection.connect();
            }
        } catch (Exception e) {
            throw new IllegalActionException(
                    "Unable to connect to XMPP server.");
        }

        try {
            // login to the server
            if (!_connection.isAuthenticated()) {
                _connection.login(_userName, _password, "ptolemy");
            }
        } catch (Exception e) {
            throw new IllegalActionException("Unable to login to XMPP server.");
        }
    }

    ///////////////////////////////////////////////////////////////////
    ////                      private variables                    ////

    /** Maintains the connection to the server. */
    private Connection _connection;

    /** Manager responsible for brokering publications and subscriptions. */
    private PubSubManager _manager;

    /** Password on the server to connect to. */
    private String _password = "tUkM6Prj";

    /** Port number of the server to connect to. */
    private int _portNumber = 5222;

    /** Address of the server to connect to. */
    private String _serverName = "localhost";

    /** User name on the server to connect to. */
    private String _userName = "ptolemy";

}
