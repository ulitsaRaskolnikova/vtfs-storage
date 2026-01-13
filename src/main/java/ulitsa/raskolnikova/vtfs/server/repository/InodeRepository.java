package ulitsa.raskolnikova.vtfs.server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ulitsa.raskolnikova.vtfs.server.model.Inode;
import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface InodeRepository extends JpaRepository<Inode, Long> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inode i WHERE i.ino = :ino")
    Optional<Inode> findByIdForUpdate(@Param("ino") Long ino);
}
