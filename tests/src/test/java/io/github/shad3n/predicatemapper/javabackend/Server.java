package io.github.shad3n.predicatemapper.javabackend;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Server {
    private String hostname;
    private boolean active;
    private Location location;

    @Data
    @AllArgsConstructor
    public static class Location {
        private String city;
    }
}
