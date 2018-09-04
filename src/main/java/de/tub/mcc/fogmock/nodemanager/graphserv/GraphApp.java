package de.tub.mcc.fogmock.nodemanager.graphserv;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;


/*
 * Without this Application we need to 
 *  - copy the jar-file of this code (and custom dependencies to $NEO4J_SERVER_HOME/plugins
 *  - tell Neo4j where to look for the extension by adding some configuration in neo4j.conf:
 *    #Comma separated list of JAXRS packages containing JAXRS Resource, one package name for each mountpoint.
 *    dbms.unmanaged_extension_classes=org.neo4j.examples.server.unmanaged=/graph/unmanaged
 *    
 *    see: 
 *    https://neo4j.com/docs/java-reference/current/server-extending/#server-unmanaged-extensions
 *    https://github.com/neo4j/neo4j-documentation/blob/3.3/server-examples/src/main/java/org/neo4j/examples/server/unmanaged/HelloWorldResource.java
 *
 */
@ApplicationPath("webapi")
public class GraphApp extends Application {}