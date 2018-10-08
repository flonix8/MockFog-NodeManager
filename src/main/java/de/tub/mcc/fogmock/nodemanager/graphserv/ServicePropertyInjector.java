package de.tub.mcc.fogmock.nodemanager.graphserv;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;


import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import de.tub.mcc.fogmock.nodemanager.graphserv.agent.ResponseFirewall;
import de.tub.mcc.fogmock.nodemanager.graphserv.agent.ResponseMessage;
import de.tub.mcc.fogmock.nodemanager.graphserv.agent.ResponseStatus;
import de.tub.mcc.fogmock.nodemanager.graphserv.agent.ResponseTcConfig;

import org.json.simple.JSONObject;
import org.neo4j.graphdb.*;
import org.neo4j.helpers.collection.MapUtil;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

//import org.glassfish.jersey.client.ClientConfig;
//import javax.ws.rs.client.ClientBuilder;
//import javax.ws.rs.client.Client;
//import javax.ws.rs.client.Entity;
//import javax.ws.rs.client.WebTarget;

import java.io.StringWriter;
import java.net.URI;
import java.util.Map;


@Path("assign")
public class ServicePropertyInjector extends ServiceUserInterface {
    private Client client;
    String ip = "172.0.0.1";
    private static Logger logger = LoggerFactory.getLogger(InfrastructureController.class);

    public ServicePropertyInjector(@Context GraphDatabaseService graphDb) {
        super(graphDb);
        ClientConfig config = new DefaultClientConfig();
        this.client = Client.create(config);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("tc-config/doc/{docId}/edge/{nodeFromId}/{nodeToId}")
    public Response editEdgeAndInject(@PathParam("docId") final Long docId, @PathParam("nodeFromId") final Long nodeFromId, @PathParam("nodeToId") final Long nodeToId, ModelEdge modelE) {
        // propagieren
        // editEdge() aufrufen (mit Path-Parametern)
        // propagieren
        // vergleiche beide "Propagier"-Ergebnisse
        // rufe alle agents der differenz der beiden ergebnisse auf und setze tc config
        // return success or error


        //First Propagation
        ResponseTcConfig[] pre = null;
        String strCurrentAdj = null;

        try ( Transaction tx = db.beginTx() ) {
            startPropagation(docId, "PREVCHANGE");
//            strCurrentAdj = getAdjLists(db, docId, "PREVCHANGE");
//            System.out.println("PREVCHANGE: \n" + strCurrentAdj);
//            pre =  new ObjectMapper().readValue(strCurrentAdj, ResponseTcConfig[].class);
            tx.success();
        } catch (Exception e) {
            logger.error("Exception while editing edge and injecting (part 1): ", e);
        }
        Response response = editEdge(docId, nodeFromId, nodeToId, modelE);
        if (response.getStatus() != 200) return response;


        //Second propagation
        ResponseTcConfig[] post = null;
        try ( Transaction tx = db.beginTx() ) {
            startPropagation(docId, "POSTCHANGE");
//            strCurrentAdj = getAdjLists(db, docId, "POSTCHANGE");
//            System.out.println("POSTCHANGE: \n" + strCurrentAdj);
//            post =  new ObjectMapper().readValue(strCurrentAdj, ResponseTcConfig[].class);
            tx.success();
        } catch (Exception e) {
            logger.error("Exception while editing edge and injecting (part 2): ", e);
        }


        StringWriter sw = new StringWriter();
        ResponseTcConfig responseTcConfig;
        try ( Transaction tx = db.beginTx() ) {
            JsonGenerator jg = objectMapper.getFactory().createGenerator( sw );
            jg.writeStartObject();
                /*
                 * Submit newly emerged ADJ edges (tcAdditions
                 */
                Result tcAdditions = db.execute("MATCH (vdoc:DOC)-[mgmtEdge:LINK]->(no:NODE)<-[eno:LINK]-(:NET) WHERE id(vdoc)="+docId+" " +
                        " OPTIONAL MATCH (no)<-[post:POSTCHANGE]-(ni:NODE)<-[eni1:LINK]-(:NET) WHERE not (no)<-[:PREVCHANGE]-(ni) " +
                        " WITH no, eno, mgmtEdge.addr as mgmtIp, collect(post {in_rate:post.in_rate+'kbps', out_rate:post.out_rate+'kbps', dispersion:post.dispersion+'ms', delay:post.delay+'ms', .loss, .corrupt, .duplicate, .reorder, dst_net:eni1.addr}) as ru1 " +
                        " OPTIONAL MATCH (no)-[post:POSTCHANGE]->(ni:NODE)<-[eni2:LINK]-(:NET) WHERE not (no)-[:PREVCHANGE]->(ni) " +
                        " WITH mgmtIp, { in_rate:eno.in_rate+'kbps', out_rate:eno.out_rate+'kbps', rules:ru1+collect(post {in_rate:post.out_rate+'kbps', out_rate:post.in_rate+'kbps', dispersion:post.dispersion+'ms', delay:post.delay+'ms', .loss, .corrupt, .duplicate, .reorder, dst_net:eni2.addr}) } as tcConfig" +
                        " RETURN mgmtIp, tcConfig, size(tcConfig.rules) as countRules " +
                        " ");
                jg.writeFieldName("tcAdditions");
                jg.writeStartObject();
                while ( tcAdditions.hasNext() ) {
                    Map<String,Object> resMap = tcAdditions.next();
                    if (resMap.get("countRules")==null || (long)resMap.get("countRules")==0) continue;
                    String ipMgmt = resMap.get("mgmtIp").toString();
//                    ResponseTcConfig tcConfig = client.resource(getBaseURI(ip, 5000)).path("api").path("tc-config/").accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON)
//                            .post(ResponseTcConfig.class, objectMapper.writeValueAsString(resMap.get("tcConfig")));
                    jg.writeFieldName( (String)resMap.get("mgmtIp") );
                    jg.writeRawValue( objectMapper.writeValueAsString(resMap.get("tcConfig")) );
                }
                jg.writeEndObject();

                /*
                 * Submit changed ADJ edges (tcUpdates)
                 */
                Result tcUpdates = db.execute("MATCH (vdoc:DOC)-[mgmtEdge:LINK]->(no:NODE)<-[eno:LINK]-(:NET) WHERE id(vdoc)="+docId+" " +
                        " OPTIONAL MATCH (:NET)-[eni1:LINK]->(ni:NODE)-[prev:PREVCHANGE]->(no)<-[post:POSTCHANGE]-(ni) WHERE (properties(prev)<>properties(post)) " +
                        " WITH no, eno, mgmtEdge.addr as mgmtIp, collect(post {in_rate:post.in_rate+'kbps', out_rate:post.out_rate+'kbps', dispersion:post.dispersion+'ms', delay:post.delay+'ms', .loss, .corrupt, .duplicate, .reorder, dst_net:eni1.addr}) as ru1 " +
                        " OPTIONAL MATCH (:NET)-[eni2:LINK]->(ni:NODE)<-[prev:PREVCHANGE]-(no)-[post:POSTCHANGE]->(ni) WHERE (properties(prev)<>properties(post)) " +
                        " WITH mgmtIp, { in_rate:eno.in_rate+'kbps', out_rate:eno.out_rate+'kbps', rules:ru1+collect(post {in_rate:post.out_rate+'kbps', out_rate:post.in_rate+'kbps', dispersion:post.dispersion+'ms', delay:post.delay+'ms', .loss, .corrupt, .duplicate, .reorder, dst_net:eni2.addr}) } as tcConfig" +
                        " RETURN mgmtIp, tcConfig, size(tcConfig.rules) as countRules " +
                        " ");
                jg.writeFieldName("tcUpdates");
                jg.writeStartObject();
                while ( tcUpdates.hasNext() ) {
                    Map<String,Object> resMap = tcUpdates.next();
                    if (resMap.get("countRules")==null || (long)resMap.get("countRules")==0) continue;
                    String ipMgmt = resMap.get("mgmtIp").toString();
//                    ResponseTcConfig tcConfig = client.resource(getBaseURI(ip, 5000)).path("api").path("tc-config/").accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON)
//                            .post(ResponseTcConfig.class, objectMapper.writeValueAsString(resMap.get("tcConfig")));
                    jg.writeFieldName( (String)resMap.get("mgmtIp") );
                    jg.writeRawValue( objectMapper.writeValueAsString(resMap.get("tcConfig")) );
                }
                jg.writeEndObject();

                /*
                 * Submit dissolved ADJ edges (tcDeletions)
                 */
                Result tcDeletions = db.execute("MATCH (vdoc:DOC)-[mgmtEdge:LINK]->(no:NODE)<-[eno:LINK]-(:NET) WHERE id(vdoc)="+docId+" " +
                        " OPTIONAL MATCH (:NET)-[eni1:LINK]->(ni:NODE)<-[:LINK]-(vdoc)-[mgmtEdge:LINK]->(no:NODE)<-[prev:PREVCHANGE]-(ni) WHERE not (no)<-[:POSTCHANGE]-(ni) " +
                        " WITH no, eno, mgmtEdge.addr as mgmtIp, collect(prev {in_rate:prev.in_rate+'kbps', out_rate:prev.out_rate+'kbps', dispersion:prev.dispersion+'ms', delay:prev.delay+'ms', .loss, .corrupt, .duplicate, .reorder, dst_net:eni1.addr}) as ru1 " +
                        " OPTIONAL MATCH (:NET)-[eni2:LINK]->(ni:NODE)<-[:LINK]-(vdoc)-[mgmtEdge:LINK]->(no:NODE)-[prev:PREVCHANGE]->(ni) WHERE not (no)-[:POSTCHANGE]->(ni) " +
                        " WITH mgmtIp, { in_rate:eno.in_rate+'kbps', out_rate:eno.out_rate+'kbps', rules:collect(prev {in_rate:prev.out_rate+'kbps', out_rate:prev.in_rate+'kbps', dispersion:prev.dispersion+'ms', delay:prev.delay+'ms', .loss, .corrupt, .duplicate, .reorder, dst_net:eni2.addr}) } as tcConfig" +
                        " RETURN mgmtIp, tcConfig, size(tcConfig.rules) as countRules " +
                        " ");
                jg.writeFieldName("tcDeletions");
                jg.writeStartObject();
                while ( tcDeletions.hasNext() ) {
                    Map<String,Object> resMap = tcDeletions.next();
                    if (resMap.get("countRules")==null || (long)resMap.get("countRules")==0) continue;
                    String ipMgmt = resMap.get("mgmtIp").toString();
//                    ResponseTcConfig tcConfig = client.resource(getBaseURI(ip, 5000)).path("api").path("tc-config/").accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON)
//                            .post(ResponseTcConfig.class, objectMapper.writeValueAsString(resMap.get("tcConfig")));
                    jg.writeFieldName( (String)resMap.get("mgmtIp") );
                    jg.writeRawValue( objectMapper.writeValueAsString(resMap.get("tcConfig")) );
                }
                jg.writeEndObject();
            jg.writeEndObject();
            jg.flush();
            jg.close();
            tx.success();
        } catch (Exception e) {
            logger.error("Exception while editing and injecting edge (part 3): ", e);
        }

        logger.info("Difference: \n" +sw.toString());
        //TODO: Find bugs (but incremental updates will probably never be used. Hence we won't spend too much time in this)
        return Response.ok().entity(sw.toString()).type(MediaType.APPLICATION_JSON).build();
    }

    /** This method returns the properties (TC configurations) of a node by requesting the current props from the respective agent.
     *
     * @param docId the id of the document to get the node from
     * @param nodeId the id of the node to get the TC configurations from
     * @return
     */
    @GET
    @Path("tc-config/doc/{docId}/node/{nodeId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTcConfigByNodeId(@PathParam("docId") final Long docId, @PathParam("nodeId") final Long nodeId) {

        //get IP from Node
        ip = getPublicIpByNodeId(docId, nodeId);

        WebResource agent = client.resource(getBaseURI(ip, 5000)); // static set to 5000
        ResponseTcConfig tcConfig = agent.path("api").path("tc-config/").accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON).get(ResponseTcConfig.class);

        //return Response.ok().entity("Agent Communication not executed and not tested yet.").type(MediaType.APPLICATION_JSON).build();
        return Response.ok().entity(tcConfig).type(MediaType.APPLICATION_JSON).build();
    }

    @POST
    @Path("tc-config/doc/{docId}/node/{nodeId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response setTcConfigByNodeId(@PathParam("docId") final Long docId, @PathParam("nodeId") final Long nodeId, ResponseTcConfig responseTcConfig) throws ExceptionInternalServerError {

        //get IP from Node
        ip = getPublicIpByNodeId(docId, nodeId);

        return setTcConfigByAgentIp(ip, responseTcConfig);
    }

    public Response setTcConfigByAgentIp(String ip, ResponseTcConfig responseTcConfig) throws ExceptionInternalServerError {

        WebResource agent = client.resource(getBaseURI(ip, 5000)); // static set to 5000
        ResponseTcConfig tcConfig = null;
        String postTcConfig = null;

        try {
            postTcConfig = objectMapper.writeValueAsString(responseTcConfig);
            tcConfig = agent.path("api").path("tc-config/").accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON).post(ResponseTcConfig.class, postTcConfig);
        } catch (Exception e) {
            throw new ExceptionInternalServerError(e.getMessage());
        }


        return Response.ok().entity(tcConfig).type(MediaType.APPLICATION_JSON).build();
    }

    @PUT
    @Path("tc-config/doc/{docId}/node/{nodeId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response editTcConfigByNodeId(@PathParam("docId") final Long docId, @PathParam("nodeId") final Long nodeId, ResponseTcConfig responseTcConfig){

        //get IP from Node
        ip = getPublicIpByNodeId(docId, nodeId);

        return editTcConfigByAgentIp(ip, responseTcConfig);
    }

    public Response editTcConfigByAgentIp(String ip, ResponseTcConfig responseTcConfig){

        WebResource agent = client.resource(getBaseURI(ip, 5000)); // static set to 5000
        ResponseTcConfig tcConfig = null;
        String postTcConfig = null;
        try {
            postTcConfig = objectMapper.writeValueAsString(responseTcConfig);
        } catch (JsonProcessingException e) {
            logger.error("Exception while editing TC config: ", e);
        }
        //TODO: Does'nt work (It should return a ResponseMessage object)
        tcConfig = agent.path("api").path("tc-config/").accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON).put(ResponseTcConfig.class, postTcConfig);
        logger.info("ResponseTcConfig object: \n" + postTcConfig);

        return Response.ok().entity(tcConfig).type(MediaType.APPLICATION_JSON).build();
    }
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("tc-config/doc/{docId}/node/{nodeId}")
    public Response deleteTcConfigByNodeId(@PathParam("docId") final Long docId, @PathParam("nodeId") final Long nodeId) {

        //get IP from Node
        ip = getPublicIpByNodeId(docId, nodeId);

        return deleteTcConfigByAgentIp(ip);
    }

    private Response deleteTcConfigByAgentIp(String ip) {
        WebResource agent = client.resource(getBaseURI(ip, 5000)); // static set to 5000
        ResponseMessage msg = agent.path("api").path("tc-config/").accept(MediaType.APPLICATION_JSON).delete(ResponseMessage.class);

        return Response.ok().entity(buildJsonMessage(msg.getMessage())).type(MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("firewall/doc/{docId}/node/{nodeId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFirewall(@PathParam("docId") final Long docId, @PathParam("nodeId") final Long nodeId){

        //get IP from Node
        ip = getPublicIpByNodeId(docId, nodeId);

        WebResource agent = client.resource(getBaseURI(ip, 5000)); // static set to 5000
        ResponseFirewall responseFirewall = agent.path("api").path("firewall/").accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON).get(ResponseFirewall.class);
        logger.info("Is Agent's firewall active?: " + responseFirewall.getActive());
        return Response.ok().entity(responseFirewall).type(MediaType.APPLICATION_JSON).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("firewall/doc/{docId}/node/{nodeId}")
    public Response setFirewall(@PathParam("docId") final Long docId, @PathParam("nodeId") final Long nodeId, ResponseFirewall responseFirewall){

        //get IP from Node
        ip = getPublicIpByNodeId(docId, nodeId);

        String postFirewall = null;
        try {
            postFirewall = objectMapper.writeValueAsString(responseFirewall);
        } catch (JsonProcessingException e) {
            logger.error("Exception while updating firewall: ", e);
        }
        //TODO: Does'nt work (also returns the status of active)
        WebResource agent = client.resource(getBaseURI(ip, 5000)); // static set to 5000
        ResponseMessage responseFirewallActive = agent.path("api").path("firewall/").accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON).post(ResponseMessage.class, postFirewall);

        return Response.ok().entity(buildJsonMessage(responseFirewallActive.getMessage())).type(MediaType.APPLICATION_JSON).build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("firewall/doc/{docId}/node/{nodeId}")
    public Response editFirewall(@PathParam("docId") final Long docId, @PathParam("nodeId") final Long nodeId, ResponseFirewall responseFirewall){

        //get IP from Node
        ip = getPublicIpByNodeId(docId, nodeId);

        String putFirewall = null;
        try {
            putFirewall = objectMapper.writeValueAsString(responseFirewall);
        } catch (JsonProcessingException e) {
            logger.error("Exception while editing firewall: ", e);
        }
        //TODO: Does'nt work (It should return a ResponseMessage object, that's why the response is "Agent status: null")
        WebResource agent = client.resource(getBaseURI(ip, 5000)); // static set to 5000
        ResponseFirewall responseFirewallActive = agent.path("api").path("firewall/").accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON).put(ResponseFirewall.class, putFirewall);
        logger.info("Agent status: " + responseFirewallActive.getActive());

        return Response.ok().entity(responseFirewallActive).type(MediaType.APPLICATION_JSON).build();
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("firewall/doc/{docId}/node/{nodeId}")
    public Response stopFirewall(@PathParam("docId") final Long docId, @PathParam("nodeId") final Long nodeId){

        //get IP from Node
        ip = getPublicIpByNodeId(docId, nodeId);

        WebResource agent = client.resource(getBaseURI(ip, 5000)); // static set to 5000
        ResponseMessage responseFirewallStopped = agent.path("api").path("firewall/").accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON).delete(ResponseMessage.class);

        return Response.ok().entity(responseFirewallStopped).type(MediaType.APPLICATION_JSON).build();
    }

//    public String getMgmtIpByNodeId(Long docId, Long nodeId){
//        Map<String, Object> params = MapUtil.map("docId", docId, "nodeId", nodeId);
//
//        //get Management IP instead of the one, which is manipulated
//        String ip = "";
//        try (Transaction tx = db.beginTx()) {
//            ResourceIterator<Relationship> relationships = db.execute("MATCH ()-[:LINK]->(n:NODE)<-[ipEdge:LINK]-(d:DOC)" +
//                                        "WHERE id(d)=$docId AND id(n)=$nodeId " +
//                                        "return ipEdge as e", params ).columnAs("e");
//            if (relationships.hasNext()){ //for the case of two ingoing LINKs in a Node there whould be while with an Array Response (which is in 2 or more different networks)
//                ip = relationships.next().getProperty("addr").toString();
//                System.out.println("Agent Management IP: " + ip);
//            }
//            tx.success();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return ip;
//    }

    public String getPublicIpByNodeId(Long docId, Long nodeId){
        Map<String, Object> params = MapUtil.map("docId", docId, "nodeId", nodeId);

        //get Public IP instead of the the one, that is manipulated
        String ip = "";
        try (Transaction tx = db.beginTx()) {
            ResourceIterator<Relationship> relationships = db.execute("MATCH ()-[:LINK]->(n:NODE)<-[ipEdge:LINK]-(d:DOC)" +
                    "WHERE id(d)=$docId AND id(n)=$nodeId " +
                    "return ipEdge as e", params ).columnAs("e");
            if (relationships.hasNext()){ //for the case of two ingoing LINKs in a Node there whould be while with an Array Response (which is in 2 or more different networks)
                ip = relationships.next().getProperty("public_ip").toString();
                logger.info("Agent Public IP: " + ip);
            }
            tx.success();
        } catch (Exception e) {
            logger.error("Exception while getting public IP: ", e);
        }
        return ip;
    }

    public String getIpByNodeId(Long docId, Long nodeId){
        Map<String, Object> params = MapUtil.map("docId", docId, "nodeId", nodeId);

        String ip = "";
        try (Transaction tx = db.beginTx()) {
            ResourceIterator<Relationship> relationships = db.execute("MATCH (d:DOC)-[:CONTAIN]->(n:NODE)<-[ipEdgeTo:LINK]-(net:NET)" +
                    "WHERE id(d)=$docId AND id(n)=$nodeId " +
                    "return ipEdgeTo as e", params ).columnAs("e");
            if (relationships.hasNext()){ //for the case of two ingoing LINKs in a Node there whould be while with an Array Response (which is in 2 or more different networks)
                ip = relationships.next().getProperty("addr").toString();
                logger.info("Agent IP: " + ip);
            }
            tx.success();
        } catch (Exception e) {
            logger.error("Exception while getting IP: ", e);
        }
        return ip;
    }

    @GET
    @Path("{host}/properties")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProperties(@PathParam("host") String host, @QueryParam("port") int port) {
//        WebTarget agent = client.target(getBaseURI(host, port == 0 ? 5000 : port)); // port defaults to 5000 if QueryParam is not given
//        ResponseMessage message = agent.path("api").path("properties").request().accept(MediaType.APPLICATION_JSON).get(ResponseMessage.class);

        WebResource agent = client.resource(getBaseURI(host, port == 0 ? 5000 : port)); // port defaults to 5000 if QueryParam is not given
        ResponseMessage message = agent.path("api").path("properties/").accept("application/json").type(MediaType.APPLICATION_JSON).get(ResponseMessage.class);

        JSONObject responseJson = new JSONObject();
        responseJson.put("host", host);
        responseJson.put("port", port);
        responseJson.put("message", message.getMessage());

        return Response.ok().entity(responseJson).type(MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("{host}/testproperty/testproperty")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTestPropertyTestProperty(@PathParam("host") String host, @QueryParam("port") int port) {
//        WebTarget agent = client.target(getBaseURI(host, port == 0 ? 5000 : port)); // port defaults to 5000 if QueryParam is not given
//        ResponseStatus status = agent.path("api").path("testproperty").path("testproperty").request().accept(MediaType.APPLICATION_JSON)
//                .get(ResponseStatus.class);
                                                // ^ first           // ^ second

        WebResource agent = client.resource(getBaseURI(host, port == 0 ? 5000 : port)); // port defaults to 5000 if QueryParam is not given
        ResponseStatus status = agent.path("api").path("testproperty").path("testproperty/").accept("application/json").type(MediaType.APPLICATION_JSON).get(ResponseStatus.class);

        JSONObject responseJson = new JSONObject();
        responseJson.put("host", host);
        responseJson.put("port", port);
        responseJson.put("status", status.getStatus());

        if(status.getStatus().contains("not"))
            return Response.status(201).entity(responseJson).type(MediaType.APPLICATION_JSON).build();
        return Response.ok().entity(responseJson).type(MediaType.APPLICATION_JSON).build();
    }

    // TODO @POST testproperty
    @POST
    @Path("{host}/testproperty/testproperty")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response setTestPropertyTestProperty(@PathParam("host") String host, @QueryParam("port") int port, ResponseTcConfig newTcConfig) {
//        WebTarget agent = client.target(getBaseURI(host, port == 0 ? 5000 : port)); // port defaults to 5000 if QueryParam is not given
//        Response response = agent.path("api").path("testproperty").path("testproperty").request().accept(MediaType.APPLICATION_JSON)
//                .post(Entity.entity(newTcConfig, MediaType.APPLICATION_JSON_TYPE));

        WebResource agent = client.resource(getBaseURI(host, port == 0 ? 5000 : port)); // port defaults to 5000 if QueryParam is not given
        ResponseMessage response = agent.path("api").path("testproperty").path("testproperty/").accept("application/json").type(MediaType.APPLICATION_JSON).post(ResponseMessage.class);

        JSONObject responseJson = new JSONObject();
        responseJson.put("host", host);
        responseJson.put("port", port);
        responseJson.put("status", response.getMessage());

        return Response.ok().entity(responseJson).type(MediaType.APPLICATION_JSON).build();
    }

    // TODO @PUT testproperty
    @PUT
    @Path("{host}/testproperty/testproperty")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response editTestPropertyTestProperty(@PathParam("host") String host, @QueryParam("port") int port, ResponseTcConfig newTcConfig) {
//        WebTarget agent = client.target(getBaseURI(host, port == 0 ? 5000 : port)); // port defaults to 5000 if QueryParam is not given
//        Response response = agent.path("api").path("testproperty").path("testproperty").request().accept(MediaType.APPLICATION_JSON)
//                .put(Entity.entity(newTcConfig, MediaType.APPLICATION_JSON_TYPE));

        WebResource agent = client.resource(getBaseURI(host, port == 0 ? 5000 : port)); // port defaults to 5000 if QueryParam is not given
        ResponseMessage response = agent.path("api").path("testproperty").path("testproperty/").accept("application/json").type(MediaType.APPLICATION_JSON).put(ResponseMessage.class);

        JSONObject responseJson = new JSONObject();
        responseJson.put("host", host);
        responseJson.put("port", port);
        responseJson.put("status", response.getMessage());

        return Response.ok().entity(responseJson).type(MediaType.APPLICATION_JSON).build();
    }

    // TODO @DELETE testproperty
    @DELETE
    @Path("{host}/testproperty/testproperty")
    @Produces(MediaType.APPLICATION_JSON)
    public Response editTestPropertyTestProperty(@PathParam("host") String host, @QueryParam("port") int port) {
//        WebTarget agent = client.target(getBaseURI(host, port == 0 ? 5000 : port)); // port defaults to 5000 if QueryParam is not given
//        Response response = agent.path("api").path("testproperty").path("testproperty").request().accept(MediaType.APPLICATION_JSON)
//                .delete();

        WebResource agent = client.resource(getBaseURI(host, port == 0 ? 5000 : port)); // port defaults to 5000 if QueryParam is not given
        ResponseMessage response = agent.path("api").path("testproperty").path("testproperty/").accept("application/json").type(MediaType.APPLICATION_JSON).delete(ResponseMessage.class);

        JSONObject responseJson = new JSONObject();
        responseJson.put("host", host);
        responseJson.put("port", port);
        responseJson.put("status", response.getMessage());

        return Response.ok().entity(responseJson).type(MediaType.APPLICATION_JSON).build();
    }





}
