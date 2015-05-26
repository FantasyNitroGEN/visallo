1. Download the [GeoLite2 Free Database](http://dev.maxmind.com/geoip/geoip2/geolite2/) in CSV format:

        http://geolite.maxmind.com/download/geoip/database/GeoLite2-City-CSV.zip

1. Unzip the downloaded file

1. Copy `GeoLite2-City-Blocks-IPv4.csv` to HDFS:

        hdfs dfs -mkdir /visallo/config/org.visallo.geoip.GeoIpGraphPropertyWorker
        hdfs dfs -put GeoLite2-City-CSV_*/GeoLite2-City-Blocks-IPv4.csv /visallo/config/org.visallo.geoip.GeoIpGraphPropertyWorker/
