# Build with "docker build . -t nodemanager"
# Run with "docker run -it --rm --name nm -v <PATH/TO/MockFog-IaC>:<PATH/INSIDE/CONTAINER> nodemanager"
# after that the nodemanager can run e.g. ansible-playbook aws.yml --tags bootstrap at <PATH/INSIDE/CONTAINER>
FROM maven:3.5.4-jdk-8 AS build
COPY src /usr/src/app/src
COPY pom.xml /usr/src/app
RUN mvn -f /usr/src/app/pom.xml clean package -DskipTests

FROM hasenburg/nodemanager
COPY --from=build /usr/src/app/target/graph-servic*.jar /var/lib/neo4j/plugins/
ENV NEO4J_AUTH none
ENV NEO4J_dbms_unmanaged__extension__classes de.tub.mcc.fogmock.nodemanager.graphserv=/webapi

COPY ansible.cfg /var/lib/neo4j
COPY mockfog.key /var/lib/neo4j
RUN chmod 600 mockfog.key
RUN chown neo4j:neo4j mockfog.key
RUN touch /var/lib/neo4j/logs/ansible.log
RUN chown neo4j:neo4j /var/lib/neo4j/logs/ansible.log

EXPOSE 7474 7687
