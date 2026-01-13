package ulitsa.raskolnikova.vtfs.server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ulitsa.raskolnikova.vtfs.server.model.DirectoryEntry;

import java.util.List;
import java.util.Optional;

@Repository
public interface DirectoryEntryRepository extends JpaRepository<DirectoryEntry, Long> {
    
    List<DirectoryEntry> findByParentIno(Long parentIno);
    
    Optional<DirectoryEntry> findByParentInoAndName(Long parentIno, String name);
    
    boolean existsByParentInoAndName(Long parentIno, String name);
    
    @Query("SELECT de FROM DirectoryEntry de WHERE de.inode.ino = :inodeIno")
    List<DirectoryEntry> findByInodeIno(@Param("inodeIno") Long inodeIno);
    
    @Query("SELECT COUNT(de) FROM DirectoryEntry de WHERE de.parentIno = :parentIno AND de.name NOT IN ('.', '..')")
    long countByParentInoExcludingDots(@Param("parentIno") Long parentIno);
}
