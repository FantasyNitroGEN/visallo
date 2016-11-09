define([
    './controlDragPopover',
    './findPathPopover',
    './createConnectionPopover',
    './controlDragPopoverTpl',
    './createConnectionPopoverTpl',
    './findPathPopoverTpl'
], function(ControlDrag, FindPath, CreateConnection) {

    return function(connectionType) {
        console.log(connectionType);

        return connectionType === 'CreateConnection' ?
            CreateConnection :
            connectionType === 'FindPath' ?
            FindPath :
            ControlDrag
    }
})
