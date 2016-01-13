define([], function() {
    'use strict';

    return withCollapsibleSections;

    function withCollapsibleSections() {

        this.attributes({
            collapsibleToggleSelector: '.collapsible .collapsible-header'
        });

        this.after('initialize', function() {
            this.on('click', {
                collapsibleToggleSelector: this.onToggleCollapsibleSection
            });
        });

        this.onToggleCollapsibleSection = function(event) {
            $(event.target).closest('.collapsible').toggleClass('expanded');
            event.stopPropagation();
        }
    }
});
