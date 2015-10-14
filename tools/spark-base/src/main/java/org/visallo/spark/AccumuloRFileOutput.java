package org.visallo.spark;

import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.file.blockfile.BlockFileWriter;
import org.apache.accumulo.core.file.blockfile.impl.CachableBlockFile;
import org.apache.accumulo.core.file.rfile.RFile;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.spark.api.java.function.VoidFunction;
import scala.Tuple2;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

class AccumuloRFileOutput implements VoidFunction<Iterator<Tuple2<Key, Value>>> {
    public static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd'T'HHmmss");
    private final String outputPath;
    private final String fsDefaultName;
    private final int blockSize;
    private final Date jobStartDate;
    private final boolean dfsClientUseDatanodeHostname;

    public AccumuloRFileOutput(String outputPath, String fsDefaultName, int blockSize, Date jobStartDate, boolean dfsClientUseDatanodeHostname) {
        this.outputPath = outputPath;
        this.fsDefaultName = fsDefaultName;
        this.blockSize = blockSize;
        this.jobStartDate = jobStartDate;
        this.dfsClientUseDatanodeHostname = dfsClientUseDatanodeHostname;
    }

    @Override
    public void call(Iterator<Tuple2<Key, Value>> it) throws Exception {
        if (!it.hasNext()) {
            return;
        }
        Tuple2<Key, Value> pair = it.next();
        Configuration conf = new Configuration();
        conf.set("fs.default.name", fsDefaultName);
        conf.set("dfs.client.use.datanode.hostname", Boolean.toString(dfsClientUseDatanodeHostname));
        AccumuloConfiguration accumuloConfiguration = AccumuloConfiguration.getDefaultConfiguration();
        FileSystem fs = FileSystem.get(conf);
        String compression = accumuloConfiguration.get(Property.TABLE_FILE_COMPRESSION_TYPE);
        String dateString = SIMPLE_DATE_FORMAT.format(jobStartDate);
        String firstKeyEscaped = pair._1().getRow().toString().replaceAll("\\W", "_");
        Path fName = new Path(outputPath + "-" + dateString + "-" + firstKeyEscaped + ".rf");
        BlockFileWriter bfw = new CachableBlockFile.Writer(fs, fName, compression, conf, accumuloConfiguration);
        RFile.Writer rfileWrite = new RFile.Writer(bfw, blockSize);
        rfileWrite.startDefaultLocalityGroup();
        while (true) {
            Key key = pair._1();
            Value value = pair._2();
            rfileWrite.append(key, value);
            if (it.hasNext()) {
                pair = it.next();
            } else {
                break;
            }
        }
        rfileWrite.close();
    }
}
