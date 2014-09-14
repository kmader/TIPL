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
    	UNDEFINED, LOCAL, VIRTUAL, HADOOP,IMAGEJ
    }
    final protected TypedPath.PATHTYPE pathType;
    /**
     * The type of the path (local, virtual, etc)
     */
    public PATHTYPE getPathType() {
    	return pathType;
    }
    /**
     * A cloning command
     * @param inTp
     */
    public TypedPath(final TypedPath inTp) {
        this.inStr = inTp.getPath();
        this.inType = inTp.getType();
        this.pathType = inTp.pathType;
    }
    /**
     * Create a typed path object from a string
     * @param currentString
     * @return a typed path object
     */
    public static TypedPath IdentifyPath(final String currentString) {
    	PATHTYPE defaultType = PATHTYPE.LOCAL; // assume local until proven otherwise
    	
    	if (currentString.toLowerCase().contains("://")) defaultType = PATHTYPE.HADOOP;
    	
    	String[] spStr = currentString.split(pathTypeSplitChr);
    	final String inStr,inType;
    	
    	final PATHTYPE tempPathType;
        switch(spStr.length) {
        case 1:
        	inStr = currentString;
            inType = "";
            tempPathType = defaultType;
            break;
        case 2:
        	inStr = spStr[0];
            inType = spStr[1];
            tempPathType = defaultType;
            break;
        case 3:
        	tempPathType = PATHTYPE.valueOf(spStr[0]);
        	inStr = spStr[1];
            inType = spStr[2];
            break;
        default:
        	throw new IllegalArgumentException("Arguments:"+currentString+" ("+pathTypeSplitChr+")"+"cannot be parsed!");
        }
        return new TypedPath(inStr,inType,tempPathType);
    }
    
    final static String pathTypeSplitChr = "::";
    /**
     * Create a new typed path object
     * @param inStr the path to the object
     * @param inPathType the type (suggestion, inStr can override if it is local)
     */
    protected TypedPath(final String inStr,TypedPath.PATHTYPE inPathType) {
    	TypedPath parsePath = IdentifyPath(inStr);
    	this.inStr = parsePath.getPath();
    	this.inType = parsePath.getType();
        this.pathType = (inPathType==PATHTYPE.UNDEFINED) ? parsePath.getPathType() : inPathType;
    }
    protected TypedPath(final String newPath,final TypedPath oldTp) {
    	this.inStr = newPath;
        this.inType = oldTp.getType();
        this.pathType = oldTp.pathType;
    }
    public TypedPath(final String inStr) {
         this(inStr,PATHTYPE.UNDEFINED);
    }
    
    private TypedPath(final String path, final String type, final PATHTYPE pathType) {
    	this.inStr = path;
        this.inType = type;
        this.pathType = pathType;
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
     * Create a typed path from a currently open ImageJ window
     * @param windowName
     * @return
     */
    public static TypedPath imagejWindow(final String windowName) {
    	return new TypedPath(windowName,PATHTYPE.IMAGEJ);
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
     * The full formatted path string 
     * (iff a.pathString == b.pathString, a == b)
     * @return string which can be fed to the constructed to regenerate the path
     */
    public String pathString() {
    	if (getPathType()==PATHTYPE.LOCAL) {
    		return (getType().length()>0) ? getPath()+pathTypeSplitChr+getType() : getPath();
    	} else {
    		return getPathType()+pathTypeSplitChr+getPath()+pathTypeSplitChr+getType();
    	}
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