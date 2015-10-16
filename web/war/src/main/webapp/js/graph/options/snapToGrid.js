define([
    'flight/lib/component',
    'util/withDataRequest'
], function(
    defineComponent, withDataRequest) {
    'use strict';

    return defineComponent(SnapToGridToggle, withDataRequest);

    function SnapToGridToggle() {

        this.after('initialize', function() {
            var self = this,
                cy = this.attr.cy,
                preferenceName = 'snapToGrid',
                preferenceValue = visalloData.currentUser.uiPreferences[preferenceName],
                snapToGrid = preferenceValue === 'true';

            $('<label>').text(i18n('controls.options.snapToGrid.toggle') + ' ')
                .css({
                    'white-space': 'nowrap'
                })
                .append('<input type="checkbox"' + (snapToGrid ? ' checked' : '') + '>')
                .appendTo(this.$node.empty())

            this.$node
                .find('input').on('change', function() {
                    var checked = $(this).is(':checked');
                    visalloData.currentUser.uiPreferences[preferenceName] = '' + checked;
                    self.trigger('toggleSnapToGrid', {
                        snapToGrid: checked
                    });
                    self.dataRequest('user', 'preference', preferenceName, checked);
                });
        });

    }
});
