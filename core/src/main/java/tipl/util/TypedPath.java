package tipl.util;

import java.io.*;

/**
 * Created by mader on 12/4/14.
 */
public interface TypedPath extends Serializable {
    public static enum PATHTYPE {
        UNDEFINED, LOCAL, VIRTUAL, REMOTE, IMAGEJ
    }

    /**
     * The type of the filesytem underlying the path
     * @return
     */
    public PATHTYPE getPathType();
    public TypedPath getParent();

    /**
     * The length of the path
     * @return the length of the path in characters (0 means it is empty)
     */
    public int length();

    public String getType();
    public String getPath();

    public boolean isLocal();

    @Deprecated
    public boolean isReadable();


    public TypedPath changePath(String newPath);

    /**
     * Use get FileObject instead
     * @return
     */
    @Deprecated
    public File getFile();


    public FileObject getFileObject();

    public String summary();

    /**
     * Add a filename to a directory
     * @param fileName the name of the file
     * @return the directory
     */
    public TypedPath append(String fileName);

    /**
     * append a subdirectory to the current path
     * @param dirName the name of the subdirectory
     * @param createDir make directory (recursive)
     * @return the new path object to the directory
     */
    public TypedPath appendDir(String dirName, boolean createDir);

    public TypedPath appendDir(String dirName);
    /**
     * Get the contents of a directory
     */
    public boolean isDirectory();

    public TypedPath[] listFiles(PathFilter pf);

    public boolean exists();

    public boolean checkSuffix(String suffix);

    public String getPathSeparator();

    /**
     * The full formatted path string
     * (iff a.pathString == b.pathString, a == b)
     * @return string which can be fed to the constructed to regenerate the path
     */
    public String pathString();

    public boolean delete();

    public boolean recursiveDelete();

    public boolean copyTo(TypedPath outputFile);

    /**
     * Make the path absolute only works on local filesystems
     * @return
     */
    public TypedPath makeAbsPath();

    /**
     * a more generic file tool that can easily be implemented for Hadoop and other FS later
     */
    public static interface FileObject {
        /**
         * read file in
         * @return as a binary array
         */
        public byte[] getData();
        public String[] getText();
        public InputStream getInputStream();

        public OutputStream getOutputStream();

    }

    /**
     * a non-standard filesystem which does not understand directories or listing operations and
     * has them automatically blanked out
     */
    abstract public static class NonPosixTypedPath extends SimpleTypedPath {

        protected NonPosixTypedPath(SimpleTypedPath inTp) {
            super(inTp);
        }

        protected NonPosixTypedPath(String newPath, SimpleTypedPath oldTp) {
            super(newPath, oldTp);
        }

        protected NonPosixTypedPath(String path, String type) {
            super(path, type);
        }

        protected NonPosixTypedPath(String combPath) {
            super(combPath);
        }

        @Override
        public File getFile() {
            throw new IllegalArgumentException(this+" cannot get a file object!");
        }

        @Override
        public TypedPath getParent() {
            throw new IllegalArgumentException(this+" does not have a parent");
        }

        @Override
        public TypedPath appendDir(String dirName, boolean createDir) {
            throw new IllegalArgumentException(this+"cannot have a directory appended");
        }

        @Override
        public boolean delete() {
            System.err.println(this + " cannot be deleted");
            return false;
        }

        @Override
        public boolean recursiveDelete() {
            System.err.println(this+" cannot be deleted");
            return false;
        }

        @Override
        public boolean copyTo(TypedPath curObj) {
            System.err.println("Copy is not a default supported operation in "+this);
            return false;
        }

        @Override
        public boolean isDirectory() {
            return false;
        }

        public TypedPath makeAbsPath() { return this;}

        public boolean exists() {
            System.err.println("Using the default, naive implementation");
            return true;
        }

        public TypedPath[] listFiles(PathFilter pf) {return new TypedPath[0];}

    }
    abstract public static class SimpleTypedPath implements TypedPath {
        final protected String inStr;
        final protected String inType;
        /**
         * used for forcing a type into the image
         */
        final static String pathTypeSplitChr = "::";

        static public String[] splitPath(final String inPath) {
            return inPath.split(TypedPath.SimpleTypedPath.pathTypeSplitChr);
        }

        /**
         * A cloning command
         * @param inTp
         */
        protected SimpleTypedPath(final SimpleTypedPath inTp) {
            this.inStr = inTp.getPath();
            this.inType = inTp.getType();
        }

        protected SimpleTypedPath(final String newPath,final SimpleTypedPath oldTp) {
            String[] spStr = splitPath(newPath);
            if(spStr.length<2) {
                this.inStr = newPath;
                this.inType = oldTp.getType();
            } else {
                this.inStr=spStr[0];
                this.inType=spStr[1];
            }

        }

        protected SimpleTypedPath(final String path, final String type) {
            this.inStr = path;
            this.inType = type;
        }

        protected SimpleTypedPath(final String combPath) {
            String[] spStr = splitPath(combPath);
            if(spStr.length<2) {
                this.inStr = combPath;
                this.inType = "";
            } else {
                this.inStr=spStr[0];
                this.inType=spStr[1];
            }
        }


        public boolean isLocal() {
            return ((getPathType()==PATHTYPE.LOCAL));
        }

        public String toString() {
            return inStr;
        }

        public String summary() {
            return "Path:"+getPath()+",Type:"+getType()+",FS:"+getPathType();
        }



        /**
         * First checks the type (since some images might have the type without having the actual
         * string (maybe?)
         * @param suffix the suffix to look for in the file
         * @return true if it contains this suffix
         */
        public boolean checkSuffix(String suffix) {
            if (getType().trim().equalsIgnoreCase(suffix)) return true;
            else return getPath().trim().toLowerCase().endsWith(suffix.toLowerCase());
        }

        /**
         * Get the path separator
         * @return
         */
        public String getPathSeparator() {
            return File.separator;
        }


        public int length() { return getPath().length();}

        public String pathString() {
            if (getPathType()==PATHTYPE.LOCAL) {
                return (getType().length()>0) ? getPath()+pathTypeSplitChr+getType() : getPath();
            } else {
                return getPathType()+pathTypeSplitChr+getPath()+pathTypeSplitChr+getType();
            }
        }

        @Override
        public String getType() {return this.inType;}

        @Override
        public String getPath() {return this.getPath();}



        @Override
        public TypedPath append(String fileName) {
            return changePath(this.getPath()+fileName);
        }

        public TypedPath[] listFiles() {return listFiles(PathFilter.empty);}

        @Override
        public TypedPath appendDir(String dirName) {return appendDir(dirName,true);}

    }

    /**
     * A filter for abstract pathnames (based on FileFilter in java.io)
     */
    public interface PathFilter {
        /**
         * Tests whether or not the specified abstract pathname should be
         * included in a pathname list.
         *
         * @param  pathname  The abstract pathname to be tested
         * @return  <code>true</code> if and only if <code>pathname</code>
         *          should be included
         */
        public boolean accept(TypedPath pathname);

        public static final PathFilter empty = new PathFilter() {
            public boolean accept(TypedPath pathname) { return true;}
        };

        /**
         * generate an extension-checking path filter for multiple extension types given as a
         * list (single items are automatically turned into lists)
         *
         */
        public static final class ExtBased implements PathFilter {
            final String[] ext;
            /**
             *
             * @param ext extension to check against (with out the .)
             */
            public ExtBased(final String ext) {
                this.ext=new String[]{ext};
            }

            public ExtBased(String... exts) {
                this.ext = exts;
            }
            @Override
            public boolean accept(TypedPath pathname) {

                for (String cExt: ext) if(pathname.checkSuffix(cExt)) return true;
                return false;
            }
        }
    }

}
