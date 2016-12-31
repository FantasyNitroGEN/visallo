# CSV/Excel File Import

Visallo can import CSVs and extract entities from the data. There is a custom wizard available that will allow you to import whatever types of data that you need.  

## Steps to import data:

<span>1. Drag the CSV or Excel file into Visallo</span>.

<img src='./drag-csv-into-visallo.gif' />

<span>2. Once the file shows a button that says "Map Structured Data" in the [detail panel](../detail-pane.md), click that button and the csv/excel import wizard will appear.</span>

<img src='./open-wizard.gif' />

<span>3. Decide which row is the first row.  If there is no header row, click the "No Header" link at the bottom.</span>

<img src='./pick-header-row.gif' />

<span>4. Create the entity and add the properties by selecting columns and following the prompts.</span>

<img src='./create-entity.gif' />

<span>5. You can add multiple different entity types.</span>

<img src='./multiple-entities.gif' />

<span>6. Add relationships between the different entities by dragging arrows between the different entity types.</span>

<img src='./add-relationship.gif' />

<span>7. Lastly, add the entities into the system by clicking the Import button</span>.

<img src='./import.gif' />

The data will be imported into your system with all of your configured entities and relationships.


### Possible problems
<a name='problems'>
 - It is not possible to import entities from a structured file that have already been published. An error will appear in the import wizard.
