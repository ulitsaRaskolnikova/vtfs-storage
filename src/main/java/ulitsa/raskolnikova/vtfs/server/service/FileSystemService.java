package ulitsa.raskolnikova.vtfs.server.service;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ulitsa.raskolnikova.vtfs.server.dto.ListResponse;
import ulitsa.raskolnikova.vtfs.server.exception.FileSystemException;
import ulitsa.raskolnikova.vtfs.server.model.DirectoryEntry;
import ulitsa.raskolnikova.vtfs.server.model.FileData;
import ulitsa.raskolnikova.vtfs.server.model.Inode;
import ulitsa.raskolnikova.vtfs.server.repository.DirectoryEntryRepository;
import ulitsa.raskolnikova.vtfs.server.repository.FileDataRepository;
import ulitsa.raskolnikova.vtfs.server.repository.InodeRepository;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileSystemService {
    
    private static final int S_IFMT = 0170000;
    private static final int S_IFREG = 0100000;
    private static final int S_IFDIR = 0040000;
    
    private static final long ROOT_INODE = 1000L;
    private static final int ROOT_MODE = 16877;
    
    private final InodeRepository inodeRepository;
    private final DirectoryEntryRepository directoryEntryRepository;
    private final FileDataRepository fileDataRepository;
    private final EntityManager entityManager;
    
    @PostConstruct
    @Transactional
    public void initializeRootDirectory() {
        if (!inodeRepository.existsById(ROOT_INODE)) {
            log.warn("Root directory (ino={}) not found. It should be created by Liquibase migration.", ROOT_INODE);
            entityManager.createNativeQuery(
                String.format("INSERT INTO inodes (ino, mode, size, nlink) VALUES (%d, %d, 0, 2) ON CONFLICT DO NOTHING", 
                    ROOT_INODE, ROOT_MODE)
            ).executeUpdate();
            entityManager.createNativeQuery("SELECT setval('inodes_ino_seq', 1000, true)").executeUpdate();
            log.info("Root directory created via fallback initialization");
        } else {
            log.info("Root directory (ino={}) already exists", ROOT_INODE);
        }
    }
    
    @Transactional
    public ListResponse list(String token, Long dirIno, Long fileIno) {
        validateToken(token);
        
        if (dirIno != null) {
            return listDirectory(dirIno);
        } else if (fileIno != null) {
            return listHardLinks(fileIno);
        } else {
            throw new FileSystemException(FileSystemException.ENOENT, "dir_ino or file_ino required");
        }
    }
    
    private ListResponse listDirectory(Long dirIno) {
        Inode dir = inodeRepository.findById(dirIno)
                .orElseThrow(() -> new FileSystemException(FileSystemException.ENOENT, "Directory not found"));
        
        if (!dir.isDirectory()) {
            throw new FileSystemException(FileSystemException.ENOTDIR, "Not a directory");
        }
        
        List<DirectoryEntry> entries = directoryEntryRepository.findByParentIno(dirIno);
        List<ListResponse.Entry> result = new ArrayList<>();
        
        for (DirectoryEntry entry : entries) {
            result.add(new ListResponse.Entry(
                    entry.getName(),
                    entry.getInode().getIno(),
                    entry.getInode().getMode()
            ));
        }
        
        return new ListResponse(result, false);
    }
    
    private ListResponse listHardLinks(Long fileIno) {
        if (!inodeRepository.existsById(fileIno)) {
            throw new FileSystemException(FileSystemException.ENOENT, "File not found");
        }
        
        List<DirectoryEntry> entries = directoryEntryRepository.findByInodeIno(fileIno);
        List<ListResponse.Entry> result = new ArrayList<>();
        
        for (DirectoryEntry entry : entries) {
            result.add(new ListResponse.Entry(entry.getName()));
        }
        
        return new ListResponse(result, true);
    }
    
    @Transactional
    public Long create(String token, Long dirIno, String name, Integer mode) {
        validateToken(token);
        
        Inode parentDir = inodeRepository.findById(dirIno)
                .orElseThrow(() -> new FileSystemException(FileSystemException.ENOENT, "Parent directory not found"));
        
        if (!parentDir.isDirectory()) {
            throw new FileSystemException(FileSystemException.ENOTDIR, "Parent is not a directory");
        }
        
        if (directoryEntryRepository.existsByParentInoAndName(dirIno, name)) {
            throw new FileSystemException(FileSystemException.EEXIST, "File already exists");
        }
        
        int fileMode = S_IFREG | (mode & ~S_IFMT);
        Inode newInode = new Inode(fileMode, 0L, 1);
        newInode = inodeRepository.save(newInode);
        
        DirectoryEntry entry = new DirectoryEntry(dirIno, name, newInode);
        directoryEntryRepository.save(entry);
        
        log.info("Created file: name={}, ino={}, mode={}", name, newInode.getIno(), fileMode);
        return newInode.getIno();
    }
    
    @Transactional
    public Long mkdir(String token, Long dirIno, String name, Integer mode) {
        validateToken(token);
        
        Inode parentDir = inodeRepository.findById(dirIno)
                .orElseThrow(() -> new FileSystemException(FileSystemException.ENOENT, "Parent directory not found"));
        
        if (!parentDir.isDirectory()) {
            throw new FileSystemException(FileSystemException.ENOTDIR, "Parent is not a directory");
        }
        
        if (directoryEntryRepository.existsByParentInoAndName(dirIno, name)) {
            throw new FileSystemException(FileSystemException.EEXIST, "Directory already exists");
        }
        
        int dirMode = S_IFDIR | (mode & ~S_IFMT);
        Inode newInode = new Inode(dirMode, 0L, 2);
        newInode = inodeRepository.save(newInode);
        
        DirectoryEntry entry = new DirectoryEntry(dirIno, name, newInode);
        directoryEntryRepository.save(entry);
        
        log.info("Created directory: name={}, ino={}, mode={}", name, newInode.getIno(), dirMode);
        return newInode.getIno();
    }
    
    @Transactional
    public byte[] read(String token, Long fileIno, Long offset, Integer len) {
        validateToken(token);
        
        Inode inode = inodeRepository.findById(fileIno)
                .orElseThrow(() -> new FileSystemException(FileSystemException.ENOENT, "File not found"));
        
        if (!inode.isRegularFile()) {
            throw new FileSystemException(FileSystemException.ENOENT, "Not a regular file");
        }
        
        if (offset >= inode.getSize()) {
            return new byte[0];
        }
        
        long endOffset = Math.min(offset + len, inode.getSize());
        long actualLen = endOffset - offset;
        
        List<FileData> chunks = fileDataRepository.findChunksInRange(fileIno, offset, endOffset);
        
        if (chunks.isEmpty()) {
            return new byte[0];
        }
        
        byte[] result = new byte[(int) actualLen];
        
        for (FileData chunk : chunks) {
            long chunkStart = chunk.getOffset();
            long chunkEnd = chunkStart + chunk.getDataSize();
            
            long readStart = Math.max(offset, chunkStart);
            long readEnd = Math.min(endOffset, chunkEnd);
            
            if (readStart < readEnd) {
                int resultPos = (int) (readStart - offset);
                int chunkPos = (int) (readStart - chunkStart);
                int copyLen = (int) (readEnd - readStart);
                
                System.arraycopy(chunk.getData(), chunkPos, result, resultPos, copyLen);
            }
        }
        
        return result;
    }
    
    @Transactional
    public Long write(String token, Long fileIno, Long offset, String data) {
        validateToken(token);
        
        Inode inode = inodeRepository.findByIdForUpdate(fileIno)
                .orElseThrow(() -> new FileSystemException(FileSystemException.ENOENT, "File not found"));
        
        if (!inode.isRegularFile()) {
            throw new FileSystemException(FileSystemException.ENOENT, "Not a regular file");
        }
        
        byte[] decodedData;
        try {
            decodedData = URLDecoder.decode(data, StandardCharsets.UTF_8.name()).getBytes(StandardCharsets.UTF_8);
        } catch (UnsupportedEncodingException e) {
            throw new FileSystemException(FileSystemException.ENOENT, "Invalid data encoding");
        }
        
        int dataLen = decodedData.length;
        
        if (offset > inode.getSize()) {
            long gapSize = offset - inode.getSize();
            byte[] zeros = new byte[(int) gapSize];
            FileData gapChunk = new FileData(inode, inode.getSize(), zeros);
            fileDataRepository.save(gapChunk);
            inode.setSize(offset);
        }
        
        long writeEnd = offset + dataLen;
        List<FileData> overlappingChunks = fileDataRepository.findChunksInRange(fileIno, offset, writeEnd);
        for (FileData chunk : overlappingChunks) {
            fileDataRepository.delete(chunk);
        }
        
        int chunkSize = 4096;
        int written = 0;
        
        while (written < dataLen) {
            long chunkOffset = offset + written;
            int chunkLen = Math.min(chunkSize, dataLen - written);
            
            byte[] chunkData = new byte[chunkLen];
            System.arraycopy(decodedData, written, chunkData, 0, chunkLen);
            
            FileData newChunk = new FileData(inode, chunkOffset, chunkData);
            fileDataRepository.save(newChunk);
            
            written += chunkLen;
        }
        
        long newSize = Math.max(inode.getSize(), offset + dataLen);
        inode.setSize(newSize);
        inodeRepository.save(inode);
        
        log.info("Written {} bytes to file ino={} at offset={}", dataLen, fileIno, offset);
        return (long) dataLen;
    }
    
    @Transactional
    public void delete(String token, Long dirIno, String name) {
        validateToken(token);
        
        DirectoryEntry entry = directoryEntryRepository.findByParentInoAndName(dirIno, name)
                .orElseThrow(() -> new FileSystemException(FileSystemException.ENOENT, "File not found"));
        
        Inode inode = entry.getInode();
        
        directoryEntryRepository.delete(entry);
        
        inode.setNlink(inode.getNlink() - 1);
        
        if (inode.getNlink() == 0) {
            fileDataRepository.deleteByInodeIno(inode.getIno());
            inodeRepository.delete(inode);
            log.info("Deleted file completely: ino={}", inode.getIno());
        } else {
            inodeRepository.save(inode);
            log.info("Removed hard link: name={}, remaining nlink={}", name, inode.getNlink());
        }
    }
    
    @Transactional
    public void rmdir(String token, Long dirIno, String name) {
        validateToken(token);
        
        DirectoryEntry entry = directoryEntryRepository.findByParentInoAndName(dirIno, name)
                .orElseThrow(() -> new FileSystemException(FileSystemException.ENOENT, "Directory not found"));
        
        Inode inode = entry.getInode();
        
        if (!inode.isDirectory()) {
            throw new FileSystemException(FileSystemException.ENOTDIR, "Not a directory");
        }
        
        long entryCount = directoryEntryRepository.countByParentInoExcludingDots(inode.getIno());
        if (entryCount > 0) {
            throw new FileSystemException(FileSystemException.ENOTEMPTY, "Directory is not empty");
        }
        
        directoryEntryRepository.delete(entry);
        inodeRepository.delete(inode);
        
        log.info("Removed directory: name={}, ino={}", name, inode.getIno());
    }
    
    @Transactional
    public void link(String token, Long fileIno, Long dirIno, String name) {
        validateToken(token);
        
        Inode inode = inodeRepository.findById(fileIno)
                .orElseThrow(() -> new FileSystemException(FileSystemException.ENOENT, "File not found"));
        
        if (!inode.isRegularFile()) {
            throw new FileSystemException(FileSystemException.EPERM, "Cannot create hard link to directory");
        }
        
        Inode parentDir = inodeRepository.findById(dirIno)
                .orElseThrow(() -> new FileSystemException(FileSystemException.ENOENT, "Parent directory not found"));
        
        if (!parentDir.isDirectory()) {
            throw new FileSystemException(FileSystemException.ENOTDIR, "Parent is not a directory");
        }
        
        if (directoryEntryRepository.existsByParentInoAndName(dirIno, name)) {
            throw new FileSystemException(FileSystemException.EEXIST, "File already exists");
        }
        
        DirectoryEntry entry = new DirectoryEntry(dirIno, name, inode);
        directoryEntryRepository.save(entry);
        
        inode.setNlink(inode.getNlink() + 1);
        inodeRepository.save(inode);
        
        log.info("Created hard link: name={}, ino={}, nlink={}", name, fileIno, inode.getNlink());
    }
    
    @Transactional
    public void truncate(String token, Long fileIno) {
        validateToken(token);
        
        Inode inode = inodeRepository.findByIdForUpdate(fileIno)
                .orElseThrow(() -> new FileSystemException(FileSystemException.ENOENT, "File not found"));
        
        if (!inode.isRegularFile()) {
            throw new FileSystemException(FileSystemException.ENOENT, "Not a regular file");
        }
        
        fileDataRepository.deleteByInodeIno(fileIno);
        
        inode.setSize(0L);
        inodeRepository.save(inode);
        
        log.info("Truncated file: ino={}", fileIno);
    }
    
    @Transactional
    public Long size(String token, Long fileIno) {
        validateToken(token);
        
        Inode inode = inodeRepository.findById(fileIno)
                .orElseThrow(() -> new FileSystemException(FileSystemException.ENOENT, "File not found"));
        
        return inode.getSize();
    }
    
    private void validateToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new FileSystemException(FileSystemException.EPERM, "Invalid token");
        }
    }
}
