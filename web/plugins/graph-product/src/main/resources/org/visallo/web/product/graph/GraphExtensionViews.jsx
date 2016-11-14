define([
    'react',
    'components/Attacher'
], function(React, Attacher) {
    'use strict';

    const GraphExtensionViews = function(props) {
        const { views, panelPadding } = props;
        const { left, right, top, bottom } = panelPadding;

        return (
            <div className="graph-views"
                 style={{ left, right, top, bottom, backgroundColor: 'rgba(255,0,0,0.1)' }}>
                {
                    views.map(({ componentPath, className }) => {
                        return (
                            <div className={className} key={componentPath}>
                                <Attacher componentPath={componentPath} />
                            </div>
                        );
                    })
                }
            </div>
        )
    };

    return GraphExtensionViews;
});
