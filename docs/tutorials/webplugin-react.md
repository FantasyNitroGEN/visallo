# Making a Web Plugin – React

## Introduction

Writing components in [React](https://facebook.github.io/react) is now the preferred way to extend Visallo with custom interfaces. Most extension points already support React, but check the [documentation](../extension-points/front-end/index.md) to make sure.

When writing a web app plugin in Visallo there are two methods to include React JavaScript components:

1. Use `registerJavaScriptComponent` to include a React `jsx` component from the plugins resource folder.

2. Integrate a build step to your plugins `pom.xml` to transpile `jsx` components and then register them with `registerCompiledJavaScript`.

## 1. `registerJavaScriptComponent`

**PROS**
* Easy to get started, or for components with minimal complexity. Doesn't require separate build step.

**CONS**: 
* Doesn't scale as well with many files. Each file must be registered.
* Each file registered slows server startup as they are compiled at runtime.
* Compilation failures will happen at runtime.

### Example

This example will create a plugin that [adds a new dashboard card](../extension-points/front-end/dashboard/item.md) that users can add to their dashboard.

In `WebAppPlugin` register `jsx`

```java
@Name("React Web Demo")
@Description("Register a React JSX File")
public class ReactWebAppPlugin implements WebAppPlugin {
    public void init(WebApp app, ServletContext servletContext, Handler authenticationHandler) {

        // Register plugin to use extension registry
        app.registerJavaScript("/org/example/plugin.js");

        // Register React components
        app.registerJavaScriptComponent("/org/example/ReactDemo.jsx");
        app.registerJavaScriptComponent("/org/example/ReactDemoConfig.jsx");
    }
}
```

`ReactDemo.jsx` will compile to `ReactDemo.js`, and creates a SourceMap at `ReactDemo.src.js`. As with other JavaScript files it will be available at `http://visallo-host:port/jsc/org/example/ReactDemo.js`.

```js
// plugin.js
define(['public/v1/api'], function(api) {
    api.registry.registerExtension('org.visallo.web.dashboard.item', {
        title: 'React Demo'
        description: 'React dashboard card demo',
        identifier: 'org-example-react',

        // Note: Leave off the file extension as requirejs assumes ".js" which
        // is created at runtime.
        componentPath: 'org/example/ReactDemo',
        configurationPath: 'org/example/ReactDemoConfig'
    })
})
```


```js
// ReactDemo.jsx
// Visallo registers 'react', 'create-react-class', and 'prop-types' in RequireJS.
define(['create-react-class'], function(createReactClass) {
    const ReactDemo = createReactClass({
        render() {
            const { item } = this.props;
            const { configuration } = item;
            const { val = 'Not Set' } = configuration
            return (<div>
                <h1>Hello Dashboard Card with React</h1>
                <h2>Config = {val}</h2>
            </div>);
        }
    })

    return ReactDemo;
})
```

```js
// ReactDemoConfig.jsx
define(['create-react-class', 'prop-types'], function(createReactClass, PropTypes) {
    const ReactDemoConfig = createReactClass({
        propTypes: {
            item: PropTypes.shape({
                configuration: PropTypes.object.isRequired
            }).isRequired,
            extension: PropTypes.object.isRequired
        },
        render() {
            const { item } = this.props;
            const { configuration } = item;
            const { val = 'Not Set' } = configuration
            return (<button onClick={this.onClick}>Config = {val}</button>);
        },
        onClick() {
            const { item, extension, configurationChanged } = this.props;
            const val = item.configuration.val || 0;
            const newConfig = {
                ...item.configuration,
                val: val + 1
            };
            item = { ...item, configuration: newConfig }
            configurationChanged({ item: item, extension: extension });
        }
    })

    return ReactDemoConfig;
})
```


All JSX components are compiled using babel so ES6/ES2015 syntax works in `.jsx` files registered with `registerJavaScriptComponent`.

## 2. `registerCompiledJavaScript`

Recommended for complex interface plugins that have deeper component hierarchy.

**PROS**
* Build step run once at build time that combines all dependencies, so no server startup delay.
* Allows use of custom transpile / babel settings.
* Performance of plugin at runtime is better as its only one request for all dependencies.
* Easier to include other build steps like linting, testing, etc.
* Compilation failures will happen at build time.

**CONS**
* Adds complexity to build, must configure maven to run webpack, define webpack build settings.

### Example

This example will create a plugin that [adds a new dashboard card](../extension-points/front-end/dashboard/item.md) that users can add to their dashboard using webpack to build.

All these files remain the same as previous example: `plugin.js`, `ReactDemo.jsx`, and `ReactDemoConfig.jsx`, but now we change `pom.xml` and `ReactDemoWebAppPlugin.java`.


First, lets create a package.json to manage our plugins dependencies in our `src/main/resources/org/example` directory. Accept the default options. (`npm install -g yarn` if you don't have yarn installed)

`yarn init`

Then, add the dependencies to build.

```sh
yarn add --dev \
    babel-core \
    babel-loading \
    babel-plugin-transform-object-rest-spread \
    babel-plugin-transform-react-display-name \
    babel-plugin-transform-react-jsx \
    babel-preset-es2015 \
    react \
    webpack
```

Now, configure babel using `.babelrc`

```sh
curl -O https://github.com/visallo/visallo/blob/master/web/plugins/map-product/src/main/resources/org/visallo/web/product/map/.babelrc > .babelrc
```

Create a webpack configuration file: `src/main/resources/org/example/webpack.config.js`

```js
// webpack.config.js
var path = require('path');
var webpack = require('webpack');
var VisalloAmdExternals = [
    'public/v1/api'
].map(path => ({ [path]: { amd: path }}));

module.exports = {
  entry: {
    ReactDemo: './ReactDemo.jsx',
    ReactDemoConfig: './ReactDemoConfig.jsx'
  },
  output: {
    path: './dist',
    filename: '[name].js',
    library: '[name]',
    libraryTarget: 'umd',
  },
  externals: VisalloAmdExternals.concat([
    {
      react: {
        root: 'React',
        commonjs2: 'react',
        commonjs: 'react',
        amd: 'react'
      },
    }
  ]),
  resolve: {
    extensions: ['', '.js', '.jsx']
  },
  module: {
    loaders: [
        {
            test: /\.jsx?$/,
            exclude: /(node_modules)/,
            loader: 'babel'
        }
    ]
  },
  devtool: 'source-map',
  plugins: [
    new webpack.optimize.UglifyJsPlugin({
        mangle: false,
        compress: {
            drop_debugger: false
        }
    })
  ]
};
```

Try a build by running webpack from the `src/main/resources/org/example` directory.

```sh
node ./node_modules/webpack/bin/webpack.js
```

Now, lets change the plugin to register the compiled files.

```java
@Name("React Web Demo")
@Description("Register a React JSX File")
public class ReactWebAppPlugin implements WebAppPlugin {
    public void init(WebApp app, ServletContext servletContext, Handler authenticationHandler) {

        // Register plugin to use extension registry
        // We don't use webpack for this file
        app.registerJavaScript("/org/example/plugin.js");

        // Register React components by pointing to the webpack compiled versions in dist folder
        app.registerCompiledJavaScript("/org/example/dist/ReactDemo.js");
        app.registerCompiledJavaScript("/org/example/dist/ReactDemoConfig.js");
    }
}
```

Finally, we need to integrate yarn and webpack into the maven build. In your plugins `pom.xml`, add the following.

```xml
<build>
    <plugins>
        <plugin>
            <groupId>com.github.eirslett</groupId>
            <artifactId>frontend-maven-plugin</artifactId>
            <version>${plugin.frontend}</version>
            <configuration>
                <workingDirectory>src/main/resources/org/example</workingDirectory>
                <installDirectory>${frontend.installDirectory}</installDirectory>
            </configuration>
            <executions>
                <execution>
                    <id>yarn install</id>
                    <goals>
                        <goal>yarn</goal>
                    </goals>
                </execution>
                <execution>
                    <id>webpack build</id>
                    <goals>
                        <goal>webpack</goal>
                    </goals>
                    <phase>generate-resources</phase>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

Now try to run `mvn compile`, there should be yarn and webpack commands running in the log.




