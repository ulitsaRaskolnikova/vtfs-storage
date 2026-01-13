package ulitsa.raskolnikova.vtfs.server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ulitsa.raskolnikova.vtfs.server.model.FileData;

import java.util.List;

@Repository
public interface FileDataRepository extends JpaRepository<FileData, Long> {
    
    List<FileData> findByInodeInoOrderByOffsetAsc(Long inodeIno);
    
    @Query(value = "SELECT * FROM file_data WHERE inode_ino = :inodeIno " +
           "AND file_offset < :endOffset AND (file_offset + length(data)) > :startOffset " +
           "ORDER BY file_offset ASC", nativeQuery = true)
    List<FileData> findChunksInRange(@Param("inodeIno") Long inodeIno,
                                      @Param("startOffset") Long startOffset,
                                      @Param("endOffset") Long endOffset);
    
    void deleteByInodeIno(Long inodeIno);
}
