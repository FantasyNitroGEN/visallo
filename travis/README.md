Travis Request Script Information
=================================

The travis-request.sh script generates a build request to a Travis repository. The script uses the Travis CI web interface API described in: https://docs.travis-ci.com/api/. 

Prerequisites
-------------
The script requires the user to obtain GitHub or Travis access token from authorized user.  For more information on how to obtain an access token refer to the “Authorization” section in https://docs.travis-ci.com/api/. Note that authentication tokens are different for the Travis private & public endpoints. 

Using the script
----------------
```
travis-request.sh [--pro] [--org] [-b | --branch <name>] [--owner <name>]
                  [--by <value>] [-v | --verbose] --repo <name> -t|--token <value>

Argument details
  --pro               Use travis-ci.com endpoint for private repositories.
                      Set by default.
  --org               Use travis-ci.org endpoint for public repositories.
  -b|--branch <name>  Sets active branch. Default: "master"
  --owner <name>      Sets slug owner name. Default: "visallo"
  --by <value>        String to be added to originator message.
                      Default: "anonymous"
  -v|--verbose        Prints Travis API responses
  --repo <name>       Sets slug repo name. Required field
  -t|--token <value>  GitHub or Travis access token. Required field
```

Example
-------
```
travis_request.sh --org -b release-2.2 --repo visallo -t 0123456789abcdef --by “John Smith”
```
 
The previous request will generate a build request for the release-2.2 branch of the public visallo repository. --org lets the script know to use the Travis public endpoint (travis-ci.org) instead of the private one (travis-ci.com).  The request will be authenticated using the 0123456789abcdef token. If a build is currently in progress, the script cancels the current build and generates a new one. The build request comment on the Travis build history reads “Originated by John Smith”
