package ulitsa.raskolnikova.vtfs.server.dto;

import lombok.AllArgsConstructor;
import lombok.Value;
import java.util.List;

@Value
public class ListResponse {
    
    List<Entry> entries;
    boolean isHardLinksList;
    
    @Value
    @AllArgsConstructor
    public static class Entry {
        String name;
        Long ino;
        Integer mode;
        
        public Entry(String name) {
            this.name = name;
            this.ino = null;
            this.mode = null;
        }
    }
}
