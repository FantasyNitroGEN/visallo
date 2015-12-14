define([
    'hbs/handlebars',
    'util/requirejs/promise!util/service/propertiesPromise'
], function(Handlebars, configProperties) {
    'use strict';

    Handlebars.registerHelper('userGuideLink', function(key, link) {
        if (configProperties['userGuide.enabled'] !== 'false') {
            var href = Handlebars.Utils.escapeExpression('ug/' + key);
            if (arguments.length === 3 && link) {
                link = link + ' User Guide';
            } else {
                link = 'User Guide';
            }
            link = Handlebars.Utils.escapeExpression(link);

            return new Handlebars.SafeString('<a href="' + href + '" target="userGuide">' + link + '</a>');
        }
        return new Handlebars.SafeString('');
    });
});
