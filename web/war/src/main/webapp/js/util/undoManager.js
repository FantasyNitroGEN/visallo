
define(['jquery'], function() {
    'use strict';

    /**
     * Manages undo,redo stack, and keyboard events to trigger
     *
     * After an action:
     *
     *  undoManager.performedAction( actionName, undoFunction, redoFunction);
     */
    function UndoManager() {
        var self = this;

        this.undos = [];
        this.redos = [];
    }

    UndoManager.prototype.performedAction = function(name, options) {
        if (name &&
             options &&
             typeof options.undo === 'function' &&
             typeof options.redo === 'function') {

            this.undos.push({
                name: name,
                undo: options.undo.bind(options.bind || this),
                redo: options.redo.bind(options.bind || this)
            });
        } else {
            throw new Error('Invalid performedAction arguments');
        }
    };

    UndoManager.prototype.canUndo = function() {
        return !!this.undos.length;
    };

    UndoManager.prototype.canRedo = function() {
        return !!this.redos.length;
    };

    UndoManager.prototype.reset = function() {
        this.undos = [];
        this.redos = [];
    };

    UndoManager.prototype.performUndo = function() {
        _performWithStacks('undo', this.undos, this.redos);
    };

    UndoManager.prototype.performRedo = function() {
        _performWithStacks('redo', this.redos, this.undos);
    };

    function _performWithStacks(name, stack1, stack2) {
        var action, undo;

        if (stack1.length) {
            action = stack1.pop();
            undo = action.undo;

            undo();

            stack2.push({
                action: action.name,
                undo: action.redo,
                redo: action.undo
            });
        }
    }

    return UndoManager;
});
