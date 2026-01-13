package ulitsa.raskolnikova.vtfs.server.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "file_data")
@Data
@NoArgsConstructor
public class FileData {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inode_ino", nullable = false)
    private Inode inode;
    
    @Column(name = "file_offset", nullable = false)
    private Long offset;
    
    @Column(nullable = false, columnDefinition = "BYTEA")
    private byte[] data;
    
    public FileData(Inode inode, Long offset, byte[] data) {
        this.inode = inode;
        this.offset = offset;
        this.data = data;
    }
    
    public int getDataSize() {
        return data != null ? data.length : 0;
    }
}
