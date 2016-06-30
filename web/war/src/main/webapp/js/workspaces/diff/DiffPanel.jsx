define([
    'react',
    'classnames',
    'util/vertex/formatters',
    'util/privileges'
], function(React, classNames, F, Privileges) {
    'use strict';

    var PropTypes = React.PropTypes;

    function formatVisibility(propertyOrProperties) {
        const property = Array.isArray(propertyOrProperties) ? propertyOrProperties[0] : propertyOrProperties;
        return JSON.stringify(property['http://visallo.org#visibilityJson']);
    }

    function formatValue(name, change, property) {
        return F.vertex.prop({
            id: property.id,
            properties: change ? Array.isArray(change) ? change : [change] : []
        }, name, property.key)
    }

    const DiffPanel = React.createClass({

        renderHeader: function(diffs) {
            const { publishing, undoing, onApplyPublishClick, onApplyUndoClick } = this.props;
            const flatDiffs = diffs.reduce((flat, diff) => {
                const { type } = diff.action;
                const nonUpdates = type !== 'update' ? [ diff ] : [];
                return [...flat, ...nonUpdates, ...diff.properties];
            }, []);
            const { publishCount, undoCount } = flatDiffs.reduce(({ publishCount, undoCount }, { publish, undo }) => ({
                publishCount: publish ? publishCount + 1 : publishCount,
                undoCount: undo ? undoCount + 1 : undoCount
            }), { publishCount: 0, undoCount: 0 });
            const publishingAll = publishCount === flatDiffs.length;
            const undoingAll = undoCount === flatDiffs.length;

            return (
              <div className="diff-header">
                  {this.renderHeaderActions(publishingAll, undoingAll)}

                  <h1 className="header">
                    {publishing || publishCount > 0 ? (
                        <button className={
                            classNames('btn btn-small publish-all btn-success', {
                                loading: publishing
                            })}
                            onClick={onApplyPublishClick}
                            disabled={publishing || undoing}
                            data-count={F.number.pretty(publishCount)}>
                            { i18n('workspaces.diff.button.publish') }
                        </button>
                    ) : null}
                    {undoing || undoCount > 0 ? (
                        <button className={
                            classNames('btn btn-small undo-all btn-danger', {
                                loading: undoing
                            })}
                            onClick={onApplyUndoClick}
                            disabled={publishing || undoing}
                            data-count={F.number.pretty(undoCount)}>
                            { i18n('workspaces.diff.button.undo') }
                        </button>
                    ) : null}

                    {publishCount === 0 && undoCount === 0 ? (
                        <span>{ i18n('workspaces.diff.header.unpublished_changes') }</span>
                    ) : null}
                  </h1>
              </div>
            );
        },

        renderHeaderActions: function(publishingAll, undoingAll) {
            const { publishing, undoing, onSelectAllPublishClick, onSelectAllUndoClick, onDeselectAllClick } = this.props;
            const applying = publishing || undoing;

            return (
              <div className="select-actions">
                  <span>{ i18n('workspaces.diff.button.select_all') }</span>
                  <div className="btn-group actions">
                    {Privileges.canPUBLISH ? (
                        <button className={
                            classNames('btn btn-mini select-all-publish requires-PUBLISH', {
                                'btn-success': publishingAll
                            })}
                            onClick={publishingAll ? onDeselectAllClick : onSelectAllPublishClick}
                            disabled={applying}
                            data-action="publish">
                            {i18n('workspaces.diff.button.publish')}
                        </button>
                    ) : null}
                    {Privileges.canPUBLISH ? (
                        <button className={
                            classNames('btn btn-mini select-all-undo requires-EDIT', {
                                'btn-danger': undoingAll
                            })}
                            onClick={undoingAll ? onDeselectAllClick : onSelectAllUndoClick}
                            disabled={applying}
                            data-action="undo">
                            {i18n('workspaces.diff.button.undo')}
                        </button>
                    ) : null}
                  </div>
              </div>
            );
        },

        renderDiffActions: function(id, { publish, undo }) {
            const { publishing, undoing, onPublishClick, onUndoClick } = this.props;
            const applying = publishing || undoing;

            return (
                <td className="actions">
                    <div className="btn-group">
                        {Privileges.canPUBLISH ? (
                            <button className={
                                classNames('btn', 'btn-mini', 'publish', 'requires-PUBLISH', {
                                    'btn-success': publish
                                })}
                                onClick={e => {
                                    e.stopPropagation();
                                    onPublishClick(id);
                                }}
                                disabled={applying}>
                                {i18n('workspaces.diff.button.publish')}
                            </button>
                        ) : null}
                        {Privileges.canEDIT ? (
                            <button className={
                                classNames('btn', 'btn-mini', 'undo', 'requires-EDIT', {
                                    'btn-danger': undo
                                })}
                                onClick={e => {
                                    e.stopPropagation();
                                    onUndoClick(id);
                                }}
                                disabled={applying}>
                                {i18n('workspaces.diff.button.undo')}
                            </button>
                        ) : null}
                    </div>
                </td>
            );
        },

        renderVertexDiff: function(diff) {
            const { action, active, className, conceptImage, deleted, publish, selectedConceptImage, title, undo, vertex, vertexId } = diff;
            const { onVertexRowClick } = this.props;
            const conceptImageStyle = {
                backgroundImage: conceptImage || vertex ? `url(${conceptImage || F.vertex.image(vertex, null, 80)})` : ''
            };
            const selectedConceptImageStyle = {
                backgroundImage: selectedConceptImage || vertex ? `url${selectedConceptImage || F.vertex.selectedImage(vertex, null, 80)})` : ''
            };

            return (
                <tr className={
                    classNames('vertex-row', className, {
                        'mark-publish': publish,
                        'mark-undo': undo,
                        active: active,
                        deleted: deleted
                    })}
                    onClick={() => onVertexRowClick(active ? null : vertexId)}
                    data-diff-id={ vertexId }
                    data-vertex-id={ vertexId }>
                    <th className="vertex-label" colSpan="2">
                        <div className="img" style={conceptImageStyle}></div>
                        <div className="selected-img" style={selectedConceptImageStyle}></div>
                        <h1 data-vertex-id={ vertexId }>
                            {title}
                            {vertex['http://visallo.org#visibilityJson'] ? (
                                <div
                                    className="visibility"
                                    data-visibility={JSON.stringify(vertex['http://visallo.org#visibilityJson'])} />
                            ) : null}
                        </h1>
                        {action.type !== 'update' ? (
                            <span className="label action-type">{ action.display }</span>
                        ) : null}
                    </th>
                    {action.type !== 'update' ?
                        this.renderDiffActions(vertexId, diff) : (
                        <td>&nbsp;</td>
                    )}
                </tr>
            );
        },

        renderEdgeDiff: function(diff) {
            const { action, active, className, deleted, edge, edgeId, edgeLabel, publish, sourceTitle, targetTitle, undo } = diff;
            const { onEdgeRowClick } = this.props;

            return (
                <tr className={
                    classNames('vertex-row', className, {
                        'mark-publish': publish,
                        'mark-undo': undo,
                        active: active,
                        deleted: deleted
                    })}
                    onClick={() => onEdgeRowClick(active ? null : edgeId)}
                    data-diff-id={edgeId}
                    data-edge-id={edgeId}>
                    <th className="vertex-label" colSpan="2">
                        <div className="img" />
                        <h1 data-edge-id={ edgeId }>
                            {sourceTitle}
                            <span className="edge-label">{edgeLabel + ' '}</span>
                            {targetTitle}
                            {edge['http://visallo.org#visibilityJson'] ? (
                                <div
                                    className="visibility"
                                    data-visibility={JSON.stringify(edge['http://visallo.org#visibilityJson'])} />
                            ) : null}
                        </h1>

                        {action.type !== 'update' ? (
                            <span className="label action-type">{ action.display }</span>
                        ) : null}
                    </th>
                    {action.type !== 'update' ?
                        this.renderDiffActions(edgeId, diff) : (
                        <td>&nbsp;</td>
                    )}
                </tr>
            );
        },

        renderPropertyDiff: function(property) {
            const { className, deleted, id, name, new: nextProp, old: previousProp, publish, undo } = property;
            const { formatLabel } = this.props;
            const nextVisibility = nextProp ? formatVisibility(nextProp) : null;
            const visibility = previousProp ? formatVisibility(previousProp) : null;
            const nextValue = nextProp ? formatValue(name, nextProp, property) : null;
            const value = previousProp ? formatValue(name, previousProp, property) : null;
            const valueStyle = value !== nextValue ? { textDecoration: 'line-through'} : {};
            const visibilityStyle = visibility !== nextVisibility ? { textDecoration: 'line-through'} : {};

            return (
                <tr className={
                    classNames(className, {
                        'mark-publish': publish,
                        'mark-undo': undo
                    })}
                    data-diff-id={id}>
                <td className="property-label">{ formatLabel(name) }</td>
                <td className={
                    classNames('property-value', {
                        deleted: deleted
                    })}>
                    {previousProp && nextProp ? (
                        <div>
                            {nextValue}
                            <div className="visibility" data-visibility={nextVisibility} />
                            <div style={valueStyle}>{value}</div>
                            <div
                                className="visibility"
                                data-visibility={visibility}
                                style={visibilityStyle} />
                        </div>
                    ) : null}
                    {!previousProp && nextProp ? (
                        <div>
                            {nextValue}
                            <div className="visibility" data-visibility={nextVisibility} />
                        </div>
                    ) : null}
                </td>
                {this.renderDiffActions(id, property)}
              </tr>
            );
        },

        renderDiff: function(diff) {
            let diffRows = [];
            if (diff.vertex) {
                diffRows = [this.renderVertexDiff(diff)];
            } else if (diff.edge) {
                diffRows = [this.renderEdgeDiff(diff)];
            }
            return [...diffRows, ...diff.properties.map(this.renderPropertyDiff)];
        },

        render: function() {
            const { diffs } = this.props;
            const flatDiffs = diffs.reduce((flat, diff) => {
                return [...flat, diff, ...diff.properties];
            }, []);

            return (
                <div className="diffs-list">
                    {this.renderHeader(diffs)}
                    <div className="diff-content">
                        <table className="table">
                            <tbody>
                                {diffs.map(this.renderDiff)}
                            </tbody>
                        </table>
                    </div>
                </div>
            );
        },

        propTypes: {
            diffs: PropTypes.array.isRequired,
            formatLabel: PropTypes.func.isRequired,
            onPublishClick: PropTypes.func.isRequired,
            onUndoClick: PropTypes.func.isRequired,
            onSelectAllPublishClick: PropTypes.func.isRequired,
            onSelectAllUndoClick: PropTypes.func.isRequired,
            onDeselectAllClick: PropTypes.func.isRequired,
            publishing: PropTypes.bool,
            undoing: PropTypes.bool,
            onApplyPublishClick: PropTypes.func.isRequired,
            onApplyUndoClick: PropTypes.func.isRequired,
            onVertexRowClick: PropTypes.func.isRequired,
            onEdgeRowClick: PropTypes.func.isRequired
        }
    });

    return React.createFactory(DiffPanel);
});
