package de.tub.mcc.fogmock.nodemanager.graphserv;

//import org.codehaus.jackson.map.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.neo4j.harness.junit.Neo4jRule;
import org.neo4j.server.rest.JaxRsResponse;
import org.neo4j.server.rest.RestRequest;

public class InjectorTests {

    private RestRequest REST_REQUEST;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Rule
    public Neo4jRule server = new Neo4jRule()
            .withConfig( "dbms.connector.http.listen_address", "127.0.0.1:7474" )
            .withExtension("/webapi","de.tub.mcc.fogmock.nodemanager.graphserv"); //.withFixture(SETUP);


    @Before
    public void setUp() {
        System.err.println(server.httpURI());
        REST_REQUEST = new RestRequest(server.httpURI());
    }

    @Test
    public void injectProperty() {
        System.out.println("Starting tests...");

        String host = "192.168.0.210";
        String port = "5000";

        JaxRsResponse response = REST_REQUEST.get("webapi/agent/"+host+"/testproperty/testproperty");
        System.out.println( response.getStatus() );

        //System.out.println( response.getStatus() );
        //URI webUri = server.httpURI().resolve("webapi/test");
        //System.out.println("Testing URI: " + webUri.toString());
    }

    public void helloWorldTest(){
        JaxRsResponse response = REST_REQUEST.get("webapi/agent/helloWorld");
        System.out.println( response.getEntity());
    }
}
