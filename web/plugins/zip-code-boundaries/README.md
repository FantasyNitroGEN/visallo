
Shape Files:

1. Download [ZIP Code Tabulation Areas (ZCTAs) Boundary File](https://www.census.gov/geo/maps-data/data/cbf/cbf_zcta.html)

1. `unzip cb_2014_us_zcta510_500k.zip`

1. Copy to HDFS
        
        hdfs dfs -mkdir -p /visallo/config/org.visallo.zipCodeBoundaries.ZipCodeBoundariesRepository/
        hdfs dfs -put cb_2014_us_zcta510_500k/* /visallo/config/org.visallo.zipCodeBoundaries.ZipCodeBoundariesRepository/

