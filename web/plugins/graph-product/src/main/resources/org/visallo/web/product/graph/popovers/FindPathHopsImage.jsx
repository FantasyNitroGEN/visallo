define([
    'create-react-class',
    'prop-types'
], function(createReactClass, PropTypes) {
    'use strict';

    const FindPathHopsImage = createReactClass({
        propTypes: {
            hops: PropTypes.number.isRequired,
            hopsTitle: PropTypes.string.isRequired,
            onChange: PropTypes.func.isRequired
        },
        render() {
            const { hops, hopsTitle, onChange } = this.props;

            return (
                <svg className={`hops${hops}`} role="img" viewBox="0 0 165 65">
                    <title>{hopsTitle}</title>
                    <style>{this.generateStyle()}</style>
                    <g transform="matrix(1,0,0,1,-77,-150)">
                        <g transform="matrix(1,0,0,1,78,152)">

                            <g id="Edges" transform="matrix(1,0,0,1,6,5)">

                                // Larger click targets
                                <path onClick={onChange.bind(null, 2)} className="hops2 clickable" 
                                      d="M0.099,23.69C0.099,23.69 63.613,-27.441 151.005,23.69" />
                                <path onClick={onChange.bind(null, 3)} className="hops3 clickable"
                                      d="M0.383,23.871C0.383,23.871 59.074,6.084 151.2,23.871" />
                                <path onClick={onChange.bind(null, 4)} className="hops4 clickable"
                                      d="M0.52,23.639C0.52,23.639 64.355,42.166 151.172,23.639" />
                                <path onClick={onChange.bind(null, 5)} className="hops5 clickable"
                                      d="M0.77,23.403C0.77,23.403 72.161,79.434 150.625,23.403" />

                                <path onClick={onChange.bind(null, 2)} className="hops2" 
                                      d="M0.099,23.69C0.099,23.69 63.613,-27.441 151.005,23.69" />
                                <path onClick={onChange.bind(null, 3)} className="hops3"
                                      d="M0.383,23.871C0.383,23.871 59.074,6.084 151.2,23.871" />
                                <path onClick={onChange.bind(null, 4)} className="hops4"
                                      d="M0.52,23.639C0.52,23.639 64.355,42.166 151.172,23.639" />
                                <path onClick={onChange.bind(null, 5)} className="hops5"
                                      d="M0.77,23.403C0.77,23.403 72.161,79.434 150.625,23.403" />

                            </g>

                            <g id="Vertices" transform="matrix(1,0,0,1,45,0)">
                                <g onClick={onChange.bind(null, 2)} className="hops2">
                                    <circle cx="35.107" cy="5.764" r="5.772" />
                                </g>
                                <g onClick={onChange.bind(null, 3)} className="hops3">
                                    <circle cx="26.219" cy="20.91" r="5.772" />
                                    <circle cx="44.679" cy="20.91" r="5.772" />
                                </g>
                                <g onClick={onChange.bind(null, 4)} className="hops4">
                                    <circle cx="16.648" cy="36.056" r="5.772" />
                                    <circle cx="35.107" cy="37.412" r="5.772" />
                                    <circle cx="53.566" cy="36.056" r="5.772" />
                                </g>
                                <g onClick={onChange.bind(null, 5)} className="hops5">
                                    <circle cx="5.811" cy="48.523" r="5.772" />
                                    <circle cx="26.27" cy="53.236" r="5.772" />
                                    <circle cx="46.73" cy="53.236" r="5.772" />
                                    <circle cx="67.189" cy="48.845" r="5.772" />
                                </g>
                            </g>
                            <g id="Source" transform="matrix(1,0,0,1,0,22)">
                                <circle className="sourceDest" cx="5.944" cy="6.616" r="5.772" />
                            </g>
                            <g id="Target" transform="matrix(1,0,0,1,150,22)">
                                <circle className="sourceDest" cx="6.792" cy="6.616" r="5.772" />
                            </g>
                        </g>
                    </g>
                </svg>
            )
        },

        generateStyle() {
            return `
                path {
                    fill:none;
                    stroke:rgb(228,228,228);
                    stroke-width:1px;
                    stroke-dasharray:2,2;
                }
                circle {
                    fill:rgb(239,239,239);
                    stroke:rgb(206,203,203);
                    stroke-width:1px;
                }
                circle.sourceDest {
                    fill:rgb(231,164,5);
                    stroke: none;
                }
                #Edges path.clickable {
                    fill:none;
                    stroke:white;
                    stroke-dasharray:none;
                    stroke-width:10px;
                }
                .hops2 path.hops2,
                .hops3 path.hops2, .hops3 path.hops3,
                .hops4 path.hops2, .hops4 path.hops3, .hops4 path.hops4,
                .hops5 path.hops2, .hops5 path.hops3, .hops5 path.hops4, .hops5 path.hops5 {
                    fill:none;
                    stroke:rgb(135,199,231);
                    stroke-width:1px;
                    stroke-dasharray: none;
                }
                .hops2 .hops2 circle,
                .hops3 .hops2 circle, .hops3 .hops3 circle,
                .hops4 .hops2 circle, .hops4 .hops3 circle, .hops4 .hops4 circle,
                .hops5 .hops2 circle, .hops5 .hops3 circle, .hops5 .hops4 circle, .hops5 .hops5 circle {
                    fill:rgb(135,199,231);
                    stroke: none;
                }
            `
        }
    });

    return FindPathHopsImage;
});
