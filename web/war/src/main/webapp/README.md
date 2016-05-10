Information about the Visallo web application and extension points can be
found in the [Front End](http://docs.visallo.org/front-end) section of the
documentation at [http://docs.visallo.org](http://docs.visallo.org)

# JavaScript and React Style Guide

Visallo's style for JavaScript and React. Inspired by / forked from the [Airbnb Style Guide](https://github.com/airbnb/javascript).

React JSX is transpiled using [Babel](https://babeljs.io). The in-browser [Babel repl](https://babeljs.io/repl/) is a great option for viewing the transpiled output.

## Table of Contents

### JavaScript
  1. [Basic Rules](#basic-rules)
  1. [References](#references)
  1. [Objects](#objects)
  1. [Destructuring Assignment](#destructuring-assignment)
  1. [Functions](#functions)
  1. [Arrow Functions](#arrow-functions)
  1. [Modules](#modules)

### React
  1. [Basic Rules](#basic-rules-1)
  1. [`React.createClass` vs. stateless function](#reactcreateclass-vs-stateless-function)
  1. [Component Design](#component-design)
  1. [Naming](#naming)
  1. [PropTypes](#proptypes)

## JavaScript

### Basic Rules

  - Prefer usage of built-in functions over their library equivalents.

### References

  - ES6 adds two new keywords for declaring variables: `let` and `const`. Variables declared with `let` and `const` are block-scoped like in Java unlike `var`, which is function-scoped:

    ```javascript
    if (true) {
      var myVar = 'ES5 var';
      let myLet = 'ES6 let';
      const myConst = 'ES6 const';
    }
    console.log(myVar); //ES5 var
    console.log(myLet); //ReferenceError
    console.log(myConst); //ReferenceError
    ```

  - Variables declared with `const` cannot be reassigned. Prefer declaring variables in JSX with `const`. If the reference needs to be reassigned, use `let` over `var`. eslint: [`prefer-const`](http://eslint.org/docs/rules/prefer-const.html), [`no-const-assign`](http://eslint.org/docs/rules/no-const-assign.html), [`no-var`](http://eslint.org/docs/rules/no-var.html).

### Destructuring Assignment

  - In JSX use ES6 object destructuring when accessing and using multiple properties of an object:

    ```javascript
    // bad
    const firstName = user.firstName;
    const lastName = user.lastName;

    // good
    const { firstName, lastName } = user;
    ```

  - In JSX use ES6 array destructuring:

    ```javascript
    const arr = [1, 2, 3, 4];

    // bad
    const first = arr[0];
    const second = arr[1];

    // good
    const [first, second] = arr;
    ```

### Functions

  - In JSX use destructuring to pull fields from objects passed as parameters:

    ```javascript
    // bad
    function getFullName(user) {
      const { firstName, lastName } = user;
      return `${firstName} ${lastName}`;
    }

    // good
    function getFullName({ firstName, lastName }) {
      return `${firstName} ${lastName}`;
    }
    ```

  - In JSX use ES6 default parameter syntax rather than mutating or reassigning function arguments:

    ```javascript
    // bad
    function handleThings(opts) {
      opts = opts || {};
      // ...
    }

    // good
    function handleThings(opts = {}) {
      // ...
    }
    ```

## React

### Basic Rules

  - Prefer including only one React component per file.

### `React.createClass` vs stateless function

  - If the component maintains internal state, create the React class using `React.createClass`. Avoid `class extends React.Component`. eslint: [`react/prefer-es6-class#never-mode`](https://github.com/yannickcr/eslint-plugin-react/blob/master/docs/rules/prefer-es6-class.md#never-mode)
  - If the component does not maintain state, prefer an arrow function assigned to a variable over using `React.createClass`.

    ```javascript
    // bad (the transpiled function won't receive a name and the component will show up in dev tools as <StatelessComponent>)
    export default (props) => <div>Hello {props.name}</div>;

    // good
    const HelloMessage = (props) => <div>Hello {props.name}</div>;
    export default HelloMessage;

    // best (uses destructuring assignment to extract the properties consumed by the component)
    const HelloMessage = ({ name }) => <div>Hello {name}</div>;
    export default HelloMessage;
    ```

  - Prefer stateless functions. eslint: [`react/prefer-stateless-function`](https://github.com/yannickcr/eslint-plugin-react/blob/master/docs/rules/prefer-stateless-function.md)

### Component Design

  - Each component should have a single responsibility. [Thinking in React](https://facebook.github.io/react/docs/thinking-in-react.html) is a great article on breaking down a user interface into its components.

### Naming

  -  **Filename**: Use PascalCase for filenames. E.g., `ReservationCard.js`.
  -  Reference Naming: Use PascalCase for React components and camelCase for their instances. eslint: react/jsx-pascal-case

    ```javascript
    // bad
    var reservationCard = require('./ReservationCard');

    // good
    var ReservationCard = require('./ReservationCard');

    // bad
    const ReservationItem = <ReservationCard />;

    // good
    const reservationItem = <ReservationCard />;
    ```

### PropTypes

  - Each component should specify propTypes. See [Prop Validation](https://facebook.github.io/react/docs/reusable-components.html#prop-validation).
