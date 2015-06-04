
Shape Files:

* Go here https://www.census.gov/geo/maps-data/data/cbf/cbf_zcta.html and download the 2013 ZIP Code Tabulation Areas (ZCTAs) Boundary File
* Unzip cb_2013_us_zcta510_500k.zip
* run `hdfs dfs -mkdir /visallo/config/org.visallo.zipCodeBoundaries.ZipCodeBoundariesRepository/`
* run `hdfs dfs -put cb_2013_us_zcta510_500k/* /visallo/config/org.visallo.zipCodeBoundaries.ZipCodeBoundariesRepository/`

