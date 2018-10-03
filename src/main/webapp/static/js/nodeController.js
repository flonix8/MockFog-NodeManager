var hostIP = window.location.hostname;
if (hostIP == "") {
    hostIP = "localhost";
}
var HOST_URL = "http://" + hostIP + "/";
var BASE_URL = "http://" + hostIP + ":7474/webapi/"; // ../{docid}/subnet|node
var DOCID;
var DOCNAME = "mgmt";
var SERVER_CRED = new Object();
var SERVER_CRED_AWS = new Object();

/**
 * Create a new profile for saving the network topologie
 * in this content we create a new docid from the docname
 */
function callDocPOST() {
    var requestJSON = new Object();
    requestJSON.name = DOCNAME;
    requestJSON.addr = "192.168.100.0/24";
    $.ajax({
        url: BASE_URL + "doc",
        type: 'POST',
        contentType: 'application/json;charset=utf-8',
        data: JSON.stringify(requestJSON),
        dataType: 'json',
        success: function(box) {
            DOCID = box.id;
            console.log("DocId=:" + DOCID);
            document.getElementById('docIdDebugging').innerText = 'Current document: ' + DOCID;
        }
    });
}
/**
 * This function returns the Docs with id
 * http://127.0.0.1:7474/webapi/doclist
 * @param id of the document
 * @return json
 */
function callDocGET(id){
    return $.ajax({
        url: BASE_URL + "doc" + "/"+id,
        async: false,
        dataType: 'json',
        type: 'GET',
        success: function(response) {
            return response;
        }, error: function(error)  {
            console.log(error);
            return error;
        }
    });
}
/**
 * This function returns all Docs with id and name
 * http://127.0.0.1:7474/webapi/doclist
 * @return json
 */
function callDocsGET(){
    $.ajax({
        url: BASE_URL + "doc",
        type: 'GET',
        success: function(response) {
            console.log(response);
            return response;
        }, error: function(error)  {
            console.log(error);
            return error;
        }
    });
}
/**
 * This function makes a API call to create a device(node) and consumes JSON obj.
 *
 * @param data
 * @param callback
 */
function callNodePOST(inputdata){
    var node = new Object();
    node.flavor = inputdata.group; //m1.smaller | m1.small
    node.image = inputdata.image;
    var l = getRandomLabel();
    node.name = l;
    $.ajax({
        type: "POST",
        async: false,
        url: BASE_URL + "doc/"+ DOCID + "/node",
        contentType: 'application/json;charset=utf-8',
        data: JSON.stringify(node),
        dataType: 'json',
        success: function(data){
            inputdata.id = Object.keys(data)[0];
            // draw the the node into the diagram
            console.log(data);
            console.log(inputdata);
            // add the Node to vis.js
            // nodes.add({id:inputdata.id,group:inputdata.group,label:"undefined"});
            nodes.add({x:inputdata.x,y:inputdata.y,"id":inputdata.id,"group":inputdata.icon,"label": l,"flavor":node.flavor,"image":inputdata.image,"name":node.name});
        },
        error: function(error)
        {
            console.log(error);
        }
    });
}
/**
 * This function makes a API call to edit a device(node) and consumes JSON obj.
 *
 * @param data
 * @param callback
 */
function callNodePUT(inputdata){
    var node = new Object();
    node.flavor = inputdata.group; //m1.smaller | m1.small
    node.image = inputdata.image;
    node.name = inputdata.name;
    node.id = inputdata.id;

    return $.ajax({
        type: "PUT",
        url: BASE_URL + "doc/"+ DOCID + "/node/" + inputdata.id,
        contentType: 'application/json;charset=utf-8',
        async: false,
        data: JSON.stringify(node),
        dataType: 'json',
        error: function(error)
        {
            console.log(error);
        }
    });
}

/**
 * This function makes a API call to disable a device(node) from the netzwork. Means set delay +10000 sec. etc.
 * http://localhost:7474/webapi/assign/firewall/doc/{docid}/node/{nodeid}
 */
function callNodeFirewallPOST(nodeID){
    var node = new Object();
    $.ajax({
        type: "POST",
        url: BASE_URL + "assign/firewall/doc/"+ DOCID + "/node/"+nodeID,
        contentType: 'application/json',
        data: JSON.stringify(node),
        dataType: 'json',
        success: function(data){
            console.log(data);
            // set text
            document.getElementById("deactivateNode").innerHTML =
                "<i class='fas fa-plug'></i>Reachable";
            // set the node in visualisation to red
            setVertexDisabled(nodeID);
            // don't show windo anymore!
            closePopUp();
        },
        error: function(error)
        {
            console.log(error);
        }
    });

}

/**
 * This function makes a API call to enable a device(node) from the network.
 * DELETE http://localhost:7474/webapi/assign/firewall/doc/{docid}/node/{nodeid}
 */
function callNodeFirewallDELETE(nodeID){
    var node = new Object();
    $.ajax({
        type: "DELETE",
        url: BASE_URL + "assign/firewall/doc/"+ DOCID + "/node/"+nodeID,
        dataType: 'json',
        success: function(data){
            document.getElementById("deactivateNode").innerHTML = "<i class='fas fa-ban'></i>Unreachable";
            setVertexEnabled(nodeID);
        },
        error: function(error)
        {
            console.log(error);
        }
    });

}

/**
 * This function creates a new network in neo4j database
 *
 * @param data
 * @param callback
 */
function callNetPOST(inputdata){
    var net = new Object();
    net.name =  inputdata.name;
    net.addr = inputdata.ip;
    $.ajax({
        type: "POST",
        url: BASE_URL + "doc/"+ DOCID + "/net",
        contentType: 'application/json;charset=utf-8',
        data: JSON.stringify(net),
        dataType: 'json',
        success: function(data){
            inputdata.id = Object.keys(data)[0];
            console.log(inputdata);
            // draw the the node into the diagram
            nodes.add({x:-200,y:-200,id:inputdata.id,group:inputdata.group,label:inputdata.name+"\n"+inputdata.ip,"name":inputdata.name,"addr":inputdata.ip});
        },
        error: function(error)
        {
            console.log(error);
        }
    });
}

/**
 * add a edge between two nodes
 * @param inputdata
 * @param callback
 */
function callEdgePOST(inputdata, callback){
    var nodeFromId = inputdata.from;
    var nodeToId = inputdata.to;
    $.ajax({
        type: "POST",
        url: BASE_URL + "doc/" + DOCID + "/edge/" + nodeFromId + "/" + nodeToId,
        dataType: 'json',
        success: function(data) {
            // draw the the node into the canvas
            callback(inputdata);
            console.log(data);
        },
        error: function(error) {
            console.log(error);
            document.getElementById("alertContent").innerHTML = "Creating this edge is (currently) impossible. See log for details.";
            $('#modelAlert').modal('toggle');
        }
    });
}
/**
 * delete a edge between two nodes
 * "/doc/{docId}/edge/{nodeFromId}/{nodeToId}"
 * @param edgeObj
 */
function callEdgeDELETE(edgeObj){
    $.ajax({
        type: "DELETE",
        url: BASE_URL + "doc/"+ DOCID + "/edge/"+edgeObj.from+"/"+edgeObj.to,
        contentType: 'application/json',
        dataType: 'Text',
        success: function(data){
            // console.log(data);
            //remove the edge on the vis.js
            edges.remove(edgeObj.id);
        },
        error: function(error) {
            console.log(error);
        }
    });
}
function callEdgePUT(fromID,toID,newEdgeProperties){
    return $.ajax({
        type: "PUT",
        url: BASE_URL + "doc/"+ DOCID + "/edge/" + fromID + "/" + toID,
        async: false,
        contentType: 'application/json;charset=utf-8',
        data: JSON.stringify(newEdgeProperties),
        success: function(response) {
            return response;
        }, error: function(error)  {
            console.log(error);
            return error;
        }
    });
}
/**
 * This function delets the nodes
 * /doc/{docId}/vertex/{id}
 * @param id from node
 */
function callVertexDELETE(elementID) {
    $.ajax({
        type: "DELETE",
        url: BASE_URL + "doc/"+ DOCID + "/vertex/" + elementID,
        contentType: 'application/json',
        dataType: 'Text',
        success: function(data){
            // Delete Request to neo4J
            console.log(data);
            //Delete the node in vis.js
            nodes.remove(elementID);
        },
        error: function(error) {
            console.log(error);
        }
    });
}


function queryStringFunction() {
    // This function is anonymous, is executed immediately and
    // the return value is assigned to QueryString!
    var query_string = {};
    var query = window.location.search.substring(1);
    var vars = query.split("&");
    for (var i=0;i<vars.length;i++) {
        var pair = vars[i].split("=");
        // If first entry with this name
        if (typeof query_string[pair[0]] === "undefined") {
            query_string[pair[0]] = decodeURIComponent(pair[1]);
            // If second entry with this name
        } else if (typeof query_string[pair[0]] === "string") {
            var arr = [ query_string[pair[0]],decodeURIComponent(pair[1]) ];
            query_string[pair[0]] = arr;
            // If third or later entry with this name
        } else {
            query_string[pair[0]].push(decodeURIComponent(pair[1]));
        }
    }
    if (Number.isInteger(parseInt(query_string.docId))) {
        DOCID = query_string.docId;
    } else {
        var userEnteredID = prompt("No document provided. Please enter the Document ID.","");
        DOCID = userEnteredID;
    }
}

