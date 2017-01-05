
define([
    'flight/lib/component',
    './entityTpl.hbs',
    '../withPropertyField',
    'util/jquery/typeAheadUtil',
    'util/withDataRequest',
    'util/formatters'
], function(defineComponent, template, withPropertyField, TypeAheadUtil, withDataRequest, F) {
    'use strict';

    return defineComponent(DirectoryEntityField, withPropertyField, withDataRequest);

    function DirectoryEntityField() {
        var self = this;

        this.after('initialize', function() {
            this.$node.html(template(_.extend({}, this.attr)));
            self.inputField = this.select('inputSelector');
            if (this.attr.vertexProperty) {
                self.loadingSpan = $('<span>').text(i18n('field.directory.form.display_name.loading'));
                this.$node.append(self.loadingSpan);
                self.inputField.hide();
            }

            self.inputField.typeahead({
                source: directorySearch,
                items: 25,
                minLength: 1,
                matcher: function(displayName) {
                    return displayName.toLowerCase().indexOf(this.query.toLowerCase()) > -1;
                },
                updater: function(displayName) {
                    onSelectionFieldSelected(self.map[displayName]);
                    return displayName;
                }
            }).on('click', function() {
                if (self.inputField.val()) {
                    self.inputField.typeahead('lookup').select();
                } else {
                    self.inputField.typeahead('lookup');
                }
                TypeAheadUtil.adjustDropdownPosition(self.inputField);
            }).on('blur', function() {
                TypeAheadUtil.clearIfNoMatch(self.inputField, 'selection', F.directoryEntity.pretty);
                var directoryEntity = self.inputField.data('selection');
                self.inputField.data('value', directoryEntity ? directoryEntity.id : null);
            });
        });

        this.isValid = function(value) {
            return !!value;
        };

        this.setValue = function(value) {
            if (_.isString(value)) {
                if (!value) {
                    return loadDirectoryEntity(null);
                }
                return self.dataRequest('directory', 'getById', value)
                    .then(loadDirectoryEntity);
            } else {
                self.inputField.val(F.directoryEntity.pretty(value));
                onSelectionFieldSelected(value);
            }

            function loadDirectoryEntity(directoryEntry) {
                if (directoryEntry && directoryEntry.id && directoryEntry.type) {
                    self.setValue(directoryEntry);
                    if (self.loadingSpan) {
                        self.loadingSpan.remove();
                    }
                    self.inputField.show();
                }
            }
        };

        this.getValue = function() {
            var directoryEntity = self.inputField.data('selection');
            return directoryEntity ? directoryEntity.id : null;
        };

        function directorySearch(search, process) {
            self.map = {};

            self.dataRequest('directory', 'search', search)
              .then(function(results) {
                  var entities = _.map(results.entities, function(entity) {
                      var str = F.directoryEntity.pretty(entity);
                      self.map[str] = entity;
                      return str;
                  });
                  process(entities);
                  TypeAheadUtil.adjustDropdownPosition(self.inputField);
              });
        }

        function onSelectionFieldSelected(directoryEntity) {
            self.inputField
              .data('selection', directoryEntity)
              .data('value', directoryEntity.id)
              .trigger('directorySelectionFieldSelected', { personOrGroup: directoryEntity });
        }
    }
});
