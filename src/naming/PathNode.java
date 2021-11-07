package naming;

import common.Path;
import storage.Command;
import storage.Storage;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Storage and Command stub pair
 */
//References :- https://www.youtube.com/watch?v=AWaSacP-hTE
//References :- https://www.baeldung.com/java-binary-tree
//References :- https://www.youtube.com/watch?v=GURClZeR96E
//References :- https://www.researchgate.net/figure/The-tree-structure-of-Domain-Name-System_fig3_2563251

class ServerStubs extends PathNode1{
    public Storage storageStub;
    public Command commandStub;

    public ServerStubs(String name, Path path, Storage storageStub, Command commandStub) {
        super(name, path);
        this.storageStub = storageStub;
        this.commandStub = commandStub;
    }

    public Storage getStorage(){
        return this.storageStub;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ServerStubs that = (ServerStubs) o;

        return storageStub.equals(that.storageStub) && commandStub.equals(that.commandStub);
    }


    @Override
    public int hashCode() {
        int result = storageStub.hashCode();
        result = 31 * result + commandStub.hashCode();

        return result;
    }

    @Override
    public boolean isDirectory() {
        return false;
    }
}

abstract class PathNode1 {
    private String name;
    private Path nodePath;
    private int accessTime;
    private ServerStubs serverStubs;    // Storage server of the original copy
    private HashSet<ServerStubs> replicaStubs;
    private HashMap<String, PathNode> childNodes;

    public PathNode1(String name, Path p) {
        this.name = name;
        this.nodePath = p;
        accessTime = 0;
        serverStubs = null;
        replicaStubs = new HashSet<>();
        childNodes = new HashMap<>();
    }

    public String getName() {
        return this.name;
    }

    public Path getPath() {
        return this.nodePath;
    }

    public abstract boolean isDirectory();

    public boolean isFile() { return serverStubs != null; }

    public ServerStubs getStubs() { return serverStubs; }

    public void setStubs(ServerStubs stubs) { serverStubs = stubs; }

    public HashMap<String, PathNode> getChildren() { return childNodes; }

    public HashSet<ServerStubs> getReplicaStubs() {
        return replicaStubs;
    }

    public void addReplicaStub(ServerStubs serverStubs) {
        // Naming server will ensure the nodes calling
        // this method refers to a file, not a directory
        replicaStubs.add(serverStubs);
    }

    public int getReplicaSize() {
        return replicaStubs.size();
    }

    public void removeReplicaStub(ServerStubs serverStubs) {
        replicaStubs.remove(serverStubs);
    }

    /** Increase the node's access time

     <p>
     Return true if the access time is beyond the pre-set multiple
     and then reset the access time to 0.
     */
    public boolean incAccessTime(int multiple) {
        if (++accessTime > multiple) {
            accessTime = 0;
            return true;
        }

        return false;
    }

    public void resetAccessTime() {
        accessTime = 0;
    }

}

class PathNode extends PathNode1{

    public ArrayList<PathNode1> files;

    public PathNode(String name, Path path){
        super(name, path);
        this.files = new ArrayList<PathNode1>();
    }

    public PathNode1 getNodeByPath(Path path) throws FileNotFoundException {

        Path path1 = new Path(path.filePath);
        return getNodeByPathHelper(path1);
    }

    public PathNode1 getNodeByPathHelper(Path path1) throws FileNotFoundException {

        if (path1.filePath.size() == 0) {
            return this;
        }

        String firstNode = path1.filePath.get(0);

        if (path1.filePath.size() == 1) {
            for (PathNode1 pathNode1 : files) {
                if (pathNode1.getName().equals(firstNode)) {
                    return pathNode1;
                }
            }
        } else {
            for (PathNode1 pathNode2 : this.files) {
                if (pathNode2.getName().equals(firstNode) && pathNode2.isDirectory()) {
                    path1.filePath.remove(0);
                    return ((PathNode) pathNode2).getNodeByPathHelper(path1);
                } else if (pathNode2.getName().equals(firstNode) && !pathNode2.isDirectory()) {
                    throw new FileNotFoundException("Enter Path Correctly, Man !!!");
                }
            }
        }
        throw new FileNotFoundException("Path is Alone, it represents no file.....!!!!");
    }

    public boolean addChild(Path path, Command command,
                                   Storage storage) {
        if (path.isRoot()) {
            return true;
        }
        Path path1 = new Path(path.filePath);
        return addChildHelper(path1, command, storage, new Path());
    }

    public boolean addChildHelper(Path path1, Command command, Storage storage,
                          Path path2) {

        if (path1.filePath.size() == 0) {
            return true;
        }

        String firstNode = path1.filePath.get(0);
        path2.filePath.add(firstNode);

        if (path1.filePath.size() == 1) {
            for (PathNode1 pathNode1 : files) {
                if (pathNode1.getName().equals(firstNode)) {
                    return false;
                }
            }
            this.files.add(new ServerStubs(firstNode, path2, storage, command));
            return true;
        }

        for (PathNode1 pathNode2 : files) {

            if (pathNode2.getName().equals(firstNode) && pathNode2.isDirectory()) {
                path1.filePath.remove(0);
                return ((PathNode) pathNode2).addChildHelper(path1, command, storage, path2);
            }
            else if (pathNode2.getName().equals(firstNode) && !(pathNode2.isDirectory())) {
                return false;
            }
        }

        PathNode pathNode3 = new PathNode(firstNode, path2);
        this.files.add(pathNode3);
        path1.filePath.remove(0);

        return pathNode3.addChildHelper(path1, command, storage, path2);
    }


    public Set<Storage> deleteDirectory() throws InterruptedException {
        Set<Storage> storageSet = new HashSet<Storage>();
        for (PathNode1 pathNode1 : this.files) {
            if (!pathNode1.isDirectory()) {
                storageSet.add(((ServerStubs) pathNode1).getStorage());
            } else {
                Set<Storage> s1 = ((PathNode) pathNode1).deleteDirectory();
                storageSet.addAll(s1);
            }
        }
        return storageSet;
    }

    public boolean deleteChild(Path path) {
        String roadToLeaf = path.filePath.get(path.filePath.size()-1);
        for (PathNode1 pathNode1 : files) {
            if (pathNode1.getName().equals(roadToLeaf)) {
                files.remove(pathNode1);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isDirectory() {
        return true;
    }
}
