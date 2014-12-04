package tipl.util;

import org.scijava.annotations.Indexable;

import java.io.Serializable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A set of interfaces for dealing with FileSystem access (apes Hadoop without having the Hadoop
 * dependencies)
 * Created by mader on 12/4/14.
 */
public interface ITIPLFileSystem extends Serializable {

/**
 * FileSystem stores information about the different FileSystems
 *
 * @author mader
 *
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Indexable
public static @interface FileSystemInfo {
    String name();
    String desc();

    /**
     * is the storage type enabled in production
     */
    boolean enabled() default true;

    /**
     * does the storage require Hadoop in order to run
     * @return
     */
    boolean hadoopBased() default false;
}
    /**
     * Create a typed path object from a string
     * @param currentString
     * @return a typed path object
     */
    public TypedPath openPath(final String currentString);

    /**
     * Is the current path valid
     * @param currentString path as a string
     * @return if it is valid or not
     */
    public boolean isValidPath(final String currentString);

    /**
     * for file-systems that use a webprefix style (http:// etc), the class can also be used for
     * paths not containing :// as long as acceptNothing is true
     */
    static abstract public class WebPrefixFileSystem implements ITIPLFileSystem {
        final String[] prefixes;
        final boolean acceptNothing;

        /**
         *
         * @param acceptNothing is a :// prefix required?
         * @param acceptPrefixes the allowed prefixes case insensitive ("http", "https", etc)
         */
        public WebPrefixFileSystem(boolean acceptNothing, String... acceptPrefixes ) {
            this.acceptNothing=acceptNothing;
            prefixes=acceptPrefixes;
        }

        public boolean isValidPath(final String currentString) {
            String lowerCS = currentString.trim().toLowerCase();
            if (lowerCS.contains("://")) {
                String[] lcs = safeSplit(lowerCS);
                String currentPrefix = lcs[0];
                for(String cPrefix : prefixes) {
                    if (currentPrefix.equalsIgnoreCase(cPrefix)) {
                        return true;
                    }
                }
                return false;
            } else {
                return acceptNothing;
            }
        }

        protected String[] safeSplit(final String currentString) {
            String lowerCS = currentString.trim().toLowerCase();
            String[] outData = lowerCS.split("://");
            String[] outResult = new String[2];
            outResult[0] = outData[0];
            if (outData.length>1) {
                outResult[1] = outData[1];
            }
            else {
                outResult[1] = "";
            }
            return outResult;
        }

        final public TypedPath openPath(final String currentString) {
            if(isValidPath(currentString)) {
                String[] lcs = safeSplit(currentString);
                return openPath(lcs[0],lcs[1],currentString);
            } else {
                throw new IllegalArgumentException(currentString+" cannot be opened by class " +
                        ""+this);
            }
        }
        abstract protected TypedPath openPath(final String prefix, final String contents, final
                                              String originalString);
    }
}
