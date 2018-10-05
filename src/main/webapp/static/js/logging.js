var refreshEnabled = true;
var subinfo = new Array();

$(document).ready(handleAutoRefresh());

function handleRefreshChange () {
    var enabled = document.getElementById("cbrefresh").checked;
    console.log("Refesh is enabled: " + enabled);
    refreshEnabled = enabled;
}

function clickSubinfo(callingElement) {
	console.log("from: " + callingElement.className);
	var idx = callingElement.className.substring(5);
	console.log("idx is " + idx)
	if (callingElement.childElementCount == 1) {
		//show subinfo
		var oldHtml = callingElement.innerHTML;
		var newHtml = oldHtml;
		var i;
		for (i = 0; i < subinfo[idx].length; ++i) {
			newHtml = newHtml + subinfo[idx][i];
		}
		callingElement.innerHTML = newHtml;
	} else {
		//hide info
		var newHtml = callingElement.firstChild;
		callingElement.innerHTML = "";
		callingElement.insertAdjacentElement("afterbegin", newHtml);
		//console.log("newHtml is " + newHtml);
	}
}

function parseAnsibleLog(logdata) {
	subinfo = new Array();
	var lines = logdata.split(/\r?\n/);
	var no = 0;
	var result = "<div class=main_" + no + " onclick=\"clickSubinfo(this)\"> <div>Inititalization:</div>";
	var length = subinfo.push(new Array());
	var index;
	for (index = 0; index < lines.length; ++index) {
		var currline = lines[index];
		if (currline.startsWith("[?] TASK") || currline.startsWith("[?] PLAY")) {
			result = result + "</div>";
			no = no+1;
			subinfo.push(new Array());
			result  = result + "<div class=main_" + no + " onclick=\"clickSubinfo(this)\"><div>" + lines[index] + "</div>";
		} else {
			subinfo[no].push("<div class=sub" + no + " style=\"background-color: #2a2a2a; margin-left: 20px\">" + lines[index] + "</div>");
		}

	}
    return result + "</div>";
}

function onClickShowMoreLogs() {
    $.ajax({
        type: 'GET',
        url: BASE_URL + 'process/logs',
        contentType: 'application/json',
        success: function(data) {
            console.log(data);
            var logField= document.getElementById('log-field');
            logField.innerHTML = parseAnsibleLog(data['msg']);
        },
        error: function (error) {
            console.log(error);
        }
    });
}

function handleAutoRefresh() {
    if (refreshEnabled) {
        console.log("Getting logfile...");
        onClickShowMoreLogs();
    }
    setTimeout(handleAutoRefresh, 5000);
}