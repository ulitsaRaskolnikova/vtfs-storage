package ulitsa.raskolnikova.vtfs.server.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "directory_entries",
       uniqueConstraints = @UniqueConstraint(columnNames = {"parent_ino", "name"}))
@Data
@NoArgsConstructor
public class DirectoryEntry {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "parent_ino", nullable = false)
    private Long parentIno;
    
    @Column(nullable = false, length = 256)
    private String name;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inode_ino", nullable = false)
    private Inode inode;
    
    public DirectoryEntry(Long parentIno, String name, Inode inode) {
        this.parentIno = parentIno;
        this.name = name;
        this.inode = inode;
    }
}
