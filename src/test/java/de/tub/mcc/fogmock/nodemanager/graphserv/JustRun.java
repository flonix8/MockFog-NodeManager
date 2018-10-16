package de.tub.mcc.fogmock.nodemanager.graphserv;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.neo4j.harness.junit.Neo4jRule;
import org.neo4j.server.rest.RestRequest;

import javax.swing.*;
import java.io.IOException;

public class JustRun {

    private RestRequest REST_REQUEST;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Rule
    public Neo4jRule server = new Neo4jRule()
            .withConfig( "dbms.connector.http.listen_address", "127.0.0.1:7474" )
//        .withFunction(Extensions.class)
            .withExtension("/webapi","de.tub.mcc.fogmock.nodemanager.graphserv"); //.withFixture(SETUP);

    @Before
    public void setUp() {
        System.err.println(server.httpURI());
        REST_REQUEST = new RestRequest(server.httpURI());
    }


    /*
     * This test just executes an empty and temporary Neo4j database (with the help of the Neo4j rule above)
     *
     */
    @Test
    public void justRun() throws IOException {
        System.out.println("Application starts...");

        JOptionPane.showMessageDialog(null, "Leave this message open in order to prevent the termination of the server process.", "RestTest Finished", JOptionPane.INFORMATION_MESSAGE);

        System.out.println("\n\nApplication finished.");
    }


}
