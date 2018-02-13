/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

//新API兼容模块
// Older browsers might not implement mediaDevices at all, so we set an empty object first
if (navigator.mediaDevices === undefined) {
    navigator.mediaDevices = {};
}

// Some browsers partially implement mediaDevices. We can't just assign an object
// with getUserMedia as it would overwrite existing properties.
// Here, we will just add the getUserMedia property if it's missing.
if (navigator.mediaDevices.getUserMedia === undefined) {
    navigator.mediaDevices.getUserMedia = function (constraints) {

        // First get ahold of the legacy getUserMedia, if present
        var getUserMedia = navigator.webkitGetUserMedia || navigator.mozGetUserMedia;

        // Some browsers just don't implement it - return a rejected promise with an error
        // to keep a consistent interface
        if (!getUserMedia) {
            return Promise.reject(new Error('getUserMedia is not implemented in this browser'));
        }

        // Otherwise, wrap the call to the old navigator.getUserMedia with a Promise
        return new Promise(function (resolve, reject) {
            getUserMedia.call(navigator, constraints, resolve, reject);
        });
    }
}


//var ws = new WebSocket('wss://' + location.host + '/broadcast-websocket');
var video;
var webRtcPeer;

window.onload = function () {
    console = new Console();
    video = document.getElementById('video');
    disableStopButton();
};

window.onbeforeunload = function () {
    //ws.close();
    stop();
};

function ajax(val) {
    $.ajax({
        url: "/broadcast-http/signalingTest",
        data: JSON.stringify({
            id: 'presenter',
            sdpOffer: val
        }),
        type: "post",
        dataType: "json",
        success: function (result) {
            console.log(result)
        }
    });
}

function test() {
    ajax(1);
    ajax(2);
    ajax(3);
    ajax(4);ajax(10);
    ajax(5);
    ajax(6);
    ajax(7);
    ajax(8);ajax(9);
}

function presenterResponse(message) {
    if (message.response !== 'accepted') {
        var errorMsg = message.message ? message.message : 'Unknow error';
        console.info('Call not accepted for the following reason: ' + errorMsg);
        dispose();
    } else {
        webRtcPeer.processAnswer(message.sdpAnswer, function (error) {
            if (error)
                return console.error(error);
        });
    }
}

function viewerResponse(message) {
    if (message.response !== 'accepted') {
        var errorMsg = message.message ? message.message : 'Unknow error';
        console.info('Call not accepted for the following reason: ' + errorMsg);
        dispose();
    } else {
        webRtcPeer.processAnswer(message.sdpAnswer, function (error) {
            if (error)
                return console.error(error);
        });
    }
}

function presenter() {
    if (!webRtcPeer) {
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
                                minWidth: 1024,
                                maxWidth: 1260,
                                minHeight: 768,
                                maxHeight: 780
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
                                onicecandidate: onIceCandidate,
                                sendSource: 'screen'
                            };

                            webRtcPeer = new kurentoUtils.WebRtcPeer.WebRtcPeerSendrecv(options, function (error) {
                                if (error) {
                                    return console.error(error);
                                }
                                webRtcPeer.generateOffer(onOfferPresenter);
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

        /*getScreenId(function (error, sourceId, screen_constraints) {
            if (!screen_constraints) {
                hideSpinner(video);
                disableStopButton();
                return;
            }
            screen_constraints.video.mandatory.maxWidth = 1280;
            screen_constraints.video.mandatory.maxHeight = 720;
            navigator.mediaDevices.getUserMedia(
                screen_constraints
            ).then(function (stream) {
                var screen_stream = stream;
                navigator.mediaDevices.getUserMedia({audio: true, video: false}).then(function (stream) {
                    var constraints = {
                        audio: true,
                        video: true
                    };
                    var options = {
                        //localVideo: video,
                        audioStream: stream,
                        videoStream: screen_stream,
                        mediaConstraints: constraints,
                        onicecandidate: onIceCandidate,
                        sendSource: 'screen'
                    };

                    webRtcPeer = new kurentoUtils.WebRtcPeer.WebRtcPeerSendrecv(options, function (error) {
                        if (error) {
                            return console.error(error);
                        }
                        webRtcPeer.generateOffer(onOfferPresenter);
                    });
                }).catch(function (error) {
                    console.error(error);
                });
            }).catch(function (error) {
                console.error(error);
            });
        });*/
        enableStopButton();
    }
}

function onOfferPresenter(error, offerSdp) {
    if (error)
        return console.error('Error generating the offer');
    console.info('Invoking SDP offer callback function ' + location.host);
    var message = {
        id: 'presenter',
        sdpOffer: offerSdp
    };
    sendMessage(message, function (result) {
        console.log(result);
        presenterResponse(result);
    });
}

function viewer() {
    if (!webRtcPeer) {
        showSpinner(video);

        var options = {
            remoteVideo: video,
            onicecandidate: onIceCandidate
        };
        webRtcPeer = new kurentoUtils.WebRtcPeer.WebRtcPeerRecvonly(options,
            function (error) {
                if (error) {
                    return console.error(error);
                }
                this.generateOffer(onOfferViewer);
            });

        enableStopButton();
    }
}

function onOfferViewer(error, offerSdp) {
    if (error)
        return console.error('Error generating the offer');
    console.info('Invoking SDP offer callback function ' + location.host);
    var message = {
        id: 'viewer',
        sdpOffer: offerSdp
    };
    sendMessage(message);
}

function onIceCandidate(candidate) {
    console.log("Local candidate" + JSON.stringify(candidate));

    var message = {
        id: 'onIceCandidate',
        candidate: candidate
    };
    sendMessage(message, function (result) {
        webRtcPeer.addIceCandidate(result.candidate, function (error) {
            if (error)
                return console.error('Error adding candidate: ' + error);
        });
    });
}

function stop() {
    var message = {
        id: 'stop'
    };
    sendMessage(message, function (result) {
        console.log(result);
    });
    dispose();
}

function dispose() {
    if (webRtcPeer) {
        webRtcPeer.dispose();
        webRtcPeer = null;
    }
    hideSpinner(video);

    disableStopButton();
}

function disableStopButton() {
    enableButton('#presenter', 'presenter()');
    enableButton('#viewer', 'viewer()');
    disableButton('#stop');
}

function enableStopButton() {
    disableButton('#presenter');
    disableButton('#viewer');
    enableButton('#stop', 'stop()');
}

function disableButton(id) {
    $(id).attr('disabled', true);
    $(id).removeAttr('onclick');
}

function enableButton(id, functionName) {
    $(id).attr('disabled', false);
    $(id).attr('onclick', functionName);
}

function sendMessage(message, callback) {
    var jsonMessage = JSON.stringify(message);
    console.log('Senging message: ' + jsonMessage);
    $.ajax({
        url: "/broadcast-http/signaling",
        data: jsonMessage,
        type: "post",
        dataType: "json",
        success: callback
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
