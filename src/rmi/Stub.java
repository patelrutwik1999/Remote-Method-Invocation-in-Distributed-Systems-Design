package rmi;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.*;
import java.util.Arrays;

/** RMI stub factory.

    <p>
    RMI stubs hide network communication with the remote server and provide a
    simple object-like interface to their users. This class provides methods for
    creating stub objects dynamically, when given pre-defined interfaces.

    <p>
    The network address of the remote server is set when a stub is created, and
    may not be modified afterwards. Two stubs are equal if they implement the
    same interface and carry the same remote server address - and would
    therefore connect to the same skeleton. Stubs are serializable.
 */
//References :- https://www.javatpoint.com/RMI
//References :- https://www.javatpoint.com/q/4645/proxy-class#:~:text=Each%20proxy%20instance%20has%20an,the%20proxy%20instance%2C%20a%20java.
//References :- https://www.logicbig.com/how-to/code-snippets/jcode-java-invocationhandler.html
//References :- https://www.baeldung.com/java-dynamic-proxies
//References :- https://www.javatpoint.com/java-method-invoke-method

public abstract class Stub
{
    /** Creates a stub, given a skeleton with an assigned adress.

        <p>
        The stub is assigned the address of the skeleton. The skeleton must
        either have been created with a fixed address, or else it must have
        already been started.

        <p>
        This method should be used when the stub is created together with the
        skeleton. The stub may then be transmitted over the network to enable
        communication with the skeleton.

        @param c A <code>Class</code> object representing the interface
                 implemented by the remote object.
        @param skeleton The skeleton whose network address is to be used.
        @return The stub created.
        @throws IllegalStateException If the skeleton has not been assigned an
                                      address by the user and has not yet been
                                      started.
        @throws UnknownHostException When the skeleton address is a wildcard and
                                     a port is assigned, but no address can be
                                     found for the local host.
        @throws NullPointerException If any argument is <code>null</code>.
        @throws Error If <code>c</code> does not represent a remote interface
                      - an interface in which each method is marked as throwing
                      <code>RMIException</code>, or if an object implementing
                      this interface cannot be dynamically created.
     */
    public static <T> T create(Class<T> c, Skeleton<T> skeleton)
        throws UnknownHostException
    {
        // Check if the Class object or skeleton is null or not....
        if (c == null || skeleton == null) {
            throw new NullPointerException("Interface is null");
        }

        // Getting the address using skeleton's Object...
        InetSocketAddress address = skeleton.getAddress();
        if (address == null) {
            throw new IllegalStateException();
        }
        // If there is no address assigned....
        if (address.isUnresolved()) {
            throw new UnknownHostException();
        }

        //Getting all the methods and then checking whether any if the exception resembles to RMIException.
        Method[] methods = c.getDeclaredMethods();
        for (Method method : methods) {
            Class[] exceptions = method.getExceptionTypes();
            if (!(Arrays.asList(exceptions).contains(RMIException.class))) {
                throw new Error("C does not represent a remote interface");
            }
        }

        // Creating Stub....
        T stub = (T) Proxy.newProxyInstance(c.getClassLoader(), new Class[]{c}, new ProxyClass(address, c));
        return stub;
    }

    /** Creates a stub, given a skeleton with an assigned address and a hostname
        which overrides the skeleton's hostname.

        <p>
        The stub is assigned the port of the skeleton and the given hostname.
        The skeleton must either have been started with a fixed port, or else
        it must have been started to receive a system-assigned port, for this
        method to succeed.

        <p>
        This method should be used when the stub is created together with the
        skeleton, but firewalls or private networks prevent the system from
        automatically assigning a valid externally-routable address to the
        skeleton. In this case, the creator of the stub has the option of
        obtaining an externally-routable address by other means, and specifying
        this hostname to this method.

        @param c A <code>Class</code> object representing the interface
                 implemented by the remote object.
        @param skeleton The skeleton whose port is to be used.
        @param hostname The hostname with which the stub will be created.
        @return The stub created.
        @throws IllegalStateException If the skeleton has not been assigned a
                                      port.
        @throws NullPointerException If any argument is <code>null</code>.
        @throws Error If <code>c</code> does not represent a remote interface
                      - an interface in which each method is marked as throwing
                      <code>RMIException</code>, or if an object implementing
                      this interface cannot be dynamically created.
     */
    public static <T> T create(Class<T> c, Skeleton<T> skeleton,
                               String hostname)
    {
        // Check if parameters are not null....
        if (c == null || skeleton == null || hostname == null) {
            throw new NullPointerException("Interface is null");
        }

        //Getting port of the address....
        int serverPort = skeleton.getAddress().getPort();

        //Getting address and port....
        InetSocketAddress address = new InetSocketAddress(hostname, serverPort);

        //Getting all the methods and then checking whether any if the exception resembles to RMIException.
        Method[] methods = c.getDeclaredMethods();
        for (Method method : methods) {
            Class[] exceptions = method.getExceptionTypes();
            if (!(Arrays.asList(exceptions).contains(RMIException.class))) {
                throw new Error("C does not represent a remote interface");
            }
        }


        //Creating stub....
        T stub = (T) Proxy.newProxyInstance(c.getClassLoader(), new Class[]{c}, new ProxyClass(address, c));
        return stub;
    }

    /** Creates a stub, given the address of a remote server.

        <p>
        This method should be used primarily when bootstrapping RMI. In this
        case, the server is already running on a remote host but there is
        not necessarily a direct way to obtain an associated stub.

        @param c A <code>Class</code> object representing the interface
                 implemented by the remote object.
        @param address The network address of the remote skeleton.
        @return The stub created.
        @throws NullPointerException If any argument is <code>null</code>.
        @throws Error If <code>c</code> does not represent a remote interface
                      - an interface in which each method is marked as throwing
                      <code>RMIException</code>, or if an object implementing
                      this interface cannot be dynamically created.
     */
    public static <T> T create(Class<T> c, InetSocketAddress address)
    {
        // Check if parameters are not null....
        if (address == null || c == null) {
            throw new NullPointerException();
        }

        //Getting all the methods and then checking whether any if the exception resembles to RMIException.
        Method[] methods = c.getDeclaredMethods();
        for (Method method : methods) {
            Class[] exceptions = method.getExceptionTypes();
            if (!(Arrays.asList(exceptions).contains(RMIException.class))) {
                throw new Error("C does not represent a remote interface");
            }
        }

        // Create the proxy Handler
        ProxyClass proxyClass = new ProxyClass(address, c);

        //Creating Stub.....
        T stub = (T) Proxy.newProxyInstance(c.getClassLoader(), new Class[]{c}, proxyClass);
        return stub;
    }
}

//References :- https://www.javatpoint.com/serialization-in-java
//References :- https://www.tutorialspoint.com/java/java_serialization.htm
//References :- https://www.javatpoint.com/java-integer-hashcode-method
//References :- https://www.journaldev.com/21095/java-equals-hashcode

//Proxy Class...This will implement Invocation Handler and Serializable to handle the logic of Proxy Class
//and send object to skeleton by using serializable...
class 	ProxyClass implements InvocationHandler, Serializable {
    public InetSocketAddress address;
    public Class<?> c;

    public ProxyClass(InetSocketAddress address,Class<?> c) {
        this.address = address;
        this.c = c;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable {
        String methodName = method.getName();
        Class[] argTypes = method.getParameterTypes();
        Object result = null;
        boolean success = false;


        //Implementing toString logic...
        if (methodName.equals("equals")) {
            Proxy s = (Proxy) args[0];
            if (s == null) {
                return false;
            }
            // To Check, c is equal to proxy's c and the addresses are also equal....
            if (this.c.equals(((ProxyClass) Proxy.getInvocationHandler(s)).c) &&
                    this.address.equals(((ProxyClass) Proxy.getInvocationHandler(s)).address)){
                return true;
            } else {
                return false;
            }
        }
        else if (methodName.equals("toString")) {
            return "Interface " + c.getClass().toString() + " @ " + address.toString();
        } else if (methodName.equals("hashCode")) {
            return this.c.hashCode() + this.address.hashCode();
        } else {
            Socket clientSocket = new Socket();
            try {
                ObjectOutputStream out = null;
                ObjectInputStream in   = null;

                clientSocket.connect(this.address);

                out = new ObjectOutputStream(clientSocket.getOutputStream());
                out.flush();
                in = new ObjectInputStream(clientSocket.getInputStream());

                out.writeObject(methodName);

                out.writeObject(argTypes);

                out.writeObject(args);

                success = (boolean) in.readObject();
                result = in.readObject();

                clientSocket.close();
            }
            catch (Exception e) {
                clientSocket.close();
                throw new RMIException("Hey man, you fail!!");
            }
            if (success == false) {
                throw ((Throwable) result);
            }
        }
        return result;
    }

}


