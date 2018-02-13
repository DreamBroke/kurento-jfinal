var console = new Console();

var webRtcPeer;
var pipeline;

var video = document.getElementById('video');

var presenterButton = document.getElementById("presenter");
var viewerButton = document.getElementById("viewer");
var stopButton = document.getElementById("stop");
var webRtcEndpointId, pipelineId;

var args = {
    ws_uri: 'wss://52.80.79.43:8433/kurento',
    ice_servers: undefined
};

window.addEventListener('load', function () {

    presenterButton.addEventListener("click", function () {
        showSpinner(video);

        var desktopCapturer = require('electron').desktopCapturer;
        desktopCapturer.getSources({types: ["window", "screen"]}, function (error, sources) {
            if (error) throw error;
            for (var i = 0; i < sources.length; ++i) {

                console.log("sources[" + i + "].name: ", sources[i].name);
                console.log("sources[" + i + "].id: ", sources[i].id);
                if (i === 0) {
                    var constrains = {
                        audio: false,
                        video: {
                            mandatory: {
                                chromeMediaSource: "screen",
                                chromeMediaSourceId: sources[i].id,
                                //minWidth: 1024,
                                maxWidth: 1920,
                                //minHeight: 768,
                                maxHeight: 1080
                            }
                        }
                    };
                    navigator.mediaDevices.getUserMedia(
                        constrains
                    ).then(function (stream) {
                        var screen_stream = stream;
                        navigator.mediaDevices.getUserMedia({audio: true, video: false}).then(function (stream) {
                            var constraints = {
                                audio: true,
                                video: true
                            };
                            var options = {
                                localVideo: video,
                                audioStream: stream,
                                videoStream: screen_stream,
                                mediaConstraints: constraints,
                                sendSource: 'screen'
                            };
                            if (args.ice_servers) {
                                console.log("Use ICE servers: " + args.ice_servers);
                                options.configuration = {
                                    iceServers: JSON.parse(args.ice_servers)
                                };
                            } else {
                                console.log("Use freeIce")
                            }

                            webRtcPeer = kurentoUtils.WebRtcPeer.WebRtcPeerSendonly(options, function (error) {
                                if (error) return onError(error);

                                this.generateOffer(onOffer)
                            });
                        }).catch(function (error) {
                            console.error(error);
                        });
                    }).catch(function (error) {
                        console.error(error);
                    });
                    break;
                }

            }
        });
    });

    viewerButton.addEventListener("click", function () {

        $.ajax({
            url: "/broadcast-js/getWebRtc",
            data: {
                sessionId: $("#sessionId").val()
            },
            type: "post",
            async: false,
            success: function (result) {
                webRtcEndpointId = result.webRtcEndpoint;
                pipelineId = result.pipeline;
            }
        });
        console.log(webRtcEndpointId + " ---- " + pipelineId);

        showSpinner(video);


        var options = {
            remoteVideo: video
        };

        if (args.ice_servers) {
            console.log("Use ICE servers: " + args.ice_servers);
            options.configuration = {
                iceServers: JSON.parse(args.ice_servers)
            };
        } else {
            console.log("Use freeIce")
        }

        webRtcPeer = kurentoUtils.WebRtcPeer.WebRtcPeerRecvonly(options, function (error) {
            if (error) return onError(error);

            this.generateOffer(onOfferViewer)
        });

    });

    stopButton.addEventListener("click", stop);

});

function stop() {
    if (webRtcPeer) {
        webRtcPeer.dispose();
        webRtcPeer = null;
    }

    if (pipeline) {
        pipeline.release();
        pipeline = null;
    }

    hideSpinner(video);
}

function onError(error) {
    if (error) {
        console.error(error);
        stop();
    }
}

function onOffer(error, sdpOffer) {
    if (error) return onError(error);

    kurentoClient(args.ws_uri, function (error, client) {
        if (error) return onError(error);

        client.create("MediaPipeline", function (error, _pipeline) {
            if (error) return onError(error);

            pipeline = _pipeline;

            pipeline.create("WebRtcEndpoint", function (error, webRtc) {
                if (error) return onError(error);

                setIceCandidateCallbacks(webRtcPeer, webRtc, onError);

                $.ajax({
                    url: "/broadcast-js/saveMassage",
                    data: {
                        pipeline: pipeline.id,
                        webRtc: webRtc.id
                    },
                    type: "post",
                    success: function (result) {
                        $("#thisSessionId").val(result.sessionId);
                    }
                });

                webRtc.processOffer(sdpOffer, function (error, sdpAnswer) {
                    if (error) return onError(error);

                    webRtcPeer.processAnswer(sdpAnswer, onError);
                });
                webRtc.gatherCandidates(onError);
            });
        });
    });
}

var offerSDP;

function onOfferViewer(error, offerSdp) {
    if (error) return onError(error);
    offerSDP = offerSdp;
    console.info('Invoking SDP offer callback function ' + location.host);
    kurentoClient(args.ws_uri, getClient);
}

function getClient(error, client) {
    if (error) return onError(error);
    client.getServerManager(getServer);
}

function getServer(error, server) {
    if (error) return onError(error);
    server.getPipelines(getPipe);
}

function getPipe(error, result) {
    if (error) return onError(error);
    for (var i = 0; i < result.length; i++) {
        var pl = result[i];
        if (pl && pl.id === pipelineId) {
            pipeline = pl;
            pipeline.getChildren(getChild);
            break;
        }
    }
}

function getChild(error, children) {
    var presenterWebRtc;
    if (error) return onError(error);
    for (var j = 0; j < children.length; j++) {
        var child = children[j];
        if (child && child.id === webRtcEndpointId) {
            presenterWebRtc = child;
            break;
        }
    }
    pipeline.create("WebRtcEndpoint", function (error, webRtc) {
        if (error) return onError(error);

        setIceCandidateCallbacks(webRtcPeer, webRtc, onError);

        webRtc.processOffer(offerSDP, function (error, sdpAnswer) {
            if (error) return onError(error);

            webRtcPeer.processAnswer(sdpAnswer, onError);
        });
        webRtc.gatherCandidates(onError);

        presenterWebRtc.connect(webRtc, function (error) {
            if (error) return onError(error);

            console.log("Loopback established");
        });
    });
}

function setIceCandidateCallbacks(webRtcPeer, webRtcEp, onerror) {
    webRtcPeer.on('icecandidate', function (candidate) {
        console.log("Local candidate:", candidate);

        candidate = kurentoClient.getComplexType('IceCandidate')(candidate);

        webRtcEp.addIceCandidate(candidate, onerror)
    });

    webRtcEp.on('OnIceCandidate', function (event) {
        var candidate = event.candidate;

        console.log("Remote candidate:", candidate);

        webRtcPeer.addIceCandidate(candidate, onerror);
    });
}

function showSpinner() {
    for (var i = 0; i < arguments.length; i++) {
        arguments[i].poster = '../static/img/transparent-1px.png';
        arguments[i].style.background = 'center transparent url("../static/img/spinner.gif") no-repeat';
    }
}

function hideSpinner() {
    for (var i = 0; i < arguments.length; i++) {
        arguments[i].src = '';
        arguments[i].poster = '../static/img/webrtc.png';
        arguments[i].style.background = '';
    }
}

/**
 * Lightbox utility (to display media pipeline image in a modal dialog)
 */
$(document).delegate('*[data-toggle="lightbox"]', 'click', function (event) {
    event.preventDefault();
    $(this).ekkoLightbox();
});