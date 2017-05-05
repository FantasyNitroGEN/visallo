# Code Generation 

Visallo has a module that will interact with your Visallo instance to generate code that will let you write customized code to ingest your data. By using the generated code, you can use any software that runs on the JVM to import data into Visallo.

## Using the code generator

It is typical to use the code generator inside of your IDE while developing in the Visallo project. The class `org.visallo.tools.ontology.ingest.codegen.ModelCodeGen` exposes a main method that you can use to interact with the ontology. There are two ways to use it:

* Connect to a running Visallo instance and get the ontology from there
* Have an ontology in json format and use it to run the code generator

Run the `org.visallo.tools.ontology.ingest.codegen.ModelCodeGen` class with no arguments in order to see what command line parameters there are. For both, you will need to specify an output directory using the command line argument -o *<outputDirectory>* but, depending on which steps you take to get the ontology, you may require more parameters.

### Connect to the Webserver

To connect to the webserver you will also need to have the command line parameters 

* -url *visallo url*
* -u *username*
* -p *password*

and you will possibly need `*--includeVisalloClasses*` if your ontology has a url that starts with `http://visallo.org` since those are filtered out by default. If your ontology does not and you do not need to interact with Visallo's built-in ontology, you can skip that command line parameter.

For example, in intellij, your run configuration may look something like the following and will automatically generate the ontology java code in the directory that you specify:

<img src='./codegen-intellij-config.png' /> 

Once run, the code will then be generated into the directory that you have specified.

### Have an ontology json file

While it is possible for you to specify a file that can be used in place of calling Visallo in order to get the ontology, there is no current native way to do it. You will have to have saved the result from the REST call into a file that you can keep using to generate code. The benefit of this approach is that you can develop with the ontology offline from having the app running, but it won't automatically be updated if there is an ontology change inside of Visallo. Therefore, while possible this way is not recommended and you should query a running instance of the app every time.

## Working with the generated code

### Create an entity

Once you have generated the code you can start using it in order to start ingesting your data. For example, using the sample ontology we can write the following code snippet in order to ingest a phone number entity into the system:

```java
package org.visallo;

import org.visallo.core.cmdline.CommandLineTool;
import org.visallo.sample.PhoneNumber;
import org.visallo.tools.ontology.ingest.common.IngestRepository;

import javax.inject.Inject;

// we are using the CommandLineTool that Visallo provides to easily inject the IngestRepository
public class SampleIngest extends CommandLineTool {
    private IngestRepository ingestRepository;

    public static void main(String[] args) throws Exception {
        CommandLineTool.main(new SampleIngest(), args);
    }

    @Override
    protected int run() throws Exception {
        //create a phone number with the id 'phonenumber1'
        PhoneNumber phonenumber1 = new PhoneNumber("phonenumber1");
        //set the title of phonenumber1 to a phonenumber 
        phonenumber1.setTitle("555-555-5555");

        //save the phone number into the system using the ingest repository
        this.ingestRepository.save(phonenumber1);

        return 0;
    }

    @Inject
    public void setIngestRepository(IngestRepository ingestRepository){
        this.ingestRepository = ingestRepository;
    }
}

```

If you are writing java code to ingest data, you will need to ensure that you have the visallo-tools-ontology-ingest jar on your classpath in order to get the implementation of the IngestRepository. After running the code snippet, you will have a single phone number in the system. 

The advantages of using Java to ingest things into the system is that it can make more complicated decisions about what to put in the system in real time. For example, if for some reason, you want to have a near real-time ingest into your system of only tweets from specific people that have specific hashtags, you can set up a java program to filter out the data and look at live tweets much more easily than [creating an RDF File](./rdfimport.md) and importing it at some later date.

### Create a relationship

Relationships can be created through your java code just as easily as you created an entity. Look at the following code snippet (Only the run method has been changed, the rest has been ommitted for brevity)

```java
    @Override
    protected int run() throws Exception {
        PhoneNumber phonenumber1 = new PhoneNumber("phonenumber1");
        phonenumber1.setTitle("555-555-5555");

        //create a second phone number entity
        PhoneNumber phonenumber2 = new PhoneNumber("phonenumber2");
        phonenumber2.setTitle("555-444-4444");

        //create a relationship between both phone numbers with a has source edge
        HasSource hasSourceEdge = new HasSource("edge1", phonenumber1, phonenumber2);

        //save everything
        this.ingestRepository.save(phonenumber1, phonenumber2, hasSourceEdge);

        return 0;
    }
```

Through this pattern you can create any number of entities and relationships by using the generated code.


### Visibility

#### Entity 
Adding visibility to the saved entities is possible through the Java API. In the following code example the first phone number entry, its title property, and the edges have the visibility label of "TS" added to them so that they may be tagged with that visibility when it enters the system:


```java
    @Override
    protected int run() throws Exception {
        PhoneNumber phonenumber1 = (PhoneNumber)new PhoneNumber("phonenumber1").withVisibility("TS");
        phonenumber1.setTitle("555-555-5555").withVisibility("A");

        PhoneNumber phonenumber2 = new PhoneNumber("phonenumber2");
        phonenumber2.setTitle("555-444-4444");

        HasSource hasSourceEdge = (HasSource)new HasSource("edge1", phonenumber1, phonenumber2).withVisibility("TS");

        this.ingestRepository.save(phonenumber1, phonenumber2, hasSourceEdge);

        return 0;
    }
```

#### Properties
Visibilities on entity properties can be set through the code that is generated. To set the visibility of a property, call the version of the method that applies the visibility string to the entity and it will be saved with that visibility.

In the following example a phone number entity is created that contains two phone number properties. If the user that views the entity has an authorization of "A" and not "B", they will only be able to see the first property that has the visibility of "A". If they have both "A" and "B", they will be able to see both values on the entity.

```java
    @Override
    protected int run() throws Exception {
        PhoneNumber phoneNumber = new PhoneNumber("phonenumber1");
        phoneNumber.addPhoneNumber("1", "(555) 555-5555", "A");
        phoneNumber.addPhoneNumber("2", "(123) 456-7890", "B");

        ingestRepository.save(phoneNumber);

        return 0;
    }
```
