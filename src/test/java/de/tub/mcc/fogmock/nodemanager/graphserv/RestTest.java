package de.tub.mcc.fogmock.nodemanager.graphserv;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.*;
import org.junit.runners.MethodSorters;
import org.neo4j.harness.junit.Neo4jRule;
import org.neo4j.server.rest.JaxRsResponse;
import org.neo4j.server.rest.RestRequest;
import javax.swing.JOptionPane;
import javax.ws.rs.core.MediaType;


public class RestTest {
	
	private RestRequest REST_REQUEST;
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private JaxRsResponse response;
	Object docId;

    @Rule
    public Neo4jRule server = new Neo4jRule()
    	.withConfig( "dbms.connector.http.listen_address", "127.0.0.1:7474" )
    	.withExtension("/webapi","de.tub.mcc.fogmock.nodemanager.graphserv"); //.withFixture(SETUP);
//        .withFunction(Extensions.class)



    @Before
    public void setUp() {
        System.err.println(server.httpURI());
        REST_REQUEST = new RestRequest(server.httpURI());
    }

	@After
	public void finish() {
	}



	/*
	 * This test executes an empty and temporary Neo4j database (with the help of the Neo4j rule above)
	 * and subsequently performs a few REST actions
	 */
	@Test
	public void restTest() throws IOException {
		System.out.println("Starting tests...");

		response = REST_REQUEST.get("webapi/test");
		//System.out.println( response.getStatus() );
		URI webUri = server.httpURI().resolve("webapi/test");
		//System.out.println("Testing URI: "+webUri.toString());

		String docName = "testdoc";

		System.out.println("\nCreate sample document with one network...");
		String data = "{ "+
							"\"allNets\": {" +
								"\"4711\": {" + //this id can be set arbitrary and can be used to reference the corresponding net
									"\"addr\": \"1.0.0.0/24\"," +
									"\"name\": \"net1\"," +
									"\"edgesBack\": {}" +
								"}" +
							"}" +
						"}";
		response = REST_REQUEST.post("webapi/doc", data,  MediaType.APPLICATION_JSON_TYPE);
		Map<String,Object> doc = (Map<String,Object>)OBJECT_MAPPER.readValue(response.getEntity(), Map.class);
		docId = doc.get("id");
		System.out.println("Received document id "+docId);
		Map<String,Object> allNets = (Map<String,Object>)doc.get("allNets");
		Object net1Id = allNets.keySet().iterator().next();
		System.out.println("net1 has id "+net1Id);


		//CREATE NET
		System.out.println("\nCreate 3 additional nets...");
		response = REST_REQUEST.post("webapi/doc/"+docId+"/net", "{ \"name\": \"net2\", \"addr\": \"2.0.0.0/24\" }", MediaType.APPLICATION_JSON_TYPE );
		Object net2Id = OBJECT_MAPPER.readValue(response.getEntity(), Map.class).keySet().iterator().next();
		System.out.println("Received net2 with id "+net2Id+" and response-entity: "+response.getEntity());

		response = REST_REQUEST.post("webapi/doc/"+docId+"/net", "{ \"name\": \"net3\", \"addr\": \"3.0.0.0/24\" }", MediaType.APPLICATION_JSON_TYPE );
		Object net3Id = OBJECT_MAPPER.readValue(response.getEntity(), Map.class).keySet().iterator().next();
		System.out.println("Received net3 with id "+net3Id+" and response-entity: "+response.getEntity());

		response = REST_REQUEST.post("webapi/doc/"+docId+"/net", "{ \"name\": \"net4\", \"addr\": \"4.0.0.0/24\" }", MediaType.APPLICATION_JSON_TYPE );
		Object net4Id = OBJECT_MAPPER.readValue(response.getEntity(), Map.class).keySet().iterator().next();
		System.out.println("Received net4 with id "+net4Id+" and response-entity: "+response.getEntity());


		//CREATE RELATIONS BETWEEN NETS
		System.out.println("\nCreate 3 connections between the nets...");

		response = REST_REQUEST.post("webapi/doc/"+docId+"/edge/"+net1Id+"/"+net2Id, "");
		Object e1Id = OBJECT_MAPPER.readValue(response.getEntity(), Map.class);
		System.out.println("Edge created with id: "+e1Id+"  Status: "+response.getStatus());

		response = REST_REQUEST.post("webapi/doc/"+docId+"/edge/"+net2Id+"/"+net3Id, "");
		Object e2Id = OBJECT_MAPPER.readValue(response.getEntity(), Map.class);
		System.out.println("Edge created with id: "+e2Id+"  Status: "+response.getStatus());

		response = REST_REQUEST.post("webapi/doc/"+docId+"/edge/"+net4Id+"/"+net2Id, "");
		Object e3Id = OBJECT_MAPPER.readValue(response.getEntity(), Map.class);
		System.out.println("Edge created with id: "+e3Id+"  Status: "+response.getStatus());

		System.out.println(response.getStatus());



		//CREATE NODES
		Settings.PATH_TO_OS_FLAVORS = System.getProperty("user.dir")+"/src/main/webapp/static/resources/os_device_to_flavor_map.json";
		Settings.PATH_TO_AWS_FLAVORS = System.getProperty("user.dir")+"/src/main/webapp/static/resources/aws_device_to_flavor_map.json";

		System.out.println("\nCreating 3 nodes...");
		response = REST_REQUEST.post("webapi/doc/"+docId+"/node", "{\"name\":\"node1\", \"flavor\":\"Banana Pi\"}", MediaType.APPLICATION_JSON_TYPE );
		Object node1Id = OBJECT_MAPPER.readValue(response.getEntity(), Map.class).keySet().iterator().next();
		System.out.println("Received node1 with id "+node1Id);

		response = REST_REQUEST.post("webapi/doc/"+docId+"/node", "{\"name\":\"node2\", \"flavor\":\"Banana Pi\"}", MediaType.APPLICATION_JSON_TYPE );
		Object node2Id = OBJECT_MAPPER.readValue(response.getEntity(), Map.class).keySet().iterator().next();
		System.out.println("Received node2 with id "+node2Id);

		response = REST_REQUEST.post("webapi/doc/"+docId+"/node", "{\"name\":\"node3\", \"flavor\":\"Banana Pi\"}", MediaType.APPLICATION_JSON_TYPE );
		Object node3Id = OBJECT_MAPPER.readValue(response.getEntity(), Map.class).keySet().iterator().next();
		System.out.println("Received node3 with id "+node3Id);


		//CREATE RELATIONS FROM NODES TO RESPONSIVE NETS
		System.out.println("\nCreate connection between the net "+net4Id+" and node "+node1Id);
		response = REST_REQUEST.post("webapi/doc/"+docId+"/edge/"+net4Id+"/"+node1Id, "");
		Object edge1Id = OBJECT_MAPPER.readValue(response.getEntity(), Map.class).keySet().iterator().next();
		System.out.println("Edge1 created with id: "+edge1Id);

		System.out.println("\nCreate connection between the net "+net1Id+" and node...");
		response = REST_REQUEST.post("webapi/doc/"+docId+"/edge/"+net1Id+"/"+node2Id, "");
		Object edge2Id = OBJECT_MAPPER.readValue(response.getEntity(), Map.class).keySet().iterator().next();
		System.out.println("Edge2 created with id: "+edge2Id);

		System.out.println("\nCreate connection between the net "+net1Id+" and node...");
		response = REST_REQUEST.post("webapi/doc/"+docId+"/edge/"+net3Id+"/"+node3Id, "");
		Object edge3Id = OBJECT_MAPPER.readValue(response.getEntity(), Map.class).keySet().iterator().next();
		System.out.println("Edge2 created with id: "+edge3Id);



		//SET EDGE PROPERTIES
		System.out.println("\nSet delay and dispersion on edge with id "+edge1Id);
		response = REST_REQUEST.post("webapi/doc/"+docId+"/edge/"+net4Id+"/"+node1Id, "{\"delay\":\"10ms\"}", MediaType.APPLICATION_JSON_TYPE);
		Map<String, Object> res = OBJECT_MAPPER.readValue(response.getEntity(), Map.class);
		Map<String, Object> node1 = (Map<String, Object>) res.get(String.valueOf(node1Id));
		Map<String, Object> node1EdgesBack = (Map<String, Object>) node1.get("edgesBack");
		Map<String, Object> edgeFromNet4 = (Map<String, Object>) node1EdgesBack.get(String.valueOf(net4Id));
		System.out.println("Edge now has delay "+edgeFromNet4.get("delay")+" and dispersion "+edgeFromNet4.get("dispersion"));


//        //DELETE NODE
//        System.out.println("\nDelete node with id "+node1Id);
//        response = REST_REQUEST.delete("webapi/doc/"+docId+"/vertex/"+node1Id);
//        System.out.println("node deleted with status: "+response.getStatus());

//        //DELETE NET
//        System.out.println("\nDelete net1 with id "+net1Id);
//        response = REST_REQUEST.delete("webapi/doc/"+docId+"/vertex/"+net1Id);
//        System.out.println("Net deleted with status: "+response.getStatus());

        //RENAME DOC
        String newTestDocName = "newTestDocName";
		System.out.println("\nRenaming the document...");
		response = REST_REQUEST.put("webapi/doc/"+docId, "{\"docName\":\"NewDocumentName\"}");
		System.out.println("Doc renamed with status: "+response.getStatus());



		//DELETE DOC
		System.out.println("\nDelete document...");
		response = REST_REQUEST.delete("webapi/doc/"+docId);
		System.out.println("Doc deleted with status: "+response.getStatus());


		System.out.println("\n\nRestTest finished.");


	}

}
