# ZipCodeLocationGraphPropertyWorker

The zip code location graph property worker will find the geolocation of a property with the intent `zipCode`. It
can also find additional properties with a list of configured properties:

        zipCodeLocation.propertyName.p1=http://visallo.org#myzipcode
        zipCodeLocation.propertyName.p2=http://visallo.org#anotherzipcode

If the zip code location graph property worker finds a geolocation it will assign a property that has the intent
`geoLocation`.
