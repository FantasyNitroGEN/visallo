
define([], function() {
    'use strict';

    function BetterGrid(options) {
        this.spaceX = options && options.spaceX || 390;
        this.spaceY = options && options.spaceY || 150;
        this.options = options;
    }

    BetterGrid.prototype.run = function() {
        var self = this,
            cy = this._private.cy,
            nodes = this.options.eles,
            bb = nodes.boundingBox(),
            len = nodes.length,
            x = bb.x1,
            y = bb.y1,
            linebreak = Math.round(
                    Math.sqrt(len * (this.spaceY / this.spaceX))
                ) * this.spaceX,
            getPos = function(i, node) {
                if ((x - bb.x1) > linebreak) {
                  x = bb.x1;
                  y += self.spaceY;
                }

                var position = { x: x, y: y };
                x += self.spaceX;
                return position;
            };

        this.options.eles.layoutPositions(this, this.options, getPos);

        return this;
    }

    return BetterGrid;
})
