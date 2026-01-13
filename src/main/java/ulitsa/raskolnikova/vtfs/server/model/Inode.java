package ulitsa.raskolnikova.vtfs.server.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "inodes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Inode {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ino")
    private Long ino;
    
    @Column(nullable = false)
    private Integer mode;
    
    @Column(nullable = false)
    private Long size;
    
    @Column(nullable = false)
    private Integer nlink;
    
    @OneToMany(mappedBy = "inode", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DirectoryEntry> directoryEntries = new ArrayList<>();
    
    @OneToMany(mappedBy = "inode", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FileData> fileDataChunks = new ArrayList<>();
    
    public Inode(Integer mode, Long size, Integer nlink) {
        this.mode = mode;
        this.size = size;
        this.nlink = nlink;
    }
    
    public boolean isRegularFile() {
        return (mode & 0170000) == 0100000;
    }
    
    public boolean isDirectory() {
        return (mode & 0170000) == 0040000;
    }
}
