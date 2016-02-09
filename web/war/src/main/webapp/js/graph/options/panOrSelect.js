define([
    'flight/lib/component',
    'util/withDataRequest',
    'hbs!./panOrSelectTpl'
], function(
    defineComponent, withDataRequest, template) {
    'use strict';

    var preferenceName = 'panOrSelect';

    return defineComponent(PanOrSelectToggle, withDataRequest);

    function PanOrSelectToggle() {

        this.after('initialize', function() {
            var self = this,
                preferenceValue = visalloData.currentUser.uiPreferences[preferenceName],
                isPan = preferenceValue === 'pan';

            this.$node.html(template({
                isPan: isPan,
                subtitle: i18n('controls.options.panOrSelect.subtitle.' + (isPan ? 'select' : 'pan'))
            }));

            this.$node
                .find('input').on('change', function() {
                    var val = $(this).val(),
                        subtitle = i18n('controls.options.panOrSelect.subtitle.' +
                            (val === 'pan' ? 'select' : 'pan')
                        );

                    self.$node.find('.subtitle').text(subtitle);
                    self.updatePreference(val);
                });
        });

        this.updatePreference = function(checked) {
            visalloData.currentUser.uiPreferences[preferenceName] = '' + checked;
            this.trigger('toggleClickAndDrag', { defaultType: checked });
            this.dataRequest('user', 'preference', preferenceName, checked);
        }
    }
});
