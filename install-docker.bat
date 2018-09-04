@echo OFF
SET sp=%~dp0

@echo ON
call docker stop neo

CALL mvn clean package -DskipTests
DEL %sp%\docker\plugins\*.jar
DEL %sp%\docker\plugins\*.war
COPY %sp%\target\graph-servic*.jar %sp%\docker\plugins
CALL docker run --name=neo -d --rm ^
  --publish=7474:7474 --publish=7687:7687 --publish=8080:8080 ^
  --volume=%sp%/docker/data:/data --volume=%sp%/docker/logs:/logs --volume=%sp%/docker/plugins:/var/lib/neo4j/plugins ^
  --env NEO4J_AUTH=none ^
  --env NEO4J_dbms_unmanaged__extension__classes=de.tub.mcc.fogmock.nodemanager.graphserv=/webapi ^
  neo4j:3.3.5

@echo OFF
echo.
echo Apply following command to stop the neo4j service:     docker stop neo
@echo ON