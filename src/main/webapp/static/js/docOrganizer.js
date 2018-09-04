var hostIP = window.location.hostname;
if (hostIP == "") {
    hostIP = "localhost";
}
var BASE_URL = "http://" + hostIP + ":7474/webapi/"; // ../{docid}/subnet|node
var NGINX_URL = "http://" + hostIP + "/";
window.element = null;

window.mouse = {
    startX: 0,
    startY: 0
};

window.ctrlPressed = false;

var nodesArray, nodes, edgesArray, edges, network;
var options = {
    width: (window.innerWidth) + "px",
    height: (window.innerHeight) + "px",
    nodes: {
        borderWidth: 1,
        color: {
            border: '#0065ff',
            background: '#9bcbff',
            highlight: {
                border: 'red',
                background: '#ff6b6b'
            }
        },        
        scaling: {
            min: 16,
            max: 32
        }
    },
    physics: {
        enabled: true,
        minVelocity: 0.2,
        maxVelocity: 2.75,
        timestep: 1,
        solver: "barnesHut",
//        solver: "forceAtlas2Based",
        forceAtlas2Based: {
            centralGravity: 0.01,
            springLength: 100,
            damping: 0.10,
            avoidOverlap: .5
        }
    },
    edges: {
        color: 'black',
        smooth: false
    },
    interaction: {
        keyboard: {
          enabled: false
        },
        multiselect: true,
        dragView: true
    },
    edges: {
        smooth: {
          type: "continuous",
          roundness: 0
        }
    },    
    layout: {  
        hierarchical: { 
            enabled: true,
            parentCentralization: false,
            levelSeparation: 180,
            nodeSpacing: 100,
            treeSpacing: 110,
            sortMethod: "directed",
            direction: "LR"
        }
    }
};
var documentKeyDownHandler = function(e) {
    if (e.which==17) window.ctrlPressed = true;
    if (e.which==16) window.shiftPressed = true;
//    if (e.which==16) { //shift
//        if ( !(network.manipulation.inMode == "addEdge") && !network.manipulation.editMode) network.manipulation.addEdgeMode();
//    }
    if (e.keyCode == 46) {
        deleteSelectedDocs();
    }
    if (e.keyCode == 113) {
        renameSelectedDoc();
    }
}
document.addEventListener('keydown', documentKeyDownHandler, false);

var documentKeyUpHandler = function(e) {
    if (e.which==17) window.ctrlPressed = false;
    if (e.which==16) window.shiftPressed = false;
//    if (e.which==16) { //shift
//        network.manipulation.disableEditMode();
//    }

}
document.addEventListener('keyup', documentKeyUpHandler, false);


function docToVis(curDoc, level)  {
    if (curDoc.allNets) {
        curDoc.countNets = Object.keys(curDoc.allNets).length;
        delete curDoc.allNets;
    }
    if (curDoc.allNodes) {
        curDoc.countNodes = Object.keys(curDoc.allNodes).length;
        delete curDoc.allNodes;
    }
    curDoc.level = level;
    curDoc.label = wordWrap(curDoc.docName,20) + "\n(" + curDoc.id + ", nets:"+curDoc.countNets+", nodes:"+curDoc.countNodes+")";
    curDoc.shape = "square";
    curDoc.size = 20;
    return curDoc;
}

function docTreeHelper(docTree, idx, appendIdxNode, nodesArray, edgesArray, level)  {
    var curDoc = docToVis( docTree.allDocs[idx],  level );
    if (appendIdxNode) nodesArray.push(curDoc);
    for (var i=0; i<docTree.tMap[idx].length; i++) {
        tgtDoc = docTreeHelper(docTree, docTree.tMap[idx][i], true, nodesArray, edgesArray, level+1);
        edgesArray.push({from: curDoc.id, to: tgtDoc.id, arrows:'to'});
    }
    return curDoc;
}

document.addEventListener("DOMContentLoaded", function() {
    var container = document.getElementById('docnetwork');

    $.ajax({
        type: 'GET',
        url: BASE_URL + "doc",
        async: false,
//        dataType: 'json',
        contentType: "application/json; charset=UTF-8",
        success: function(response) {
            console.log("received:   " + JSON.stringify(response));
//            document.getElementById('cardnetwork').style.visibility = "visible";
            


            nodesArray = [];
            edgesArray = [];
            for (var i=0; i<response.length; i++) {  
                edgesArray.push({from: -1, to: response[i].allDocs[0].id});
                docTreeHelper(response[i], 0, true, nodesArray, edgesArray, 0);
            }
            nodes = new vis.DataSet(nodesArray);
            edges = new vis.DataSet(edgesArray);
            var data = { nodes:nodes, edges:edges };
            network = new vis.Network(container, data, options);

//            options.layout.hierarchical.enabled = false; 
//            options.physics.enabled = true; 
//            network.setOptions(options);

            // add event listeners
            network.on('select', function (params) {
                //document.getElementById('selection').innerHTML = 'Selection: ' + params.nodes;
                console.log(  'Selection: ' + params.nodes  );
            });
            network.on("mousedown", function (params) {
                alert();
            });

            network.on("click", function (params) {
                //params.event = "[original event]";
                //console.log( '<h2>Click event:</h2>' + JSON.stringify(params, null, 4) );
                //console.log('click event, getNodeAt returns: ' + this.getNodeAt(params.pointer.DOM));
            });
            network.on("doubleClick", function (params) {
                //params.event = "[original event]";
                var docId = this.getNodeAt(params.pointer.DOM);
                if (docId !== null && docId !== undefined && docId >= 0) {
                    window.open(NGINX_URL+"editor.html?docId="+docId, "_blank");
                }
                //console.log( '<h2>doubleClick event:</h2>' + JSON.stringify(params, null, 4) );
            });
            network.on("oncontext", function (params) {
                params.event = "[original event]";
                console.log( '<h2>oncontext (right click) event:</h2>' + JSON.stringify(params, null, 4) );
            });
            network.on("dragStart", function (params) {
                var e = params.event;
                // There's no point in displaying this event on screen, it gets immediately overwritten
                //params.event = "[original event]";
                console.log('dragStart Event:', params);
                console.log('dragStart event, getNodeAt returns: ' + this.getNodeAt(params.pointer.DOM));

                
                if ( (!params.nodes || params.nodes.length == 0) && !window.shiftPressed ) {
                    mouse.startX = e.center.x;
                    mouse.startY = e.center.y;
                    window.selRect = document.createElement('div');
                    window.selRect.className = 'selectionRectangle';
                    window.selRect.style.left = e.center.x + "px";
                    window.selRect.style.top = e.center.y + "px";
                    document.getElementById('docnetwork').appendChild(selRect);
                }
                
            });
            network.on("dragging", function (params) {
                var e = params.event;
                //params.event = "[original event]";
                //console.log( '<h2>dragging event:</h2>' + JSON.stringify(params, null, 4) );

                if ( (!params.nodes || params.nodes.length == 0) && window.selRect !== null) {
                    var newWidth = ( window.Math.abs(e.center.x - mouse.startX) );
                    var newHeight = ( window.Math.abs(e.center.y - mouse.startY) );
                    var newLeft = ( (e.center.x - mouse.startX < 0) ? e.center.x : mouse.startX );
                    var newTop = ( (e.center.y - mouse.startY < 0) ? e.center.y : mouse.startY );
                    window.selRect.style.width  = newWidth + 'px';
                    window.selRect.style.height = newHeight + 'px';
                    window.selRect.style.left = newLeft + 'px';
                    window.selRect.style.top =  newTop + 'px';
                    var canvasXY1 = network.DOMtoCanvas({x:newLeft,y:newTop});
                    var canvasXY2 = network.DOMtoCanvas({x:newLeft+newWidth,y:newTop+newHeight});
                    var nodesToSelect = network.getAllNodesOverlappingWith({left:canvasXY1.x, top:canvasXY1.y, right:canvasXY2.x, bottom:canvasXY2.y});
                    //console.log( "blub: " + JSON.stringify(  nodesToSelect  )  );
                    //network.unselectAll();
                    if (window.ctrlPressed) {
                        network.setSelection( { nodes:nodesToSelect } , {unselectAll: false, highlightEdges: false} );
                    } else {
                        network.setSelection( { nodes:nodesToSelect } , {unselectAll: true, highlightEdges: false} );
                    }
                    

/*
                    var overlappingNodes = [];
                    var nodes = this.body.nodes;
                    for (var i = 0; i < this.body.nodeIndices.length; i++) {
                        var nodeId = this.body.nodeIndices[i];
                        if (nodes[nodeId].isOverlappingWith(object)) {
                            overlappingNodes.push(nodeId);
                        }
                    }
                    console.log( "blub: " + network.getAllNodesOverlappingWith );                    
*/                    
                }
            });
            network.on("dragEnd", function (params) {
                //params.event = "[original event]";
                console.log( '<h2>dragEnd event:</h2>' + JSON.stringify(params) );
                console.log('dragEnd Event:', params);
                console.log('dragEnd event, getNodeAt returns: ' + this.getNodeAt(params.pointer.DOM));

                if (window.selRect) {
                    window.selRect.parentElement.removeChild(window.selRect);
                    window.selRect = null;
                }
            });
            network.on("zoom", function (params) {
                console.log( '<h2>zoom event:</h2>' + JSON.stringify(params, null, 4) );
            });
            network.on("showPopup", function (params) {
                console.log( '<h2>showPopup event: </h2>' + JSON.stringify(params, null, 4) );
            });
            network.on("hidePopup", function () {
                console.log('hidePopup Event');
            });
            network.on("select", function (params) {
                console.log('select Event:', params);
            });
            network.on("selectNode", function (params) {
                console.log('selectNode Event:', params);
                //var withoutDeletedNodes = params.nodes.filter(function (n) { return n.deleted !== true;});
                //network.setSelection({nodes:withoutDeletedNodes});
            });
            network.on("selectEdge", function (params) {
                console.log('selectEdge Event:', params);
            });
            network.on("deselectNode", function (params) {
                console.log('deselectNode Event:', params);
            });
            network.on("deselectEdge", function (params) {
                console.log('deselectEdge Event:', params);
            });
            network.on("hoverNode", function (params) {
                console.log('hoverNode Event:', params);
            });
            network.on("hoverEdge", function (params) {
                console.log('hoverEdge Event:', params);
            });
            network.on("blurNode", function (params) {
                console.log('blurNode Event:', params);
            });
            network.on("blurEdge", function (params) {
                console.log('blurEdge Event:', params);
            });

            

        }, error: function(error)  {
            alert( "Failed to retrieve doc. \n\n"+httpErrorCapture(error) );
        }
    });
  
});



function renameSelectedDoc() {
    var fnodes = network.getSelectedNodes().filter(function(x){return !(nodes.get(x).deleted)});
    if (fnodes.length == 1) {
        var n = nodes.get(fnodes[0]);
        var newDocName = prompt("Rename document", n.docName);
        if (newDocName) {
            $.ajax({
                type: "PUT",
                url: BASE_URL + "doc/"+n.id,
                dataType: "json",
                data: JSON.stringify( {docName: newDocName} ),
                contentType: 'application/json;charset=utf-8',
                async: false,
                "success": function(data) {
                    /*
                    var defaultData = {
                        id: 123,
                        docName: "test",
                        allNodes: [],
                        allNets: [],
                        x: 0,
                        y: 0
                    };
                    */ 
                    console.log (  JSON.stringify(data)  ) ;
                    nodes.update(  docToVis(data, n.level)  );
                },
                "error": function(error) {
                    alert( "Failed to create doc. \n\n"+httpErrorCapture(error) );
                }
            });            
        }

    }

    
}

function importNewDoc(files) {
    //var file_data = $("#avatar").prop("files")[0];
    $.ajax({
                type: 'post',
                url: "doc/trees",
                dataType: "json",
                contentType: "application/json; charset=UTF-8",
                cache: false,
                processData: false,
                //data: file_data,
                data: files[0],
                success: function(response) {
                    console.log("received222:   " + JSON.stringify(response));
                    nodesArray = [];
                    edgesArray = [];
                    for (var i=0; i<response.length; i++) {  
                        edgesArray.push({from: -1, to: response[i].allDocs[0].id});
                        docTreeHelper(response[i], 0, true, nodesArray, edgesArray, 0);
                    }
                    nodes.update(  nodesArray  );
                    edges.update(  edgesArray  );
                }, error: function(error)  {
                    alert( "Failed to retrieve doc. \n\n"+httpErrorCapture(error) );
                }
    })
    document.getElementById("fileinput").value = "";
}

function exportAllDocs() {
    window.open(BASE_URL+"doc/deep", "_blank");
}

function createNewDoc() {
    $.ajax({
        type: "POST",
        url: BASE_URL + "doc",
        dataType: "json",
        data: JSON.stringify( { "addr" : "192.168.100.0/24" } ),
        contentType: 'application/json;charset=utf-8',
        async: false,
        "success": function(data) {
            /*
            var defaultData = {
                id: 123,
                docName: "test",
                allNodes: [],
                allNets: [],
                x: 0,
                y: 0
            };
            */ 
            console.log (  JSON.stringify(data)  ) ;
            network.insertNewNode(  docToVis(data, 0) );
            network.setSelection({nodes:[data.id]});
        },
        "error": function(error) {
            alert( "Failed to create doc. \n\n"+httpErrorCapture(error) );
        }
    });    
}

function openSelectedDocs() {
    if(network.getSelectedNodes().length==0){
        alert("Please select a project.");
    } else {
        var snodes = network.getSelectedNodes();
        for (var i=0; i<snodes.length; i++) {
            if (snodes[i] !== null && snodes[i] !== undefined && snodes[i] >= 0) {
                window.open(NGINX_URL+"editor.html?docId="+snodes[i], "_blank");
            }
        }
    }
}

function deleteSelectedDocs() {
    var snodes = network.getSelectedNodes();
    var fnodes = network.getSelectedNodes().filter(function(x){return !(nodes.get(x).deleted)});
    var dnodes = network.getSelectedNodes().filter(function(x){return (nodes.get(x).deleted)});
    if (fnodes.length > 0) {
        $.ajax(BASE_URL+"deleteDocs",
        {
            "method": "DELETE",
            dataType: "Text",
            "contentType": "application/json; charset=UTF-8",
            data: JSON.stringify( fnodes ),
            "async": false,
            "success": function(data) {
                
                for (var i=0; i<snodes.length; i++) {
                    if ( !hasEdgesOut(snodes[i]) ) {
                        detachDelete(snodes[i])
                    }
                }

                for (var i=0; i<snodes.length; i++) {
                    if ( !hasEdgesOut(snodes[i]) ) {
                        detachDelete(snodes[i])
                    } else {
                        var n = nodes.get(snodes[i]);
                        n.deleted = true;
                        n.color = {
//                            border: '#000000',
                            background: '#ccc',
                            highlight: {
//                                border: '#ccc',
                                background: '#ccc'
                            }                            
                        };
                        nodes.update(n);
                        console.log(  JSON.stringify(nodes.get(snodes[i]))  );
                    }
                }                

            },
            "error": function(error) {
                alert( "Failed to delete doc. \n\n"+httpErrorCapture(error) );
            }
        });         
    } 

    var performedDeletion = false;
    for (var i=0; i<dnodes.length; i++) {
        if ( !hasEdgesOut(dnodes[i]) ) {
            detachDelete(dnodes[i])
            performedDeletion = true;
        }
    }

    if (fnodes.length == 0 && !performedDeletion) alert('document(s) not selected');
}

function branchSelectedDoc() {
    var snodes = network.getSelectedNodes().filter(function(x){return !(nodes.get(x).deleted)});
    if (snodes.length == 1) {
        var docId = snodes[0];
        if (docId !== null && docId !== undefined && docId >= 0) {
            $.ajax(BASE_URL+"doc/"+docId+"/savestate/copyIsHead",
            {
                "contentType": "application/json; charset=UTF-8",
                "async": false,
                "method": "POST",
                "success": function(data) {
                    var curDoc = nodes.get(docId);

                    removeRecursive(docId, false);
                    
                    nodesArray = [];
                    edgesArray = [];
                    docTreeHelper(data, 0, false, nodesArray, edgesArray, curDoc.level);
                    nodes.update(  nodesArray  );
                    edges.update(  edgesArray  );

                },
                "error": function(data) {
                    alert( "Failed to add new fog. \n\n"+httpErrorCapture(data) );
                }
            });

        }                
    } else {
        alert('single document not selected');
    }
}




function detachDelete(node) {
    var delEdges = getEdgesInOut(node);
    for (var i=0; i<delEdges.length; i++) {
        edges.remove( delEdges[i] );
    }
    nodes.remove(node);
}


function removeRecursive(nodeId, eraseInitialNode) {
    var edgesNext = getEdgesOut(nodeId);
    for (var i=0; i<edgesNext.length; i++) {
        removeRecursive(edgesNext[i].to, true);
        edges.remove( edgesNext[i].id );
    }
    if (eraseInitialNode) detachDelete(  nodeId  );
}

function getEdgesOut(nodeId) {
    return edges.get().filter(function (edge) {
        return edge.from === nodeId;
    });
}
function getEdgesIn(nodeId) {
    return edges.get().filter(function (edge) {
        return edge.to === nodeId;
    });
}
function getEdgesInOut(nodeId) {
    return edges.get().filter(function (edge) {
        return edge.from === nodeId || edge.to === nodeId;
    });
}
function hasEdgesOut(nodeId) {
    var e = edges.get();
    for (var i=0; i<e.length; i++) {
        if (e[i].from === nodeId) return true;
    }
}
function hasEdgesIn(nodeId) {
    var e = edges.get();
    for (var i=0; i<e.length; i++) {
        if (e[i].to === nodeId) return true;
    }
}


function wordWrap(text, maxChars) {
    var ret = [];
    var words = text.split(/\b/);

    var currentLine = '';
    var lastWhite = '';
    words.forEach(function(d) {
        var prev = currentLine;
        currentLine += lastWhite + d;

        var l = currentLine.length;

        if (l > maxChars) {
            ret.push(prev.trim());
            currentLine = d;
            lastWhite = '';
        } else {
            var m = currentLine.match(/(.*)(\s+)$/);
            lastWhite = (m && m.length === 3 && m[2]) || '';
            currentLine = (m && m.length === 3 && m[1]) || currentLine;
        }
    });

    if (currentLine) {
        ret.push(currentLine.trim());
    }

    return ret.join("\n");
}












/////////////////////////////////////////////////////////////////////////////////////
//      START UTILS



function httpErrorCapture(data) {
    var jsonData = JSON.stringify(data);
    if (data == undefined) return ""; 
    if (data.status == 401) { return "Authorization failed\n\n"+jsonData;
    } else if (data.status == 403) { return "Access forbidden\n\n"+jsonData;
    } else if (data.status == 404) { return "Not found\n\n"+jsonData;
    } else {
        return JSON.stringify(jsonData);
    }    
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
  return query_string;
}




//      END UTILS
/////////////////////////////////////////////////////////////////////////////////////
