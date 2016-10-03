# RDF Import

For simple ingestion processes, it is possible to use an RDF file with both xml and n-triple files. 

## RDF Files

### XML Format

### NT Format

NT files that can be imported into Visallo are 

Each line in an NT file represents a single item of data that can be imported into Visallo. An NT file will have the following format:


```xml
  # create an email
  <http://visallo.com/email1> <http://visallo.org#conceptType> "http://visallo.org/sample#emailAddress"
  <http://visallo.com/email1> <http://visallo.org#title> "ryan@v5analytics.com"
  
  # create another email
  <http://visallo.com/email2> <http://visallo.org#conceptType> "http://visallo.org/sample#emailAddress"
  <http://visallo.com/email2> <http://visallo.org#title> "marcelo.sabino@gmail.com"
  
  # create an edge
  <http://visallo.com/email1> <http://visallo.org/structured-file#elementHasSource> <http://visallo.com/email2>
</code>
```

In this file, two email entities are created and an edge is created between them. 


#### Vertex

A vertex is made implicity by creating a property on it. In Visallo, the minimal vertex that can be used inside of the application smoothly is one with a concept type. To create a vertex it is preferable to create one as the following by specifying both the id of the future vertex and the concept type of the vertex. In the following example, we are assuming that you are using the [sample ontology](https://github.com/v5analytics/visallo/config/ontology/sample.owl) with Visallo.

```xml
<email1> <http://visallo.org#conceptType> "http://visallo.org/sample#emailAddress"
```

If you were to create multiple vertices with multiple concept types the NT file would look like:

```xml
  # create two vertices with the concept type of email address
<email1> <http://visallo.org#conceptType> "http://visallo.org/sample#emailAddress"
<email2> <http://visallo.org#conceptType> "http://visallo.org/sample#emailAddress"

  # create three vertices with the concept type of phone number
<phonenumber1> <http://visallo.org#conceptType> "http://visallo.org/sample#phoneNumber"
<phonenumber2> <http://visallo.org#conceptType> "http://visallo.org/sample#phoneNumber"
<phonenumber3> <http://visallo.org#conceptType> "http://visallo.org/sample#phoneNumber"

  # create one vertex with a concept type of contact info
<contactinfo1> <http://visallo.org#conceptType> "http://visallo.org/sample#contactInfo"

```

#### Vertex Properties

Creating a vertex property is very similar to the above examples of creating a vertex because when you are setting the concept type property on a vertex, you are in fact setting a property on a vertex which implicitly creates the vertex. In order to create a property on a vertex follow the same format as above, but substitute the correct property inside of the second set of brackets to set the property. Lets consider a possible ontology for a person that has been added to the sample ontology and has 4 different properties:

<table>
<thead>
<tr><th>Property</th><th>Ontology IRI</th></tr>
</thead>
<tbody>
<tr><td>name</td><td>http://visallo.com/sample#name</td></tr>
<tr><td>address</td><td>http://visallo.com/sample#address</td></tr>
<tr><td>is employed</td><td>http://visallo.com/sample#isemployed</td></tr>
<tr><td>height (in inches)</td><td>http://visallo.com/sample#height</td></tr>
</tbody>
</table>

If we load an element with complete data through the RDF Importer file, it will look like the following:

```xml
# create the vertex
<person1> <http://visallo.org#conceptType> "http://visallo.org/sample#person"
<person1> <http://visallo.org#sample> "http://visallo.org/sample#person"



```



##### Property Types

You probably noticed that we add data types onto the properties when we are ingesting the properties. Although the datatypes are not enforced by the RDF Importer, there can be unexpected behavior inside of Visallo when data is ingested as a different datatype than the one that is specified in the ontology. 

To specify the data type for the data that you are ingesting, you must create the data property as follows:

<table><thead>
<tr><th>Type</th><th>Data Type</th><th>Description</th><th>Example</th></tr>
</thead>
<tbody>
<tr><td>Geolocation</td><td>http://visallo.org#geolocation</td><td></td><td></td></tr>
<tr>
  <td>Streaming Property Value</td>
  <td>http://visallo.org#streamingPropertyValue</td>
  <td></td>
  <td></td>
</tr>
<tr>
<td>Streaming Property Value Inline</td>
<td>http://visallo.org#streamingPropertyValueInline</td>
<td></td>
<td></td></tr>
<tr>
<td>Date</td>
<td>http://www.w3.org/2001/XMLSchema#date</td>
<td></td>
<td></td></tr>
<tr>
<td>DateTime</td>
<td>http://www.w3.org/2001/XMLSchema#dateTime</td>
<td></td>
<td></td></tr>
<tr>
<td>gYear</td>
<td>http://www.w3.org/2001/XMLSchema#gYear</td>
<td></td>
<td></td></tr>
<tr>
<td>gMonthDay</td>
<td>http://www.w3.org/2001/XMLSchema#gMonthDay</td>
<td></td>
<td></td></tr>
<tr>
<td>String</td>
<td>http://www.w3.org/2001/XMLSchema#string</td>
<td></td>
<td></td></tr>
<tr>
<td>Boolean</td>
<td>http://www.w3.org/2001/XMLSchema#boolean</td>
<td></td>
<td></td></tr>
<tr>
<td>Double</td>
<td>http://www.w3.org/2001/XMLSchema#double</td>
<td></td>
<td></td></tr>
<tr>
<td>Currency</td>
<td>http://visallo.org#currency</td>
<td></td>
<td></td></tr>
<tr>
<td>Integer</td>
<td>http://www.w3.org/2001/XMLSchema#int</td>
<td></td>
<td></td></tr>
<tr>
<td>Integer</td>
<td>http://www.w3.org/2001/XMLSchema#integer</td>
<td></td>
<td></td></tr>
</tbody>
</table>








#### Edge

#### Edge Properties

#### Authorizations


## How to import RDF files
 
### External process
 
org.visallo.tools.RdfImport

### Through the webapp UI

show admin panel
