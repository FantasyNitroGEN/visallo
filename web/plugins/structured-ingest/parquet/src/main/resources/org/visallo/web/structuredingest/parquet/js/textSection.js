define([
    'flight/lib/component'
], function(defineComponent) {
    'use strict';

    return defineComponent(ParquetTextSection);

    function ParquetTextSection() {

        this.after('initialize', function() {
            this.$node.text('It is a parquet file!');
        });

    }
});
