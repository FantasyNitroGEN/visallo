define(['react', 'util/dnd'], function(React, dnd) {
    'use strict';

    const Events = 'dragover dragenter dragleave drop'.split(' ');
    const { PropTypes } = React;
    const DroppableHOC = (WrappedComponent, cls) => React.createClass({
        displayName: `DroppableHOC(${WrappedComponent.displayName || 'Component'})`,
        propTypes: {
            mimeTypes: PropTypes.arrayOf(PropTypes.string).isRequired,
            onDrop: PropTypes.func.isRequired,
            style: PropTypes.object
        },
        getInitialState() {
            return { cls: '' }
        },
        componentDidMount() {
            Events.forEach(event => {
                if (event in this) {
                    this.refs.div.addEventListener(event, this[event], false)
                } else console.error('No handler for event: ' + event);
            })
        },
        componentWillUnmount() {
            Events.forEach(event => {
                if (event in this) {
                    this.refs.div.removeEventListener(event, this[event])
                } else console.error('No handler for event: ' + event);
            })
        },
        dataTransferHasValidMimeType(dataTransfer) {
            return dnd.dataTransferHasValidMimeType(dataTransfer, this.props.mimeTypes)
        },
        dragover(event) {
            const { dataTransfer } = event;
            if (this.dataTransferHasValidMimeType(dataTransfer)) {
                event.preventDefault();
                event.stopPropagation();
            }
        },
        dragleave(event) {
            _.delay(() => {
                const index = this.dragstack.indexOf(event.target);
                if (index >= 0) {
                    this.dragstack.splice(index, 1);
                }
                if (!this.dragstack.length) {
                    this.toggleClass(false);
                }
            }, 1);
        },
        dragenter(event) {
            if (!this.dragstack) this.dragstack = [];

            if (!this.dragstack.length) {
                if (this.dataTransferHasValidMimeType(event.dataTransfer)) {
                    this.toggleClass(true);
                }
            }
            this.dragstack.push(event.target);
        },
        drop(event) {
            if (!this.dataTransferHasValidMimeType(event.dataTransfer)) {
                return;
            }
            event.preventDefault();
            event.stopPropagation();
            this.toggleClass(false);

            const { pageX, pageY } = event;
            const box = (cls ? $(event.target).closest(cls)[0] : event.target)
                .getBoundingClientRect();

            var positionTransform, comp = this.refs.wrapped;
            while (!positionTransform) {
                if (!comp) break;
                if (comp && comp.droppableTransformPosition) {
                    positionTransform = comp.droppableTransformPosition;
                    break;
                }
                comp = comp.refs.wrapped;
            }
            const position = (positionTransform || _.identity)({
                x: pageX - box.left,
                y: pageY - box.top
            });

            this.props.onDrop(event, position);
        },
        toggleClass(toggle) {
            const cls = toggle ? 'accepts-draggable' : '';
            if (this.state.cls !== cls) {
                this.setState({ cls });
            }
        },
        render() {
            const { cls } = this.state;
            const { onDrop, mimeTypes, style = { position: 'relative' }, ...props} = this.props;
            return (
                <div ref="div" style={style} className={cls}><WrappedComponent ref="wrapped" {...props} /></div>
            )
        }
    });

    return DroppableHOC
});
