package tipl.util;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedList;
import java.util.List;

import static java.nio.file.Files.*;

/**
 * Basically a fancy wrapper for a string class with added functionality
 * A class that allows paths to be specified in the format
 * path::extension and be parsed properly / handled correctly by
 * IO plugins which need to save stacks in folders in the proper format
 * the default extension is "" which maps to the first writing tool.
 * Additionally the tool support multiple types of path from local, to 
 * virtual, and hadoop formats making it clearer which can be read
 *
 * @author mader
 */
public class LocalTypedPath extends TypedPath.SimpleTypedPath {

    final protected PATHTYPE pathType = PATHTYPE.LOCAL;

    @Deprecated
    public LocalTypedPath(final String inStr) {
         this(inStr,"");
    }
    /**
     * Create a typed path from a local file object (ensures it is in fact local)
     * @param localF a file object
     * @return the typed path
     */
    public LocalTypedPath(final File localF) { super(localF.getAbsolutePath(),"");}
    
    private LocalTypedPath(final String path, final String type) {
    	super(path,type);
    }

    private LocalTypedPath(final String newPath, final LocalTypedPath oldPath) {
        super(newPath,oldPath);
    }

    /** 
     * Get a file object from the path (safer than creating it from the getPath command
     * @return a file object
     */
    public File getFile() {
    	if(isLocal()) return new File(getPath());
    	else throw new IllegalArgumentException("A file-based object cannot be created from:"+pathType+" paths");
    }

    @Override
    public FileObject getFileObject() {
        final Path absPath = Paths.get(getFile().getAbsolutePath());
        return new FileObject() {

            @Override
            public byte[] getData() {
                try {
                    return readAllBytes(absPath);
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new IllegalArgumentException(absPath+" could not be read! "+e);
                }
            }

            @Override
            public String[] getText() {
                try {
                    return readAllLines(absPath).toArray(new String[]{});
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new IllegalArgumentException(absPath+" could not be read! "+e);
                }
            }

            @Override
            public InputStream getInputStream() {
                try {
                    return Files.newInputStream(absPath);
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new IllegalArgumentException(absPath+" could not be read! "+e);
                }
            }

            @Override
            public OutputStream getOutputStream() {
                try {
                    return Files.newOutputStream(absPath);
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new IllegalArgumentException(absPath+" could not be written! "+e);
                }
            }
        };
    }

    @Override
    public PATHTYPE getPathType() {
        return PATHTYPE.LOCAL;
    }

    public TypedPath getParent() {
    	return new LocalTypedPath(makeAbsPath().getFile().getParentFile());
    }

    public boolean isReadable() {return exists();}

    @Override
    public TypedPath changePath(String newPath) {
        return new LocalTypedPath(newPath,this);
    }

    public boolean isLocal() {return true;}


    protected File getFileObj() {
        if (!isLocal()) throw new IllegalArgumentException(summary()+" is not local and can not be converted to an file object");
        return new File(getPath());
    }
    /** 
     * Make the path absolute only works on local filesystems
     * @return
     */
    public TypedPath makeAbsPath() {

    	final String newPath = getFileObj().getAbsolutePath();
    	return new LocalTypedPath(newPath,this);
    }

    public TypedPath append(String fileName) {
    	return new LocalTypedPath(getPath()+fileName,this);
    }


    public TypedPath appendDir(String dirName, boolean createDir) {
        TypedPath outPath = append(getPathSeparator()+dirName+getPathSeparator());
        if (createDir) {
            switch(pathType) {
                case LOCAL:
                    outPath.getFile().mkdirs();
                    break;
                default:
                    System.err.println("Creating directories for types:"+pathType+" does not make sense");
            }
        }
        return outPath;
    }

    public TypedPath appendDir(String dirName) { return appendDir(dirName,true); }

    /**
     * Return true if the current path is empty
     * @note now  this is narrowly defined as equaling ""
     * @return
     */
    public boolean isEmpty() {
        return this.getPath().equals("");
    }


    /**
     * Get the contents of a directory
     */
    public boolean isDirectory() {
        switch(pathType) {
            case LOCAL:
                return getFileObj().isDirectory();
            case REMOTE:
                throw new IllegalArgumentException(summary()+" is not local and cannot (yet) be done on non-local paths");
            default:
                System.err.println("Creating directories for types:"+pathType+" does not make sense");
                return false;
        }
    }
    public TypedPath[] listFiles() { return listFiles(PathFilter.empty);}

    /**
     * get the contents of a typedpath object
     * @param pf the criteria for including and image
     * @return an array of preserved images
     */
    public TypedPath[] listFiles(PathFilter pf) {
        if(isDirectory()) {
            switch(pathType) {
                case LOCAL:
                    File[] fileList = getFileObj().listFiles();
                    List<TypedPath> outList = new LinkedList<TypedPath>();
                    for(File cFile : fileList) {
                        final TypedPath cPath = new LocalTypedPath(cFile);
                        if (pf.accept(cPath)) {
                            outList.add(cPath);
                        }
                    }
                    return outList.toArray(new TypedPath[outList.size()]);
                case REMOTE:
                    throw new IllegalArgumentException(summary() + " is not local and cannot (yet) be done on non-local paths");

                default:
                    System.err.println("Listing directories for types:" + pathType + " does not " +
                            "make sense");
                    return new TypedPath[] {};
            }
        } else {
            System.err.println(summary() + " is not a directory object and has no " +
                    "files");
            return new TypedPath[] {};
        }
    }

    public boolean exists() {
        switch(pathType) {
            case LOCAL:
               return getFile().exists();
            default:
                System.err.println("Existence cannot be verified for non-local objects, so " +
                        "hoped to be true:" +
                        pathType + this);
                return true;
        }
    }

    @Override
    public boolean delete() {
        return DeleteFile(this, "Unk");
    }

    @Override
    public boolean recursiveDelete() {
        return RecursivelyDelete(this);
    }
    /**
     * Delete files
     */
    public static boolean DeleteFile(final TypedPath file, final String whoDel) {
        if (!file.isLocal()) throw new IllegalArgumentException("File must be local for delete function to work:"+file.summary());

        final File f1 = new File(file.getPath());
        final boolean success = f1.delete();
        if (!success) {
            System.out.println(whoDel + "\t" + "ERROR:" + file
                    + " could not be deleted.");
            return false;
        } else {
            System.out.println(whoDel + "\t" + file + " successfully deleted.");
            return true;
        }
    }
    public static boolean RecursivelyDelete(final TypedPath delName) {
        assert(delName.isLocal()); // needs to be local
        Path directory = Paths.get(delName.getPath());
        try {
            Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }

            });
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Directory:"+directory+" could not be recursively deleted");
            return false;
        }
    }

    protected static void nonLocalError(final TypedPath inst, String message) throws
            IllegalArgumentException {
        throw new IllegalArgumentException(message+", "+inst.summary()+
                " is not local and cannot (yet) be done on non-local paths");

    }

}