define(['handlebars'], function(Handlebars) {
    return {
        load: function(name, req, load) { // , config
            const newName = `${name}.hbs`;
            const oldName = `hbs!${name}`;
            const getText = `text!${name}.hbs`;

            req([newName],
                function success(result) {
                    console.warn(`hbs! RequireJs Plugin Deprecated!

Use require(['${newName}']) instead of
    require(['${oldName}'])

Found precompiled at new location, automatically switching...
                    `);
                    load(result)
                },
                function failure() {
                    req([getText], function(result) {
                        load(Handlebars.compile(result));
                    }, function(e) {
                        console.log('Error evaluating template', e)
                    });
                }
            );
        }
    }
})
