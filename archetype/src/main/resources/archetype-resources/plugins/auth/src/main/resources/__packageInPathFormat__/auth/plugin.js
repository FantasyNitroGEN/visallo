#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
define(['public/v1/api'], function(visallo) {
    'use strict';

    visallo.registry.registerExtension('org.visallo.authentication', {
        componentPath: '${packageInPathFormat}/auth/authentication'
    })
});
