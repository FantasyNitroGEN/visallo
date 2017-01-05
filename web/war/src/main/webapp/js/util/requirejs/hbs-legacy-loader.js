define(['handlebars'], function(Handlebars) {
    return {
        load: function(name, req, load) { // , config
            if (!(/^(com|org|net)/.test(name))) {
                console.warn(`hbs! Plugin Deprecated
    Use require(['${name}.hbs']) instead of require(['hbs!${name}'])
                `);
            }
            req([`text!${name}.hbs`], function(result) {
                load(Handlebars.compile(result));
            }, function(e) {
                console.log('Error evaluating template', e)
            });
        }
    }
})
