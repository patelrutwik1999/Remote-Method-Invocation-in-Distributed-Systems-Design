package rmi;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.net.Socket;

//Reference :- https://www.w3schools.com/java/java_threads.asp
//Reference :- https://www.javatpoint.com/creating-thread
//Reference :- https://www.tutorialspoint.com/java/lang/class_getdeclaredmethod.htm

public class ReadingThread<T> implements Runnable {
    Socket socket;
    ObjectInputStream objectInputStream = null;
    ObjectOutputStream objectOutputStream = null;
    Class<T> c;
    T server;
    Boolean stop;

    public ReadingThread(Socket socket, Class<T> c, T server, Boolean stop) {
        this.socket = socket;
        this.c = c;
        this.server = server;
        this.stop = stop;
    }

    public void run() {
        try {
            if (!stop) {
                this.objectOutputStream = new ObjectOutputStream(
                        this.socket.getOutputStream());
                this.objectOutputStream.flush();
                this.objectInputStream = new ObjectInputStream(
                        this.socket.getInputStream());

                String methodName = (String) objectInputStream.readObject();
                Class[] argTypes = (Class[]) objectInputStream.readObject();
                Object[] args = (Object[]) objectInputStream.readObject();
                Method m = c.getDeclaredMethod(methodName, argTypes);
                Object result = m.invoke(server, args);
                objectOutputStream.writeObject(true);
                objectOutputStream.writeObject(result);
                socket.close();
            }
        } catch (Exception e) {
            try {
                objectOutputStream.writeObject(false);
                objectOutputStream.writeObject(e.getCause());
                socket.close();
            } catch (IOException e1) {
                System.out.println("Error in writing exception to user.");
            }
        }
    }
}
