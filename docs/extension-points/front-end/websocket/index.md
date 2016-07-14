
# Websocket Handlers

Extension to register new listeners for websocket messages. Must be registered in JavaScript file registered with `app.registerWebWorkerJavaScript` in web app plugin.

```js
registry.registerExtension('org.visallo.websocket.message', {
    name: name,
    handler: handler
});
```

## Configuration

* `name` `[String]` The message name to listen for. Matches the `type` parameter in message json.
* `handler` `[Function]` The function to invoke when messages arrive.

    * Parameters:
        * `data`: `[Object]` The data in message.
    

