define([
    'react',
    'react-redux',
    './BrowserDirectory'
], function(React, redux, BrowserDirectory) {
    'use strict';

    const Browser = React.createClass({
        render() {
            const { contentsByDir, cwd, onOpenDirectory, onRefreshDirectories, onSelectItem, selected } = this.props;
            const path = cwd.join('/')
            const contents = contentsByDir[path];

            return (
                <div className="ingest-s3-browser">
                    <h1 title={path}>/{path}</h1>
                    <div className="buttons">
                        <button className="parent-dir btn btn-mini" onClick={this.onParentDirectory} disabled={cwd.length === 0}>Parent Directory</button>
                        <button className="refresh" onClick={onRefreshDirectories} />
                    </div>
                    <BrowserDirectory
                        onOpenDirectory={onOpenDirectory}
                        onSelectItem={onSelectItem}
                        selected={selected}
                        contents={contents} />
                
                    <button className="btn btn-primary" disabled={selected.length === 0} onClick={this.onImport}>Import</button>
                </div>
            );
        },
        onParentDirectory() {
            this.props.onOpenDirectory('..')
        },
        onImport() {
            const { auth, contentsByDir, cwd, selected } = this.props;
            const fullPath = cwd.join('/')
            const contents = contentsByDir[fullPath];
            const bucket = cwd[0];
            const path = cwd.slice(1).join('/');

            if (contents) {
                this.props.onImport({
                    auth,
                    bucket,
                    paths: selected.map(s => `${path}/${s}`)
                });
            }
        }
    });

    return Browser;
});
