package ptolemy.actor.corba.util;


/**
 * ptolemy/actor/corba/util/CorbaIndexOutofBoundExceptionHolder.java
 * Generated by the IDL-to-Java compiler (portable), version "3.0"
 * from CorbaActor.idl
 * Thursday, January 18, 2001 7:07:58 PM PST
 */
public final class CorbaIndexOutofBoundExceptionHolder
    implements org.omg.CORBA.portable.Streamable {
    public ptolemy.actor.corba.util.CorbaIndexOutofBoundException value = null;

    public CorbaIndexOutofBoundExceptionHolder() {
    }

    public CorbaIndexOutofBoundExceptionHolder(
        ptolemy.actor.corba.util.CorbaIndexOutofBoundException initialValue) {
        value = initialValue;
    }

    public void _read(org.omg.CORBA.portable.InputStream i) {
        value = ptolemy.actor.corba.util.CorbaIndexOutofBoundExceptionHelper
            .read(i);
    }

    public void _write(org.omg.CORBA.portable.OutputStream o) {
        ptolemy.actor.corba.util.CorbaIndexOutofBoundExceptionHelper.write(o,
            value);
    }

    public org.omg.CORBA.TypeCode _type() {
        return ptolemy.actor.corba.util.CorbaIndexOutofBoundExceptionHelper
        .type();
    }
}
