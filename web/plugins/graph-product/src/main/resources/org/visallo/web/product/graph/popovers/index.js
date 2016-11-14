define([
    './controlDragPopover',
    './findPathPopover',
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
