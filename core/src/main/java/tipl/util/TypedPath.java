package tipl.util;

import java.io.File;
import java.io.Serializable;

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
public class TypedPath implements Serializable {
    final protected String inStr;
    final protected String inType;
    
    public static enum PATHTYPE {
    	LOCAL, VIRTUAL, HADOOP
    }
    final protected TypedPath.PATHTYPE pathType;
    /**
     * A cloning command
     * @param inTp
     */
    public TypedPath(final TypedPath inTp) {
        this.inStr = inTp.getPath();
        this.inType = inTp.getType();
        this.pathType = inTp.pathType;
    }
    
    protected TypedPath(final String inStr,TypedPath.PATHTYPE inPathType) {
    	String[] spStr = inStr.split("::");
        if (spStr.length > 1) {
            this.inStr = spStr[0];
            this.inType = spStr[1];
        } else {
            this.inStr = inStr;
            this.inType = "";
        }
        this.pathType = inPathType;
    }
    protected TypedPath(final String newPath,final TypedPath oldTp) {
    	this.inStr = newPath;
        this.inType = oldTp.getType();
        this.pathType = oldTp.pathType;
    }
    public TypedPath(final String inStr) {
         this(inStr,PATHTYPE.LOCAL);
    }
    
    /**
     * For files or datasets not based directly on real files (transforms of images)
     * @note The construction of File or Stream objects from these objects will eventually throw an error
     * @param virtualName
     * @return
     */
    public static TypedPath virtualPath(final String virtualName) {
    	return new TypedPath(virtualName,PATHTYPE.VIRTUAL);
    }
    public static TypedPath hadoopPath(final String hadoopPath) {
    	return new TypedPath(hadoopPath,PATHTYPE.HADOOP);
    }
    /** 
     * Get a file object from the path (safer than creating it from the getPath command
     * @return a file object
     */
    public File getFile() {
    	if(isLocal()) return new File(getPath());
    	else throw new IllegalArgumentException("A file-based object cannot be created from:"+pathType+" paths");
    }
    
    public int length() { return getPath().length();}

    public boolean isReadable() {
    	return ((pathType==PATHTYPE.LOCAL) || (pathType==PATHTYPE.HADOOP));
    }
    
    public boolean isLocal() {
    	return ((pathType==PATHTYPE.LOCAL));
    }

    public String getType() {
        return this.inType;
    }

    public String getPath() {
        return this.inStr;
    }

    public String toString() {
        return inStr;
    }
    public String summary() {
    	return "Path:"+getPath()+",Type:"+getType()+",FS:"+pathType;
    }
    /** 
     * Make the path absolute only works on local filesystems
     * @return
     */
    public TypedPath makeAbsPath() {
    	if (!isLocal()) throw new IllegalArgumentException(summary()+" is not local and can not be converted to an absolute path");
    	final String newPath = (new File(getPath())).getAbsolutePath(); 
    	return new TypedPath(newPath,this);
    }
    /** 
     * Add a filename to a directory
     * @param fileName the name of the file
     * @return the directory
     */
    public TypedPath append(String fileName) {
    	return new TypedPath(getPath()+fileName,this);
    }
}