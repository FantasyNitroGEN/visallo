
# Websocket Handlers

Extension to register new listeners for websocket messages. Must be registered in JavaScript file registered with `registerWebWorkerJavaScript`

            registry.registerExtension('org.visallo.websocket.message', {
                name: name,
                handler: handler
            });
