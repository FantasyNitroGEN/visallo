
# Sizing

## Development

For development the [core team](http://v5analytics.com/) uses the `visallo/dev` docker image
to run the infrastructure components (HDFS, Accumulo, Elasticsearch, and RabbitMQ)
but with the web application (and often also the YARN-deployed components)
from the Java IDE on our laptop OS.

On Mac OS X or Windows with 16GB of RAM we recommend running a docker-machine/boot2docker
virtual machine with 8GB of RAM in which to run the docker container and do not recommend
less than 4GB. If running Linux natively 8GB total RAM could be fine.

The docker-machine/boot2docker VM is configured by default to use up to
20GB of disk space but typical use is less than 10GB.

## Production

For production it always depends... but you shouldn’t use fewer than 3 servers and we do not typically
use less than 5 HDFS/YARN/Accumulo/Elasticsearch servers plus HDFS/YARN and Accumulo master servers,
2 RabbitMQ servers, and 1 or more web servers.

Visallo’s data capacity scales directly with how much HDFS space is available for Accumulo tables
and local disk space available for Elasticsearch indices.

Search performance is dependent on the number of Elasticsearch nodes and how much RAM
you can allocate to each Elasticsearch instance (more than 2GB but less than 32GB).

Visallo’s analytic and data enrichment features scale based on number of YARN nodemanager instances
where Graph Property Worker and Long Running Processes run.

Further considerations include:

  - data ingest mode(s)
    - batch
      - mimimize batch completion time or cost?
    - streaming
      - required ingest rate?
  - data type(s)
    - structured
    - unstructured text
    - media (audio, video, images)
  - analytics and data enrichment
    - what Visallo features including Graph Property Workers do you need
      to run?

