# Map/Reduce

Visallo Map/Reduce ingest supports reading data from a Hadoop input format and output that data directly to Visallo backed by Accumulo. Ingesting data this way does not add it to the search index so a reindex will be required after the ingest.

## Example

For this example, lets imagine we have a lot of transaction data to import.
```
id|date|cardId|amount|merchantId|merchantName|merchantCategoryCode|merchantCity|merchantState|merchantZipCode
0|2014-01-05T18:36:01|178460786|562.67|502600000003|Office Depot|5943|Washington|DC|20045
1|2014-01-09T05:13:39|178460786|976.48|998500000004|Stater Bros. Holdings|5198|Wayne|NJ|07474
2|2014-01-10T16:26:30|178460786|220.36|864700000007|Dollar General|8021|Christiansted|VI|00820
3|2014-01-12T09:46:45|178460786|191.29|957600000009|Publix|3259|Golf|IL|60029
4|2014-01-13T20:11:26|178460786|37.24|502600000003|Office Depot|5943|Washington|DC|20045
5|2014-01-17T15:31:48|178460786|388.99|999100000001|PetSmart|5814|Portland|OR|97225
6|2014-01-22T11:56:55|178460786|430.04|799200000006|TJX|5815|Boise|ID|83719
7|2014-01-23T19:36:41|178460786|562.84|864700000007|Dollar General|8021|Christiansted|VI|00820
8|2014-01-27T07:54:18|178460786|572.07|864700000007|Dollar General|8021|Christiansted|VI|00820
```

For the import we would like to create a vertex for every card and every merchant. We also want to create an edge with the amount for every transaction. The MR job is setup just like any other MR job. We'll use `org.apache.hadoop.mapreduce.lib.input.TextInputFormat` as our input format and `org.vertexium.accumulo.mapreduce.AccumuloElementOutputFormat` as the output format. Here are the important bits.

```java
AccumuloElementOutputFormat.setOutputInfo(job, accumuloInstanceName, zooKeepers, principal, authorizationToken);
FileInputFormat.addInputPath(job, new Path(inputFileName));

job.setJarByClass(ImportTransactionMR.class);
job.setMapperClass(ImportTransactionMRMapper.class);
job.setNumReduceTasks(0);
job.setMapOutputValueClass(Mutation.class);
job.setInputFormatClass(TextInputFormat.class);
job.setOutputFormatClass(AccumuloElementOutputFormat.class);
```

To map the lines from the CSV to Vertexium vertices and edges we'll extend our `ImportTransactionMRMapper` class from `org.vertexium.accumulo.mapreduce.ElementMapper`.

```java
public static class ImportTransactionMRMapper extends AccumuloMutationElementMapper<LongWritable, Text> {
  @Override
  protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
    String line = value.toString();
    Visibility visibility = new Visibility("");
    Authorizations authorizations = getGraph().createAuthorizations();
    context.setStatus("reading line: " + key);
    ... process the line ...
  }
}
```

The Vertexium ElementMapper class will create a Vertexium graph that will output it's save calls to the Accumulo output format. So from this point on we can use the familiar Vertexium APIs to create vertices and edges. The following is an example of saving a card.

```java
String cardVertexId = "CARD_" + cardId;
VertexBuilder cardVertexBuilder = graph.prepareVertex(cardVertexId, visibility);
VisalloProperties.CONCEPT_TYPE.setProperty(cardVertex, "http://visallo.org/tx#card", visibility);
cardVertexBuilder.setProperty("http://visallo.org/tx#cardId", tx.getCardId(), visibility);
cardVertexBuilder.save(authorizations);
```

For edges we'll do the same thing

```java
String txEdgeId = "TX_" + txId;
EdgeBuilderByVertexId e = graph.prepareEdge(txEdgeId, cardVertexId, merchantVertexId, "http://visallo.org/tx#tx", visibility);
e.setProperty("http://visallo.org/tx#amount", tx.getAmount(), visibility);
e.setProperty("http://visallo.org/tx#date", tx.getDate(), visibility);
e.save(authorizations);
```

To run our example we just need to package up a JAR and run

```bash
hadoop jar importTransaction.jar --in transaction.csv
```
