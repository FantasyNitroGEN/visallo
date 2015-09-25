
Shape Files:

1. Download:

        curl http://www2.census.gov/geo/tiger/GENZ2014/shp/cb_2014_us_zcta510_500k.zip -O

1. Unzip:

        mkdir cb_2014_us_zcta510_500k
        cd cb_2014_us_zcta510_500k
        unzip ../cb_2014_us_zcta510_500k.zip
        cd ..

1. Copy to HDFS
        
        hdfs dfs -mkdir -p /visallo/config/org.visallo.zipCodeBoundaries.ZipCodeBoundariesRepository/
        hdfs dfs -put cb_2014_us_zcta510_500k/* /visallo/config/org.visallo.zipCodeBoundaries.ZipCodeBoundariesRepository/

