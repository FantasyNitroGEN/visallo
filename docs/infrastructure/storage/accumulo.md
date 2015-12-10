# Storage: Accumulo

Accumulo is the preferred storage platform for large graphs. It provides server side security using the built-in Accumulo's visibility strings. The Accumulo storage does not provide the searching functionality, for that you will need to use one of the search storage engines such as Elasticsearch.

### History

By default Visallo is configured to retain all history in Accumulo. If you don't need this you can run the following:

        config -t visallo_vertexium_v -s table.split.threshold=128M
        config -t visallo_vertexium_e -s table.split.threshold=128M
        config -t visallo_vertexium_d -s table.split.threshold=128M
        config -t visallo_vertexium_v -s table.iterator.majc.vers=20,org.apache.accumulo.core.iterators.user.VersioningIterator
        config -t visallo_vertexium_e -s table.iterator.majc.vers=20,org.apache.accumulo.core.iterators.user.VersioningIterator
        config -t visallo_vertexium_d -s table.iterator.majc.vers=20,org.apache.accumulo.core.iterators.user.VersioningIterator
        config -t visallo_vertexium_v -s table.iterator.majc.vers.opt.maxVersions=1
        config -t visallo_vertexium_e -s table.iterator.majc.vers.opt.maxVersions=1
        config -t visallo_vertexium_d -s table.iterator.majc.vers.opt.maxVersions=1
        config -t visallo_vertexium_v -s table.iterator.minc.vers=20,org.apache.accumulo.core.iterators.user.VersioningIterator
        config -t visallo_vertexium_e -s table.iterator.minc.vers=20,org.apache.accumulo.core.iterators.user.VersioningIterator
        config -t visallo_vertexium_d -s table.iterator.minc.vers=20,org.apache.accumulo.core.iterators.user.VersioningIterator
        config -t visallo_vertexium_v -s table.iterator.minc.vers.opt.maxVersions=1
        config -t visallo_vertexium_e -s table.iterator.minc.vers.opt.maxVersions=1
        config -t visallo_vertexium_d -s table.iterator.minc.vers.opt.maxVersions=1
        config -t visallo_vertexium_v -s table.iterator.scan.vers=20,org.apache.accumulo.core.iterators.user.VersioningIterator
        config -t visallo_vertexium_e -s table.iterator.scan.vers=20,org.apache.accumulo.core.iterators.user.VersioningIterator
        config -t visallo_vertexium_d -s table.iterator.scan.vers=20,org.apache.accumulo.core.iterators.user.VersioningIterator
        config -t visallo_vertexium_v -s table.iterator.scan.vers.opt.maxVersions=1
        config -t visallo_vertexium_e -s table.iterator.scan.vers.opt.maxVersions=1
        config -t visallo_vertexium_d -s table.iterator.scan.vers.opt.maxVersions=1