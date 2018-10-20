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
