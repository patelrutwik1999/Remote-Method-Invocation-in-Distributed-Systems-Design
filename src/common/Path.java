package common;

import java.io.*;
import java.util.*;

/** Distributed filesystem paths.

    <p>
    Objects of type <code>Path</code> are used by all filesystem interfaces.
    Path objects are immutable.

    <p>
    The string representation of paths is a forward-slash-delimeted sequence of
    path components. The root directory is represented as a single forward
    slash.

    <p>
    The colon (<code>:</code>) and forward slash (<code>/</code>) characters are
    not permitted within path components. The forward slash is the delimeter,
    and the colon is reserved as a delimeter for application use.
 */

//References :- https://www.javatpoint.com/serialization-in-java
//References :- https://www.geeksforgeeks.org/iterators-in-java/
//References :- https://www.geeksforgeeks.org/collections-in-java-2/
//References :- https://www.javatpoint.com/collections-in-java
//References :- https://www.geeksforgeeks.org/arraylist-in-java/
//References :- https://www.javatpoint.com/string-tokenizer-in-java
//References :- https://www.geeksforgeeks.org/equals-hashcode-methods-java/
//References :- https://www.javatpoint.com/StringBuilder-class

public class Path implements Iterable<String>, Serializable
{
    private static final long serialVersionUID = 6641292594239992029L;
    public ArrayList<String> filePath;

    /** Creates a new path which represents the root directory. */
    public Path()
    {
        filePath = new ArrayList<String>();
    }

    /** Creates a new path by appending the given component to an existing path.

        @param path The existing path.
        @param component The new component.
        @throws IllegalArgumentException If <code>component</code> includes the
                                         separator, a colon, or
                                         <code>component</code> is the empty
                                         string.
    */
    public Path(Path path, String component)
    {
        if ((component.contains("/")) || (component.contains(":")) || (component.isEmpty())) {
            throw new IllegalArgumentException("Component has '/', ':' or is empty");
        }

        ArrayList<String> getPathComponents = path.getPathComponents();
        filePath = new ArrayList<String>();

        for (String paths : getPathComponents){
            filePath.add(paths);
        }

        this.filePath.add(component);
    }

    public Path(ArrayList<String> parentFilePath) {
        this.filePath = (ArrayList<String>) parentFilePath.clone();
    }

    private ArrayList<String> getPathComponents() {
        return this.filePath;
    }

    /** Creates a new path from a path string.

        <p>
        The string is a sequence of components delimited with forward slashes.
        Empty components are dropped. The string must begin with a forward
        slash.

        @param path The path string.
        @throws IllegalArgumentException If the path string does not begin with
                                         a forward slash, or if the path
                                         contains a colon character.
     */
    public Path(String path)
    {
        if (!path.startsWith("/") || path.contains(":")) {
            throw new IllegalArgumentException("the path string does not begin with\n" +
                    "                                         a forward slash, or if the path\n" +
                    "                                         contains a colon character.");
        }
        filePath = new ArrayList<String>();

        // It breaks the whole path in tokens where '/' has been encountered and then add all the tokens.
        StringTokenizer tokens = new StringTokenizer(path, "/");
        while (tokens.hasMoreTokens()) {
            filePath.add(tokens.nextToken());
        }
    }


    /** Returns an iterator over the components of the path.

        <p>
        The iterator cannot be used to modify the path object - the
        <code>remove</code> method is not supported.

        @return The iterator.
     */
    @Override
    public Iterator<String> iterator()
    {
        return new PathIterator();
    }

    class PathIterator implements Iterator<String> {
        Iterator<String> iterator;

        public PathIterator() {
            iterator = filePath.iterator();
        }

        @Override
        public boolean hasNext() {
            return this.iterator.hasNext();
        }

        @Override
        public String next() {
            return this.iterator.next();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Remove Not Allowed.");
        }
    }

    /** Lists the paths of all files in a directory tree on the local
        filesystem.

        @param directory The root directory of the directory tree.
        @return An array of relative paths, one for each file in the directory
                tree.
        @throws FileNotFoundException If the root directory does not exist.
        @throws IllegalArgumentException If <code>directory</code> exists but
                                         does not refer to a directory.
     */
    public static Path[] list(File directory) throws FileNotFoundException
    {
        if (!directory.exists()) {
            throw new FileNotFoundException("Directory Not Exist.");
        }
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException("Directory exists but does not refer to a directory.");
        }

        ArrayList<Path> pathArrayList = new ArrayList<>();
        ArrayList<Path> newPathArrayList = listHelper(new Path(), directory, pathArrayList);

        Path[] filePaths = new Path[0];

        return newPathArrayList.toArray(filePaths);
    }

    private static ArrayList<Path> listHelper(Path prefix, File directory, ArrayList<Path> filePath) throws FileNotFoundException {
        if (!directory.exists()){
            throw new FileNotFoundException("Directory Does not Exist.");
        }

        if (!directory.isDirectory()){
            throw new IllegalArgumentException("Directory Exist but does not refer to a directory.");
        }

        File file[] = directory.listFiles();
        for (File f : file){
            if (f.isDirectory()){
                listHelper(new Path(prefix, f.getName()), f, filePath);
            }else if(f.isFile()){
                filePath.add(new Path(prefix, f.getName()));
            }
        }
        return filePath;
    }

    /** Determines whether the path represents the root directory.

        @return <code>true</code> if the path does represent the root directory,
                and <code>false</code> if it does not.
     */
    public boolean isRoot()
    {
        return this.filePath.isEmpty();
    }

    /** Returns the path to the parent of this path.

        @throws IllegalArgumentException If the path represents the root
                                         directory, and therefore has no parent.
     */
    public Path parent()
    {
        if (this.isRoot()){
            throw new IllegalArgumentException("Path represents the root directory and has no parents.");
        }
        ArrayList<String> parentFilePath = new ArrayList<String>();
        parentFilePath.addAll(filePath);
        parentFilePath.remove(this.filePath.size()-1);
        return new Path(parentFilePath);
    }

    /** Returns the last component in the path.

        @throws IllegalArgumentException If the path represents the root
                                         directory, and therefore has no last
                                         component.
     */
    public String last()
    {
        if (this.isRoot()){
            throw new IllegalArgumentException("Path represent the root, and have no Last component.");
        }else {
            return this.filePath.get(this.filePath.size()-1);
        }
    }

    /** Determines if the given path is a subpath of this path.

        <p>
        The other path is a subpath of this path if is a prefix of this path.
        Note that by this definition, each path is a subpath of itself.

        @param other The path to be tested.
        @return <code>true</code> If and only if the other path is a subpath of
                this path.
     */
    public boolean isSubpath(Path other)
    {
        Iterator<String> otherIterator = other.iterator();
        Iterator<String> thisIterator = this.iterator();

        if (other.getPathComponents().size() > this.filePath.size()){
            return false;
        }

        while (otherIterator.hasNext()){
            if (!otherIterator.next().equals(thisIterator.next())){
                return false;
            }
        }
        return true;
    }

    /** Converts the path to <code>File</code> object.

        @param root The resulting <code>File</code> object is created relative
                    to this directory.
        @return The <code>File</code> object.
     */
    public File toFile(File root)
    {
        return new File(root.getPath().concat(this.toString()));
    }

    /** Compares two paths for equality.

        <p>
        Two paths are equal if they share all the same components.

        @param other The other path.
        @return <code>true</code> if and only if the two paths are equal.
     */
    @Override
    public boolean equals(Object other)
    {
        ArrayList<String> stringArrayList = ((Path)other).getPathComponents();

        if (stringArrayList.size() != this.filePath.size()){
            return false;
        }

        Iterator<String> otherIterator = stringArrayList.iterator();
        Iterator<String> thisIterator = this.filePath.iterator();
        while (thisIterator.hasNext()){
            if (!thisIterator.next().equals(otherIterator.next())){
                return false;
            }
        }
        return true;
    }

    /** Returns the hash code of the path. */
    @Override
    public int hashCode()
    {
        int hashCode = 0;
        int multiplier = 31;
        for (String filepath : filePath){
            hashCode += multiplier * filepath.hashCode();
            multiplier *=31;
        }
        return hashCode;
    }

    /** Converts the path to a string.

        <p>
        The string may later be used as an argument to the
        <code>Path(String)</code> constructor.

        @return The string representation of the path.
     */
    @Override
    public String toString()
    {
        StringBuilder stringbuilder = new StringBuilder();
        if (this.isRoot()){
            stringbuilder.append("/");
        }

        for (String filepath : filePath){
            stringbuilder.append("/");
            stringbuilder.append(filepath);
        }
        return stringbuilder.toString();
    }
}
