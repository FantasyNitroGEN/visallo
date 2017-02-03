import underscore from 'underscore'
import jquery from 'jquery'

// Globals needed for tests
global._ = underscore
global.$ = jquery
global.VISALLO_MIMETYPES = []
global.i18n = str => str

console.warn = () => {}
console.error = () => {}
