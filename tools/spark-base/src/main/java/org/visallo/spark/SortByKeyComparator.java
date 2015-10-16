package org.visallo.spark;

import org.apache.accumulo.core.data.Key;

import java.io.Serializable;
import java.util.Comparator;

public class SortByKeyComparator implements Comparator<Key>, Serializable {
    @Override
    public int compare(Key o1, Key o2) {
        return o1.compareTo(o2);
    }
}
