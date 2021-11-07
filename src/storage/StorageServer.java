package storage;

import java.io.*;
import java.net.*;

import common.*;
import rmi.*;
import naming.*;

/** Storage server.

    <p>
    Storage servers respond to client file access requests. The files accessible
    through a storage server are those accessible under a given directory of the
    local filesystem.
 */

//References :- https://www.javatpoint.com/how-to-create-a-file-in-java
//References :- https://docs.oracle.com/javase/tutorial/essential/io/delete.html
//References :- https://www.programiz.com/java-programming/filereader
//References :- https://www.geeksforgeeks.org/file-listfiles-method-in-java-with-examples/
//References :- https://www.javatpoint.com/java-fileoutputstream-class
//References :- https://www.youtube.com/watch?v=Djf89MG7CcA
//References :- https://www.youtube.com/watch?v=X-bL0S8b6C4
//References :- https://www.youtube.com/watch?v=0pp2WvZlJmM

public class StorageServer implements Storage, Command
{
    public File root;
    Skeleton<Storage> storageSkeleton;
    Skeleton<Command> commandSkeleton;

    /** Creates a storage server, given a directory on the local filesystem.

        @param root Directory on the local filesystem. The contents of this
                    directory will be accessible through the storage server.
        @throws NullPointerException If <code>root</code> is <code>null</code>.
    */
    public StorageServer(File root)
    {
        if (root == null){
            throw new NullPointerException("Root is Null!!!!");
        }

        if (!root.exists()){
            root.mkdir();
        }

        this.root = root;

        storageSkeleton = new Skeleton<Storage>(Storage.class, this);
        commandSkeleton = new Skeleton<Command>(Command.class, this);
    }

    /** Starts the storage server and registers it with the given naming
        server.

        @param hostname The externally-routable hostname of the local host on
                        which the storage server is running. This is used to
                        ensure that the stub which is provided to the naming
                        server by the <code>start</code> method carries the
                        externally visible hostname or address of this storage
                        server.
        @param naming_server Remote interface for the naming server with which
                             the storage server is to register.
        @throws UnknownHostException If a stub cannot be created for the storage
                                     server because a valid address has not been
                                     assigned.
        @throws FileNotFoundException If the directory with which the server was
                                      created does not exist or is in fact a
                                      file.
        @throws RMIException If the storage server cannot be started, or if it
                             cannot be registered.
     */
    public synchronized void start(String hostname, Registration naming_server)
        throws RMIException, UnknownHostException, FileNotFoundException
    {
        if (!root.exists() || root.isFile()){
            throw new FileNotFoundException("the directory with which the server was created does not exist " +
                    "or is in fact a file.");
        }
        storageSkeleton.start();
        commandSkeleton.start();

        Storage stub = (Storage)Stub.create(Storage.class, storageSkeleton, hostname);
        Command commandStub = (Command)Stub.create(Command.class, commandSkeleton, hostname);

        Path[] files = Path.list(root);
        Path[] duplicateFiles = naming_server.register(stub, commandStub, files);

        for (Path path : duplicateFiles){
            File file = path.toFile(root);
            File parentFile = new File(file.getParent());
            file.delete();

            while(!parentFile.equals(root)){
                if (parentFile.list().length == 0 ){
                    parentFile.delete();
                    parentFile = new File(parentFile.getParent());
                }else {
                    break;
                }
            }
        }
    }

    /** Stops the storage server.

        <p>
        The server should not be restarted.
     */
    public void stop()
    {
        storageSkeleton.stop();
        commandSkeleton.stop();
        this.stopped(null);
    }

    /** Called when the storage server has shut down.

        @param cause The cause for the shutdown, if any, or <code>null</code> if
                     the server was shut down by the user's request.
     */
    protected void stopped(Throwable cause)
    {
    }

    // The following methods are documented in Storage.java.
    @Override
    public synchronized long size(Path file) throws FileNotFoundException
    {
        File file1 = file.toFile(root);

        if (!file1.exists() || file1.isDirectory()){
            throw new FileNotFoundException("File not found.");
        }

        return file1.length();
    }

    @Override
    public synchronized byte[] read(Path file, long offset, int length)
        throws FileNotFoundException, IOException
    {
        File file1 = file.toFile(root);

        System.out.println("The File to read is:"+ file.toString());

        if (!file1.exists() || file1.isDirectory()){
            throw new FileNotFoundException("File cannot be found");
        }

        if ((offset< 0) || (length < 0) || (offset+length > file1.length())){
            throw new IndexOutOfBoundsException("Sequence specified is outside of the bounds of the file," +
                    "or length is negative.");
        }

        InputStream reader = new FileInputStream(file1);
        byte[] output = new byte[length];
        reader.read(output, (int) offset, length);
        reader.close();
        return output;
    }

    @Override
    public synchronized void write(Path file, long offset, byte[] data)
        throws FileNotFoundException, IOException
    {
        File file1 = file.toFile(root);
        if (!file1.exists() || file1.isDirectory()){
            throw new FileNotFoundException("File Cannot be Found.");
        }

        if (offset<0){
            throw new IndexOutOfBoundsException("Negative Offset.");
        }

        InputStream reader = new FileInputStream(file1);
        FileOutputStream writerStream = new FileOutputStream(file1);

        long readLength = Math.min(offset, file1.length());
        byte[] bytes = new byte[(int)readLength];

        reader.read(bytes);
        writerStream.write(bytes, 0, (int) readLength);
        long fillLength = offset - file1.length();
        if (fillLength > 0 ){
            for (int i=0; i<(int) fillLength; i++){
                writerStream.write(0);
            }
        }

        reader.close();
        writerStream.write(data);
        writerStream.close();
    }

    // The following methods are documented in Command.java.
    @Override
    public synchronized boolean create(Path file)
    {
        if (file == null){
            throw new NullPointerException("Null Argument.");
        }

        if (file.isRoot()){
            return false;
        }

        Path parent = file.parent();
        File parentFile = parent.toFile(root);

        if (!parentFile.exists()){
            parentFile.mkdirs();
        }

        File file1 =file.toFile(root);

        try {
            return file1.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public synchronized boolean delete(Path path)
    {
        if(path.isRoot()){
            return false;
        }

        File file = path.toFile(root);
        if (file.isFile()){
            return file.delete();
        }else {
            return delete_directory(file);
        }
    }

    private boolean delete_directory(File f) {
        if (f.isDirectory()){
            File[] files = f.listFiles();
            for (File subFile : files){
                if (!delete_directory(subFile)){
                    return false;
                }
            }
        }
        return f.delete();
    }
}
