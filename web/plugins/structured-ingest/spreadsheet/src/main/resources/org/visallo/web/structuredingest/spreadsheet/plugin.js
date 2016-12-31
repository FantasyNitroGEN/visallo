define(['configuration/plugins/registry'], function(registry) {

    var CSV_MIME_TYPE = 'text/csv';
    var XSL_MIME_TYPES = ['application/xls',
         'application/excel',
         'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    ];

    registry.registerExtension('org.visallo.structuredingest', {
        mimeType: CSV_MIME_TYPE
    });

    XSL_MIME_TYPES.forEach(function(m) {
        registry.registerExtension('org.visallo.structuredingest', {
            mimeType: m
        });
    });
})