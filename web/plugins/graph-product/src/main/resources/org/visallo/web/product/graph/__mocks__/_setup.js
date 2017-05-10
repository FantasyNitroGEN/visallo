import bluebird from 'bluebird'
import underscore from 'underscore'


global.Promise = bluebird
global._ = underscore
if (!Object.values) Object.values = _.values

