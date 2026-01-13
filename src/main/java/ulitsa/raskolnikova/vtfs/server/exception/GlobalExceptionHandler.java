package ulitsa.raskolnikova.vtfs.server.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.nio.ByteBuffer;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(FileSystemException.class)
    public ResponseEntity<byte[]> handleFileSystemException(FileSystemException e) {
        log.error("FileSystemException: code={}, message={}", e.getErrorCode(), e.getMessage());
        
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.order(java.nio.ByteOrder.LITTLE_ENDIAN);
        buffer.putLong(e.getErrorCode());
        
        return ResponseEntity.ok(buffer.array());
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<byte[]> handleGenericException(Exception e) {
        log.error("Unexpected error", e);
        
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.order(java.nio.ByteOrder.LITTLE_ENDIAN);
        buffer.putLong(-HttpStatus.INTERNAL_SERVER_ERROR.value());
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(buffer.array());
    }
}
