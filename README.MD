# Coding Exercise

While this challenge can be presented to any level of software engineer, an SEI may struggle to complete and deliver all requirements (especially
with the object detection API). Each candidate’s code should be evaluated based on their level. A Senior should absolutely be capable of
completing this challenge in its entirety.

## Overview
Build a HTTP REST API in {Java|Node.js|Python} for a service that ingests user images, analyzes them for object detection, and returns the
enhanced content. It should implement the following specification::

### API Specification

**GET /images**
Returns HTTP 200 OK with a JSON response containing all image metadata.

**GET /images?objects="dog,cat"**
Returns a HTTP 200 OK with a JSON response body containing only images that have the detected objects specified in the query
parameter.

**GET /images/{imageId}**
Returns HTTP 200 OK with a JSON response containing image metadata for the specified image.

**POST /images**
Send a JSON request body including an image file or URL, an optional label for the image, and an optional field to enable object
detection.
Returns a HTTP 200 OK with a JSON response body including the image data, its label (generate one if the user did not provide it), its
identifier provided by the persistent data store, and any objects detected (if object detection was enabled).

### Object detection instructions
Image object detection can be performed using any API offering of your choosing (such as Google, IBM, Imagga, etc), or with a process
managed by your backend. The only requirement is that it must return a list of object names detected within that image.
That is the extent of the API contract. HTTP error codes should be used to indicate the proper level of system failure (i.e. client versus server).

### Database
A persistent data store is required, any variant of SQL is encouraged.

### Expectations

No frontend is required, but you may create one to demo the API. Regardless, a user of the API should be able to:
  - Upload an optionally labelled image and run image object detection on it
  - Retrieve all images and any metadata obtained from their analyses
  - Search for images based on detected objects


------------------
 
## Design

### HTTP Server
Utilizing AkkaHttp. I made an attempt at an Http4s but I was hitting some road blocks and for the sake 
of time decided to go back to Akka Http.

### Database
Utilizing Postgres because ... who doesn't love Postgres.

#### Table Design

See [Migrations](api/src/main/resources/db/migration) for DDLs

##### images
| Column         | Type                        | Description               |
|----------------|-----------------------------|---------------------------|
| id             | UUID PRIMARY KEY auto_gen() | The uuid of the image     |
| image_source   | TEXT NOT NULL               | The image or url          |
| image_metadata | jsonb NOT NULL              | the metadata of the image |

#### Performance Considerations

  - In general RDBMs aren't a great solution for large images. I would use something like GCS/S3 Buckets or Azure Blob
    Storage to store the actual images and then refer to those from the RDBMS.
  - Went with UUIDs for image ids since it's a little more secure than just using integer IDs but I am aware this affects
     performance. Another option is to use MD5 Hashes. This would allow multiple systems and users the ability to generate
     a natural key for each image/object that could be referred to by many systems.
    - Although it is possible to have MD5 collisions, it is statistically rare for those to happen naturally 
      without a targeted attack. Because this for an interview I will stick with UUIDs because it's 'proper form' 
      but were this a real application I would want to open the discussion of MD5s.  
  - I normally wouldn't add DB migrations at app start up but given there is no CI in this example, adding it app startup

## Docker
Ran out of time to get this solution fully dockerized. Having some issues with sbt-assembly. 
Specifically, when I assemble the fat-jar it can't find my main class ┬─┬ ︵ /(.□. \）

## Postman Collection
There is Postman collection [RedOktober.postman_collection.json](RedOktober.postman_collection.json) in
the root of the repo.




