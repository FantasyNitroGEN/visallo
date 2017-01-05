define([
    './controlDragPopover',
    './findPathPopoverShim',
    './createConnectionPopover',
    './controlDragPopoverTpl',
    './createConnectionPopoverTpl',
    './findPathPopoverTpl'
], function(ControlDrag, FindPath, CreateConnection) {

    return function(connectionType) {
        return connectionType === 'CreateConnection' ?
            CreateConnection :
            connectionType === 'FindPath' ?
            FindPath :
            ControlDrag
    }
})
