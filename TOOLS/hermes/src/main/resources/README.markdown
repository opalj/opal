# Configuration and Running Hermes

## application.conf
Static configuration of all available queries as well as further global settings.

## hermes.json
The configuration file which is used to specify the projects which belong to a test corpus. 
By default, those projects are specified which belong to OPAL's test corpus. 
To evaluate another corpus just have to adapt this file. 
Basically every project has the following settings: `id`, `cp`, `libcp` and `libcp_defaults`.

### `id`
`id` specifies the unique name of the project.

### `cp`
`cp` specifies the class path of the project that references all classes that are developed as part of the project.
It is possible to specify multiple folders/jars by separating them using the system's folder separator (on Mac: `:`).

### `libcp` and `libcp_defaults`(Optional)
These parameters specify the used libraries that should be loaded.
The latter two settings are particularly required by more advanced queries which, e.g., require a complete class hierarchy.
`libcp_defaults` is used to specify that some generally available APIs should be added to the class path.
Currently, the keys `JRE` and `RTJar` are predefined and will always resolve against the current ***JRE***/the ***rt.jar***.

    {
      "id": "OPAL-bytecode-infrastructure_2.11-SNAPSHOT-08-14-2014",
      "cp": "../../OPAL/bi/src/test/resources/classfiles/OPAL-bytecode-infrastructure_2.11-SNAPSHOT-08-14-2014.jar",
      "libcp": "../../OPAL/bi/src/test/resources/classfiles/OPAL-common_2.11-SNAPSHOT-08-14-2014.jar"
      "libcp_defaults" : ["JRE"]
    }

## Note
Splitting the configuration across the two files: `application.conf` and `hermes.json` was purely done to provide some structure and to facilitate reuse. 
All settings defined in `application.conf` can also be set in `hermes.json`. 
It is in particular possible to turn of or add new queries by specifying them in `hermes.json`.
