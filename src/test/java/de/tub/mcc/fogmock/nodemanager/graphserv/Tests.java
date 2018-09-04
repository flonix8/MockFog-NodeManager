package de.tub.mcc.fogmock.nodemanager.graphserv;

import java.io.IOException;
import java.net.URI;

import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.neo4j.harness.junit.Neo4jRule;
import org.neo4j.server.rest.JaxRsResponse;
import org.neo4j.server.rest.RestRequest;

import javax.swing.JOptionPane;
import javax.ws.rs.core.MediaType;

//python -m http.server 8080
public class Tests {
	
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

	@Test
	public void firstTest() {
		System.out.println("Starting tests...");


/*
		JaxRsResponse response = REST_REQUEST.get("webapi/test");		
		//System.out.println( response.getStatus() );
		URI webUri = server.httpURI().resolve("webapi/test");
		//System.out.println("Testing URI: " + webUri.toString());
		
		String docName = "testdoc";
		
		System.out.println("Creating sample document...");
		response = REST_REQUEST.post("webapi/doc/testdoc1", "");
		long docId = OBJECT_MAPPER.readValue(response.getEntity(), ModelGraph.class).id;
		System.out.println("Received document id " + docId);



		System.out.println("Creating 4 sample nets...");
		response = REST_REQUEST.post("webapi/doc/"+docId+"/subnet", "{\"edgesIn\": [], props:{\"name\":\"subnet1\", \"posX\":0, \"posY\":0}}", MediaType.APPLICATION_JSON_TYPE );
		String test = response.getEntity();
		System.out.println("respones-entity: "+test);
		long node1Id = OBJECT_MAPPER.readValue(response.getEntity(), ModelVertex.class).id;
		System.out.println("Received net1 with id " + node1Id);
		
		response = REST_REQUEST.post("webapi/doc/"+docId+"/subnet", "{\"edgesIn\": [], props:{\"name\":\"subnet2\", \"posX\":400, \"posY\":0}}", MediaType.APPLICATION_JSON_TYPE );
		long node2Id = OBJECT_MAPPER.readValue(response.getEntity(), ModelVertex.class).id;
		System.out.println("Received net2 with id " + node2Id);
		
		response = REST_REQUEST.post("webapi/doc/"+docId+"/subnet", "{\"edgesIn\": [], props:{\"name\":\"subnet3\", \"posX\":0, \"posY\":400}}", MediaType.APPLICATION_JSON_TYPE );
		long node3Id = OBJECT_MAPPER.readValue(response.getEntity(), ModelVertex.class).id;
		System.out.println("Received net3 with id " + node3Id);
		
		response = REST_REQUEST.post("webapi/doc/"+docId+"/subnet", "{\"edgesIn\": [], props:{\"name\":\"subnet4\", \"posX\":400, \"posY\":400}}", MediaType.APPLICATION_JSON_TYPE );
		long node4Id = OBJECT_MAPPER.readValue(response.getEntity(), ModelVertex.class).id;
		System.out.println("Received net4 with id " + node4Id);

		response = REST_REQUEST.post("webapi/doc/"+docId+"/net", "{\"name\":\"testnet2\", \"netType\":\"EDGE\"}", MediaType.APPLICATION_JSON_TYPE );
		long net2Id = OBJECT_MAPPER.readValue(response.getEntity(), ModelVertex.class).id;
		System.out.println("Received net2 with id " + net2Id);
		
		response = REST_REQUEST.post("webapi/doc/"+docId+"/net", "{\"name\":\"testnet3\", \"netType\":\"CLOUD\"}", MediaType.APPLICATION_JSON_TYPE );
		long net3Id = OBJECT_MAPPER.readValue(response.getEntity(), ModelVertex.class).id;
		System.out.println("Received net3 with id " + net3Id);

		response = REST_REQUEST.post("webapi/doc/"+docId+"/net", "{\"name\":\"testnet4\", \"netType\":\"CLOUD\"}", MediaType.APPLICATION_JSON_TYPE );
		long net4Id = OBJECT_MAPPER.readValue(response.getEntity(), ModelVertex.class).id;
		System.out.println("Received net4 with id " + net4Id);

		
		System.out.println("Create 3 connections between the nets...");
		response = REST_REQUEST.post("webapi/doc/"+docId+"/net/"+net1Id+"/edgeTo/"+net2Id, "");
		System.out.println(response.getStatus());		
		response = REST_REQUEST.post("webapi/doc/"+docId+"/net/"+net2Id+"/edgeTo/"+net3Id, "");
		System.out.println(response.getStatus());
		response = REST_REQUEST.post("webapi/doc/"+docId+"/net/"+net4Id+"/edgeTo/"+net2Id, "");
		System.out.println(response.getStatus());

*/
		
/* //Todo from init-node-management-java (Data Types have to be changed)
		//CREATE NODE
		System.out.println("Creating 2 nodes...");
		response = REST_REQUEST.post("webapi/doc/"+docId+"/node", "{\"name\":\"testnode1\", \"flavour\":\"m1.small\"}", MediaType.APPLICATION_JSON_TYPE );
		long node1Id = OBJECT_MAPPER.readValue(response.getEntity(), ModelNode.class).id;
        String node1Flavour = OBJECT_MAPPER.readValue(response.getEntity(), ModelNode.class).flavour;
		System.out.println("Received node1 with id " + node1Id + " and flavour " + node1Flavour);

		response = REST_REQUEST.post("webapi/doc/"+docId+"/node", "{\"name\":\"testnode2\", \"flavour\":\"m1.small\"}", MediaType.APPLICATION_JSON_TYPE );
		long node2Id = OBJECT_MAPPER.readValue(response.getEntity(), ModelNode.class).id;
		String node2Flavour = OBJECT_MAPPER.readValue(response.getEntity(), ModelNode.class).flavour;
		System.out.println("Received node1 with id " + node2Id + " and flavour " + node2Flavour);

		//CREATE RELATION FROM NODES TO RESPONSIVE NETS
		System.out.println("Create connection between the net " +net1Id+ " and node...");
		response = REST_REQUEST.post("webapi/doc/"+docId+"/net/"+net1Id+"/edgeTo/"+node1Id, "");
		long edge1Id = OBJECT_MAPPER.readValue(response.getEntity(), ModelEdge.class).id;
		System.out.println("Edge1 created with id: " + edge1Id);

		System.out.println("Create connection between the net " +net1Id+ " and node...");
		response = REST_REQUEST.post("webapi/doc/"+docId+"/net/"+net1Id+"/edgeTo/"+node2Id, "");
		long edge2Id = OBJECT_MAPPER.readValue(response.getEntity(), ModelEdge.class).id;
		System.out.println("Edge2 created with id: " + edge2Id);

		//SET EDGE PROPERTIES
		System.out.println("Create tc config with only delay for edge " +edge1Id);
		response = REST_REQUEST.post("webapi/doc/"+docId+"/edge/"+edge1Id, "{\"delay\":\"10ms\"}", MediaType.APPLICATION_JSON_TYPE);
		String edge1Delay = OBJECT_MAPPER.readValue(response.getEntity(), ModelEdge.class).delay;
		String edge1Dispersion = OBJECT_MAPPER.readValue(response.getEntity(), ModelEdge.class).dispersion;
		System.out.println("Delay created with value: " + edge1Delay + " and dispersion (default if not in JSON) set to: " + edge1Dispersion);

		System.out.println("Create tc config with delay and dispersion for edge " +edge2Id);
		response = REST_REQUEST.post("webapi/doc/"+docId+"/edge/"+edge2Id, "{\"delay\":\"10ms\", \"dispersion\":\"20ms\"}", MediaType.APPLICATION_JSON_TYPE);
		String edge2Delay = OBJECT_MAPPER.readValue(response.getEntity(), ModelEdge.class).delay;
		String edge2Dispersion = OBJECT_MAPPER.readValue(response.getEntity(), ModelEdge.class).dispersion;
		System.out.println("Delay created with value: " + edge2Delay);
		System.out.println("Dispersion created with value: " + edge2Dispersion);

        //DELETE NODE
        System.out.println("Deleting the node ...");
        response = REST_REQUEST.delete("webapi/doc/"+docId+"/node/"+node1Id);
        System.out.println("Node deleted ! "+response.getStatus());

        //DELETE NET
        System.out.println("Deleting the net1 ...");
        response = REST_REQUEST.delete("webapi/doc/"+docId+"/net/"+net1Id);
        System.out.println("Net deleted ! "+response.getStatus());

        //RENAME DOC
        String newTestDocName = "newTestDocName";
		System.out.println("Renaming the document...");
		response = REST_REQUEST.put("webapi/doc/"+docName, newTestDocName);
		System.out.println("Doc renamed Doc! "+response.getStatus());

*/
		JOptionPane.showMessageDialog(null, "Leave this message open in order to prevent the termination of the server process.", "Tests Finished", JOptionPane.INFORMATION_MESSAGE);

/*
		//DELETE DOC
		System.out.println("Deleting the document...");
		response = REST_REQUEST.delete("webapi/doc/newTestDocName");
		System.out.println("Doc deleted with all it's relationships! "+response.getStatus());
*/

		System.out.println("Tests finished.");
		
	}
	
//    private static TestServerBuilder getServerBuilder( ) throws IOException
//    {
//        TestServerBuilder serverBuilder = TestServerBuilders.newInProcessBuilder();
//        String path = ServerTestUtils.getRelativePath(
//                getSharedTestTemporaryFolder(), LegacySslPolicyConfig.certificates_directory );
//        serverBuilder.withConfig( LegacySslPolicyConfig.certificates_directory.name(), path )
//                     .withConfig( ServerSettings.script_enabled.name(), Settings.TRUE );
//        return serverBuilder;
//    }	
}
