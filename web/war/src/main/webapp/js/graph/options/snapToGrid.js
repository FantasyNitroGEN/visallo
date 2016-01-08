define([
    'flight/lib/component',
    'util/withDataRequest'
], function(
    defineComponent, withDataRequest) {
    'use strict';

    var preferenceName = 'snapToGrid';

    return defineComponent(SnapToGridToggle, withDataRequest);

    function SnapToGridToggle() {

        this.after('initialize', function() {
            var self = this,
                cy = this.attr.cy,
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
                    self.trigger('toggleSnapToGrid', {
                        snapToGrid: checked
                    });
                    self.updatePreference(checked);
                });

            this.on(document, 'toggleSnapToGrid', this.onToggleSnapToGrid);
        });

        this.onToggleSnapToGrid = function(event, data) {
            if (!data) return;

            var checked = data.snapToGrid;
            this.$node.find('input').prop('checked', checked);
            this.updatePreference(data.snapToGrid);
        }

        this.updatePreference = function(checked) {
                visalloData.currentUser.uiPreferences[preferenceName] = '' + checked;
                this.dataRequest('user', 'preference', preferenceName, checked);
        }
    }
});
