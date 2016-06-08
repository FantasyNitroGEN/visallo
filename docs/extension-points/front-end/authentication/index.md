# Authentication

Plugin to configure the user interface for authentication. Provide a flight component to render login screen.

```js
registry.registerExtension('org.visallo.authentication', {
    componentPath: 'org/visallo/web/auth/usernameonly/authentication'
});
```

When the client verifies authentication, trigger a `loginSuccess` event. Visallo will then request protected resources with the current session.

# Example

Visallo includes some default authentication plugins, including username and password, with forgot password support.

[Example Authentication Component](https://github.com/v5analytics/visallo/blob/master/web/plugins/auth-username-password/src/main/resources/org/visallo/web/auth/usernamepassword/authentication.js)

[Example Login Route](https://github.com/v5analytics/visallo/blob/master/web/plugins/auth-username-password/src/main/java/org/visallo/web/auth/usernamepassword/routes/Login.java)
