define([
    'flight/lib/component',
    'util/withDropdown',
    'tpl!./commentForm',
    'tpl!util/alert',
    'util/vertex/formatters',
    'util/withDataRequest'
], function(
    defineComponent,
    withDropdown,
    commentTemplate,
    alertTemplate,
    F,
    withDataRequest) {
    'use strict';

    return defineComponent(CommentForm, withDropdown, withDataRequest);

    function CommentForm() {

        this.defaultAttrs({
            inputSelector: 'textarea',
            primarySelector: '.btn-primary'
        });

        this.before('initialize', function(n, c) {
            c.manualOpen = true;
        })

        this.after('initialize', function() {
            var self = this;

            this.on('change keyup paste', {
                inputSelector: this.onChange
            });
            this.on('click', {
                primarySelector: this.onSave
            });
            this.on('visibilitychange', this.onVisibilityChange);

            this.$node.html(commentTemplate({
                graphVertexId: this.attr.data.id,
                commentText: this.attr.comment && this.attr.comment.value || '',
                buttonText: i18n('detail.comment.form.button')
            }));

            this.on('opened', function() {
                this.select('inputSelector').focus();
            });

            require([
                'util/visibility/edit'
            ], function(Visibility) {
                Visibility.attachTo(self.$node.find('.visibility'), {
                    value: self.attr.comment &&
                        self.attr.comment.metadata &&
                        self.attr.comment.metadata['http://visallo.org#visibilityJson'] &&
                        self.attr.comment.metadata['http://visallo.org#visibilityJson'].source
                });
                self.manualOpen();
            });

            this.checkValid();
        });

        this.onChange = function(event) {
            this.checkValid();
        };

        this.onVisibilityChange = function(event, data) {
            this.visibilitySource = data;
            this.checkValid();
        };

        this.onSave = function(event) {
            var self = this,
                comment = this.attr.comment,
                metadata = comment && comment.metadata,
                visibilityJson = metadata && metadata['http://visallo.org#visibilityJson'],
                params = {
                    name: 'http://visallo.org/comment#entry',
                    key: comment && comment.key,
                    value: this.getValue(),
                    metadata: this.attr.path && {
                        'http://visallo.org/comment#path': this.attr.path
                    },
                    visibilitySource: this.visibilitySource && this.visibilitySource.value || '',
                    sourceInfo: this.attr.sourceInfo
                };

            if (visibilityJson) {
                params.oldVisibilitySource = visibilityJson.source;
            }

            this.buttonLoading();

            this.dataRequest(this.attr.type, 'setProperty', this.attr.data.id, params)
                .then(function() {
                    self.teardown();
                })
                .catch(function(error) {
                    self.markFieldErrors(error);
                    self.clearLoading();
                })
        };

        this.getValue = function() {
            return $.trim(this.select('inputSelector').val());
        };

        this.checkValid = function() {
            var val = this.getValue();

            if (val.length && this.visibilitySource && this.visibilitySource.valid) {
                this.select('primarySelector').removeAttr('disabled');
            } else {
                this.select('primarySelector').attr('disabled', true);
            }
        }
    }
});
