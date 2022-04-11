var webSocket = new WebSocket("ws://" + location.hostname + ":" + location.port + "/push");

webSocket.onclose = function () {
    //alert("WebSocket connection closed")
    setTimeout(function(){
        prevWebSocket = webSocket;
        webSocket = new WebSocket(prevWebSocket.url);
        webSocket.onmessage = prevWebSocket.onmessage;
        webSocket.onclose = prevWebSocket.onclose;
    }, 3000);
};

webSocket.onmessage = function (e) {
    try {
        eval(e.data);
    } catch(e) {}
};

