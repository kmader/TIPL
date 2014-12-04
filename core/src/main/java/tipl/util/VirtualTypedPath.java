package tipl.util;

import java.io.*;

/**
 * A virtual typed path for images only stored in memory
 * Created by mader on 12/4/14.
 */
public class VirtualTypedPath extends TypedPath.SimpleTypedPath {
    protected FileObject virtualFile = null;

    public VirtualTypedPath(String newPath, String type) {
        super(newPath,type);
    }

    protected VirtualTypedPath(String newPath, SimpleTypedPath oldTp) {
        super(newPath, oldTp);
    }

    @Override
    public PATHTYPE getPathType() {
        return PATHTYPE.VIRTUAL;
    }

    @Override
    public boolean isReadable() {
        return true;
    }

    @Override
    public TypedPath changePath(String newPath) {
        return new VirtualTypedPath(newPath,this);
    }

    @Override
    public FileObject getFileObject() {
        // create on demand
        synchronized (virtualFile) {
            if (virtualFile == null) {
                virtualFile = new VirtualFileObj();
            }
        }
        return virtualFile;

    }



    @Override
    public TypedPath[] listFiles() {
        return new TypedPath[0];
    }

    public static class VirtualFileObj implements FileObject {
        private ByteArrayOutputStream baos = new ByteArrayOutputStream();

        @Override
        public byte[] getData() {
            return baos.toByteArray();
        }

        @Override
        public String[] getText() {
            try {
                return baos.toString("UTF-8").split("\n");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                throw new IllegalArgumentException(baos+" could not be read! "+e);
            }
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(baos.toByteArray());
        }

        @Override
        public OutputStream getOutputStream() {
            return baos;
        }
    }

}
