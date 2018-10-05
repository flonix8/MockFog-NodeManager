package de.tub.mcc.fogmock.nodemanager.graphserv;


import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import de.tub.mcc.fogmock.nodemanager.graphserv.agent.ResponseTcConfig;
import de.tub.mcc.fogmock.nodemanager.graphserv.ansible.ResponseAnsible;
import de.tub.mcc.fogmock.nodemanager.graphserv.aws.AWSConfig;
import de.tub.mcc.fogmock.nodemanager.graphserv.aws.ResponseAWSConfig;
import de.tub.mcc.fogmock.nodemanager.graphserv.openstack.OpenstackConfig;
import de.tub.mcc.fogmock.nodemanager.graphserv.openstack.ResponseOpenstackConfig;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.helpers.collection.MapUtil;


import javax.print.attribute.standard.Media;
import javax.ws.rs.*;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Pattern;

import static org.neo4j.graphdb.Direction.INCOMING;
import static org.neo4j.graphdb.Direction.OUTGOING;
import org.neo4j.kernel.configuration.Config;
import org.neo4j.server.web.WebServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class serves all the REST functionality concerning graph manipulation.
 */
@Path("/")
public class ServiceUserInterface extends ServiceCommon {

	private static Server jetty;

    private static Logger logger = LoggerFactory.getLogger(InfrastructureController.class);

	ServicePropertyInjector propertyInjectorService;

    String ip = "localhost"; //current Agent IP for Testing

    static Map<String,File> resources = new ConcurrentHashMap<>();

    private File resourceDir;


    public ServiceUserInterface(@Context GraphDatabaseService graphDb) {
        super(graphDb);
    }


    /** This method returns the media type for a requested file type.
     *  If the file type is not found, the default media type is text/plain.
     *
     * @param file the file type to get the media type from
     * @return
     */
    public String mediaType(String file) {
        int dot = file.lastIndexOf(".");
        if (dot == -1) return MediaType.TEXT_PLAIN;
        String ext = file.substring(dot + 1).toLowerCase();
        switch (ext) {
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "json":
                return MediaType.APPLICATION_JSON;
            case "js":
                return "text/javascript";
            case "css":
                return "text/css";
            case "svg":
                //return MediaType.APPLICATION_SVG_XML;
                return "image/svg+xml";
            case "html":
                return MediaType.TEXT_HTML;
            case "txt":
                return MediaType.TEXT_PLAIN;
            case "jpg":
            case "jpeg":
                return "image/jpg";
            case "ttf":
                return "font/opentype";
            case "woff":
            case "woff2":
                //return "application/x-font-woff";
            default:
                return MediaType.TEXT_PLAIN;
        }
    }

    /** This method returns the file for a requested file path.
     *
     * @param filePath the file path where the requested file is located
     * @return
     * @throws IOException
     */
    @GET
    @Path("{file:(?i).+\\.(png|jpg|jpeg|svg|gif|html?|js|json|css|txt|grass|ttf|woff2|woff|eot)(\\?.*)?}")
    public Response file(@PathParam("file") String filePath) throws IOException {
        logger.info("file: "+filePath);
        InputStream fileStream = getClass().getResourceAsStream("/static/"+filePath);
        if (fileStream == null) return Response.status(Response.Status.NOT_FOUND).build();
        else return Response.ok(fileStream, mediaType(filePath)).build();
    }


    /** TODO
     *
     * @param modelDocTree
     * @param idx
     * @return
     * @throws ExceptionInvalidData
     */
    private Node createDocTreeHelper(ModelDocTree modelDocTree, Integer idx) throws ExceptionInvalidData {
        Map<String, Object> docMap = objectMapper.convertValue(modelDocTree.allDocs[idx], Map.class);
        Node docSrc = createDocHelper( docMap );
        for (Integer next : modelDocTree.tMap.get(idx)) {
            Node docTgt = createDocTreeHelper(modelDocTree, next);
            docSrc.createRelationshipTo(docTgt, REVISION);
        }
        return docSrc;
    }

    /** TODO
     *
     * @param docTree
     * @return
     */
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/doc/tree")
    @POST
    public Response createDocTree( ModelDocTree docTree) {
        return createDocTrees( new ModelDocTree[]{docTree} );
    }

    /** TODO
     *
     * @param docTrees
     * @return
     */
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/doc/trees")
    @POST
    public Response createDocTrees( ModelDocTree[] docTrees) {
        logger.info("Reveived object...");
        /*
         * insert whole doc tree
         */
        StringWriter sw = new StringWriter();
        try ( Transaction tx = db.beginTx() ) {
            JsonGenerator jg = objectMapper.getFactory().createGenerator( sw );
            jg.writeStartArray();
            for (ModelDocTree curTree : docTrees) {
                Node root = createDocTreeHelper(curTree, 0);
                writeDocTree(jg, root, true );
            }
            jg.writeEndArray();
            jg.flush();
            jg.close();

            tx.success();
        } catch (ExceptionInvalidData e) {
            return Response.status(400).entity( e.getMessage() ).type( MediaType.TEXT_PLAIN ).build();
        } catch (Exception e) {
            logger.error("Exception while creating doc trees", e);
            return Response.status(500).entity( e.getMessage() ).type( MediaType.TEXT_PLAIN ).build();
        }

        responseAnsible.setStatus(ResponseAnsible.Status.NOT_STARTED);
        return Response.ok().entity( sw.toString() ).type( MediaType.APPLICATION_JSON ).build();
    }

    /** TODO
     *
     * @param modelDoc
     * @return
     */
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/doc/plain")
    @POST
    public Response createDoc( ModelDoc modelDoc) {
        Map<String, Object> docMap = objectMapper.convertValue(modelDoc, Map.class);
        logger.info("Reveived object:\n" + docMap);
        /*
         * insert whole doc
         */
        StringWriter sw = new StringWriter();
        try ( Transaction tx = db.beginTx() ) {

            Node docNode = createDocHelper(docMap);

            JsonGenerator jg = objectMapper.getFactory().createGenerator( sw );
            writeDoc(jg, docNode, true);

            jg.flush();
            jg.close();

            tx.success();
        } catch (ExceptionInvalidData e) {
            return Response.status(400).entity( e.getMessage() ).type( MediaType.TEXT_PLAIN ).build();
        } catch (Exception e) {
            logger.error("Exception while creating doc", e);
            return Response.status(500).entity( e.getMessage() ).type( MediaType.TEXT_PLAIN ).build();
        }

        responseAnsible.setStatus(ResponseAnsible.Status.NOT_STARTED);
        return Response.ok().entity( sw.toString() ).type( MediaType.APPLICATION_JSON ).build();
    }

    /** TODO
     *
     * @param modelDoc
     * @return
     */
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/doc")
    @POST
    public Response createDocLegacy( ModelDoc modelDoc) {
        return createDoc(modelDoc);
    }

    /** TODO
     *
     * @param docMap
     * @return
     * @throws ExceptionInvalidData
     */
    private Node createDocHelper( Map<String, Object> docMap ) throws ExceptionInvalidData {
        ResourceIterator<Node> r;
        r = db.execute("CREATE (vdoc: DOC)-[:CONTAIN]->(vdoc) SET vdoc=$propsAll " +
            " FOREACH (kn in keys($allNodes)| " +
                " CREATE (vnode:NODE)<-[:CONTAIN]-(vdoc) SET vnode+=$allNodes[kn].propsAll " +
                " FOREACH (ke in keys($allNodes[kn].edgesBack)| " +
                    " MERGE (vnet:NET{tmpId:ke}) " +
                    " MERGE (vnet)-[r:LINK]->(vnode) SET r=$allNodes[kn].edgesBack[ke].propsAll " +
                " ) " +
            " ) " +
            " FOREACH (kn in keys($allNets)| " +
                " MERGE (vnet:NET{tmpId:kn}) SET vnet+=$allNets[kn].propsAll " +
                " MERGE (vdoc)-[:CONTAIN]->(vnet) " +
                " FOREACH (ke in keys($allNets[kn].edgesBack) | " +
                    " MERGE (vsnet:NET{tmpId:ke}) " +
                    " MERGE (venet:NET{tmpId:kn}) " +
                    " MERGE (vsnet)-[r:LINK]->(venet) SET r=$allNets[kn].edgesBack[ke].propsAll " +
                " ) " +
            " ) RETURN distinct vdoc " +
            " ", docMap ).columnAs("vdoc");

        Node docNode = r.next();
        Map<String, Object> docIdMap = MapUtil.map("docId", docNode.getId());

        //remove tmpId
        db.execute("MATCH (d:DOC)-[:CONTAIN]->(n) WHERE id(d)=$docId AND exists(n.tmpId) REMOVE n.tmpId ", docIdMap );

        // insert addr on ingoing edge to each Net or Node
        r = db.execute("MATCH (doc:DOC)-[:CONTAIN]->(nTo)<-[:LINK]-(n:NET) WHERE id(doc)=$docId AND (nTo:NET OR nTo:NODE) RETURN n", docIdMap ).columnAs("n");
        r.forEachRemaining( n-> setNewNetIpOnIncomingLinks(n));

        r = db.execute("MATCH (doc:DOC)-[:CONTAIN]->(nTo:NODE)<-[:LINK]-(n:NET) WHERE id(doc)=$docId RETURN nTo", docIdMap ).columnAs("nTo");
        while (r.hasNext()){
            Node n = r.next();
            //setMaxRatesByFlavor(n);
        }

        checkIntegrityCycle(docNode.getId());
        checkIntegrityNodeHasOutgoingLinks(docNode.getId());
        checkAnsibleIntegrityNodeLinkedToMultipleNets(docNode.getId());

        return docNode;
    }

    /**  This method sets or updates the IP address as a property of a net aswell on the incoming edges to that net as an edge property.
     *   The node addresses are ignored because they are only set as an edge property.
     *
     * @param n the NET node that's IP address was updated
     */
    public void setNewNetIpOnIncomingLinks(Node n){
        if (n.hasLabel(NET)){
            Iterable<Relationship> ingoing = n.getRelationships(Direction.INCOMING, LINK);
            for (Relationship r : ingoing){
                r.setProperty("addr", n.getProperty("addr"));
            }
        }
    }

    /** This method sets in_rate and out_rate on an incoming edge to a node specified by the node's flavor.
     *
     * @param endNode the node with a special flavor for which the rates have to be set respectively
     * @throws ExceptionInvalidData
     */
    public void setMaxRatesByFlavor(Node endNode, boolean openStack) throws ExceptionInvalidData {
        if (endNode.hasLabel(NODE)){
            Iterable<Relationship> ingoing = endNode.getRelationships(Direction.INCOMING, LINK);
            String flavor = (String)endNode.getProperty("flavor");

            for (Relationship r : ingoing){
                if (openStack) {
                    r.setProperty("in_rate", getRateByFlavorFile(flavor, Settings.PATH_TO_OS_FLAVORS));
                    r.setProperty("out_rate", getRateByFlavorFile(flavor, Settings.PATH_TO_OS_FLAVORS));
                } else {
                    r.setProperty("in_rate", getRateByFlavorFile(flavor, Settings.PATH_TO_AWS_FLAVORS));
                    r.setProperty("out_rate", getRateByFlavorFile(flavor, Settings.PATH_TO_AWS_FLAVORS));
                }
            }
        }
    }

    /** This method returns the respective in_rate and out_rate for a device type (e.g. raspberry pi 2 model b has 100000mbps).
     *
     * @param device the device type that specifies the rates of an edge device
     * @return the rate of the given device type
     * @throws ExceptionInvalidData
     */
    public Long getRateByFlavorFile(String device, String flavorFile) throws ExceptionInvalidData {
        Long rate = 1000000L; //all other devices (that are included) usually have 1000000 mbps
        int end = device.indexOf("(");
        if (end != -1) {
            device = device.substring(0, end).trim();
        }
        JSONParser jsonParser = new JSONParser();
        try {
            Object object = jsonParser.parse(new FileReader(flavorFile));
            JSONObject jsonObject = (JSONObject)object;
            if (jsonObject.containsKey(device)){
                Object objectProps = jsonObject.get(device);
                JSONObject jsonProps = (JSONObject)objectProps;
                rate = Long.parseLong((String)jsonProps.get("rate"));
            } else if (device.equals("")){
                return rate;
            }
            else {
                throw new ExceptionInvalidData("Invalid Flavor chosen (" + device + "). Please select one of " + jsonObject.keySet());
            }
        } catch (IOException e) {
            logger.error("IOException while getting rate by flavor", e);
        } catch (ParseException e) {
            logger.error("ParseException while getting rate by flavor", e);
        }
        return rate; //default
    }

    @Path("/doc/{docId}")
    @DELETE
    public Response deleteDocLegacy( @PathParam("docId") final Long docId ) {
        return deleteDoc( docId, "erase" );
    }


    /** This method deletes a document by it's id in two different ways:
     *      erase: delete current doc with docId
     *      prune: delete every doc revision behind current doc with docId
     *
     * @param docId the ID of the document to be deleted
     * @param action the action to be executed (either erase or prune)
     * @return
     */
    @Path("/doc/{docId}/{action:erase|prune|eraseprune}")
    @DELETE
    public Response deleteDoc( @PathParam("docId") final Long docId, @PathParam("action") final String action ) {
        if ( docId.equals(docIdStatic) ) {
            return Response.status(428).entity("illegal doc deletion after bootstrap").type(MediaType.TEXT_PLAIN).build();
        }
        final List<Long> delDocIds = new LinkedList<Long>();
        StringWriter sw = new StringWriter();
        try ( Transaction tx = db.beginTx() ) {
            if (!db.getNodeById(docId).hasLabel(DOC)) throw new org.neo4j.graphdb.NotFoundException("doc not found, id: "+docId);
            if ( action.contains("prune") ) {
                ResourceIterator<Long> resIds = db.execute( "MATCH (root:DOC)-[:REVISION*]->(d:DOC) WHERE ID(root)="+docId+
                        " RETURN id(d) as docId").columnAs("docId");
                while (resIds.hasNext()) {
                    Long curDocId = resIds.next();
                    if ( curDocId.equals(docIdStatic) ) {
                        return Response.status(428).entity("illegal doc deletion after bootstrap").type(MediaType.TEXT_PLAIN).build();
                    }
                    delDocIds.add( curDocId );
                }
            }
            if ( action.contains("erase")  ) {
                delDocIds.add(docId);
                if ( !db.getNodeById(docId).hasRelationship(REVISION, INCOMING) && db.getNodeById(docId).hasRelationship(REVISION, OUTGOING) && !action.contains("prune") ) {
                    return Response.status(400).entity( "can't erase root doc without pruning:  " +
                            "call /doc/"+docId+"/eraseprune to delete all revisions" ).type( MediaType.TEXT_PLAIN ).build();
                }
            }
            docDeleteHelper(  MapUtil.map("docIds", delDocIds.toArray(new Long[]{}))  );
            tx.success();
        } catch (org.neo4j.graphdb.NotFoundException e) {
            return Response.status(400).entity( e.getMessage() ).type( MediaType.TEXT_PLAIN ).build();
        } catch (Exception e) {
            logger.error("Exception while deleting doc:", e);
            return Response.status(500).build();
        }

        return Response.ok().build();
    }

    /** This method deletes multiple documents at once.
     *
     * @param docIds the list of the documents to be deleted
     * @return
     */
    @Path("/deleteDocs")
    @DELETE
    public Response deleteDocs( Long[] docIds ) {
        logger.info("Received docIds to delete: "+Arrays.toString(docIds));

        for (Long curDocId : docIds) {
            if ( curDocId.equals(docIdStatic) ) {
                return Response.status(428).entity("illegal doc deletion after bootstrap").type(MediaType.TEXT_PLAIN).build();
            }
        }

        StringWriter sw = new StringWriter();
        try ( Transaction tx = db.beginTx() ) {
            final List<Node> roots = new LinkedList<Node>();
            for ( long docId : docIds ) {
                if (!db.getNodeById(docId).hasLabel(DOC)) throw new org.neo4j.graphdb.NotFoundException("doc not found, id: "+docId);
                Node doc = db.getNodeById(docId);
                if ( !doc.hasRelationship(REVISION,INCOMING) ) { //&& doc.getDegree(REVISION,OUTGOING) > 1 ) {
                    roots.add(doc);
                    continue;
                }
                docDeleteHelper(  MapUtil.map("docIds", new Long[]{doc.getId()})  );
            }
            for (Node root : roots) {
                if ( root.getDegree(REVISION,OUTGOING) > 1 ) {
                    throw new ExceptionInvalidData("deletion not executed due to illegal tree disintegration ");
                } else {
                    docDeleteHelper(  MapUtil.map("docIds", new Long[]{root.getId()})  );
                }
            }
            tx.success();
        } catch (ExceptionInvalidData e) {
            return Response.status(400).entity( e.getMessage() ).type( MediaType.TEXT_PLAIN ).build();
        } catch (org.neo4j.graphdb.NotFoundException e) {
            return Response.status(400).entity( e.getMessage() ).type( MediaType.TEXT_PLAIN ).build();
        } catch (Exception e) {
            logger.error("Exception while deleting docs:", e);
            return Response.status(500).build();
        }

        return Response.ok().build();
    }

    /** TODO
     *
     * @param params
     * @throws ExceptionInvalidData
     */
    private void docDeleteHelper(Map<String, Object> params) throws ExceptionInvalidData {
        db.execute( "UNWIND $docIds as did MATCH (d:DOC) WHERE ID(d)=did " +
                " MATCH (prev:DOC)-[:REVISION]->(d)-[:REVISION]->(post:DOC) CREATE (prev)-[:REVISION]->(post)", params);
        db.execute("UNWIND $docIds as did MATCH (d:DOC)-[:CONTAIN]->(n) WHERE ID(d)=did DETACH DELETE n ", params);
        db.execute("UNWIND $docIds as did MATCH (d:DOC) WHERE ID(d)=did DETACH DELETE d ", params);
    }

    /** This method allows to edit document specific properties. (e.g. docName)
     *
     * @param docId the ID of the document to be edited
     * @param modelDoc the model object of the document containing the new properties
     * @return
     */
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/doc/{docId}")
    @PUT
    public Response editDoc( @PathParam("docId") final Long docId, ModelDoc modelDoc ) {
        if ( docId.equals(docIdStatic) ) {
            //return Response.status(428).entity("illegal modification of instantiated document").type( MediaType.TEXT_PLAIN).build();
            modelDoc.props.entrySet().removeIf(e-> !e.getKey().equals("docName") );
        }

        Map<String, Object> docMap = objectMapper.convertValue(modelDoc, Map.class);

        StringWriter sw = new StringWriter();
        try ( Transaction tx = db.beginTx() ) {
            db.execute("MATCH (vdoc: DOC) WHERE id(vdoc)="+docId+" SET vdoc+=$props ", docMap );

            JsonGenerator jg = objectMapper.getFactory().createGenerator( sw );
            writeDoc(jg, db.getNodeById(docId), true);
            jg.flush();
            jg.close();

            tx.success();
        } catch (NotFoundException e) {
            return Response.status(400).entity( e.getMessage() ).type( MediaType.TEXT_PLAIN ).build();
        } catch (Exception e) {
            logger.error("Exception while editing doc:", e);
            return Response.status(500).entity( e.getMessage() ).type( MediaType.TEXT_PLAIN ).build();
        }

        return Response.ok().entity( sw.toString() ).type( MediaType.APPLICATION_JSON ).build();
    }

    /** This method creates a new node in the current document that is of the type NET.
     *  For the case of the new node to be created having a real IP (non '0.0.0.0/0') address and therefore is not a virtual net,
     *  it is checked if the bootstrapping is already done.
     *  If yes, no new net can be created besides virtual nets and an Exception is thrown.
     *
     * @param docId the id of the document in which a net should be created
     * @param net the model object of the net to be created containing properties (e.g. addr)
     * @return
     */
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/doc/{docId}/net")
    @POST
    public Response createNet(  @PathParam("docId") final Long docId, ModelNet net ) {
        if ( docId.equals(docIdStatic) && !net.addr.equals("0.0.0.0/0") ) {
            return Response.status(428).entity("illegal net creation after bootstrap").type(MediaType.TEXT_PLAIN).build();
        }
        return createVertex ( docId, net, "NET");
	}

    /** This method creates a new node in the current document that is of the type NODE.
     *  For the case that the document is already bootstrapped, an exception is thrown if another node is tried to be added.
     *
     * @param docId the id of the document in which a node should be created
     * @param node the model object of the node to be created containing properties (e.g. flavor)
     * @return
     */
	@Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/doc/{docId}/node")
    @POST
    public Response createNode( @PathParam("docId") final Long docId, ModelNode node ) { //SkeletonVertex[]
        if ( docId.equals(docIdStatic) ) {
            return Response.status(428).entity("illegal node addition after bootstrap").type(MediaType.TEXT_PLAIN).build();
        }
        return createVertex ( docId, node, "NODE");
    }

    /** This method creates and saves a new node (either NET or NODE) with it's properties to the db.
     *
     * @param docId the id of the document in which a node should be created
     * @param modelV the model object of the node
     * @param label the label of the db object (either NET or NODE)
     * @return
     */
    private Response createVertex(Long docId, ModelVertex modelV, String label) {
        Map<String, Object> vMap = objectMapper.convertValue(modelV, Map.class);
        vMap.put("docId", docId);
        logger.info("Reveived object:\n" + vMap);

        /*
         * execute vertex creation (inkl. edgesBack if provided)
         */
        StringWriter sw = new StringWriter();
        try ( Transaction tx = db.beginTx() ) {
            ResourceIterator<Node> ri = db.execute( "MATCH (d:DOC) WHERE ID(d)=$docId " +
                    " CREATE (d)-[r:CONTAIN]->(n:"+label+") SET n=$propsAll RETURN n " +
                    " ", vMap ).columnAs("n");
            if ( !ri.hasNext() ) {
                return Response.status(400).entity( "missing document reference" ).type( MediaType.TEXT_PLAIN ).build();
            }
            Node newNet = ri.next();
            vMap.put("netId", newNet.getId());
            Result r = db.execute( "MATCH (n) WHERE id(n)=$netId " +
                    " UNWIND keys($edgesBack) as ke MATCH (ne:"+label+") WHERE id(ne)=toInteger(ke) AND id(ne)<>id(n) " +
                    " CREATE (n)<-[r:LINK]-(ne) SET r=$edgesBack[ke].propsAll" +
                    " ", vMap );

            if ( r.getQueryStatistics().getRelationshipsCreated() != modelV.edgesBack.size() ) {
                return Response.status(400).entity( "illegal edge reference" ).type( MediaType.TEXT_PLAIN ).build();
            }

            JsonGenerator jg = objectMapper.getFactory().createGenerator( sw );
            jg.writeStartObject();
            writeVertex(jg, newNet);
            if (modelV.props.containsKey("flavor")) {
                jg.writeStringField("icon", getIconFromDeviceFile((String) modelV.props.get("flavor"), false));
            }
            jg.writeEndObject();
            jg.flush();
            jg.close();

            tx.success();
        } catch (Exception e) {
            logger.error("Exception while creating vertex:", e);
            return Response.status(500).build();
        }
        return Response.ok().entity( sw.toString() ).type( MediaType.APPLICATION_JSON ).build();
    }

    /** This method creates and saves an edge (with a LINK relationship label) in between two nodes with label NODE or NET to the db.
     *  Because the edge direction is always going from a NET to a NODE/NET in the db it is checked and corrected
     *  for not having the effort to pay attention to the order of the node IDs in the URL.
     *  If the document is already bootstrapped, the network is propagated afterwards and
     *  the TC properties are communicated to the respective Agents.
     *
     * @param docId the id of the document in which an edge should be created
     * @param nodeFromId the id of one node the edge should be connected to
     * @param nodeToId the id of the other node the edge should be connected to
     * @return
     */
    @Path("/doc/{docId}/edge/{nodeFromId}/{nodeToId}")
    @POST
    public Response createEdge( @PathParam("docId") final Long docId, @PathParam("nodeFromId") final Long nodeFromId, @PathParam("nodeToId") final Long nodeToId) {
        if (nodeFromId == nodeToId){
            return Response.status(400).entity( "You can not insert an edge from a node to itself. " ).type( MediaType.TEXT_PLAIN ).build();
        }

        long nodeFromIdChecked = nodeFromId;
        long nodeToIdChecked = nodeToId;

        if (!isFromIdNET(docId, nodeFromId, nodeToId)){
            nodeFromIdChecked = nodeToId;
            nodeToIdChecked = nodeFromId;
        }

        Map<String, Object> vMap = MapUtil.map("docId", docId, "nodeFromId", nodeFromIdChecked, "nodeToId", nodeToIdChecked, "eMap", objectMapper.convertValue(ModelEdge.STANDARD_EDGE, Map.class) );

        StringWriter sw = new StringWriter();
        try ( Transaction tx = db.beginTx() ) {
            //Result result = db.execute( "MATCH (n1:NET)<-[:CONTAIN]-(d:DOC)-[:CONTAIN]->(n2:NET) WHERE ID(d)=$docId AND ID(n1)=$fromId AND ID(n2)=$toId MERGE (n1)-[r:NET]->(n2)", eMap )
            ResourceIterator<Node> result = db.execute( "MATCH (nFrom:NET)<-[:CONTAIN]-(d:DOC)-[:CONTAIN]->(nTo) " +
                    " WHERE ID(d)=$docId AND ID(nFrom)=$nodeFromId AND ID(nTo)=$nodeToId AND (nTo:NODE OR nTo:NET) " +
                    " MERGE (nFrom)-[e:LINK]->(nTo) SET e=$eMap.propsAll " +
                    " RETURN nTo" +
                    " ", vMap ).columnAs("nTo");

            checkIntegrityCycle(docId);
            checkIntegrityNodeHasOutgoingLinks(docId);
            checkAnsibleIntegrityNodeLinkedToMultipleNets(docId);

            if (!result.hasNext()) {
                return Response.status(404).entity( "Can't find net (HAS TO BE A NET!) with id " + nodeFromId +
                        " or net/node "+ nodeToId +" in doc with id " + docId).type( MediaType.TEXT_PLAIN ).build();
            }

            Node nodeEnd = result.next();
            setNewNetIpOnIncomingLinks(nodeEnd);
            //Todo: if openStack => call with true
            // if AWS => call with false
            setMaxRatesByFlavor(nodeEnd, false);
            JsonGenerator jg = objectMapper.getFactory().createGenerator( sw );
            jg.writeStartObject();
            writeVertex(jg, nodeEnd);
            jg.writeEndObject();
            if ( docId.equals(docIdStatic) ) {
                if (db.getNodeById(nodeToId).hasLabel(NODE)) {
                    return Response.status(428).entity("illegal node edge creation after bootstrap").type(MediaType.TEXT_PLAIN).build();
                }

                /*
                 *  Post-BOOTSTRAP
                 */
                startPropagation(docId, "WADJ");
                getAdjListsAndSendToNA(null, docId, "WADJ", "put");
                //getAdjLists(jg, docId, "WADJ");
            }

            jg.flush();
            jg.close();


            tx.success();
        } catch (ExceptionInvalidData e) {
            return Response.status(400).entity( e.getMessage() ).type( MediaType.TEXT_PLAIN ).build();
        } catch (Exception e) {
            logger.error("Exception while creating edge:", e);
            return Response.status(500).build();
        }
        return Response.ok().entity( sw.toString() ).type( MediaType.APPLICATION_JSON ).build();
    }

    /** This methods returns the node id order how to be represented in the db.
     *  It checks if the source node is a NET and that, if it is to be connected to a NODE, it's ID is set the destination ID.
     *
     * @param docId the id of the document in which an edge should be created
     * @param nodeFromId the id of one node the edge should be connected to
     * @param nodeToId the id of the other node the edge should be connected to
     * @return
     */
    public boolean isFromIdNET(Long docId, Long nodeFromId, Long nodeToId){
        boolean isRight = false;
        Map<String, Object> vMap = MapUtil.map("docId", docId, "nodeFromId", nodeFromId, "nodeToId", nodeToId);
        try ( Transaction tx = db.beginTx() ) {
            ResourceIterator<Node> labelResult = db.execute("MATCH (nFrom)<-[:CONTAIN]-(d:DOC)-[:CONTAIN]->(nTo) " +
                    " WHERE ID(d)=$docId AND ID(nFrom)=$nodeFromId AND ID(nTo)=$nodeToId AND (nTo:NODE OR nTo:NET) AND (nFrom:NODE OR nFrom:NET)" +
                    " RETURN nFrom" +
                    " ", vMap).columnAs("nFrom");

            if (labelResult.hasNext()) {
                Node node = labelResult.next();
                if (node.hasLabel(NET)) {
                    isRight = true;
                }
            }
            tx.success();
        } catch (Exception e) {
            logger.error("Exception while checking id net:", e);
        }
        return isRight;
    }

    /** This method returns an edge containing it's properties by the source and destination node IDs.
     *
     * @param docId the id of the document in which the edge is
     * @param nodeFromId the id of one node the edge is connected to
     * @param nodeToId the id of the other node the edge is connected to
     * @return
     */
    @Path("/doc/{docId}/edge/{nodeFromId}/{nodeToId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getEdgeById( @PathParam("docId") final Long docId, @PathParam("nodeFromId") final Long nodeFromId, @PathParam("nodeToId") final Long nodeToId) {
        Map<String, Object> vMap = MapUtil.map("docId", docId, "nodeFromId", nodeFromId, "nodeToId", nodeToId );

        StringWriter sw = new StringWriter();
        try ( Transaction tx = db.beginTx() ) {
            //Result result = db.execute( "MATCH (n1:NET)<-[:CONTAIN]-(d:DOC)-[:CONTAIN]->(n2:NET) WHERE ID(d)=$docId AND ID(n1)=$fromId AND ID(n2)=$toId MERGE (n1)-[r:NET]->(n2)", eMap )
            ResourceIterator<Relationship> result = db.execute( "MATCH (nTo)<-[e:LINK]-(nFrom)<-[:CONTAIN]-(d:DOC) " +
                    " WHERE ID(d)=$docId AND ID(nFrom)= $nodeFromId AND ID(nTo)= $nodeToId" +
                    " RETURN e" +
                    " ", vMap ).columnAs("e");

            if (result.hasNext()){
                Relationship edge = result.next();

                JsonGenerator jg = objectMapper.getFactory().createGenerator( sw );
                jg.writeStartObject();
                writeNeo4jProps(jg, edge.getAllProperties());
                jg.writeEndObject();
                jg.flush();
                jg.close();

            } else {
                return Response.status(404).entity( "Can't find node/net with id " + nodeFromId + " or "+ nodeToId +" in doc with id " + docId ).type( MediaType.TEXT_PLAIN ).build();
            }

            tx.success();
        } catch (Exception e) {
            logger.error("Exception while getting edge by id:", e);
            return Response.status(500).build();
        }
        return Response.ok().entity( sw.toString() ).type( MediaType.APPLICATION_JSON ).build();
    }

    /** This method allows to edit edge specific properties, especially the rules containing the TC properties. (e.g. delay, dispersion).
     *  If the document is already bootstrapped, the network is propagated afterwards and
     *  the TC properties are communicated to the respective Agents.
     *
     * @param docId the id of the document in which the edge is
     * @param fromId the id of one node the edge is connected to
     * @param toId the id of the other node the edge is connected to
     * @param modelE the model object of the edge containing the properties (TC configurations inside the rules)
     * @return
     */
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @PUT
    @Path("/doc/{docId}/edge/{fromId}/{toId}")
    public Response editEdge(@PathParam("docId") final Long docId, @PathParam("fromId") final Long fromId, @PathParam("toId") final Long toId, ModelEdge modelE) {
        Map<String, Object> eMap = objectMapper.convertValue(modelE, Map.class);
        Map<String, Object> props = (Map<String, Object>) eMap.get("props");
        logger.info("Reveived edge:\n" + eMap);


        StringWriter sw = new StringWriter();
        try ( Transaction tx = db.beginTx() ) {
            JsonGenerator jg = objectMapper.getFactory().createGenerator( sw );
            Relationship r = getRelationship(fromId, toId, LINK);
            String addrOld = (String) ( r.getProperty("addr") );
            String addrNew = (String)(props.get("addr") );
            if ( docId.equals(docIdStatic) ) props.remove("addr");

            //insert newly set properties
            final Map<String, Object> params = MapUtil.map("docId", docId,"nodeFromId", fromId, "nodeToId", toId, "eMap", eMap);
            ResourceIterator<Node> result = db.execute( "MATCH (d:DOC)-[:CONTAIN]->(nStart)-[e:LINK]->(nEnd) " +
                    " WHERE ID(d)=$docId AND ID(nEnd)= $nodeToId AND ID(nStart)= $nodeFromId" +
                    " SET e += $eMap.props RETURN nEnd", params ).columnAs("nEnd");
            if (!result.hasNext()) {
                return Response.status(404).entity("Can't find node/net with id " + fromId + " or " +
                        toId + " in doc with id " + docId).type(MediaType.TEXT_PLAIN).build();
            }
            Node nodeEnd = result.next();

            setNewNetIpOnIncomingLinks(nodeEnd);
            jg.writeStartObject();
            writeVertex(jg, nodeEnd);
            jg.writeEndObject();

            if ( docId.equals(docIdStatic) ) {
                /*
                 *  Post-BOOTSTRAP
                 */
                if (addrNew != null && addrOld != null && !addrNew.contains(addrOld)) {
                    return Response.status(428).entity("illegal post-bootstrap ip change").type(MediaType.TEXT_PLAIN).build();
                }

                startPropagation(docId, "WADJ");
                getAdjListsAndSendToNA(null, docId, "WADJ", "put");
                //getAdjLists(jg, docId, "WADJ");

            }
            jg.flush();
            jg.close();

            tx.success();
            logger.info(sw.toString());
            return Response.ok().entity( sw.toString() ).type( MediaType.APPLICATION_JSON ).build();
        } catch (ExceptionInvalidData e) {
            return Response.status(400).entity(e.getMessage()).type(MediaType.TEXT_PLAIN).build();
        } catch (Exception e) {
            logger.error("Exception while editing edge:", e);
            return Response.status(500).entity(e.getMessage()).type(MediaType.TEXT_PLAIN).build();
        }

    }

    /** This method deleted an edge with all it's properties and returns the node (NET or NODE) where the deleted edge was ingoing.
     *  If the document is already bootstrapped, the network is propagated afterwards and
     *  the TC properties are communicated to the respective Agents.
     *
     * @param docId the id of the document in which the edge is to be deleted
     * @param nodeFromId the id of one node the edge is connected to
     * @param nodeToId the id of the other node the edge is connected to
     * @return
     */
    @Produces(MediaType.APPLICATION_JSON)
    @DELETE
    @Path("/doc/{docId}/edge/{nodeFromId}/{nodeToId}")
    public Response deleteEdge(@PathParam("docId") final Long docId, @PathParam("nodeFromId") final Long nodeFromId, @PathParam("nodeToId") final Long nodeToId){
        final Map<String, Object> params = MapUtil.map("docId", docId,"nodeFromId", nodeFromId, "nodeToId", nodeToId);
        /*
         * execute deletion
         */
        StringWriter sw = new StringWriter();
        try ( Transaction tx = db.beginTx() ) {
            if ( docId.equals(docIdStatic) ) {
                if (db.getNodeById(nodeToId).hasLabel(NODE)) {
                    return Response.status(428).entity("illegal node edge deletion after bootstrap").type(MediaType.TEXT_PLAIN).build();
                }
            }
            //Result result = db.execute( "MATCH (n1:NET)<-[:CONTAIN]-(d:DOC)-[:CONTAIN]->(n2:NET) WHERE ID(d)=$docId AND ID(n1)=$fromId AND ID(n2)=$toId MERGE (n1)-[r:NET]->(n2)", eMap )
            ResourceIterator<Node> result = db.execute( "MATCH (d:DOC)-[:CONTAIN]->(nStart)-[e:LINK]->(nEnd) " +
                    " WHERE ID(d)=$docId AND ID(nEnd)= $nodeToId AND ID(nStart)= $nodeFromId" +
                    " DELETE e RETURN nEnd" +
                    " ", params ).columnAs("nEnd");
            if (result.hasNext()){
                Node nodeEnd = result.next();

                JsonGenerator jg = objectMapper.getFactory().createGenerator( sw );
                jg.writeStartObject();
                writeVertex(jg, nodeEnd);
                jg.writeEndObject();

                if ( docId.equals(docIdStatic) ) {
                    /*
                     *  Post-BOOTSTRAP
                     */
                    startPropagation(docId, "WADJ");
                    getAdjListsAndSendToNA(null, docId, "WADJ", "put");
                    //getAdjLists(jg, docId, "WADJ");
                }

                jg.flush();
                jg.close();
            }
            tx.success();
        } catch (NotFoundException e) {
            return Response.status(400).entity(e.getMessage()).type(MediaType.TEXT_PLAIN).build();
        } catch (ExceptionInvalidData e) {
            return Response.status(400).entity(e.getMessage()).type(MediaType.TEXT_PLAIN).build();
        } catch (Exception e) {
            logger.error("Exception while deleting edge:", e);
            return Response.status(500).entity(e.getMessage()).type(MediaType.TEXT_PLAIN).build();
        }

        return Response.ok().entity( sw.toString() ).type( MediaType.APPLICATION_JSON ).build();
    }

    /** This method deletes a vertex (either NET or NODE) and all it's conntected edges.
     *  If the document is already bootstrapped, a NODE can not be deleted and a NET only in the case of being a virtual net,
     *  so an Exception is thrown.
     *
     * @param docId the id of the document in which the node is to be deleted
     * @param nId the id of the node (either NET of NODE) to be deleted
     * @return
     */
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/doc/{docId}/vertex/{id}") //{action:vertex|node|net}
    @DELETE
    public Response deleteVertex(  @PathParam("docId") final Long docId, @PathParam("id") final Long nId) {
        Map<String, Object> params = MapUtil.map("docId", docId,"nId", nId);
        StringWriter sw = new StringWriter();
        try ( Transaction tx = db.beginTx() ) {
            if ( docId.equals(docIdStatic) ) {
                Node n = db.getNodeById(nId);
                if (  n.hasLabel(NODE) || ( n.hasLabel(NET) && !"0.0.0.0/0".equals(n.getProperty("addr")) )  ) {
                    return Response.status(428).entity("illegal deletion after bootstrap").type(MediaType.TEXT_PLAIN).build();
                }
            }
            Result result = db.execute( "MATCH (d:DOC)-[r1:CONTAIN]->(n)" +
                    "WHERE ID(d)=$docId AND ID(n)=$nId OPTIONAL MATCH (n)-[r]-()" +
                    " DELETE r1, r, n " +
                    " ", params );
            if ( result.getQueryStatistics().getNodesDeleted() != 1) {
                logger.info("Nodes deleted: " + result.getQueryStatistics().getNodesDeleted());
                return Response.status(400).entity( "illegal document reference" ).type( MediaType.TEXT_PLAIN ).build();
            }
            JsonGenerator jg = objectMapper.getFactory().createGenerator( sw );
            jg.writeObject(nId);
            jg.flush();
            jg.close();

            tx.success();
        } catch (NotFoundException e) {
            return Response.status(400).entity(e.getMessage()).type(MediaType.TEXT_PLAIN).build();
        } catch (Exception e) {
            logger.error("Exception while deleting vertex:", e);
            return Response.status(500).entity(e.getMessage()).type(MediaType.TEXT_PLAIN).build();
        }
//        return Response.ok().entity(sw.toString()).type(MediaType.APPLICATION_JSON).build();
        return Response.ok().build();
    }

    /** This method deletes multiple vertices (NET or NODE).
     *
     * @param docId the id of the document in which the vertices are to be deleted
     * @param message the list of all vertice IDs to be deleted
     * @return
     */
	@Path("/doc/{docId}/vertex")
	@DELETE
	public Response deleteVertices( @PathParam("docId") final Long docId, final Long[] message ) {
		final Map<String, Object> params = MapUtil.map("docId", docId, "delIds", message);
        logger.info(Arrays.toString(message));
        /*
         * execute deletion
         */
        StringWriter sw = new StringWriter();
        try ( Transaction tx = db.beginTx() ) {
            if ( docId.equals(docIdStatic) ) {
                for (Long nId : message) {
                    Node n = db.getNodeById(nId);
                    if (  n.hasLabel(NODE) || ( n.hasLabel(NET) && !"0.0.0.0/0".equals(n.getProperty("addr")) )  ) {
                        return Response.status(428).entity("illegal deletion after bootstrap").type(MediaType.TEXT_PLAIN).build();
                    }
                }
            }

            Result result = db.execute( "UNWIND $delIds AS delId MATCH (d:DOC)-[d2f:CONTAIN]->(n) " +
                    " WHERE ID(d)=$docId AND ID(n)=delId OPTIONAL MATCH (n)-[r:LINK]-(n2) DELETE d2f,r,n", params );
            if ( result.getQueryStatistics().getNodesDeleted() == message.length ) {
                JsonGenerator jg = objectMapper.getFactory().createGenerator( sw );
                jg.writeObject(message);
                jg.flush();
                jg.close();

            	tx.success();
            } else {
            	return Response.notModified().build();
            }
	    } catch (Exception e) {
            logger.error("Exception while deleting vertices:", e);
            return Response.status(500).entity(e.getMessage()).type(MediaType.TEXT_PLAIN).build();
	    }
        return Response.ok().entity( sw.toString() ).type( MediaType.APPLICATION_JSON ).build();
	}

    /** TODO
     *
     * @return
     */
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/doc")
    @GET
    public Response getDocsLegacy() {
        return getDocs("shallow");
    }

    /** TODO
     *
     * @param action
     * @return
     */
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/doc/{action:deep|shallow}")
	@GET
    public Response getDocs(@PathParam("action") final String action) {
        logger.info("Query: get all docs");
        StringWriter sw = new StringWriter();
        try (Transaction tx = db.beginTx()) {
            ResourceIterator<Node> d = db.execute("MATCH (d:DOC) WHERE not ()-[:REVISION]->(d) RETURN d").columnAs("d");

            JsonGenerator jg = objectMapper.getFactory().createGenerator(sw);
            jg.setPrettyPrinter(new DefaultPrettyPrinter());
            jg.writeStartArray();
                while (d.hasNext()) {
                    Node curDoc = d.next();
                    writeDocTree(jg, curDoc, action.equals("deep"));
                }
            jg.writeEndArray();
            jg.flush();
            jg.close();

            tx.success();
        } catch(NotFoundException e){
            return Response.status(404).entity(e.getMessage()).type(MediaType.TEXT_PLAIN).build();
        } catch(Exception e){
            logger.error("Exception while getting docs:", e);
            return Response.status(500).entity(e.getMessage()).type(MediaType.TEXT_PLAIN).build();
        }

        return Response.ok().entity( sw.toString() ).type( MediaType.APPLICATION_JSON ).build();
	}

    /** TODO
     *
     * @param docId
     * @return
     */
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/doc/{docId}")
    @GET
    public Response getDocByIdLegacy( @PathParam("docId") final Long docId ) {
        return getDocById( docId, "plain" );
    }

    /** TODO
     *
     * @param docId
     * @param action
     * @return
     */
    @Produces(MediaType.APPLICATION_JSON)
	@Path("/doc/{docId}/{action:plain|tree}")
	@GET
	public Response getDocById( @PathParam("docId") final Long docId, @PathParam("action") final String action ) {

		StringWriter sw = new StringWriter();
		try ( Transaction tx = db.beginTx() ) {
			Node docNode = db.getNodeById(docId);
	    	if ( docNode == null || !docNode.hasLabel(DOC) ) {
                throw new NotFoundException("unable to find document with id " + docId);
	    	}

            JsonGenerator jg = objectMapper.getFactory().createGenerator( sw );
            jg.setPrettyPrinter(new DefaultPrettyPrinter());
            if (action.equals("plain")) writeDoc(jg, docNode, true);
            if (action.equals("tree")) writeDocTree(jg, docNode, true);

            jg.flush();
            jg.close();

	    	tx.success();
		} catch (NotFoundException e) {
			return Response.status(404).entity( e.getMessage() ).type( MediaType.TEXT_PLAIN ).build();
		} catch (Exception e) {
            logger.error("Exception while getting doc by id:", e);
            return Response.status(500).entity(e.getMessage()).type(MediaType.TEXT_PLAIN).build();
        }

        return Response.ok().entity( sw.toString() ).type( MediaType.APPLICATION_JSON ).build();
	}

//    @Produces(MediaType.APPLICATION_JSON)
//    @Path("/doc/{docName}")
//    @GET
//    public Response getDocByName( @PathParam("docName") final String docName ) {
//
//        Node docNode;
//        StringWriter sw = new StringWriter();
//        try ( Transaction tx = db.beginTx() ) {
//            ResourceIterable<Node> ri = db.getAllNodes();
//            while ( ri.iterator().hasNext()){
//                Node node = ri.iterator().next();
//                if ( node.hasLabel(DOC) ) {
//                    if (node.getProperty("docName").equals(docName)){
//                        docNode = node;
//                        JsonGenerator jg = objectMapper.getFactory().createGenerator( sw );
//                        writeDoc(jg, docNode);
//
//                        jg.flush();
//                        jg.close();
//                    }
//                }
//            }
//            tx.success();
//        } catch (NotFoundException e) {
//            return Response.status(404).entity( e.getMessage() ).type( MediaType.APPLICATION_JSON ).build();
//        } catch (Exception e) {
//            e.printStackTrace();
//            return Response.status(500).build();
//        }
//
//        return Response.ok().entity( sw.toString() ).type( MediaType.APPLICATION_JSON ).build();
//    }

    /** This method returns a vertex (either NET or NODE) by it's id.
     *
     * @param docId the id of the document in which the node is requested by it's id
     * @param vertexId the id of the node that's object is to be returned
     * @return
     */
    @Produces(MediaType.APPLICATION_JSON)
	@Path("/doc/{docId}/vertex/{vertexId}")
	@GET
	public Response getVertexById( @PathParam("docId") final Long docId, @PathParam("vertexId") final Long vertexId ) {
		final Map<String, Object> params = MapUtil.map( "docId", docId, "vertexId", vertexId );

        StringWriter sw = new StringWriter();
        try ( Transaction tx = db.beginTx() ) {
            ResourceIterator<Node> ri = db.execute("MATCH (d:DOC)-[r:CONTAIN]->(n) WHERE ID(d)=$docId AND ID(n)=$vertexId RETURN n", params ).columnAs("n");
            JsonGenerator jg = objectMapper.getFactory().createGenerator( sw );
            if ( !ri.hasNext() ) {
                return Response.status(404).entity( "Can't find node/net with id " + vertexId + " in doc with id " + docId ).type( MediaType.TEXT_PLAIN ).build();
            }

            jg.writeStartObject();
            writeVertex(jg, ri.next());
            jg.writeEndObject();
            jg.flush();
            jg.close();
            tx.success();
        } catch (Exception e) {
            logger.error("Exception while getting vertex by id:", e);
            return Response.status(500).entity(e.getMessage()).type(MediaType.TEXT_PLAIN).build();
        }
        return Response.ok().entity( sw.toString() ).type( MediaType.APPLICATION_JSON ).build();
	}

    /** This method edits the properties of the given NET.
     *  If the document is already bootstrapped, the network is propagated afterwards and
     *  the TC properties are communicated to the respective Agents.
     *
     * @param docId the id of the document in which the NET is to be edited
     * @param netId the id of the NET to be edited
     * @param net the model object of the NET containing the new properties
     * @return
     */
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/doc/{docId}/net/{netId}") ///{action:edgeAugmenting|edgeDeleting}
    @PUT
    public Response editNet( @PathParam("docId") final Long docId, @PathParam("netId") final Long netId, final ModelNet net ) {
        return editVertex ( docId, netId, net);
    }

    /** This method edits the properties of the given NODE.
     *  If the document is already bootstrapped, the network is propagated afterwards and
     *  the TC properties are communicated to the respective Agents.
     *
     * @param docId the id of the document in which the NODE is to be edited
     * @param nodeId the id of the NODE to be edited
     * @param node the model object of the NODE containing the new properties
     * @return
     */
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/doc/{docId}/node/{nodeId}") ///{action:edgeAugmenting|edgeDeleting}
    @PUT
    public Response editNode( @PathParam("docId") final Long docId, @PathParam("nodeId") final Long nodeId, final ModelNode node ) {
        return editVertex ( docId, nodeId, node);
    }

    private Response editVertex( Long docId, Long vId, ModelVertex vertex) {
        if ( docId.equals(docIdStatic) ) {
//            return Response.status(428).entity("illegal modification after bootstrap").type(MediaType.TEXT_PLAIN).build();
            vertex.props.entrySet().removeIf(e-> !e.getKey().equals("cancelled") );
        }
        Map<String, Object> modelV = new ObjectMapper().convertValue(vertex, Map.class);
        final Map<String, Object> params = MapUtil.map("docId", docId, "vId", vId, "modelV", modelV);

        StringWriter sw = new StringWriter();
        try ( Transaction tx = db.beginTx() ) {
            //TODO: If addr is changed or even added the first time the edgesBack are not changed here...
            ResourceIterator<Node> n = db.execute( "MATCH (d:DOC)-[:CONTAIN]->(n) "+
                    "WHERE ID(d)=$docId AND ID(n)=$vId SET n+=$modelV.props RETURN n", params ).columnAs("n");

            if ( !n.hasNext() ) {
                return Response.status(400).entity( buildJsonMessage("Illegal document or net reference") ).type( MediaType.APPLICATION_JSON ).build();
            }
            Node editedV = n.next();
            setNewNetIpOnIncomingLinks(editedV);
            JsonGenerator jg = objectMapper.getFactory().createGenerator( sw );
            jg.writeStartObject();
                writeVertex(jg, editedV);
            jg.writeEndObject();

            if ( docId.equals(docIdStatic) ) {
                /*
                 *  Post-BOOTSTRAP
                 */
                startPropagation(docId, "WADJ");
                getAdjListsAndSendToNA(null, docId, "WADJ", "put");
                //getAdjLists(jg, docId, "WADJ");

            }
            jg.flush();
            jg.close();

            tx.success();
        } catch (Exception e) {
            logger.error("Exception while editing vertex:", e);
            return Response.status(500).entity(e.getMessage()).type(MediaType.TEXT_PLAIN).build();
        }

        return Response.ok().entity( sw.toString() ).type( MediaType.APPLICATION_JSON ).build();
    }

    /** This method executes the propagation if the document is already instantiated (bootstrapped).
     *  Depending on the communication with the agents, there are two actions:
     *      readonly:       executes propagation only
     *      writeagents:    executes propagation and communication of the properties to the agents
     *
     * @param action the action to be performed (either readonly or writeagents)
     * @return
     */
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/propagate/{action:readonly|writeagents}")
    @GET
    public Response initialPropagationWithoutDocId(@PathParam("action") final String action) {
        if (docIdStatic < 0) return Response.status(428).entity(buildJsonMessage("Document not instantiated")).type( MediaType.APPLICATION_JSON ).build();
        return initialPropagation(docIdStatic, action);
    }

    @Produces(MediaType.APPLICATION_JSON)
    @Path("/doc/{docId}/propagate/{action:readonly|writeagents}")
    @GET
    public Response initialPropagation( @PathParam("docId") final Long docId, @PathParam("action") final String action ) {

        if (action.equals("writeagents")) {
            responseAnsible.setStatus(ResponseAnsible.Status.PROPAGATING);
        }


        StringWriter sw = new StringWriter();
        try ( Transaction tx = db.beginTx() ) {
            JsonGenerator jg = objectMapper.getFactory().createGenerator( sw );
            if ( action.equals("readonly") ) {
                startPropagation(docId, "RADJ");
                getAdjLists(jg, docId, "RADJ");
            }
            if ( action.equals("writeagents") ) {
                startPropagation(docId, "WADJ");
                getAdjListsAndSendToNA(jg, docId, "WADJ", "put");
            }

            jg.flush();
            jg.close();
            tx.success();
        } catch (Exception e) {
            logger.error("Exception while iniotial propagation:", e);
            responseAnsible.setError(ResponseAnsible.ErrorStatus.NOT_PROPAGATED);
            return Response.status(500).entity(buildJsonMessage(e.getMessage())).type(MediaType.APPLICATION_JSON).build();
        }
        if (action.equals("writeagents")) {
            responseAnsible.setError(ResponseAnsible.ErrorStatus.NO_ERROR);
            responseAnsible.setStatus(ResponseAnsible.Status.PROPAGATED);
        }

        String result = sw.toString();
        logger.info("Initial propagation result:\n"+result+"\n\n");
        return Response.ok().entity(result).type(MediaType.APPLICATION_JSON).build();
    }

    /** This method saves the OpenStack credentials to a file and before checks if the OpenStack credential parameters are all existing.
     *
     * @param openStackConfig the object of the OpenStack credentials
     * @return
     */
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/yml-config/os")
    @POST
    public Response parseYmlConfigOS( ResponseOpenstackConfig openStackConfig ) {
        responseAnsible.setStatus(ResponseAnsible.Status.GENERATING_YML);
        /*
        Check if params are missing (not validating)
         */
        String checked = checkNullValuesInOSConfig(openStackConfig);
        //If all props are inserted occured
        if (!checked.equals("")){
            return Response.status(400).entity(buildJsonMessage(checked)).type(MediaType.APPLICATION_JSON).build();
        }
        logger.info("All config values inserted! ");

        return writeYmlOsToFile(openStackConfig);
    }

    /** This method exports the yml-config for OpenStack from the credentials saved as a *.yml-file.
     *
     * @param openStackConfig the object of the OpenStack credentials
     * @return
     */
    private Response writeYmlOsToFile(ResponseOpenstackConfig openStackConfig){
        try {
            //String PATH_TO_HOST_FILE = "{{ ansible_env.PWD }}/all_fog_nodes.json";

            YAMLFactory yamlFactory = new YAMLFactory();

            StringWriter sw = new StringWriter();
            YAMLGenerator yamlGenerator = yamlFactory.createGenerator(sw).enable(YAMLGenerator.Feature.MINIMIZE_QUOTES);

            yamlGenerator.writeStartObject();
            yamlGenerator.writeFieldName("openstack");
            yamlGenerator.writeStartObject();
            yamlGenerator.writeFieldName("auth");
            yamlGenerator.writeStartObject();
            yamlGenerator.writeFieldName("auth_url");
            yamlGenerator.writeString(openStackConfig.getAuthUrl());

            yamlGenerator.writeFieldName("username");
            yamlGenerator.writeString(openStackConfig.getUsername());

            yamlGenerator.writeFieldName("password");
            yamlGenerator.writeString(openStackConfig.getPassword());

            yamlGenerator.writeFieldName("project_name");
            yamlGenerator.writeString(openStackConfig.getProjectName());
            yamlGenerator.writeEndObject();
            yamlGenerator.writeEndObject();

            yamlGenerator.writeFieldName("external_network");
            yamlGenerator.writeString(openStackConfig.getExternalNetwork());

            yamlGenerator.writeFieldName("mgmt_network_name");
            yamlGenerator.writeString("mgmt");//openStackConfig.getMgmtNetworkName()); //"mgmt"

            yamlGenerator.writeFieldName("ssh_key_name");
            yamlGenerator.writeString(openStackConfig.getOsSshKeyName());

            yamlGenerator.writeFieldName("ssh_user");
            yamlGenerator.writeString(openStackConfig.getSshUser());
            yamlGenerator.writeEndObject();
//        yamlGenerator.writeFieldName("path_to_hosts_file");
//        yamlGenerator.writeString(PATH_TO_HOST_FILE);

            /*
            Overwriting! TODO should be overwriting...
             */
//            if(!(new File(Settings.PATH_TO_OS_CONFIG).exists())) {
            PrintWriter writer = new PrintWriter(Settings.PATH_TO_OS_CONFIG, "UTF-8");
            writer.println(sw.toString());
            writer.flush();
            writer.close();
//            } else {
//                System.out.println("[WARNING] os_example_config.yml already existed (path: " + Settings.PATH_TO_OS_CONFIG + "!");
//            }

            //Set from file now!
            //this.openStackConfig = openStackConfig;

            responseAnsible.setError(ResponseAnsible.ErrorStatus.NO_ERROR);
            responseAnsible.setStatus(ResponseAnsible.Status.GENERATED_YML);

            /*
            returns file with set external_network
             */
            return Response.ok().entity(getYmlConfigOsFromFileAsJson()).type(MediaType.APPLICATION_JSON).build();

        } catch (IOException e) {
            logger.error("IOException while writing yml to file:", e);
            return Response.status(500).entity(buildJsonMessage(e.getMessage())).type( MediaType.APPLICATION_JSON ).build();
        } catch (Exception e) {
            logger.error("Exception while writing yml to file:", e);
            return Response.status(500).entity(buildJsonMessage(e.getMessage())).type( MediaType.APPLICATION_JSON ).build();
        }
    }

    /** This method checks existence of all OpenStack credential parameters.
     *
     * @param openStackConfig the object of the OpenStack credentials
     * @return
     */
    private String checkNullValuesInOSConfig(ResponseOpenstackConfig openStackConfig) {
        /*
        Check if params are missing (not validating)
         */
        String missingParams = "";
        try {
            if (openStackConfig.getExternalNetwork().equals("") || openStackConfig.getExternalNetwork() == null ||
                    openStackConfig.getAuthUrl().equals("") || openStackConfig.getAuthUrl()== null
                    || openStackConfig.getOsSshKeyName().equals("") || openStackConfig.getOsSshKeyName()== null
                    || openStackConfig.getUsername().equals("") || openStackConfig.getUsername() == null
                    || openStackConfig.getProjectName().equals("") || openStackConfig.getProjectName()==null) {

                missingParams += "Please insert the missing values: ";

                if (openStackConfig.getExternalNetwork().equals("") || openStackConfig.getExternalNetwork() == null) {
                    missingParams += "External network ";
                }
                if (openStackConfig.getAuthUrl().equals("") || openStackConfig.getAuthUrl()== null) {
                    missingParams += "Authentication URL ";
                }
                if (openStackConfig.getOsSshKeyName().equals("") || openStackConfig.getOsSshKeyName()== null ) {
                    missingParams += "SSH key name ";
                }
                if (openStackConfig.getUsername().equals("") || openStackConfig.getUsername() == null) {
                    missingParams += "User name ";
                }
                if (openStackConfig.getPassword().equals("") || openStackConfig.getPassword() == null) {
                    missingParams += "Password ";
                }
                if (openStackConfig.getProjectName().equals("") || openStackConfig.getProjectName()==null) {
                    missingParams += "Project name ";
                }
                logger.info(missingParams);
                responseAnsible.setError(ResponseAnsible.ErrorStatus.NOT_GENERATED_YML);
            }
        } catch (NullPointerException n){
            return missingParams = "Please set all parameters [ssh_key_name, key, external_network, auth_url, username, password, project_name] ";
        }
        return missingParams;
    }

    /** This method allows editing the yml-config for OpenStack (overwriting the old version).
     *
     * @param openStackConfig the object of the OpenStack credentials
     * @return
     */
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/yml-config/os")
    @PUT
    public Response editYmlConfigOS( ResponseOpenstackConfig openStackConfig ) {

        responseAnsible.setStatus(ResponseAnsible.Status.GENERATING_YML);
        try {
            ResponseOpenstackConfig configFromFile = objectMapper.readValue(getYmlConfigOsFromFileAsJson(), ResponseOpenstackConfig.class);
            /*
            Merge new Props into old object and write all into file
             */
            if (!openStackConfig.getProjectName().equals("")) configFromFile.setProjectName(openStackConfig.getProjectName());
            if (!openStackConfig.getUsername().equals("")) configFromFile.setUsername(openStackConfig.getUsername());
            if (!openStackConfig.getPassword().equals("")) configFromFile.setPassword(openStackConfig.getPassword());
            if (!openStackConfig.getOsSshKeyName().equals("")) configFromFile.setOsSshKeyName(openStackConfig.getOsSshKeyName());
            if (!openStackConfig.getAuthUrl().equals("")) configFromFile.setAuthUrl(openStackConfig.getAuthUrl());
            if (!openStackConfig.getSshUser().equals("")) configFromFile.setSshUser(openStackConfig.getSshUser());
            if (!openStackConfig.getExternalNetwork().equals("")) configFromFile.setExternalNetwork(openStackConfig.getExternalNetwork());

            return writeYmlOsToFile(configFromFile);

        } catch (IOException e) {
            logger.error("IOException while editing yml config:", e);
            return Response.status(500).entity(buildJsonMessage(e.getMessage())).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /** This method saves the AWS credentials to a file and before checks if the AWS credential parameters are all existing.
     *
     * @param awsConfig the object of the AWS credentials
     * @return
     */
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/yml-config/aws")
    @POST
    public Response parseYmlConfigAWS( ResponseAWSConfig awsConfig ) {

        responseAnsible.setStatus(ResponseAnsible.Status.GENERATING_YML);

         /*
        Check if params are missing (not validating)
         */
        String checked = checkNullValuesInAWSConfig(awsConfig);
        //If all props are inserted occured
        if (!checked.equals("")){
            return Response.status(400).entity(buildJsonMessage(checked)).type(MediaType.APPLICATION_JSON).build();
        }
        logger.info("All config values inserted! ");

        return writeYmlAWSToFile(awsConfig);
    }

    /** This method exports the yml-config for AWS from the credentials saved as a *.yml-file.
     *
     * @param awsConfig the object of the AWS credentials
     * @return
     */
    private Response writeYmlAWSToFile(ResponseAWSConfig awsConfig){
        try {
            YAMLFactory yamlFactory = new YAMLFactory();

            StringWriter sw = new StringWriter();
            YAMLGenerator yamlGenerator = yamlFactory.createGenerator(sw).enable(YAMLGenerator.Feature.MINIMIZE_QUOTES);
            yamlGenerator.writeStartObject();

            yamlGenerator.writeFieldName("ec2_access_key");
            yamlGenerator.writeString(awsConfig.getEc2accessKey());

            yamlGenerator.writeFieldName("ec2_secret_access_key");
            yamlGenerator.writeString(awsConfig.getEc2SecretAccessKey());

            yamlGenerator.writeFieldName("ec2_region");
            yamlGenerator.writeString(awsConfig.getEc2Region());

            yamlGenerator.writeFieldName("mgmt_network_name");
            yamlGenerator.writeString("mgmt");//awsConfig.getMgmtNetworkName());

            yamlGenerator.writeFieldName("ssh_key_name");
            yamlGenerator.writeString(awsConfig.getSshKeyName());

            yamlGenerator.writeFieldName("ssh_user");
            yamlGenerator.writeString(awsConfig.getSshUser());

            /*
            overwriting config
             */
//            if(!(new File(Settings.PATH_TO_AWS_CONFIG).exists())) {
            PrintWriter writer = new PrintWriter(Settings.PATH_TO_AWS_CONFIG, "UTF-8");
            writer.println(sw.toString());
            writer.flush();
            writer.close();

//            } else {
//                System.out.println("[WARNING] aws_example_config.yml already existed (path: " + Settings.PATH_TO_AWS_CONFIG + "!");
//            }

            responseAnsible.setError(ResponseAnsible.ErrorStatus.NO_ERROR);
            responseAnsible.setStatus(ResponseAnsible.Status.GENERATED_YML);

            return Response.ok().entity(getYmlConfigAWSFromFileAsJson()).type(MediaType.APPLICATION_JSON).build();

        } catch (IOException e) {
            logger.error("IOException while writing yml to file:", e);
            return Response.status(500).entity(e.getMessage()).type( MediaType.APPLICATION_JSON ).build();
        }
    }

    /** This method checks existence of all AWS credential parameters.
     *
     * @param awsConfig the object of the AWS credentials
     * @return
     */
    private String checkNullValuesInAWSConfig(ResponseAWSConfig awsConfig) {
        /*
        Check if params are missing (not validating)
         */
        String missingParams = "";
        try {
            if (awsConfig.getEc2accessKey().equals("") || awsConfig.getEc2accessKey() == null ||
                    awsConfig.getEc2Region().equals("") || awsConfig.getEc2Region()== null
                    || awsConfig.getEc2SecretAccessKey().equals("") || awsConfig.getEc2SecretAccessKey()== null
                    || awsConfig.getSshKeyName().equals("") || awsConfig.getSshKeyName() == null
                    || awsConfig.getSshUser().equals("") || awsConfig.getSshUser()==null) {

                missingParams += "Please insert the missing values: ";

                if (awsConfig.getEc2accessKey().equals("") || awsConfig.getEc2accessKey() == null) {
                    missingParams += "Ec2 Access Key ";
                }
                if (awsConfig.getEc2Region().equals("") || awsConfig.getEc2Region()== null) {
                    missingParams += "Ec2 Region ";
                }
                if (awsConfig.getEc2SecretAccessKey().equals("") || awsConfig.getEc2SecretAccessKey()== null ) {
                    missingParams += "Ec2 Secret Key ";
                }
                if (awsConfig.getSshKeyName().equals("") || awsConfig.getSshKeyName() == null) {
                    missingParams += "Ssh Key name ";
                }
                if (awsConfig.getSshUser().equals("") || awsConfig.getSshUser() == null) {
                    missingParams += "Ssh User ";
                }
                logger.info(missingParams);
                responseAnsible.setError(ResponseAnsible.ErrorStatus.NOT_GENERATED_YML);
            }
        } catch (NullPointerException n){
            return missingParams = "Please set all parameters [ssh_key_name, key, external_network, auth_url, username, password, project_name] ";
        }
        return missingParams;
    }

    /** This method allows editing the yml-config for AWS (overwriting the old version).
     *
     * @param awsConfig the object of the AWS credentials
     * @return
     */
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/yml-config/aws")
    @PUT
    public Response editYmlConfigAWS( ResponseAWSConfig awsConfig ) {

        responseAnsible.setStatus(ResponseAnsible.Status.GENERATING_YML);
        try {
            ResponseAWSConfig configFromFile = objectMapper.readValue(getYmlConfigAWSFromFileAsJson(), ResponseAWSConfig.class);
            /*
            Merge new Props into old object and write all into file
             */
            if (!awsConfig.getSshKeyName().equals("")) configFromFile.setSshKeyName(awsConfig.getSshKeyName());
            if (!awsConfig.getEc2SecretAccessKey().equals("")) configFromFile.setEc2SecretAccessKey(awsConfig.getEc2SecretAccessKey());
            if (!awsConfig.getEc2Region().equals("")) configFromFile.setEc2Region(awsConfig.getEc2Region());
            if (!awsConfig.getEc2accessKey().equals("")) configFromFile.setEc2accessKey(awsConfig.getEc2accessKey());
            if (!awsConfig.getSshUser().equals("")) configFromFile.setSshUser(awsConfig.getSshUser());

            return writeYmlAWSToFile(configFromFile);

        } catch (IOException e) {
            logger.error("IOException while editing yml aws config:", e);
            return Response.status(500).entity(buildJsonMessage(e.getMessage())).type(MediaType.APPLICATION_JSON).build();
        }
    }


    /** This method returns the yml-config file with the AWS credentials.
     *
     * @return
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/yml-config/aws")
    public Response loadYmlConfigAWSFromFileAsJson(){
        try {
            return Response.ok().entity(getYmlConfigAWSFromFileAsJson()).type(MediaType.APPLICATION_JSON).build();
        } catch (IOException e) {
            logger.error("IOException while loading aws yml", e);
            return Response.status(500).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /** This method returns the yml-config file with the OpenStack credentials.
     *
     * @return
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/yml-config/os")
    public Response loadYmlConfigOsFromFileAsJson(){
        try {
            return Response.ok().entity(getYmlConfigOsFromFileAsJson()).type(MediaType.APPLICATION_JSON).build();
        } catch (IOException e) {
            logger.error("IOException while loading OS yml:", e);
            return Response.status(500).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    public String getYmlConfigOsFromFileAsJson() throws IOException {
        String jsonString = "";
        OpenstackConfig openstackConfig = new OpenstackConfig();
        ResponseOpenstackConfig response = new ResponseOpenstackConfig();

        objectMapper = new ObjectMapper(new YAMLFactory());
        openstackConfig = objectMapper.readValue(new File(Settings.PATH_TO_OS_CONFIG), OpenstackConfig.class);
        response.setOsSshKeyName(openstackConfig.getSshKeyName());
        response.setSshUser(openstackConfig.getSshUser());
        response.setExternalNetwork(openstackConfig.getExternalNetwork());
        response.setAuthUrl(openstackConfig.getOpenstack().getAuth().getAuthUrl());
        response.setUsername(openstackConfig.getOpenstack().getAuth().getUsername());
        response.setPassword(openstackConfig.getOpenstack().getAuth().getPassword());
        response.setProjectName(openstackConfig.getOpenstack().getAuth().getProjectName());

        objectMapper = new ObjectMapper(new JsonFactory());
        jsonString = objectMapper.writeValueAsString(response);

        logger.info(jsonString);

        /*
        Openstack object instance initialising by setting external network as the same as in the current doc
         */
        this.openStackConfig.setExternalNetwork(openstackConfig.getExternalNetwork());

        return jsonString;
    }

    public String getYmlConfigAWSFromFileAsJson() throws IOException {
        String jsonString = "";
        AWSConfig awsConfig = new AWSConfig();
        ResponseAWSConfig response = new ResponseAWSConfig();

        objectMapper = new ObjectMapper(new YAMLFactory());
        awsConfig = objectMapper.readValue(new File(Settings.PATH_TO_AWS_CONFIG), AWSConfig.class);
        response.setEc2accessKey(awsConfig.getEc2AccessKey());
        response.setEc2Region(awsConfig.getEc2Region());
        response.setEc2SecretAccessKey(awsConfig.getEc2SecretAccessKey());
        response.setSshKeyName(awsConfig.getSshKeyName());
        response.setSshUser(awsConfig.getSshUser());

        objectMapper = new ObjectMapper(new JsonFactory());
        jsonString = objectMapper.writeValueAsString(response);

        logger.info(jsonString);

        return jsonString;
    }

    @Deprecated
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/doc/{docId}/parseDhcp")
    @POST
    public Response parseDhcp( @PathParam("docId") final Long docId, Map<String, Object> dhcpMap ) {
        final Map<String, Object> params = MapUtil.map( "docId", docId, "dhcpMap", dhcpMap);
        logger.info("Reveived object:\n" + params);

        responseAnsible.setStatus(ResponseAnsible.Status.PARSING_DHCP);
        /*
         * insert IaC json
         */
        try ( Transaction tx = db.beginTx() ) {
            db.execute("MATCH (vdoc: DOC) WHERE id(vdoc)=$docId " +
                    " UNWIND keys($dhcpMap) AS nno " +
                    " MATCH (vno:NODE{name:nno})<-[:CONTAIN]-(vdoc) " +
                    " MERGE (vno)<-[r:LINK]-(vdoc) SET r.public_ip=$dhcpMap[nno].public_addr " +
                    " WITH vdoc, nno, vno " +
                    " UNWIND keys($dhcpMap[nno]) AS nne " +
                    " MATCH (vne{name:nne})<-[:CONTAIN]-(vdoc) " +
                    " UNWIND $dhcpMap[nno][nne] AS o " +
                    " MATCH (vne)-[r:LINK]->(vno) SET r+=o " +
                    " ", params );

            tx.success();
        } catch (Exception e) {
            logger.error("Exception while parsing DHCP:", e);
            responseAnsible.setError(ResponseAnsible.ErrorStatus.NOT_PARSED_DHCP);
            return Response.status(500).entity( buildJsonMessage(e.getMessage()) ).type( MediaType.APPLICATION_JSON ).build();
        }
        responseAnsible.setError(ResponseAnsible.ErrorStatus.NO_ERROR);
        responseAnsible.setStatus(ResponseAnsible.Status.PARSED_DHCP);

        return Response.ok().entity(buildJsonMessage("Dhcp parsed.Ansible status: " + responseAnsible.getMessage()) ).type(MediaType.APPLICATION_JSON).build();
    }

    /** This method writes the IP addresses given in the dhcpMap to their respective nodes to the db after the bootstrapping process.
     *
     * @param dhcpMap the map of the nodes with their IP adresses
     * @return
     */
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/parseDhcp")
    @POST
    public Response parseDhcpStaticDocId( Map<String, Object> dhcpMap ) {
        if (docIdStatic < 0) return Response.status(428).entity(buildJsonMessage("Document not instantiated.")).type( MediaType.APPLICATION_JSON ).build();
        Response r = parseDhcp(docIdStatic, dhcpMap );
        return r;
    }


    /** This method sets a status that describes the current processing by ansible.
     *
     * @param responseAnsible the status that provides a little message about what's happening
     * @return
     */
    @Path("/ansiblelog")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @POST
    public Response setEnvironmentStatus(ResponseAnsible responseAnsible){

        try {
            if (docIdStatic < 0) {
                return Response.status(428).entity(buildJsonMessage("Document not instantiated.")).type(MediaType.APPLICATION_JSON).build();
            }

            this.responseAnsible = responseAnsible;
            return Response.ok().entity(buildJsonMessage(responseAnsible.getMessage())).type(MediaType.APPLICATION_JSON).build();
        } catch (Exception e){
            return Response.status(500).entity(buildJsonMessage("Error occured. Status not set.")).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /** This method provides little insight into what happens in ansible very high level by returning a status message.
     *
     * @return
     */
    @Path("/ansiblelog")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public Response getEnvironmentStatus(){
        if (docIdStatic < 0){
            return Response.status(428).entity(buildJsonMessage("Document not instantiated.")).type( MediaType.APPLICATION_JSON ).build();
        }
        return Response.ok().entity(buildJsonMessage(responseAnsible.getMessage())).type( MediaType.APPLICATION_JSON ).build();
    }


    // TODO: This method won't be necessary anymore...
    @Deprecated
    @Path("bootstrap-test")
    @GET
    public Response bootstrapTest() {
        //try {
        //   Thread.sleep(2500);
        //} catch (InterruptedException e) {
        //    e.printStackTrace();
        //}

        String ansibleLog = null;
        try {
            ansibleLog = InfrastructureController.getInstance().bootstrapSetup(true);
        } catch (IOException e) {
            logger.error("IOException while bootstrapping test:", e);
            return Response.status(500).entity(buildJsonMessage(e.getMessage())).type( MediaType.APPLICATION_JSON ).build();
        }
        logger.info(ansibleLog);

        return Response
                .ok()
               // .entity(StringUtils.join(is.currentLog, "\n"))
                .entity(ansibleLog)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    /** This method initiates the bootstrapping process by ...
     *  (after checking integrity, exporting the given document as platform-specific yml representation)
     *  ... starting ansible.
     *
     *  While the ansible process is started in a new thread, this method returns a temporal status of the started ansible call.
     *
     * @param docId the id of the document to be bootstrapped on an IaaS provider
     * @param platform the platform where the document should be bootstrapped (either aws or os)
     * @return
     * @throws IOException
     */
    //@Path("bootstrap")
    @Path("/doc/{docId}/bootstrap/{platform:aws|os}")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public Response bootstrap( @PathParam("docId") final Long docId, @PathParam("platform") final String platform) throws IOException {
        logger.info("Bootstrapping requested, docID is " + docId + ", plattform is " + platform);
        if (docIdStatic > -1) return Response.status(400)
                .entity(buildJsonMessage("Document with id "+docIdStatic+" already instantiated"))
                .type(MediaType.APPLICATION_JSON).build();
        if (docId == null) return Response.status(400).entity(buildJsonMessage("Invalid doc id")).type(MediaType.APPLICATION_JSON).build();
//        //TODO: please change the code below if you don't want to share the yml via the tmp folder
//        String tmpPath = System.getProperty("java.io.tmpdir");
//        System.out.println("storing ansible yml files to tmp folder " + tmpPath);
//
//        File tmpFile = new File(tmpPath+(platform.equals("os") ? "/os_example_vars.yml" : "/aws_example_vars.yml"));

        String yml = "";
        String ansibleLog = "";
        try ( Transaction tx = db.beginTx() ) {
            checkIntegrityNodeHasOutgoingLinks(docId);
            checkIntegrityCycle(docId);
            checkIntegrityLinksBetweenDocs(docId);
            checkIpNetIpRealmCollision(docId);
            checkAnsibleIntegrityNetNameNotUniqueOrMissing(docId);
            checkAnsibleIntegrityNodeLinkedToMultipleNets(docId);

            yml = ( platform.equals("os") ? getYmlOS(docId) : getYmlAWS(docId) );
            logger.info("yml is " + yml);
            //TODO: uncomment before pushing
            responseAnsible.setStatus(ResponseAnsible.Status.BOOTSTRAPPING);
            ansibleLog = InfrastructureController.getInstance().bootstrapSetup(platform.equals("os"));
            logger.info(ansibleLog);

            tx.success();
        } catch (ExceptionInvalidData e) {
            responseAnsible.setError(ResponseAnsible.ErrorStatus.NOT_BOOTSTRAPPED);
            return Response.status(400).entity(buildJsonMessage(e.getMessage())).type( MediaType.APPLICATION_JSON ).build();
        } catch (ExceptionInternalServerError e) {
            responseAnsible.setError(ResponseAnsible.ErrorStatus.NOT_BOOTSTRAPPED);
            return Response.status(500).entity(buildJsonMessage(e.getMessage())).type( MediaType.APPLICATION_JSON ).build();
        }
        //TODO: uncomment before pushing
        catch (IOException e) {
            responseAnsible.setError(ResponseAnsible.ErrorStatus.NOT_BOOTSTRAPPED);
            return Response.status(500).entity(buildJsonMessage(e.getMessage())).type( MediaType.APPLICATION_JSON ).build();
        }

//        System.out.println( yml );
//        BufferedWriter bw = new BufferedWriter(new FileWriter(tmpFile));
//        bw.write(yml);
//        bw.close();

        docIdStatic = docId;
        return Response.ok().entity(buildJsonMessage("Document instantiated. \nBootstrapping successfully started.\n" +
                "Ansible status: " + responseAnsible.getMessage() +
                "\nPlease click on Expert view to see the details of the environment setup.")).type(MediaType.APPLICATION_JSON).build();
    }

    /** This method destroys the whole environment running on a platform but not the NodeManager.
     *
     * @param docId the id of the document to be destroyed
     * @param platform the platform of the provider where the document to be destroyed is running
     * @return
     * @throws IOException
     */
    @Path("/doc/{docId}/destroy/{platform:aws|os}")
    @Produces(MediaType.APPLICATION_JSON)
    @DELETE
    public Response destroy( @PathParam("docId") final Long docId, @PathParam("platform") final String platform) throws IOException {
        if (docId == null) return Response.status(400).entity(buildJsonMessage("Invalid doc id")).type(MediaType.APPLICATION_JSON).build();

        responseAnsible.setStatus(ResponseAnsible.Status.DESTROYING);
        String ansibleLog = null;

        try {
            ansibleLog = InfrastructureController.getInstance().destroySetup(platform.equals("os"));
            logger.info(ansibleLog);
        }
        catch (Exception e) {
            responseAnsible.setError(ResponseAnsible.ErrorStatus.NOT_DESTROYED);
            return Response.status(500).entity(buildJsonMessage(e.getMessage())).type( MediaType.APPLICATION_JSON ).build();
        }

        responseAnsible.setError(ResponseAnsible.ErrorStatus.NO_ERROR);
        responseAnsible.setStatus(ResponseAnsible.Status.DESTROYED);

        docIdStatic = -1;
        return Response.ok().entity(buildJsonMessage("[LOG]: " + ansibleLog.substring(0, Math.min(ansibleLog.length(), 300)) + " ..." + "\n"
                + "Ansible status: " + responseAnsible.getMessage())).type(MediaType.APPLICATION_JSON).build();

    }

    /** This method returns more detailed ansible logs.
     *
     * @return
     * @throws IOException
     */
    @Path("/process/logs")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public Response getAnsibleLog() throws IOException {
        return Response.ok().entity(buildJsonMessage(String.join("\n", InfrastructureController.getInstance().currentLog))).type(MediaType.APPLICATION_JSON).build();
    }

    /** This method returns the *.yml file for the current document and the specific platform how it is possible to be given to ansible.
     *
     * @param platform the platform that the yml is specified for
     * @return
     */
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/yml/{platform:aws|os}")
    @GET
    public Response getYml( @PathParam("platform") final String platform  ) {
        if (docIdStatic < 0) return Response.status(428).entity("Document not instantiated.").type( MediaType.TEXT_PLAIN ).build();
        return getYmlByDocId( docIdStatic,  platform  );
    }

    @Deprecated
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/doc/{docId}/yml/{platform:aws|os}")
    @GET
    public Response getYmlByDocId( @PathParam("docId") final Long docId,  @PathParam("platform") final String platform  ) {

        try {
            if (platform.equals("aws")) return Response.ok().entity(getYmlAWS(docId)).type( MediaType.TEXT_PLAIN ).build();
            if (platform.equals("os"))  {
                //required for external network
                if (openStackConfig.getExternalNetwork() == null){
                    responseAnsible.setError(ResponseAnsible.ErrorStatus.NOT_GENERATED_YML);
                    return Response.status(428).entity("OpenstackConfig not initiated. Create OpenstackConfig first!").type(MediaType.TEXT_PLAIN).build();
                }
                return Response.ok().entity( getYmlOS(docId)).type( MediaType.TEXT_PLAIN ).build();
            }
        } catch (ExceptionInvalidData e) {
            responseAnsible.setError(ResponseAnsible.ErrorStatus.NOT_GENERATED_YML);
            return Response.status(400).entity(e.getMessage()).type( MediaType.TEXT_PLAIN ).build();
        } catch (ExceptionInternalServerError e) {
            responseAnsible.setError(ResponseAnsible.ErrorStatus.NOT_GENERATED_YML);
            return Response.status(500).entity(e.getMessage()).type( MediaType.TEXT_PLAIN ).build();
        }
        return Response.status(500).entity(responseAnsible.getMessage()).build();
    }

    /** This method generates the OS specific yml from the given document and writes it into a *.yml file.
     *
     * @param docId the id of the document to get the yml for
     * @return
     * @throws ExceptionInvalidData
     * @throws ExceptionInternalServerError
     */
    public String getYmlOS( Long docId ) throws ExceptionInvalidData, ExceptionInternalServerError {
        final Map<String, Object> params = MapUtil.map( "docId", docId);

        responseAnsible.setStatus(ResponseAnsible.Status.GENERATING_YML);

        ResourceIterator<Node> ri;
        StringWriter sw = new StringWriter();

        try ( Transaction tx = db.beginTx() ) {
            YAMLFactory yamlFac = new YAMLFactory();
            YAMLGenerator yg = yamlFac.createGenerator( sw ).enable(YAMLGenerator.Feature.MINIMIZE_QUOTES); //.enable(YAMLGenerator.Feature.INDENT_ARRAYS );

            yg.writeStartObject();

            yg.writeFieldName("networks");
                yg.writeStartArray();
                    yg.writeStartObject();
                    ri = db.execute("MATCH (d:DOC) WHERE ID(d)=$docId RETURN d", params ).columnAs("d");
                    Node d = ri.next();
                    if (!existsProperty(d, "name", "")) throw new ExceptionInvalidData("property 'name' not specified!");
                    if (!existsProperty(d, "addr", "")) throw new ExceptionInvalidData("property 'addr' not specified!");
//                    String mgmtNetName = ((String)d.getProperty("name"));
//                    String mgmtNetNameSub = ((String)d.getProperty("name"))+"_sub";
//                    String mgmtNetAddr = ((String)d.getProperty("addr"));
//                        yg.writeFieldName("name");
//                        yg.writeString(mgmtNetName); //mgmt
//                        yg.writeFieldName("subnet_name");
//                        yg.writeString(mgmtNetNameSub); //mgmt_sub
//                        yg.writeFieldName("subnet");
//                        yg.writeString(mgmtNetAddr); //"196.168.100.0/24"
//                    yg.writeEndObject();

                        yg.writeFieldName("name");
                        yg.writeString("mgmt"); //mgmt
                        yg.writeFieldName("subnet_name");
                        yg.writeString("mgmt_sub"); //mgmt_sub
                        yg.writeFieldName("subnet");
                        yg.writeString("196.168.100.0/24"); //"196.168.100.0/24"
                        yg.writeEndObject();
            ri = db.execute("MATCH (d:DOC)-[r:CONTAIN]->(n:NET) WHERE ID(d)=$docId AND n._mask>0 AND n._mask<31 RETURN n", params ).columnAs("n");

            ArrayList<Node> netNodes = new ArrayList<>();
            while ( ri.hasNext() ) {
                netNodes.add( ri.next() );
            }
            int i = 0;
            while ( i < netNodes.size()) {
                Node n = netNodes.get(i);
                yg.writeStartObject();
                    yg.writeFieldName("name");
                    yg.writeString((String)n.getProperty("name"));
                    yg.writeFieldName("subnet_name");
                    yg.writeString(n.getProperty("name") +"_sub");
                    yg.writeFieldName("subnet");
                    yg.writeString((String)n.getProperty("addr"));
                    yg.writeFieldName("reachable_subnets");
                    if (netNodes.size() > 1){
                        yg.writeStartArray();
                        int temp = 0;

                        while (temp < netNodes.size()) {
                            if (!((String)n.getProperty("name")).equals((String)netNodes.get(temp).getProperty("name"))) {
                                yg.writeStartObject();
                                yg.writeFieldName("destination");
                                yg.writeString((String) netNodes.get(temp).getProperty("addr"));                     //TODO
                                yg.writeFieldName("nexthop");
                                yg.writeString(new ModelIp((String) n.getProperty("addr")).getLastIp());   //TODO
                                yg.writeEndObject();
                            }
                            temp++;
                        }
                        yg.writeEndArray();
                    } else {
                        yg.writeString("");
                    }
                yg.writeEndObject();
                i++;
            }
            yg.writeEndArray();

            yg.writeFieldName("routers");
            yg.writeStartArray();
                yg.writeStartObject();
                    yg.writeFieldName("name");
                    yg.writeString("mgmt-router");

                    yg.writeFieldName("interfaces");
                    yg.writeStartArray();
                    yg.writeStartObject();
                    yg.writeFieldName("net");
//                    yg.writeString(mgmtNetName); //mgmt
                    yg.writeString("mgmt"); //mgmt
                    yg.writeFieldName("subnet");
//                    yg.writeString(mgmtNetNameSub); //mgmt_sub
                    yg.writeString("mgmt_sub"); //mgmt_sub
                    yg.writeFieldName("portip");
                    yg.writeString("192.168.100.1"); //mgmtNetAddr);
                    yg.writeEndObject();
                    yg.writeEndArray();
                    yg.writeFieldName("external_network");
                    yg.writeString(openStackConfig.getExternalNetwork());
                yg.writeEndObject();
                yg.writeStartObject();
                    yg.writeFieldName("name");
                    yg.writeString("MockFog_router");

                    yg.writeFieldName("interfaces");
                    ri = db.execute("MATCH (d:DOC)-[r:CONTAIN]->(n:NET) WHERE ID(d)=$docId AND n._mask>0 AND n._mask<31 RETURN n", params ).columnAs("n");
                    if (ri.hasNext()) {
                        yg.writeStartArray();
                        while (ri.hasNext()) {
                            Node n = ri.next();
                            yg.writeStartObject();
                            yg.writeFieldName("net");
                            yg.writeString((String) n.getProperty("name"));
                            yg.writeFieldName("subnet");
                            yg.writeString(n.getProperty("name") + "_sub");
                            yg.writeFieldName("portip");
                            yg.writeString(new ModelIp((String) n.getProperty("addr")).getLastIp());
                            yg.writeEndObject();
                        }
                        yg.writeEndArray();
                    } else {
                        yg.writeString("");
                    }
                    yg.writeFieldName("external_network");
                    yg.writeString(""); //no external network set
                yg.writeEndObject();
            yg.writeEndArray();

            yg.writeFieldName("vms");
            ri = db.execute("MATCH (d:DOC)-[r:CONTAIN]->(n:NODE) WHERE ID(d)=$docId RETURN n", params ).columnAs("n");
            if (ri.hasNext()){
                yg.writeStartArray();
                while ( ri.hasNext() ) {
                    Node n = ri.next();
                    yg.writeStartObject();
                    yg.writeFieldName("name");
                    yg.writeString( (String)n.getProperty("name") );
                    yg.writeFieldName("image");
                    String image = (String)n.getProperty("image");
                    yg.writeString(getOSImage(image));//yg.writeString("ubuntu-16.04");
                    yg.writeFieldName("flavor");
                    yg.writeString(getFlavorFromDeviceFile((String)n.getProperty("flavor"), true));//yg.writeString("m1.small");
//                    yg.writeFieldName("auto_ip");
//                    yg.writeString("no");
                    yg.writeFieldName("nics");
                    yg.writeStartArray();
                    yg.writeStartObject();
                    yg.writeFieldName("net-name");
//                    yg.writeString( mgmtNetName ); //mgmt
                    yg.writeString( "mgmt" ); //mgmt
                    yg.writeEndObject();
                    for (Relationship r : n.getRelationships(LINK, INCOMING) ) {
                        if (r.getOtherNode(n).hasLabel(NET)) {
                            yg.writeStartObject();
                            yg.writeFieldName("net-name");
                            yg.writeString( (String)r.getOtherNode(n).getProperty("name") );
                            yg.writeEndObject();
                        }
                    }
                    yg.writeEndArray();
                    yg.writeEndObject();
                }
                yg.writeEndArray();
            } else {
                yg.writeString("");
            }

            yg.writeEndObject();

            /*
            Overwrite vars file!
             */
            //if(!(new File(Settings.PATH_TO_OS_VARS).exists())) {
                PrintWriter writer = new PrintWriter(Settings.PATH_TO_OS_VARS, "UTF-8");
                writer.println(sw.toString());
                writer.flush();
                writer.close();
            //} else {
             //   System.out.println("[WARNING] os_example_vars.yml already existed (path: " + Settings.PATH_TO_OS_VARS + "!");
            //}


            //jg.flush();
            //jg.close();
            tx.success();
        } catch (Exception e) {
            logger.error("Exception while getting OS yml:", e);
            responseAnsible.setError(ResponseAnsible.ErrorStatus.NOT_GENERATED_YML);
            throw new ExceptionInternalServerError(e.getMessage());
        }

        responseAnsible.setError(ResponseAnsible.ErrorStatus.NO_ERROR);
        responseAnsible.setStatus(ResponseAnsible.Status.GENERATED_YML);

        return sw.toString();
    }


    /** This method generates the AWS specific yml from the given document and writes it into a *.yml file.
     *
     * @param docId the id of the document to get the yml for
     * @return
     * @throws ExceptionInvalidData
     * @throws ExceptionInternalServerError
     */
    public String getYmlAWS( Long docId ) throws ExceptionInvalidData, ExceptionInternalServerError {
        final Map<String, Object> params = MapUtil.map( "docId", docId);

        responseAnsible.setStatus(ResponseAnsible.Status.GENERATING_YML);
        ResourceIterator<Node> ri;
        StringWriter sw = new StringWriter();

        try ( Transaction tx = db.beginTx() ) {
            YAMLFactory yamlFac = new YAMLFactory();
            YAMLGenerator yg = yamlFac.createGenerator( sw ).enable(YAMLGenerator.Feature.MINIMIZE_QUOTES); //.enable(YAMLGenerator.Feature.INDENT_ARRAYS );

            yg.writeStartObject();

            yg.writeFieldName("vpc");
            yg.writeStartObject();
            yg.writeFieldName("name");
            yg.writeString("MockFog");
            yg.writeFieldName("cidr");
            yg.writeString("10.0.0.0/16"); // TODO CIDR out of private IPv4 address ranges is recommended e.g. 10.10.0.0/16
            yg.writeEndObject();

            yg.writeFieldName("networks");
            yg.writeStartArray();
            yg.writeStartObject();
            ri = db.execute("MATCH (d:DOC) WHERE ID(d)=$docId RETURN d", params ).columnAs("d");
            Node d = ri.next();
            if (!existsProperty(d, "name", "")) throw new ExceptionInvalidData("property 'name' not specified!");
            if (!existsProperty(d, "addr", "")) throw new ExceptionInvalidData("property 'addr' not specified!");
//            String mgmtNetName = ((String)d.getProperty("name"));
//            String mgmtNetNameSub = ((String)d.getProperty("name"))+"_sub";
//            String mgmtNetAddr = ((String)d.getProperty("addr"));
            yg.writeFieldName("name");
//            yg.writeString(mgmtNetName); //mgmt
            yg.writeString("mgmt"); //mgmt
            yg.writeFieldName("subnet_name");
//            yg.writeString(mgmtNetNameSub); //mgmt_sub
            yg.writeString("mgmt_sub"); //mgmt_sub
            yg.writeFieldName("subnet");
//            yg.writeString(mgmtNetAddr); //"196.168.100.0/24"
            yg.writeString("10.0.1.0/24"); //"196.168.100.0/24 (only works for OpenStack)"
            yg.writeEndObject();
            ri = db.execute("MATCH (d:DOC)-[r:CONTAIN]->(n:NET) WHERE ID(d)=$docId AND n._mask>0 AND n._mask<31 RETURN n", params ).columnAs("n");
            while ( ri.hasNext() ) {
                Node n = ri.next();
                yg.writeStartObject();
                yg.writeFieldName("name");
                yg.writeString((String)n.getProperty("name"));
                yg.writeFieldName("subnet_name");
                yg.writeString(n.getProperty("name") +"_sub");
                yg.writeFieldName("subnet");
                yg.writeString((String)n.getProperty("addr"));
                yg.writeEndObject();
            }
            yg.writeEndArray();

            yg.writeFieldName("vms");
            ri = db.execute("MATCH (d:DOC)-[r:CONTAIN]->(n:NODE) WHERE ID(d)=$docId RETURN n", params ).columnAs("n");
            if (ri.hasNext()){
                yg.writeStartArray();
                while ( ri.hasNext() ) {
                    Node n = ri.next();
                    yg.writeStartObject();
                    yg.writeFieldName("name");
                    yg.writeString( (String)n.getProperty("name") );

                    for (Relationship r : n.getRelationships(LINK, INCOMING) ) {
// TODO: bugfix, this condition can be violated even for nodes having assigned nets
                        if (r.getOtherNode(n).hasLabel(NET)) {
                            yg.writeFieldName("net");
                            yg.writeString( (String)r.getOtherNode(n).getProperty("name") );
                        } else {
//                            return Response.status(400).entity("node has no belonging net!").type( MediaType.TEXT_PLAIN ).build();
                        }
                    }
                    yg.writeFieldName("image");
                    String image = (String)n.getProperty("image");
                    yg.writeString(getAWSImage(image));
                    yg.writeFieldName("flavor");
                    yg.writeString(getFlavorFromDeviceFile((String)n.getProperty("flavor"), false));
                    yg.writeEndObject();
                }
                yg.writeEndArray();
            } else {
                yg.writeString("");
            }
            yg.writeFieldName("security_group_rules");
            yg.writeStartArray();
            yg.writeStartObject();
                yg.writeFieldName("proto");
                yg.writeString("tcp");
                yg.writeFieldName("ports");
                yg.writeStartArray();
                    yg.writeString("22");
                    yg.writeString("5000");
                yg.writeEndArray();
                yg.writeFieldName("cidr_ip");
                yg.writeString("0.0.0.0/0");
            yg.writeEndObject();
            yg.writeStartObject();
                yg.writeFieldName("proto");
                yg.writeString("icmp");
                yg.writeFieldName("ports");
                yg.writeString("-1");
                yg.writeFieldName("cidr_ip");
                yg.writeString("0.0.0.0/0");
            yg.writeEndObject();
            yg.writeEndArray();

            yg.writeEndObject();

           // if(!(new File(Settings.PATH_TO_AWS_VARS).exists())) {
                PrintWriter writer = new PrintWriter(Settings.PATH_TO_AWS_VARS, "UTF-8");
                writer.println(sw.toString());
                writer.flush();
                writer.close();
           // } else {
            //    System.out.println("[WARNING] aws_example_vars.yml already existed (path: " + Settings.PATH_TO_AWS_VARS + "!");
           // }


            tx.success();
        } catch (Exception e) {
            logger.error("Exception while getting AWS yml:", e);
            responseAnsible.setError(ResponseAnsible.ErrorStatus.NOT_GENERATED_YML);
            throw new ExceptionInternalServerError(e.getMessage());
        }

        responseAnsible.setError(ResponseAnsible.ErrorStatus.NO_ERROR);
        responseAnsible.setStatus(ResponseAnsible.Status.GENERATED_YML);

        return sw.toString();

    }

    /** This method returns the image of the OpenStack specific images.
     *
     * @param image the image to be mapped
     * @return
     */
    private String getOSImage(String image) {
        String osImage;
        //default image "ubuntu-16.04"
        if (image.equals("")){
            return osImage = "ubuntu-16.04";
        }
        /*
        other images can be set here
         */
        if (image.toLowerCase().contains("ubuntu")){
            return osImage = "ubuntu-16.04";
        }
        else {
            return osImage = "ubuntu-16.04";
        }
    }

    /** This method returns the image of the AWS specific images.
     *
     * @param image the image to be mapped
     * @return
     */
    private String getAWSImage(String image) {
        String awsImage;
        //default image "ubuntu-16.04"
        if (image.equals("")){
            return awsImage = "ami-0bdb1d6c15a40392c";
        }
        /*
        other images can be set here
         */
        if (image.toLowerCase().contains("ubuntu")){
            return awsImage = "ami-0bdb1d6c15a40392c";
        }
        else {
            return awsImage = "ami-0bdb1d6c15a40392c";
        }
    }

    /** TODO
     *
     * @param docId the id of the document to be saved
     * @return
     */
    @Path("/doc/{docId}/savestate")
    @Produces(MediaType.APPLICATION_JSON)
    @POST
    public Response saveStateLegacy( @PathParam("docId") final Long docId ) {
        return saveState( docId, "originalIsHead" );
    }

    /** TODO
     *
     * @param docId
     * @param action
     * @return
     */
    @Path("/doc/{docId}/savestate/{action:originalIsHead|copyIsHead}")
    @Produces(MediaType.APPLICATION_JSON)
    @POST
    public Response saveState( @PathParam("docId") final Long docId, @PathParam("action") final String action ) {

        /*
         * duplicate doc
         */
        StringWriter sw = new StringWriter();
        try ( Transaction tx = db.beginTx() ) {
            JsonGenerator jg = objectMapper.getFactory().createGenerator( sw );
            if (!db.getNodeById(docId).hasLabel(DOC)) throw new org.neo4j.graphdb.NotFoundException();

            db.execute("MATCH (n) WHERE EXISTS(n.tmpId) REMOVE n.tmpId");

            for (String label : new String[]{"DOC", "NET", "NODE"})
                db.execute("MATCH (n:"+label+")<-[:CONTAIN]-(vdoc:DOC) WHERE id(vdoc)="+docId+" CREATE (n2:"+label+") SET n2=n, n2.tmpId=id(n)");
//            db.execute("MATCH (n:DOC)<-[:P2D]-(p:PROJECT) WHERE id(n)="+docId+" CREATE (n2:DOC) SET n2=n, n2.tmpId=id(n) CREATE (n2)<-[:P2D]-(p)");
//            db.execute("MATCH (n:NET)<-[:CONTAIN]-(vdoc:DOC) WHERE id(vdoc)="+docId+" CREATE (n2:NET) SET n2=n, n2.tmpId=id(n)");
//            db.execute("MATCH (n:NODE)<-[:CONTAIN]-(vdoc:DOC) WHERE id(vdoc)="+docId+" CREATE (n2:NODE) SET n2=n, n2.tmpId=id(n)");

            db.execute("MATCH (n)<-[:CONTAIN]-(vdoc:DOC) WHERE id(vdoc)="+docId+
                    " MATCH (n)-[r:LINK]->(m) WHERE id(n)=id(vdoc) OR (n)<-[:CONTAIN]-(vdoc) " +
                    " MATCH (n2{tmpId:id(n)}), (m2{tmpId:id(m)}) " +
                    " CREATE (n2)-[r2:LINK]->(m2) set r2=r " +
                    " ");

            ResourceIterator<Node> rd = db.execute("MATCH (d:DOC) WHERE EXISTS(d.tmpId) " +
                    " MATCH (n) WHERE EXISTS(n.tmpId) " +
                    " CREATE (n)<-[:CONTAIN]-(d) REMOVE n.tmpId RETURN distinct d" ).columnAs("d");

            if (rd.hasNext()) {
                Node dNew = rd.next();
                Node dOld = db.getNodeById(docId);
                if (action.equals("copyIsHead")) {
                    dOld.createRelationshipTo(dNew, REVISION);
                    writeDocTree(jg, dOld, false);
                } else {
                    dOld.getRelationships(REVISION, OUTGOING).forEach(x->{dNew.createRelationshipTo(x.getOtherNode(dOld), REVISION); x.delete();});
                    dOld.getRelationships(REVISION, INCOMING).forEach(x->{x.getOtherNode(dOld).createRelationshipTo(dNew, REVISION); x.delete();});
                    dNew.createRelationshipTo(dOld, REVISION);
                    writeDocTree(jg, dNew, false);
                }
            }

            jg.flush(); jg.close();
            tx.success();
        } catch (org.neo4j.graphdb.NotFoundException e) {
            return Response.status(400).entity( buildJsonMessage("Unable to find doc with id "+docId) ).type( MediaType.APPLICATION_JSON ).build();
        } catch (Exception e) {
            logger.error("Exception while saving state:", e);
            return Response.status(500).entity( buildJsonMessage(e.getMessage()) ).type( MediaType.APPLICATION_JSON ).build();
        }

        return Response.ok().entity( sw.toString() ).type( MediaType.APPLICATION_JSON ).build();
    }


    /** This method returns all shortest paths for a node to all reachable other nodes.
     *
     * @param srcId the id of the node to get all shortest paths from
     * @return
     */
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/shortestPaths/{srcId}")
    @GET
    public Response getAllShortestPaths( @PathParam("srcId") final Long srcId ) {

        /*
         * naive computation of all shortest paths starting from node with id(node)=srcId
         */
        String result = "";
        try ( Transaction tx = db.beginTx() ) {
            if (db.getNodeById(srcId).hasLabel(DOC)) throw new org.neo4j.graphdb.NotFoundException();

            Result r = db.execute(
                " MATCH p=(n1)<-[:LINK*0..]-(:NET)-[:LINK*0..]->(n2) WHERE not n2:DOC AND not n1:DOC AND id(n1)="+srcId +
                " WITH n2, p, reduce(w=0, r IN rels(p) | w+r.delay) AS ddist, " +
                    " reduce(w=0, r IN rels(p) | w+r.delay) AS ddelay, " +
                    " reduce(w=0, r IN rels(p) | w+r.dispersion) AS ddispersion, " +
                    " reduce(w=0, r IN rels(p) | 1-(1-w)*(1-r.loss)) AS dloss, " +
                    " reduce(w=0, r IN rels(p) | 1-(1-w)*(1-r.corrupt)) AS dcorrupt, "+
                    " reduce(w=0, r IN rels(p) | 1-(1-w)*(1-r.duplicate)) AS dduplicate, "+
                    " reduce(w=0, r IN rels(p) | 1-(1-w)*(1-r.reorder)) AS dreorder " +
                " WITH n2, p, ddist, ddelay, ddispersion, dloss, dcorrupt, dduplicate, dreorder ORDER BY n2, ddist ASC " +
                " WITH distinct n2, head(collect(p)) as hp, head(collect(ddist)) as dist, head(collect(ddelay)) as delay, " +
                    " head(collect(ddispersion)) as dispersion, head(collect(dloss)) as loss, head(collect(dcorrupt)) as corrupt, " +
                    " head(collect(dduplicate)) as duplicate, head(collect(dreorder)) as reorder " +
                " RETURN id(n2) as tgtId, extract(x IN nodes(hp) | id(x)) as path, dist, delay, dispersion, round(1000000*loss)/1000000 as loss, " +
                    " round(1000000*corrupt)/1000000 as corrupt, round(1000000*duplicate)/1000000 as duplicate, " +
                    " round(1000000*reorder)/1000000 as reorder "
            );

            result = objectMapper.writeValueAsString(r.stream().toArray());
            tx.success();
        } catch (org.neo4j.graphdb.NotFoundException e) {
            return Response.status(400).entity( buildJsonMessage("Unable to find find net or node with id "+srcId) ).type( MediaType.APPLICATION_JSON ).build();
        } catch (Exception e) {
            logger.error("Exception while getting all shortest paths:", e);
            return Response.status(500).entity( buildJsonMessage(e.getMessage()) ).type( MediaType.APPLICATION_JSON ).build();
        }

        return Response.ok().entity( result ).type( MediaType.APPLICATION_JSON ).build();

    }

    /** This method returns the flavor for the specific edge device representation.
     *
     * @param device the device to be mapped to a flavor
     * @param isOpenStack - true, if provider is OpenStack
     * @return
     * @throws ExceptionInvalidData
     */
    public String getFlavorFromDeviceFile (String device, boolean isOpenStack) throws ExceptionInvalidData {
        String flavor = "";
        int end = device.indexOf("(");
        if (end != -1) {
            device = device.substring(0, end).trim();
        }
        String file = Settings.PATH_TO_OS_FLAVORS;
        if (!isOpenStack) {
            file = Settings.PATH_TO_AWS_FLAVORS;
        }
        JSONParser jsonParser = new JSONParser();
        try {
            Object object = jsonParser.parse(new FileReader(file));
            JSONObject jsonObject = (JSONObject)object;
            if (jsonObject.containsKey(device)){
                Object objectProps = jsonObject.get(device);
                JSONObject jsonProps = (JSONObject)objectProps;
                flavor = (String)jsonProps.get("flavor");
            } else if (device.equals("")){
                flavor = (String)jsonObject.keySet().toArray()[8]; //
            }
            else {
                throw new ExceptionInvalidData("Invalid Flavor chosen (" + device + "). Please select one of " + jsonObject.keySet());
            }
        } catch (IOException e) {
            logger.error("IOException while getting flavor from device:", e);
        } catch (ParseException e) {
            logger.error("ParseException while getting flavor from device:", e);
        }
        return flavor;
    }

    /** This method returns the icon for a specific edge device representation.
     *
     * @param device the device to be mapped to a icon
     * @param isOpenStack true, if provider is OpenStack
     * @return icon name
     * @throws ExceptionInvalidData
     */
    public static String getIconFromDeviceFile (String device, boolean isOpenStack) throws ExceptionInvalidData {
        String icon = "";
        int end = device.indexOf("(");
        if (end != -1) {
            device = device.substring(0, end).trim();
        }
        String file = Settings.PATH_TO_OS_FLAVORS;
        if (!isOpenStack) {
            file = Settings.PATH_TO_AWS_FLAVORS;
        }
        JSONParser jsonParser = new JSONParser();
        try {
            Object object = jsonParser.parse(new FileReader(file));
            JSONObject jsonObject = (JSONObject)object;
            if (jsonObject.containsKey(device)){
                Object objectProps = jsonObject.get(device);
                JSONObject jsonProps = (JSONObject)objectProps;
                icon = (String)jsonProps.get("icon");
            } else if (device.equals("")){
                logger.warn("Unable to find \"" + device + "\" in mapping file.");
                icon = (String)jsonObject.keySet().toArray()[8]; //
            }
            else {
                throw new ExceptionInvalidData("Invalid Icon chosen (" + device + "). Please select one of " + jsonObject.keySet());
            }
        } catch (IOException e) {
            logger.error("IOException while getting icon from device:", e);
        } catch (ParseException e) {
            logger.error("ParseException while getting icon from device:", e);
        }
        return icon;
    }
}
