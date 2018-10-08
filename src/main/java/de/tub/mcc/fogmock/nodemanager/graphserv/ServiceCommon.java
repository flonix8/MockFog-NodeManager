package de.tub.mcc.fogmock.nodemanager.graphserv;


import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.util.*;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.PartialRequestBuilder;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import de.tub.mcc.fogmock.nodemanager.graphserv.agent.ResponseTcConfig;
import de.tub.mcc.fogmock.nodemanager.graphserv.ansible.ResponseAnsible;
import de.tub.mcc.fogmock.nodemanager.graphserv.openstack.ResponseOpenstackConfig;
import org.neo4j.graphdb.*;
import org.neo4j.helpers.collection.MapUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import static org.neo4j.graphdb.Direction.INCOMING;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class ServiceCommon {

    GraphDatabaseService db;
    public static ResponseAnsible responseAnsible = new ResponseAnsible();
    public static ResponseOpenstackConfig openStackConfig = new ResponseOpenstackConfig();
    public static long docIdStatic = -1;
    private static Logger logger = LoggerFactory.getLogger(InfrastructureController.class);
    final Label CONFIG = Label.label( "CONFIG" );
    final Label DOC = Label.label( "DOC" );
	final Label NET = Label.label( "NET" );
	final Label NODE = Label.label( "NODE" );
	static final RelationshipType LINK = RelationshipType.withName( "LINK" );
    static final RelationshipType CONTAIN = RelationshipType.withName( "CONTAIN" );
    static final RelationshipType REVISION = RelationshipType.withName( "REVISION" );


	protected ObjectMapper objectMapper = new ObjectMapper();

    public ServiceCommon(@Context GraphDatabaseService graphDb) {
        this.db = graphDb;
    }

    // default baseURI
    protected URI getBaseURI() {
        return UriBuilder.fromUri("http://localhost:5555/").build();
    }

    // custom baseURI
    protected URI getBaseURI(String host, int port) {
        return UriBuilder.fromUri("http://" + host + ":" + port + "/").build();
    }


    /** This method returns the relationship by the given source and destination node and the relationship type (either LINK, CONTAIN, etc.).
     *
     * @param from the id of the source node of the requested relationship
     * @param to the id of the destination node of the requested relationship
     * @param typeR the type of the relationship to be requested
     * @return
     * @throws ExceptionInvalidData
     */
    public Relationship getRelationship(Node from, Node to, RelationshipType typeR) throws ExceptionInvalidData {
        for ( Relationship r : from.getRelationships(typeR, OUTGOING) ) {
            if ( r.getOtherNode(from).getId() == to.getId() ) {
                return r;
            }
        }
        throw new ExceptionInvalidData("relationship not found:  from id " + from.getId() + "  to id " + to.getId());
    }
    public Relationship getRelationship(long idFrom, long idTo, RelationshipType typeR) throws ExceptionInvalidData {
        return getRelationship(db.getNodeById(idFrom), db.getNodeById(idTo), typeR);
    }

    /** This method checks if a property of a node exists and returns a boolean.
     *
     * @param n the node object to get the property from
     * @param name the name of the property to be checked
     * @param nullVal
     * @return
     */
	public boolean existsProperty(Node n, String name, String nullVal) {
        if (!n.hasProperty(name) || n.getProperty(name)==null || ((String)n.getProperty(name)).equals("") ) {
            return false;
        } else {
            return true;
        }
    }

    /* ***************************************************************************************
     *
     *          N4J ==> JSON
     *
     ****************************************************************************************/

    public void writeNeo4jProps(JsonGenerator jg, Map<String, Object> propMap) throws IOException {
        for (Map.Entry<String, Object> e : propMap.entrySet() ) {
            if (!e.getKey().startsWith("_")) {
                jg.writeFieldName(e.getKey());
                jg.writeObject(e.getValue());
            }
        }
    }

    /** This method returns a JSON representation of a node requesting all relevant properties from the db.
     *
     * @param jg the JsonGenerator to write the output
     * @param n the node object to be represented
     * @throws IOException
     */
	public void writeVertex(JsonGenerator jg, Node n) throws IOException {

        jg.writeFieldName( String.valueOf(n.getId()) );
        jg.writeStartObject();
        writeNeo4jProps(jg, n.getAllProperties());
        if (n.hasLabel(NODE)) {
            try {
                jg.writeStringField("icon", ServiceUserInterface.getIconFromDeviceFile((String) n.getProperty("flavor"), false));
            } catch (ExceptionInvalidData e) {
                logger.error("unable to add icon field to json:", e);
            }
        }

        Relationship tmpMgmtEdge = null;

        jg.writeFieldName( "edgesBack" );
        jg.writeStartObject();
            for (Relationship r : n.getRelationships(INCOMING, LINK)) {
                if ( r.getOtherNode(n).hasLabel(DOC) ) {
                    tmpMgmtEdge = r;
                }
                if ( !(r.getOtherNode(n).hasLabel(NET) || r.getOtherNode(n).hasLabel(NODE)) ) continue;

                jg.writeFieldName( String.valueOf( r.getOtherNode(n).getId() ) );
                jg.writeStartObject();
                    jg.writeFieldName("idEdge");
                    jg.writeObject( String.valueOf(r.getId()) );
                    writeNeo4jProps(jg, r.getAllProperties());

                jg.writeEndObject();
            }
        jg.writeEndObject();

        if (tmpMgmtEdge != null) {
            writeNeo4jProps(jg, tmpMgmtEdge.getAllProperties());
        }
        jg.writeEndObject();

	}

    /** This method returns a JSON representation of one document (or topology).
     *  By the given param deep, it is possible to show also the nets, nodes, edges and their properties.
     *
     * @param jg the JsonGenerator to write the output
     * @param n the document node to be represented
     * @param deep the param for selecting if the representation details should be shown
     * @throws IOException
     */
    public void writeDoc(JsonGenerator jg, Node n, boolean deep) throws IOException {

        jg.writeStartObject();
            writeNeo4jProps(jg, n.getAllProperties());
            jg.writeFieldName( "id" );
            jg.writeNumber( String.valueOf(n.getId()) );

            if (deep) {
                jg.writeFieldName( "allNets" );
                jg.writeStartObject();
                    for (Relationship r : n.getRelationships(OUTGOING, CONTAIN)) {
                        Node vertex = r.getOtherNode(n);
                        if (vertex.hasLabel(NET)) {
                            writeVertex(jg, vertex);
                        }
                    }
                jg.writeEndObject();

                jg.writeFieldName( "allNodes" );
                jg.writeStartObject();
                    for (Relationship r : n.getRelationships(OUTGOING, CONTAIN)) {
                        Node vertex = r.getOtherNode(n);
                        if (vertex.hasLabel(NODE)) {
                            writeVertex(jg, vertex);
                        }
                    }
                jg.writeEndObject();
            } else {
                int countNodes = 0;
                int countNets = 0;
                for (Relationship r : n.getRelationships(OUTGOING, CONTAIN)) {
                    if (r.getOtherNode(n).hasLabel(NODE)) countNodes+=1;
                    if (r.getOtherNode(n).hasLabel(NET)) countNets+=1;
                }
                jg.writeFieldName( "countNodes" );
                jg.writeNumber( countNodes );
                jg.writeFieldName( "countNets" );
                jg.writeNumber( countNets );
            }

        jg.writeEndObject();

    }

    /** This method generates a JSON representation of the documents and their hierarchical relationships in a tree representation.
     *
     * @param jg the JsonGenerator to write the output
     * @param root the root node to get the representation starting from
     * @param deep the param for selecting if the representation details should be shown
     * @throws IOException
     */
    public void writeDocTree(JsonGenerator jg, Node root, boolean deep) throws IOException {
//        while (root.hasRelationship(REVISION, INCOMING)) {
//            root = root.getSingleRelationship(REVISION, INCOMING).getOtherNode(root);
//        }
        Map<String, List<String>> tMap = new HashMap<>();
        jg.writeStartObject();
            jg.writeFieldName( "allDocs" );
            jg.writeStartArray();
                writeDocTreeHelper(jg, root, tMap, 0, deep);
            jg.writeEndArray();
            jg.writeFieldName( "tMap" );
            objectMapper.writeValue(jg, tMap);  //objectMapper.convertValue(resMap.get("tcConfig"), ResponseTcConfig.class))
        jg.writeEndObject();
    }
    private int writeDocTreeHelper(JsonGenerator jg, Node d, Map<String, List<String>> tMap, int arrPos, boolean deep) throws IOException {
        String docPos = String.valueOf(arrPos);
        writeDoc(jg, d, deep);

        List<String> list = new LinkedList<String>();

        for (Relationship r : d.getRelationships(OUTGOING, REVISION)) {
            arrPos++;
            list.add( String.valueOf(arrPos) );
            arrPos = writeDocTreeHelper(jg, r.getOtherNode(d), tMap, arrPos, deep );
        }
        tMap.put( docPos, list );
        return arrPos;
    }



    /* ***************************************************************************************
     *
     *          Node Agent Communication
     *
     ****************************************************************************************/

    public void getAdjListsAndSendToNA(JsonGenerator jg, Long docId, String edgeLabel, String naMethod) throws Exception {
        gatherAdjLists(jg, docId, edgeLabel, naMethod);
    }
    public void getAdjLists(JsonGenerator jg, Long docId, String edgeLabel) throws Exception {
        gatherAdjLists(jg, docId, edgeLabel, "none");
    }
    public void gatherAdjLists(JsonGenerator jg, Long docId, String edgeLabel, String naMethod) throws Exception {
        if (edgeLabel == null){
            throw new Exception("Edge Label undefined.");
        }

        // for the case of not using the ResponseTcConfig Class but directly querying the JSON from Neo4j
        Result tcConfigs = db.execute(
                "MATCH (vdoc:DOC)-[mgmtEdge:LINK]->(no:NODE)<-[eno:LINK]-(:NET) WHERE id(vdoc)="+docId+" AND (no)-[:"+edgeLabel+"]-() " +
                        " OPTIONAL MATCH (no)<-[r1:"+edgeLabel+"]-(:NODE)<-[eni1:LINK]-(:NET) " + // in case of incoming ADJ edges consider out_rate
                        " WITH no, eno, mgmtEdge.addr as mgmtIp, collect(r1 {in_rate:r1.in_rate+'kbps', out_rate:r1.out_rate+'kbps', dispersion:r1.dispersion+'ms', delay:r1.delay+'ms', .loss, .corrupt, .duplicate, .reorder, dst_net:eni1.addr}) as ru1 " +
                        " OPTIONAL MATCH (no)-[r2:"+edgeLabel+"]->(:NODE)<-[eni2:LINK]-(:NET) " + // in case of outgoing ADJ edges consider in_rate
                        " WITH mgmtIp, { in_rate:eno.in_rate+'kbps', out_rate:eno.out_rate+'kbps', rules:ru1+collect(r2 {in_rate:r2.out_rate+'kbps', out_rate:r2.in_rate+'kbps', dispersion:r2.dispersion+'ms', delay:r2.delay+'ms', .loss, .corrupt, .duplicate, .reorder, dst_net:eni2.addr}) } as tcConfig " +
                        " RETURN mgmtIp, tcConfig, size(tcConfig.rules) as countRules " +
                        " ");

        ClientConfig config = new DefaultClientConfig();
        Client client = Client.create(config);

        if (jg != null) jg.writeStartObject();
        while ( tcConfigs.hasNext() ) {
            Map<String,Object> resMap = tcConfigs.next(); //tcConfig for one agent with mgmtIp...
            if (resMap.get("countRules")==null || (long)resMap.get("countRules")==0) continue;
            String ipMgmt = resMap.get("mgmtIp").toString();

            /*
             * Nodeagent communication
             */
            ClientResponse resp = null;
            if (naMethod.toLowerCase().equals("post")) {
                logger.info("CALLING AGENT FIREWALL " + getBaseURI(ipMgmt, 5000).toString());
                resp = client.resource(getBaseURI(ipMgmt, 5000))
                        .path("api").path("firewall/").accept(MediaType.TEXT_PLAIN).type(MediaType.APPLICATION_JSON)
                        .post(ClientResponse.class, MapUtil.map("active", true));
                if (resp != null && resp.getStatus() != 200) {
                    throw new Exception("Agent Firewall POST call to "+getBaseURI(ipMgmt, 5000)+
                            " returned with code "+resp.getStatus()+": "+resp.getEntity(String.class));
                }

                logger.info("CALLING AGENT POST " + getBaseURI(ipMgmt, 5000).toString());
                resp = client.resource(getBaseURI(ipMgmt, 5000))
                        .path("api").path("tc-config/").accept(MediaType.TEXT_PLAIN).type(MediaType.APPLICATION_JSON)
                        .post(ClientResponse.class, objectMapper.writeValueAsString(resMap.get("tcConfig")));
            }
            if (naMethod.toLowerCase().equals("put")) {
                logger.info("CALLING AGENT PUT " + getBaseURI(ipMgmt, 5000).toString());
                resp = client.resource(getBaseURI(ipMgmt, 5000))
                        .path("api").path("tc-config/").accept(MediaType.TEXT_PLAIN).type(MediaType.APPLICATION_JSON)
                        .put(ClientResponse.class, objectMapper.writeValueAsString(resMap.get("tcConfig")));
            }
            if (resp != null && resp.getStatus() != 200) {
                throw new Exception("Agent call to "+getBaseURI(ipMgmt, 5000)+
                        " returned with code "+resp.getStatus()+": "+resp.getEntity(String.class));
            }

            if (jg != null) jg.writeFieldName( ipMgmt );
            if (jg != null) jg.writeRawValue( objectMapper.writeValueAsString(resMap.get("tcConfig")) );
        }
        if (jg != null) jg.writeEndObject();

    }


    /* ***************************************************************************************
     *
     *          Integrity Checks
     *
     ****************************************************************************************/

    private void cycleHelper(Node node, HashSet<Long> visited) {
        //wait if there exists an unvisited node behind an outgoing link (appearance of the property tmpId)
        for (Relationship r : node.getRelationships(OUTGOING, LINK)) {
            if (!visited.contains( r.getOtherNode(node).getId() )) return;
        }

        //visit node
        visited.add(node.getId());

        //move on to the next nodes
        for (Relationship r : node.getRelationships(INCOMING, LINK)) {
            cycleHelper(r.getOtherNode(node), visited);
        }
    }

    public void checkIntegrityCycle(long docId) throws ExceptionInvalidData {
        // choose all leaf nodes for traversing from outside in
        final HashSet<Long> hashSet = new HashSet<Long>();
        final HashSet<Long> allSet = new HashSet<Long>();
        ResourceIterator<Node> r = db.execute("MATCH (doc:DOC)-[:CONTAIN]->(n) " +
                " WHERE id(doc)="+docId+" AND NOT (n)-[:LINK]->() RETURN n").columnAs("n");
        r.forEachRemaining( n -> cycleHelper( n, hashSet ) );

        // check for unvisited net nodes
        r = db.execute("MATCH (d:DOC)-[:CONTAIN]->(n) " +
                " WHERE id(d)="+docId+" AND id(n)<>id(d) AND not n:DOC RETURN n").columnAs("n");
        if ( !r.stream().allMatch(x->hashSet.contains(x.getId())) ) {
            throw new ExceptionInvalidData("illegal graph: found cyclic linked nets ");
        }
    }

    public void checkIntegrityNodeHasOutgoingLinks(long docId) throws ExceptionInvalidData {
        //no nodes as link sources
        ResourceIterator<Node> r = db.execute("MATCH (d:DOC)-[:CONTAIN]->(n:NODE)-[:LINK]->() WHERE id(d)="+docId+" RETURN n").columnAs("n");
        if (r.hasNext()) throw new ExceptionInvalidData("illegal outgoing link occurred at node with name " + r.next().getProperty("name"));
    }

    public void checkIntegrityLinksBetweenDocs(long docId) throws ExceptionInvalidData {
        //no links between elements of different documents
        ResourceIterator<Node> r = db.execute("MATCH (d1:DOC)-[:CONTAIN]->(n1)-[:LINK]-(n2)<-[:CONTAIN]-(d2:DOC) " +
                " WHERE id(d1)<>id(d2) and id(d1)="+docId+" RETURN n1").columnAs("n1");
        if (r.hasNext()) throw new ExceptionInvalidData("illegal inter-document link occurred at vertex with name " + r.next().getProperty("name"));
    }

    public void checkAnsibleIntegrityNodeLinkedToMultipleNets(long docId) throws ExceptionInvalidData {
        //no nodes which are connected to multiple nets
        ResourceIterator<Node> r = db.execute("MATCH (d:DOC)-[:CONTAIN]->(n:NODE), (ne1:NET)-[:LINK]->(n)<-[:LINK]-(ne2:NET) " +
                " WHERE id(d)="+docId+" AND id(ne1)<>id(ne2) RETURN distinct n").columnAs("n");
        if (r.hasNext()) throw new ExceptionInvalidData("illegal links to multiple nets at node with name " + r.next().getProperty("name"));
    }

    public void checkAnsibleIntegrityNetNameNotUniqueOrMissing(long docId) throws ExceptionInvalidData {
        //no nets with same name (inkl. doc)
        ResourceIterator<Node> r = db.execute("MATCH (n1)<-[:CONTAIN]-(d:DOC)-[:CONTAIN]->(n2) " +
                " WHERE id(d)="+docId+" AND id(n1)<>id(n2) AND (n1.name=n2.name or n1.name='') RETURN distinct n1 " +
                " ").columnAs("n1");
        if (r.hasNext()) throw new ExceptionInvalidData("encountered net with non-unique or missing name: " + r.next().getProperty("name") + "  " + r.next().getId() );
    }

    public void checkIpNodeLacksMgmtEdge(long docId) throws ExceptionInvalidData {
        //no nodes without ip
        ResourceIterator<Node> r = db.execute("MATCH (n:NODE)<-[:CONTAIN]-(d:DOC) " +
                " WHERE id(d)="+docId+" AND not (n)<-[r:LINK]-(d) RETURN n ").columnAs("n");
        if (r.hasNext()) throw new ExceptionInvalidData("encountered node without mgmt ip: "+r.next().getId());
    }

    public void checkIpNodeIpMissing(long docId) throws ExceptionInvalidData {
        //no nodes without ip
        ResourceIterator<Node> r = db.execute("MATCH (n:NODE)<-[r:LINK]-(dn)<-[:CONTAIN]-(d:DOC) " +
                " WHERE id(d)="+docId+" AND (dn:NET OR dn:DOC) AND (not exists(r.addr) OR r.addr='0.0.0.0/0') RETURN n ").columnAs("n");
        if (r.hasNext()) throw new ExceptionInvalidData("encountered node without ip: "+r.next().getId());
    }

    public void checkIpNodeIpOutOfSubnetRange(long docId) throws ExceptionInvalidData {
        //Check if node ips are in their corresponding subnet
        Result res = db.execute("MATCH (dn)<-[:CONTAIN]-(d:DOC) WHERE id(d)="+docId+" AND dn:NET OR dn:DOC " +
                " MATCH (no:NODE)<-[r:LINK]-(dn) WHERE exists(r.addr) AND exists(dn.addr) RETURN dn.addr as netaddr, r.addr as nodeaddr ");
        while (res.hasNext()) {
            Map<String, Object> cur = res.next();
            ModelIp netIp = new ModelIp( (String)cur.get("netaddr") );
            ModelIp nodeIp = new ModelIp( (String)cur.get("nodeaddr") );
            //if ( nodeIp.intMask == 0 && nodeIp.intIp == 0) continue; // don't check zero ips
            if ( nodeIp.getNetworkPart(netIp.intMask) != netIp.getNetworkPart() ) {
                throw new ExceptionInvalidData("Illegal edge ip "+nodeIp.getFullIp()+":  network part mismatches subnet "+netIp.getNetworkIp());
            }
        }
    }

    public void checkIpNodeIpRealmCollision(long docId) throws ExceptionInvalidData {
        //no realm collisions between net ips (inkl. mgmt net)
        Result res = db.execute("MATCH (d:DOC)-[:CONTAIN]->(net:NET) WHERE id(d)="+docId+" " +
                " MATCH (n1:NODE)<-[r1:LINK]-(net)-[r2:LINK]->(n2:NODE) WHERE id(n1)<>id(n2) " +
                " RETURN r1.addr as ip1, r2.addr as ip2 ");
        while (res.hasNext()) {
            Map<String, Object> cur = res.next();
            ModelIp ip1 = new ModelIp( (String)cur.get("ip1") );
            ModelIp ip2 = new ModelIp( (String)cur.get("ip2") );
            if ( ip1.getNetworkPart() == ip2.getNetworkPart(ip1.intMask) ) {
                throw new ExceptionInvalidData("Found colliding node ips: " + ip1.getFullIp() + "  and  " + ip2.getFullIp() );
            }
        }
    }

    public void checkIpNetIpRealmCollision(long docId) throws ExceptionInvalidData {
        //no realm collisions between net ips (inkl. mgmt net)
        Result res = db.execute("MATCH (dn1)<-[:CONTAIN]-(d:DOC)-[:CONTAIN]->(dn2) " +
                " WHERE id(d)="+docId+" AND id(dn1)<>id(dn2) AND ((dn1:DOC) OR (dn1:NET AND (dn1)-[:LINK]->(:NODE))) AND ((dn2:DOC) OR (dn2:NET AND (dn2)-[:LINK]->(:NODE))) " +
                " RETURN dn1.addr as ip1, dn2.addr as ip2 ");
        while (res.hasNext()) {
            Map<String, Object> cur = res.next();
            ModelIp ip1 = new ModelIp( (String)cur.get("ip1") );
            ModelIp ip2 = new ModelIp( (String)cur.get("ip2") );

            if ( ip1.getNetworkPart() == ip2.getNetworkPart(ip1.intMask) ) {
                throw new ExceptionInvalidData("Found colliding net ips: " + ip1.getFullIp() + "  and  " + ip2.getFullIp() );
            }
        }
    }




/* ******************************************************************************************************************
 *
 *           Propagation
 *
 *******************************************************************************************************************/

    public void startPropagation(final Long docId, final String edgeLabel) {

        Map<String, Object> params;

        //remove old adjacency links (LADJ)
        db.execute("MATCH ()-[r:"+ edgeLabel +"]-() DELETE r");

        //create reflexive adjacency information on each node
        params = MapUtil.map("docId", docId, "props", ModelEdge.MIN_DISTANT_EDGE.props);
        db.execute("MATCH (d:DOC)-[:CONTAIN]->(n) WHERE id(d)=$docId AND (n:NET OR n:NODE) CREATE (n)-[r:"+ edgeLabel +"]->(n) SET r=$props RETURN n", params);

        //propagate from leaf nodes - PHASE1
        ResourceIterator<Node> leafs = db.execute("MATCH (d:DOC)-[:CONTAIN]->(n) WHERE id(d)=$docId AND (n:NET OR n:NODE) AND NOT (n)-[:LINK]->() RETURN n", params ).columnAs("n");
        leafs.forEachRemaining( n -> propagate( 1, OUTGOING, INCOMING, n, RelationshipType.withName(edgeLabel)) );

        //propagate from root nodes - PHASE2
        ResourceIterator<Node> roots = db.execute("MATCH (d:DOC)-[:CONTAIN]->(n) WHERE id(d)=$docId AND (n:NET OR n:NODE) AND NOT (n)<-[:LINK]-(:NET) RETURN n", params ).columnAs("n");
        roots.forEachRemaining( n -> propagate( 2, INCOMING, OUTGOING, n, RelationshipType.withName(edgeLabel) ) );

        //delete reflexive adjacency information on each node
        db.execute("MATCH (d:DOC)-[:CONTAIN]->(n)-[r:"+ edgeLabel +"]->(n) WHERE id(d)=$docId AND (n:NET OR n:NODE) DELETE r", params);
    }


    private void propagate(int phase, Direction dCheck, Direction dNext, Node node, RelationshipType edgeType) {

        //wait if there exists an unvisited node behind an outgoing link (appearance/absence of the property _marker)
        for (Relationship r : node.getRelationships(LINK, dCheck)) {
            Node other = r.getOtherNode(node);
            if ( !(other.hasLabel(NET) || other.hasLabel(NODE)) ) continue;
            if ( phase == 1 && !r.getOtherNode(node).hasProperty("_marker") ) return;
            if ( phase == 2 &&  r.getOtherNode(node).hasProperty("_marker") ) return;
        }

        //visit node
        if (phase == 1) {
            node.setProperty("_marker", "true");
        } else {
            node.removeProperty("_marker");
        }

        //walk through the node's LINK predecessors
        for (Relationship r : node.getRelationships(LINK, dCheck)) {
            Node other = r.getOtherNode(node); // current predecessor of LADJ information flow
            if ( !(other.hasLabel(NET) || other.hasLabel(NODE)) ) continue;
            if ( r.getProperty("cancelled") != null && r.getProperty("cancelled").equals(true) ) continue;
            if ( other.getProperty("cancelled") != null && other.getProperty("cancelled").equals(true) ) continue;
            if ( node.getProperty("cancelled") != null && node.getProperty("cancelled").equals(true) ) continue;

            //merge predecessor's (other's) LADJs into this node (node)
            for ( Relationship srcAdj : other.getRelationships(edgeType, OUTGOING) ) { //just consider OUTGOING => behaves like inner-node adjacency lists (break link symmetry)
                //load node's corresponding prior LADJ (tgtAdj) - create if not exists
                Relationship tgtAdj = getOrCreateAdjRel( srcAdj.getOtherNode(other), node , edgeType);

                // we just need to consider LADJs which start at node
                // otherwise, we would mess up in_rate/out_rate asymmetry
                if ( tgtAdj.getStartNodeId() == node.getId() ) {
                    //merge properties of srcAdj+r into tgtAdj
                    getMin( addMaps(srcAdj.getAllProperties(), r.getAllProperties(), phase)  ,  tgtAdj.getAllProperties() ).entrySet()
                            .forEach( x->tgtAdj.setProperty(x.getKey(), x.getValue()) );
                }

            }

        }

        //move to the next nodes
        for (Relationship r : node.getRelationships(LINK, dNext)) {
            if ( !(r.getOtherNode(node).hasLabel(NET) || r.getOtherNode(node).hasLabel(NODE)) ) continue;
            propagate(phase, dCheck, dNext, r.getOtherNode(node), edgeType);
        }
    }

    private Relationship getOrCreateAdjRel(Node from, Node to, RelationshipType edgeType) {
        for ( Relationship r: to.getRelationships(edgeType) ) {
            if ( r.getOtherNode(to).equals(from) ) return r;
        }
        Relationship rNew = to.createRelationshipTo(from, edgeType);
        ModelEdge.MAX_DISTANT_EDGE.props.entrySet()
                .forEach(x->rNew.setProperty(x.getKey(), x.getValue()) );
        return rNew;
    }

    private Map<String, Object> addMaps(Map<String, Object> m1, Map<String, Object> m2, int phase) {
        for (Map.Entry<String, Object> e : m1.entrySet()) {
            if ( e.getKey().equals("delay") || e.getKey().equals("dispersion"))
                m2.merge(e.getKey(), e.getValue(), (v1, v2) -> ((long)v1) + ((long)v2));
            if ( e.getKey().equals("loss") || e.getKey().equals("corrupt") || e.getKey().equals("duplicate") || e.getKey().equals("reorder"))
                m2.merge(e.getKey(), e.getValue(), (v1, v2) -> round(((double)1-(1-(double)v1)*((double)1-(double)v2)), 6));
        }
        long i2=(long)m2.get("in_rate"); long o2=(long)m2.get("out_rate");
        long i1=(long)m1.get("in_rate"); long o1=(long)m1.get("out_rate");

        // In phase2, LINK/LADJ edges have to be traversed in different order: ()<-[LINK]-()-[LADJ]->()
        // In phase1, LINK/LADJ edges have to traversed in same order: ()<-[LINK]-()<-[LADJ]-()
        m2.put( "in_rate" , Long.min(phase==1 ? i2 : o2, i1) );
        m2.put( "out_rate" , Long.min(phase==1 ? o2 : i2, o1) );

        return m2;
    }

    private Map<String, Object> getMin(Map<String, Object> m1, Map<String, Object> m2) {
        long m1Val = ((long) m1.get("delay"));
        long m2Val = ((long) m2.get("delay"));
        if (m1Val <= m2Val) return m1;
        return m2;
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    public String buildJsonMessage(String message) {
        StringWriter sw = new StringWriter();
        try {
            JsonGenerator jg = objectMapper.getFactory().createGenerator( sw );
            jg.writeStartObject();
            jg.writeFieldName("msg");
            jg.writeString(message);
            jg.flush(); jg.close();
        } catch (IOException e) {
            logger.error("IOException while building JSON message: ", e);
        }
        return sw.toString();
    }


}
