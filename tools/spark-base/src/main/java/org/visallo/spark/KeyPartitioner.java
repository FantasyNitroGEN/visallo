package org.visallo.spark;

import org.apache.accumulo.core.data.Key;
import org.apache.spark.Partitioner;

public class KeyPartitioner extends Partitioner {
    private final int numPartitions;

    public KeyPartitioner(int numPartitions) {
        this.numPartitions = numPartitions;
    }

    @Override
    public int numPartitions() {
        return this.numPartitions;
    }

    @Override
    public int getPartition(Object o) {
        if (o instanceof Key) {
            Key key = (Key) o;
            if (key.getRow().getLength() < 2) {
                return 0;
            }
            // we use one here because the keys start with a single character which will always be the same
            return Math.abs(key.getRow().charAt(1)) % numPartitions();
        }
        throw new RuntimeException("Cannot hash non-Keys");
    }
}
