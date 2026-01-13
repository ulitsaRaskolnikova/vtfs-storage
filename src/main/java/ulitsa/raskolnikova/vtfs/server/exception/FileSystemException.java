package ulitsa.raskolnikova.vtfs.server.exception;

public class FileSystemException extends RuntimeException {
    
    private final int errorCode;
    
    public FileSystemException(int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public int getErrorCode() {
        return errorCode;
    }
    
    public static final int EEXIST = 1;
    public static final int ENOENT = 2;
    public static final int ENOTDIR = 3;
    public static final int ENOTEMPTY = 4;
    public static final int EPERM = 5;
    public static final int ENOMEM = 6;
    public static final int ENOSPC = 7;
}
