# AUTH-Service
Authorisation service helps with determination either particular client has permission to execute a method or an anction in a system.

## Technologies
Service uses next technologies:
* Redis (for keeping authorization tokens and internal identities).
* SocketIO client (for connect to a config service and receive config)

## Features
* Checks headers of an incoming request with the predefined address (example: `/api/**`) and determine whether it contains authorization or not.
* Defines internal identifiers and replaces headers with them with internal identify headers.
* Proxies the request, if it is allowed, to the pre-specified service.
* Receives a configuration with settings and dynamically updates it in case it has been changed (keeps a permanent WebSocket connection with the configuration service).

## Setup
* Download [nginx-clojure](https://nginx-clojure.github.io/downloads.html) or compile from the source.
* Compile current project as usual in `jar` file.
* Put compiled jar file in next nginx directory `../nginx/libs/` (there is also a gradle task to do it `copyJarToBin` but you need to specify your correct nginx path).
* First necessary parameters you must specify in `nginx.conf` in a `http` section.

 ```
    jvm_options "-Dconfig.server.ip=http://10.250.9.114:5000";
    jvm_options "-Dinstance.uuid=bf482806-0c3d-4e0d-b9d4-12c037b12d70";
    jvm_options "-Dmodule.name=auth";
 ```
* Put `nginx.conf` in next nginx directory `../nginx/conf/` (there is also a gradle task to do it `copyConfToBin` but you need to specified your correct nginx path).
* After it everything is usual for nginx server.
* Java code has its own log file default in directory `logs/app.log` with rotation by time and size. The behavior can be changed. The logger settings is in the file `../src/main/resources/log4j2.xml`.

## Usage
* When the app is starting the method `JvmInitHandler.invoke` call just one time. The main work like config, WebSocket initialization is accomplished also here.
* Next step is that the app is waiting for config from config service and in case of config had been successfully received initializations process for redis connection.
* After it, the app is ready to handle requests.

## Redis db
A data in redis separeated a next way:
The app uses first 6 dbs from 16 default once in redis.
1. The first db with index `0` contains records with Map, where key is `APPLICATION_TOKEN` and values:
 
```
    "1": "SYSTEM_IDENTITY_AS_A_STRING",
    "2": "DOMAIN_IDENTITY_AS_A_STRING",
    "3": "SERVICE_IDENTITY_AS_A_STRING",
    "4": "APPLICATION_IDENTITY_AS_A_STRING",
```
If the record doesn't exist, that means the token isn't valid.
2. The second DB with index `1` contains records to determine whether it's allowed for this token call a method. For the token: "APPLICATION_IDENTITY_AS_A_STRING" and requested URL: "/api/userProfile/get/1" an allowed record will be: `APPLICATION_IDENTITY_AS_A_STRING|apiuserprofileget1: 1` and prohibit record is `APPLICATION_IDENTITY_AS_A_STRING|apiuserprofileget1: 0`. If the record with the key: "APPLICATION_IDENTITY_AS_A_STRING|apiuserprofileget1" absents that also means that the request is prohibited.
3. The third DB has index `2` and contains `USER_TOKEN|DOMAIN_IDENTITY: USER_IDENTITY` for example: "5e386895-f934-4290-a6ca-d3d388d88af5|DOMAIN_IDENTITY_AS_A_STRING: USER_IDENTITY_AS_A_STRING". 
4. The forth DB with index `3` contains only records like:  `apiuserprofileget1: 0`, what means that call method: /api/userProfile/get/1 without `USER_TOKEN` or with an invalid token is prohibited. A record like: `apiuserprofileget1: 1` doesn't have any sense because by default it's allowed to use any methods without USER_TOKEN.
5. The fifth DB has index `4` and contains `DEVICE_TOKEN|DOMAIN_IDENTITY: DEVICE_IDENTITY` for example: "5e386895-f934-4290-a6ca-d3d388d88af5|DOMAIN_IDENTITY_AS_A_STRING: DEVICE_IDENTITY_AS_A_STRING". 
6. The sixth and the last one DB with index `5` contains only records like:  `apiuserprofileget1: 0`, what means that call method: /api/userProfile/get/1 without `DEVICE_TOKEN` or with an invalid token is prohibited. A record like: `apiuserprofileget1: 1` doesn't have any sense because by default it's allowed to use any methods without DEVICE_TOKEN.

## Remote config example
```json
{
    "redis": {
        "ip": "10.250.9.114",
        "port": 6379
    },
    "proxy.address": "http://10.250.101.64:3000/test",
    "unauthorized.html": "<HTML><BODY><H1>401 Unauthorized BAD CREDENTIALS</H1></BODY></HTML>",
    "header": {
        "token": {
            "application": "X-APPLICATION-TOKEN",
            "user": "X-USER-TOKEN",
            "device": "X-DEVICE-TOKEN"
        },
        "identity": {
            "application": "X-APPLICATION-IDENTITY",
            "user": "X-USER-IDENTITY",
            "device": "X-DEVICE-IDENTITY",
            "service": "X-SERVICE-IDENTITY",
            "domain": "X-DOMAIN-IDENTITY",
            "system": "X-SYSTEM-IDENTITY"
        }
    }
}
```
