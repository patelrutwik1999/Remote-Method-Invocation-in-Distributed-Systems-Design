package naming;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.*;

import rmi.*;
import common.*;
import storage.*;

/** Naming server.

    <p>
    Each instance of the filesystem is centered on a single naming server. The
    naming server maintains the filesystem directory tree. It does not store any
    file data - this is done by separate storage servers. The primary purpose of
    the naming server is to map each file name (path) to the storage server
    which hosts the file's contents.

    <p>
    The naming server provides two interfaces, <code>Service</code> and
    <code>Registration</code>, which are accessible through RMI. Storage servers
    use the <code>Registration</code> interface to inform the naming server of
    their existence. Clients use the <code>Service</code> interface to perform
    most filesystem operations. The documentation accompanying these interfaces
    provides details on the methods supported.

    <p>
    Stubs for accessing the naming server must typically be created by directly
    specifying the remote network address. To make this possible, the client and
    registration interfaces are available at well-known ports defined in
    <code>NamingStubs</code>.
 */

//References :- https://www.geeksforgeeks.org/java-util-random-class-java/
//References :- https://www.geeksforgeeks.org/java-util-hashmap-in-java-with-examples/
//References :- https://www.geeksforgeeks.org/set-in-java/
//References :- https://www.javatpoint.com/RMI
//References :- https://www.youtube.com/watch?v=Djf89MG7CcA
//References :- https://www.youtube.com/watch?v=X-bL0S8b6C4
//References :- https://www.youtube.com/watch?v=0pp2WvZlJmM

public class NamingServer implements Service, Registration
{
    Skeleton<Service> serviceSkeleton;
    Skeleton<Registration> registrationSkeleton;
    PathNode pathNode;
    public ArrayList<Command> commandArrayList;
    public HashMap<Command, Storage> commandStorageHashMap;
    public HashMap<Storage, Command> storageCommandHashMap;
    public HashMap<Path, Set<Storage>> pathStorageSetMap;

    /** Creates the naming server object.

        <p>
        The naming server is not started.
     */
    public NamingServer()
    {
        this.pathNode = new PathNode("/", new Path());
        this.commandArrayList = new ArrayList<Command>();
        this.commandStorageHashMap = new HashMap<Command, Storage>();
        this.pathStorageSetMap = new HashMap<Path, Set<Storage>>();
        this.storageCommandHashMap = new HashMap<Storage, Command>();

        serviceSkeleton = new Skeleton<Service>(Service.class, this, new InetSocketAddress(NamingStubs.SERVICE_PORT));
        registrationSkeleton = new Skeleton<Registration>(Registration.class, this,
                new InetSocketAddress(NamingStubs.REGISTRATION_PORT));
    }

    /** Starts the naming server.

        <p>
        After this method is called, it is possible to access the client and
        registration interfaces of the naming server remotely.

        @throws RMIException If either of the two skeletons, for the client or
                             registration server interfaces, could not be
                             started. The user should not attempt to start the
                             server again if an exception occurs.
     */
    public synchronized void start() throws RMIException
    {
        this.serviceSkeleton.start();
        this.registrationSkeleton.start();
    }

    /** Stops the naming server.

        <p>
        This method waits for both the client and registration interface
        skeletons to stop. It attempts to interrupt as many of the threads that
        are executing naming server code as possible. After this method is
        called, the naming server is no longer accessible remotely. The naming
        server should not be restarted.
     */
    public void stop()
    {
        this.serviceSkeleton.stop();
        this.registrationSkeleton.stop();
        stopped(null);
    }

    /** Indicates that the server has completely shut down.

        <p>
        This method should be overridden for error reporting and application
        exit purposes. The default implementation does nothing.

        @param cause The cause for the shutdown, or <code>null</code> if the
                     shutdown was by explicit user request.
     */
    protected void stopped(Throwable cause)
    {
    }

    // The following methods are documented in Service.java.
    @Override
    public boolean isDirectory(Path path) throws FileNotFoundException
    {
        if (path.isRoot()) {
            return true;
        }
        return pathNode.getNodeByPath(path).isDirectory();
    }

    @Override
    public String[] list(Path directory) throws FileNotFoundException
    {
        if (!isDirectory(directory)) {
            throw new FileNotFoundException(
                    "Given Path does not refer to a directory!");
        }

        ArrayList<PathNode1> listContents;

        if (directory.isRoot()) {
            listContents = this.pathNode.files;
            System.out.println("Map1 "+listContents);
        } else {
            listContents = ((PathNode) pathNode.getNodeByPath(directory)).files;
            System.out.println("Map2 "+listContents);
        }

        String[] stringArray = new String[0];
        ArrayList<String> stringArrayList = new ArrayList<String>();

        for (PathNode1 pathNode1 : listContents) {
            stringArrayList.add(pathNode1.getName());
        }

        System.out.println("ArrayList"+stringArrayList);
        return stringArrayList.toArray(stringArray);
    }

    @Override
    public boolean createFile(Path file)
        throws RMIException, FileNotFoundException
    {
        if (file == null){
            throw new NullPointerException("File Parameter is Empty.");
        }

        if (file.isRoot()){
            return false;
        }

        Path parentPath = file.parent();
        ArrayList<PathNode1> pathNodes;

        if (!isDirectory(file.parent())) {
            throw new FileNotFoundException(
                    "directory not exist.");
        }

        if (parentPath.isRoot()) {
            pathNodes = this.pathNode.files;
        } else {
            pathNodes = ((PathNode) pathNode.getNodeByPath(parentPath)).files;
        }

        for (PathNode1 pathNode1 : pathNodes) {
            if (pathNode1.getName().equals(file.last())) {
                return false;
            }
        }

        if (parentPath.isRoot()) {
            Path path = new Path(this.pathNode.getPath(), file.last());

            Random randomGenerator = new Random();
            int getRandomIndex = randomGenerator.nextInt(commandArrayList.size());
            Command command = commandArrayList.get(getRandomIndex);
            Storage storage = this.commandStorageHashMap.get(command);

            this.pathNode.files.add(new ServerStubs(file.last(), path,
                    storage, command));

            command.create(file);
        } else {
            PathNode pathNode5 = (PathNode) pathNode.getNodeByPath(parentPath);
            Path path1 = new Path(pathNode5.getPath(), file.last());
            // Give it to a storage using a randomly selected command stub
            Random random = new Random();
            int getRandomIndex = random.nextInt(commandArrayList.size());

            Command command1 = commandArrayList.get(getRandomIndex);
            Storage storage1 = this.commandStorageHashMap.get(command1);

            // Add it to the list of Files in the tree
            pathNode5.files.add(new ServerStubs(file.last(), path1, storage1,
                    command1));

            // Now create it in the storage server
            command1.create(file);
        }
        return true;
    }

    @Override
    public boolean createDirectory(Path directory) throws FileNotFoundException
    {
        if (directory == null){
            throw new NullPointerException("No directory Passed while creating directory!!!!");
        }

        if (directory.isRoot()) {
            return false;
        }

        Path parentPath = directory.parent();

        if (!isDirectory(parentPath)) {
            throw new FileNotFoundException(
                    "Injustice to Directory...SHe's not present...!!!");
        }

        ArrayList<PathNode1> pathNode1s;

        if (parentPath.isRoot()) {
            pathNode1s = this.pathNode.files;
        } else {
            pathNode1s = ((PathNode) pathNode.getNodeByPath(parentPath)).files;
        }

        for (PathNode1 pathNode3 : pathNode1s) {
            if (pathNode3.getName().equals(directory.last())) {
                return false;
            }
        }

        if (parentPath.isRoot()) {
            Path path2 = new Path(this.pathNode.getPath(), directory.last());
            this.pathNode.files.add(new PathNode(directory.last(), path2));
        } else {
            PathNode pathNode6 = (PathNode) pathNode.getNodeByPath(parentPath);
            Path path2 = new Path(pathNode6.getPath(), directory.last());
            pathNode6.files.add(new PathNode(directory.last(), path2));
        }
        return true;
    }

    @Override
    public boolean delete(Path path) throws FileNotFoundException, RMIException {
        if (path == null) {
            throw new NullPointerException("Path is Empty!!!!");
        }

        if (path.isRoot()) {
            return false;
        }

        PathNode1 ancestor = this.pathNode.getNodeByPath(path.parent());
        PathNode1 deleteFile = this.pathNode.getNodeByPath(path);

        Set<Storage> storageSet = pathStorageSetMap.remove(path);

        if (!deleteFile.isDirectory()) {
            for(Storage s : storageSet) {
                Command commandToDeleteFile = storageCommandHashMap.get(s);
                commandToDeleteFile.delete(path);
            }
            return ((PathNode) ancestor).deleteChild(path);
        } else {

            Set<Storage> storageSet1;

            try {
                storageSet1 = ((PathNode) deleteFile).deleteDirectory();
            } catch(InterruptedException e) {
                return false;
            }
            for(Storage storageIteration : storageSet1) {
                Command commandToDeleteFile1 = storageCommandHashMap.get(storageIteration);
                commandToDeleteFile1.delete(path);
            }
            ((PathNode) ancestor).files.remove(deleteFile);
        }
        return true;
    }

    @Override
    public Storage getStorage(Path file) throws RMIException, FileNotFoundException {
        if (this.pathNode.getNodeByPath(file).isDirectory()) {
            throw new FileNotFoundException("Path referred to a directory!");
        }
        return ((ServerStubs) this.pathNode.getNodeByPath(file)).getStorage();
    }


    @Override
    public Path[] register(Storage client_stub, Command command_stub,
                           Path[] files)
    {
        if (client_stub == null || command_stub == null || files == null) {
            throw new NullPointerException("Empty!!!");
        }


        if (this.commandArrayList.contains(command_stub)) {
            throw new IllegalStateException("Already Registered!!!");
        }

        this.commandArrayList.add(command_stub);
        this.commandStorageHashMap.put(command_stub, client_stub);
        this.storageCommandHashMap.put(client_stub, command_stub);

        ArrayList<Path> duplicatePathList = new ArrayList<Path>();
        Path[] pathOfDuplicateList = new Path[0];

        for (Path pathUseInRegistration : files) {
            boolean created = pathNode.addChild(pathUseInRegistration, command_stub,
                    client_stub);
            if (created == false) {
                duplicatePathList.add(pathUseInRegistration);
            } else {
                if (pathStorageSetMap.containsKey(pathUseInRegistration)) {
                    pathStorageSetMap.get(pathUseInRegistration).add(client_stub);
                } else {
                    Set<Storage> storageSetForRegistration = new HashSet<Storage>();
                    storageSetForRegistration.add(client_stub);
                    pathStorageSetMap.put(pathUseInRegistration, storageSetForRegistration);
                }
            }
        }
        return duplicatePathList.toArray(pathOfDuplicateList);
    }
}
