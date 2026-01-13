package ulitsa.raskolnikova.vtfs.server.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import ulitsa.raskolnikova.vtfs.server.dto.ListResponse;
import ulitsa.raskolnikova.vtfs.server.service.FileSystemService;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class FsController {
    
    private final FileSystemService fileSystemService;
    
    @GetMapping(value = "/list", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void list(
            @RequestParam String token,
            @RequestParam(required = false) Long dir_ino,
            @RequestParam(required = false) Long file_ino,
            HttpServletResponse response) throws IOException {
        
        log.debug("list: token={}, dir_ino={}, file_ino={}", token, dir_ino, file_ino);
        
        ListResponse listResponse = fileSystemService.list(token, dir_ino, file_ino);
        
        StringBuilder sb = new StringBuilder();
        for (ListResponse.Entry entry : listResponse.getEntries()) {
            if (listResponse.isHardLinksList()) {
                sb.append(entry.getName()).append("\n");
            } else {
                sb.append(entry.getName())
                  .append("\t")
                  .append(entry.getIno())
                  .append("\t")
                  .append(entry.getMode())
                  .append("\n");
            }
        }
        
        byte[] responseData = sb.toString().getBytes(StandardCharsets.UTF_8);
        writeSuccessResponse(response, responseData);
    }
    
    @GetMapping(value = "/create", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void create(
            @RequestParam String token,
            @RequestParam Long dir_ino,
            @RequestParam String name,
            @RequestParam Integer mode,
            HttpServletResponse response) throws IOException {
        
        log.debug("create: token={}, dir_ino={}, name={}, mode={}", token, dir_ino, name, mode);
        
        Long ino = fileSystemService.create(token, dir_ino, name, mode);
        byte[] responseData = ino.toString().getBytes(StandardCharsets.UTF_8);
        writeSuccessResponse(response, responseData);
    }
    
    @GetMapping(value = "/mkdir", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void mkdir(
            @RequestParam String token,
            @RequestParam Long dir_ino,
            @RequestParam String name,
            @RequestParam Integer mode,
            HttpServletResponse response) throws IOException {
        
        log.debug("mkdir: token={}, dir_ino={}, name={}, mode={}", token, dir_ino, name, mode);
        
        Long ino = fileSystemService.mkdir(token, dir_ino, name, mode);
        byte[] responseData = ino.toString().getBytes(StandardCharsets.UTF_8);
        writeSuccessResponse(response, responseData);
    }
    
    @GetMapping(value = "/read", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void read(
            @RequestParam String token,
            @RequestParam Long file_ino,
            @RequestParam Long offset,
            @RequestParam Integer len,
            HttpServletResponse response) throws IOException {
        
        log.debug("read: token={}, file_ino={}, offset={}, len={}", token, file_ino, offset, len);
        
        byte[] data = fileSystemService.read(token, file_ino, offset, len);
        writeSuccessResponse(response, data);
    }
    
    @GetMapping(value = "/write", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void write(
            @RequestParam String token,
            @RequestParam Long file_ino,
            @RequestParam Long offset,
            @RequestParam String data,
            HttpServletResponse response) throws IOException {
        
        log.debug("write: token={}, file_ino={}, offset={}, data_length={}", token, file_ino, offset, data.length());
        
        Long written = fileSystemService.write(token, file_ino, offset, data);
        byte[] responseData = written.toString().getBytes(StandardCharsets.UTF_8);
        writeSuccessResponse(response, responseData);
    }
    
    @GetMapping(value = "/delete", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void delete(
            @RequestParam String token,
            @RequestParam Long dir_ino,
            @RequestParam String name,
            HttpServletResponse response) throws IOException {
        
        log.debug("delete: token={}, dir_ino={}, name={}", token, dir_ino, name);
        
        fileSystemService.delete(token, dir_ino, name);
        writeSuccessResponse(response, new byte[0]);
    }
    
    @GetMapping(value = "/rmdir", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void rmdir(
            @RequestParam String token,
            @RequestParam Long dir_ino,
            @RequestParam String name,
            HttpServletResponse response) throws IOException {
        
        log.debug("rmdir: token={}, dir_ino={}, name={}", token, dir_ino, name);
        
        fileSystemService.rmdir(token, dir_ino, name);
        writeSuccessResponse(response, new byte[0]);
    }
    
    @GetMapping(value = "/link", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void link(
            @RequestParam String token,
            @RequestParam Long file_ino,
            @RequestParam Long dir_ino,
            @RequestParam String name,
            HttpServletResponse response) throws IOException {
        
        log.debug("link: token={}, file_ino={}, dir_ino={}, name={}", token, file_ino, dir_ino, name);
        
        fileSystemService.link(token, file_ino, dir_ino, name);
        writeSuccessResponse(response, new byte[0]);
    }
    
    @GetMapping(value = "/truncate", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void truncate(
            @RequestParam String token,
            @RequestParam Long file_ino,
            HttpServletResponse response) throws IOException {
        
        log.debug("truncate: token={}, file_ino={}", token, file_ino);
        
        fileSystemService.truncate(token, file_ino);
        writeSuccessResponse(response, new byte[0]);
    }
    
    @GetMapping(value = "/size", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void size(
            @RequestParam String token,
            @RequestParam Long file_ino,
            HttpServletResponse response) throws IOException {
        
        log.debug("size: token={}, file_ino={}", token, file_ino);
        
        Long fileSize = fileSystemService.size(token, file_ino);
        byte[] responseData = fileSize.toString().getBytes(StandardCharsets.UTF_8);
        writeSuccessResponse(response, responseData);
    }
    
    private void writeSuccessResponse(HttpServletResponse response, byte[] data) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(8 + data.length);
        buffer.order(java.nio.ByteOrder.LITTLE_ENDIAN);
        buffer.putLong(0L);
        buffer.put(data);
        
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setContentLength(buffer.array().length);
        response.setStatus(HttpServletResponse.SC_OK);
        
        try (var out = response.getOutputStream()) {
            out.write(buffer.array());
            out.flush();
        }
    }
}
