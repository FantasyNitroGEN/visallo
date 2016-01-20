
define([
    'flight/lib/component',
    'hbs!./entityTpl',
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
                TypeAheadUtil.clearIfNoMatch(self.inputField, 'selection', directoryEntityToString);
                var directoryEntity = self.inputField.data('selection');
                self.inputField.data('value', directoryEntity ? directoryEntity.id : null);
            });
        });

        this.isValid = function(value) {
            return value && value.type && value.displayName;
        };

        this.setValue = function(value) {
            if (_.isString(value)) {
                self.inputField.val(value);
                self.dataRequest('directory', 'getById', value)
                  .then(function(results) {
                      if (results && results.id && results.type) {
                          self.setValue(results);
                      }
                  });
            } else {
                self.inputField.val(directoryEntityToString(value));
                onSelectionFieldSelected(value);
            }
        };

        this.getValue = function() {
            return self.inputField.data('selection');
        };

        function directorySearch(search, process) {
            self.map = {};

            self.dataRequest('directory', 'search', search)
              .then(function(results) {
                  var entities = _.map(results.entities, function(entity) {
                      var str = directoryEntityToString(entity);
                      self.map[str] = entity;
                      return str;
                  });
                  process(entities);
                  TypeAheadUtil.adjustDropdownPosition(self.inputField);
              });
        }

        function directoryEntityToString(directoryEntity) {
            if (directoryEntity && directoryEntity.type) {
                return directoryEntity.displayName + ' (' + (directoryEntity.type === 'group' ? 'Group' : 'Person') + ')';
            } else {
                return '';
            }
        }

        function onSelectionFieldSelected(directoryEntity) {
            self.inputField
              .data('selection', directoryEntity)
              .data('value', directoryEntity.id)
              .trigger('directorySelectionFieldSelected', { personOrGroup: directoryEntity });
        }
    }
});
