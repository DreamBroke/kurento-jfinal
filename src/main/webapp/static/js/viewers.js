window.onerror = function (message, source, lineno, colno, error) {
    consoleAdd.append("message: " + message + "<br>");
    consoleAdd.append("source: " + source + "<br>");
    consoleAdd.append("lineno: " + lineno + "<br>");
    consoleAdd.append("colno: " + colno + "<br>");
    consoleAdd.append("error: " + error + "<br>");
};

var ws = new WebSocket('wss://' + location.host + '/happy-new-year-viewer-websocket');
var video = document.getElementById("video");
var webRtcPeer;
var consoleAdd = $("#console");
window.onbeforeunload = function() {
    ws.close();
};
ws.onerror = function(evt) {
    consoleAdd.append("Error occurred.<br>");
    consoleAdd.append("Error: " + evt.data + "<br>");
    console.log(evt.data);
    console.log(evt);
};
ws.onclose = function (e1, e2) {
    consoleAdd.append("Close occurred.<br>");
    consoleAdd.append("Close: " + e1.code + "<br>");
    consoleAdd.append("Close: " + e1.reason + "<br>");
    console.log(e1);
};
ws.onopen = function (event) {
    console.log(event);
    consoleAdd.append("Open: " + event + "<br>");
};
ws.onmessage = function(message) {
    var parsedMessage = JSON.parse(message.data);
    console.info('Received message: ' + message.data);

    switch (parsedMessage.id) {
        case 'viewerResponse':
            viewerResponse(parsedMessage);
            break;
        case 'iceCandidate':
            webRtcPeer.addIceCandidate(parsedMessage.candidate, function(error) {
                if (error)
                    return console.error('Error adding candidate: ' + error);
            });
            break;
        case 'count-viewers':
            $("#viewer-count").text(parsedMessage.count);
            break;
        default:
            console.error('Unrecognized message', parsedMessage);
    }
};
window.onerror = function (message, source, lineno, colno, error) {
    consoleAdd.append("message: " + message + "<br>");
    consoleAdd.append("source: " + source + "<br>");
    consoleAdd.append("lineno: " + lineno + "<br>");
    consoleAdd.append("colno: " + colno + "<br>");
    consoleAdd.append("error: " + error + "<br>");
};

$("#play-video").click(function () {
    consoleAdd.append(ws.url + "<br>");
    consoleAdd.append(ws.readyState + "<br>");
    $(".video-cover").attr("hidden", true);
    $("#video").css("height",document.body.clientWidth * 9 / 16).attr("hidden", false);
    video.poster = '../static/img/transparent-1px.png';
    video.style.background = 'center transparent url("../static/img/spinner.gif") no-repeat';
    viewer();
});

function viewer() {
    if (!webRtcPeer) {

        var options = {
            remoteVideo : video,
            onicecandidate : onIceCandidate
        };
        webRtcPeer = new kurentoUtils.WebRtcPeer.WebRtcPeerRecvonly(options,
            function(error) {
                if (error) {
                    return console.error(error);
                }
                this.generateOffer(onOfferViewer);
            });


    }
}

function viewerResponse(message) {
    if (message.response !== 'accepted') {
        var errorMsg = message.message ? message.message : 'Unknow error';
        console.info('Call not accepted for the following reason: ' + errorMsg);
    } else {
        webRtcPeer.processAnswer(message.sdpAnswer, function(error) {
            if (error)
                return console.error(error);
        });
    }
}

function onIceCandidate(candidate) {
    console.log("Local candidate" + JSON.stringify(candidate));

    var message = {
        id : 'onIceCandidate',
        candidate : candidate
    };
    sendMessage(message);
}

function onOfferViewer(error, offerSdp) {
    if (error)
        return console.error('Error generating the offer');
    console.info('Invoking SDP offer callback function ' + location.host);
    var message = {
        id : 'viewer',
        sdpOffer : offerSdp
    };
    sendMessage(message);
}

function sendMessage(message) {
    var jsonMessage = JSON.stringify(message);
    console.log('Senging message: ' + jsonMessage);
    ws.send(jsonMessage);
}

