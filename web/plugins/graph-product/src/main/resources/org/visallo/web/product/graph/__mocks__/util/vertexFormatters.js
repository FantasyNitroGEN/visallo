import formatters from './formatters'
import _ from 'underscore'

export default Object.assign({}, formatters, {
    vertex: {
        title: v => v.id,
        prop: (v, name) => {
            const p = _.findWhere(v.properties, { name });
            return p ? p.value : null
        },
        image: v => undefined,
        selectedImage: v => undefined
    }
})
