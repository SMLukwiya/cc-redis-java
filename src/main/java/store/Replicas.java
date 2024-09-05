package store;

import java.io.OutputStream;
import java.util.HashSet;

public class Replicas {
    volatile HashSet<OutputStream> replicas = new HashSet<>();

    public HashSet<OutputStream> getReplicas() { return replicas; }

    public synchronized void setReplica(OutputStream os) {
        this.replicas.add(os);
    }
}
