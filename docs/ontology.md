# Ontology

Visallo uses OWL files to define what type of entities you can create, what properties they can have and what they
can connect to. Visallo has extended the OWL file to include additional attributes that will change how Visallo
works.

For an example see the [dev ontology](../examples/ontology-dev/).

## Loading an ontology via configuration

Add the following to your [configuration](configuration.md):

```
repository.ontology.owl.dev.iri=http://visallo.org/dev
repository.ontology.owl.dev.dir=/<path-to-visallo-source>/examples/ontology-dev/

#repository.ontology.owl.wikipedia.iri=http://visallo.org/wikipedia
#repository.ontology.owl.wikipedia.dir=/<path-to-visallo-source>/datasets/wikipedia/data/ontology
```

## Loading an ontology via web plugin

To load an ontology you will need a zip file containing the OWL file and all of it's dependencies. For the dev
ontology you would zip all the files in the ontology-dev directory and call it dev.owl. Then you will need to upload
the ontology into Visallo.

* [Install](build.md#web-plugin) the dev-tools web plugin.

* Click the "Admin" tool button.

  ![Admin Tool](img/admin.jpg)

* Click the "Upload Ontology" button.

  ![Upload Ontology](img/upload-ontology.jpg)

* Click the "Select File..." button and select you zip file. Assuming your OWL file has `owl:Ontology/rdf:about`
  then the document IRI is not required.

  ![Ontology Select File](img/ontology-select-file.jpg)

* Click "Upload"

## Visallo OWL Extensions

### DatatypeProperty

* **intent** - See the [Intents](#intent) section
* **textIndexHints** - Specifies how text is indexed in the full text search. By default text will not be indexed.
  **NOTE: The default behavior for strings is to not index them. If you would like them indexed be sure to set one
  of these values**
  The following options can be included (comma separated):
  * NONE - Do not index this property (default).
  * ALL - Combination of FULL_TEXT and EXACT_MATCH.
  * FULL_TEXT - Allow full text searching. Good for large text.
  * EXACT_MATCH - Allow exact matching. Good for multi-word known values.
* **searchable** - Should this property show up in the UI for searching.
* **deleteable** - Should this property be deleteable from the UI.
* **updateable** - Should this property be updateable from the UI.
* **addable** - Should this property be addable from the UI.
* **displayType** - Specifies how the UI should display the value.
  * dateOnly
  * link
  * textarea
* **propertyGroup** - Allows multiple properties to be included under a unified header in the UI.
* **possibleValues** - Creates a pick list on the UI. The value is a JSON document describing the possible values.

        {
          "M": "Male",
          "F": "Female"
        }
        
### ObjectProperty

* **deleteable** - Should this property be deleteable from the UI.
* **updateable** - Should this property be updateable from the UI.

### Class

* **intent** - See the [Intents](#intent) section
* **glyphIconFileName** - The image to use on the UI to display the entity.
* **color** - The color to use when underling the entity in a document.
* **displayType** - Specifies how the UI should display the entity.
  * audio
  * image
  * video
  * document
* **titleFormula** - A javascript function used to display the title of the entity.
* **subtitleFormula** - A javascript function used to display additional information in the search results.
* **timeFormula** - A javascript function used to display additional information in the search results.
* **addRelatedConceptWhiteList** - Limits what items can be added via the "Add Related" dialog.
* **deleteable** - Should this entity be deleteable from the UI.
* **updateable** - Should this entity be updateable from the UI.

<a name="intent"/>
## Intents

The ontology defines concepts, relationships, and properties. During data processing Visallo needs to know
 what type of concept, relationship, and property to assign when it finds them. For example if Visallo is scanning a
 document and finds a phone number, Visallo will need to assign a concept to that phone number. This is where
 intents come in.

Intents can be defined in the ontology and overridden in the configuration. To assign an intent you add the
 intent attribute to an OWL element.

    <owl:Class rdf:about="http://visallo.org/dev#phoneNumber">
      <rdfs:label xml:lang="en">Phone Number</rdfs:label>
      <visallo:intent>phoneNumber</visallo:intent>
      ...
    </owl:Class>

To override an intent you can add the following to your configuration.

    ontology.intent.concept.phoneNumber=http://visallo.org/dev#phoneNumber
    ontology.intent.relationship.hasMedia=http://visallo.org/dev#hasMedia
    ontology.intent.property.pageCount=http://visallo.org/dev#pageCount

### Concepts

| Name         | Description                               |
|--------------|-------------------------------------------|
| audio        | Audio file                                |
| city         | Geographic city                           |
| country      | Geographic country                        |
| csv          | Comma separated file                      |
| document     | Document                                  |
| email        | E-Mail address                            |
| entityImage  | Image assigned to an entity               |
| image        | Image file                                |
| location     | Geographic location                       |
| organization | Organization                              |
| person       | Person                                    |
| phoneNumber  | Phone number                              |
| rdf          | Resource description framework (RDF) file |
| state        | Geographic state                          |
| video        | Video file                                |
| zipCode      | Zip code                                  |

### Relationships

| Name                          | Description                          |
|-------------------------------|--------------------------------------|
| artifactContainsImageOfEntity | Artifact has image of entity         |
| artifactHasEntity             | Artifact has entity                  |
| entityHasImage                | Entity has image                     |
| hasMedia                      | Thing has media                      |

### Properties

| Name                    | Type        | Description                                 |
|-------------------------|-------------|---------------------------------------------|
| artifactTitle           | string      | The title of an artifact (fallback if documentTitle, etc. is not specified) |
| city                    | string      | Geographic city                             |
| documentTitle           | string      | The title of a document                     |
| geocodable              | string      | Marks a property as being geocoded by a configured org.visallo.core.geocoding.GeocoderRepository |
| geoLocation             | geoLocation | Geo-location                                |
| media.clockwiseRotation | integer     | Image clockwise rotation                    |
| media.dateTaken         | date        | Date/time image was taken                   |
| media.deviceMake        | string      | The device make                             |
| media.deviceModel       | string      | The device model                            |
| media.duration          | double      | The length in seconds of the media file     |
| media.fileSize          | long        | The filesize of the media file              |
| media.height            | integer     | The height in pixels of the media           |
| media.imageHeading      | double      | The compass direction the camera was facing |
| media.metadata          | json        | Additional metadata found in the media      |
| media.width             | integer     | The width in pixels of the media            |
| media.yAxisFlipped      | boolean     | Is image Y-axis flipped                     |
| pageCount               | long        | Number of pages in the artifact             |
| state                   | string      | Geographic state                            |
| zipCode                 | string      | Zip code                                    |
