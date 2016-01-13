
define([
    'flight/lib/component',
    'hbs!./image-tpl',
    'util/vertex/formatters',
    'util/privileges',
    'util/detectedObjects/withFacebox',
    'require'
], function(
    defineComponent,
    template,
    F,
    Privileges,
    withFacebox,
    require) {
    'use strict';

    return defineComponent(ImageView, withFacebox);

    function ImageView() {

        this.attributes({
            model: null,
            imageSelector: 'img',
            artifactImageSelector: '.image-preview'
        });

        this.after('initialize', function() {
            this.$node
                .addClass('org-visallo-image loading')
                .html(template({
                    src: F.vertex.imageDetail(this.attr.model),
                    id: this.attr.model.id
                }));

            this.on('detectedObjectCoordsChange', this.onCoordsChanged);

            var self = this,
                image = this.select('imageSelector'),
                imageEl = image.get(0),
                naturalWidth = imageEl.naturalWidth,
                naturalHeight = imageEl.naturalHeight;

            if (naturalWidth === 0 || naturalHeight === 0) {
                image.on('load', this.onImageLoaded.bind(this))
            } else {
                this.onImageLoaded();
            }
        });

        this.onCoordsChanged = function(event, data) {
            var self = this,
                vertex = this.attr.model,
                width = parseFloat(data.x2) - parseFloat(data.x1),
                height = parseFloat(data.y2) - parseFloat(data.y1),
                artifactImage = this.$node.find('.image-preview'),
                isLargeEnough = (artifactImage.width() * width) > 5 &&
                    (artifactImage.height() * height) > 5,
                detectedObject;

            if (data.id && data.id !== 'NEW') {
                detectedObject = _.first(F.vertex.props(vertex, 'detectedObject', data.id));
            } else {
                data = _.omit(data, 'id');
            }

            this.trigger('detectedObjectEdit', isLargeEnough ?
                {
                    property: detectedObject,
                    value: data
                } : null
            );
        };


        this.onImageLoaded = function() {
            this.$node.removeClass('loading');

            if (Privileges.missingEDIT) {
                return this.$node.css('cursor', 'default')
            }

            var artifactImage = this.select('artifactImageSelector');
            this.initializeFacebox(artifactImage);
        }
    }
});
