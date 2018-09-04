1. `docker pull swaggerapi/swagger-ui`
2. Im Hauptverzeichnis des Git-Projektes ausführen:
  * Linux: `docker run --name=nms -p 8888:8080 -e SWAGGER_JSON=/mnt/swagger.json -v $(pwd)/swagger/:/mnt swaggerapi/swagger-ui`
  * Windows: `docker run --name=nms -p 8888:8080 -e SWAGGER_JSON=/mnt/swagger.json -v %cd%/swagger/:/mnt swaggerapi/swagger-ui`

Beispiele: [http://petstore.swagger.io/](http://petstore.swagger.io/)
