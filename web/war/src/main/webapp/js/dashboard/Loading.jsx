define([
    'react'
], function(React) {
    'use strict';

    const Loading = function(props) {
        var style = {
                position: 'absolute',
                top: '60%',
                left: '50%',
                transform: 'translate(-50%,0)'
            },
            img = {
                minWidth: '100px',
                background: 'url(img/loading-large@2x.png) no-repeat top',
                backgroundSize: '100%',
                position: 'absolute',
                width: '100%',
                height: '200px',
                bottom: '30px',
                maxWidth: '200px',
                backgroundPosition: 'bottom center',
                left: '50%',
                transform: 'translate(-50%, 0)'
            },
            text = {
                fontSize: '120%',
                textAlign: 'center',
                color: '#ccc',
                fontWeight: 'normal',
                fontFamily: 'HelveticaNeue-Light,Helvetica',
                letterSpacing: '0.7px',
                margin: 0,
                padding: 0,
                lineHeight: '1.2em'
            };

        return (
            <div style={style}>
                <h1 style={text}>{props.message}
                    <div style={img} />
                </h1>
            </div>
        )
    };

    return Loading;
});
