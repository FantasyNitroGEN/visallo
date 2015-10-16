package org.visallo.spark;

import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Value;
import org.apache.spark.api.java.function.Function;
import scala.Tuple2;

class SortByKeyFunction implements Function<Tuple2<Key, Value>, Key> {
    @Override
    public Key call(Tuple2<Key, Value> fileKeyValue) throws Exception {
        return fileKeyValue._1();
    }
}
