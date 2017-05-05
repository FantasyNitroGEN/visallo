# Ontology

Visallo uses OWL files to define what type of entities you can create, what properties they can have and what they
can connect to. Visallo has extended the OWL file to include additional attributes that will change how Visallo
works.

For an example see the [sample ontology](https://github.com/visallo/visallo/tree/master/config/ontology-sample).

## Loading an ontology via configuration

Add the following to your [configuration](configuration.md).

```
repository.ontology.owl.default.iri=http://visallo.org/sample
repository.ontology.owl.default.dir=$VISALLO_DIR/config/ontology-sample
```

## Visallo OWL Extensions

### DatatypeProperty

* **intent** - See the [Intents](#intent) section
* **objectPropertyDomain** - Allows property to be added to edge / ObjectProperty. Similar syntax to `rdfs:domain`

    ```xml
    <visallo:objectPropertyDomain rdf:resource="http://example.com/objectProp" />
    ```

* **textIndexHints** - Specifies how text is indexed in the full text search. By default text will not be indexed.
  **NOTE: The default behavior for strings is to not index them. If you would like them indexed be sure to set one
  of these values**
  The following options can be included (comma separated):
  * NONE - Do not index this property (default).
  * ALL - Combination of FULL_TEXT and EXACT_MATCH.
  * FULL_TEXT - Allow full text searching. Good for large text.
  * EXACT_MATCH - Allow exact matching. Good for multi-word known values.

  ```xml
  <visallo:textIndexHints>ALL</visallo:textIndexHints>
  ```

* **searchable** - Should this property show up in the _Filter By Property_ list in the search interface.

    ```xml
    <visallo:searchable rdf:datatype="&xsd;boolean">false</visallo:searchable>
    ```

* **deleteable** - Should the delete button show in the UI and allow deleting properties in REST calls.

    ```xml
    <visallo:deleteable rdf:datatype="&xsd;boolean">false</visallo:deleteable>
    ```

* **updateable** - Should the edit button show in the UI and allow updating property values in REST calls.

    ```xml
    <visallo:updateable rdf:datatype="&xsd;boolean">false</visallo:updateable>
    ```

* **addable** - Should the add property list show this property and allow creating property values in REST calls.

    ```xml
    <visallo:addable rdf:datatype="&xsd;boolean">false</visallo:addable>
    ```

* **displayType** - Specifies how the UI should display the value. Plugins can add new display types, see the _Ontology Property Display Types_ section in [Front-end Plugins](../front-end/index.md).
  * `bytes`: Show the value in a human readable size unit based on size. Assumes the value is in bytes.
    ```xml
    <visallo:displayType>bytes</visallo:displayType>
    ```
  * `dateOnly`: Remove the time from the property value and stop timezone shifting display for users (Date will be same regardless of users timezone).
    ```xml
    <visallo:displayType>dateOnly</visallo:displayType>
    ```
  * `geoLocation`: Show the geolocation using description (if available), and truncated coordinates. All `<rdfs:range rdf:resource="&visallo;geolocation"/>` properties automatically use this for display.
    ```xml
    <visallo:displayType>geoLocation</visallo:displayType>
    ```
  * `heading`: Show a direction arrow, assumes the value is number in degrees.
    ```xml
    <visallo:displayType>heading</visallo:displayType>
    ```
  * `link`: Show the value as a link (assumes the value is valid href). If the property has a metadata value `http://visallo.org#linkTitle` it will be displayed instead of the raw value.  
    ```xml
    <visallo:displayType>link</visallo:displayType>
    ```
  * `textarea`: Show the value using multiline whitespace, and allow editing in a `<textarea>` instead of one line `<input>`
    ```xml
    <visallo:displayType>textarea</visallo:displayType>
    ```
* **propertyGroup** - Allows multiple properties to be included under a unified collapsible header in the element inspector. All properties that match the value (case-sensitive) will be placed in a section.

    ```xml
    <visallo:propertyGroup xml:lang="en">My Group</visallo:propertyGroup>
    ```

* **possibleValues** - Creates a pick list on the UI. The value is a JSON document describing the possible values. In this example, `F` will be the raw value saved in the property value, but `Female` would be displayed to user in pick list and in the element inspector.

    ```json
    {
      "M": "Male",
      "F": "Female"
    }
    ```
        
### ObjectProperty

* **deleteable** - Should the delete button show in the UI and allow deleting properties in REST calls.

    ```xml
    <visallo:deleteable rdf:datatype="&xsd;boolean">false</visallo:deleteable>
    ```

* **updateable** - Should the edit button show in the UI and allow updating property values in REST calls.

    ```xml
    <visallo:updateable rdf:datatype="&xsd;boolean">false</visallo:updateable>
    ```

### Class

* **intent** - See the [Intents](#intent) section
* **glyphIconFileName** - The image to use on the UI to display the entity.
* **color** - The color to use when underling the entity in a document text section.
* **displayType** - Specifies how the UI should display the entity.
  * audio
  * image
  * video
  * document
* **titleFormula** - A JavaScript snippet used to display the title of the entity. The snipped could be a single expression, or multiple lines with a `return`. All formulas have access to:
    * `vertex`: The json vertex object (if the element is vertex)
    * `edge`: The json edge object (if the element is an edge)
    * `ontology`: The json ontology object (concept/relation)
        * `id` The iri
        * `displayName` The display name of type
        * `parentConcept` Parent iri (if vertex and is a child type)
        * `parentIri` Parent iri (if edge and is a child type)
        * `pluralDisplayName` The plural display name of type
        * `properties` The property iris defined on this type
    * `prop`: Function that accepts a property IRI and returns the display value.
    * `propRaw`: Function that accepts a property IRI and returns the raw value.

    ```xml
    <!-- Expression with CDATA -->
    <visallo:titleFormula xml:lang="en">![CDATA[
        prop('http://example.org/aProp') || ''
    ]]></visallo:titleFormula>

    <!-- Expression escaped -->
    <visallo:titleFormula xml:lang="en">
        prop(&apos;http://example.org/aProp&apos;) || &apos;&apos;
    </visallo:titleFormula>

    <!-- Multiline with return -->
    <visallo:titleFormula xml:lang="en">![CDATA[
        var p = prop('http://example.org/aProp');
        if (p) return p;
        return 'Not available';
    ]]></visallo:titleFormula>
    ```

* **subtitleFormula** - A JavaScript snippet used to display additional information in the search results.
* **timeFormula** - A JavaScript snippet used to display additional information in the search results.
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

```xml
<owl:Class rdf:about="http://visallo.org/sample#phoneNumber">
  <rdfs:label xml:lang="en">Phone Number</rdfs:label>
  <visallo:intent>phoneNumber</visallo:intent>
  ...
</owl:Class>
```

To override an intent you can add the following to your configuration.

    ontology.intent.concept.phoneNumber=http://visallo.org/sample#phoneNumber
    ontology.intent.relationship.hasMedia=http://visallo.org/sample#entityHasMedia

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
