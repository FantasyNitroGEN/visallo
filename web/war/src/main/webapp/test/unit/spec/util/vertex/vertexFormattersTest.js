
define(['util/vertex/formatters'], function(f) {
    'use strict';

    var V = f.vertex,
        VERTEX_ERROR = 'Vertex is invalid',
        PROPERTY_NAME_ERROR = 'Property name is invalid',
        PROPERTY_NAME_FIRST = 'http://visallo.org/dev#firstName',
        PROPERTY_NAME_LAST = 'http://visallo.org/dev#lastName',
        PROPERTY_NAME_TITLE = 'http://visallo.org#title',
        PROPERTY_NAME_RAW = 'http://visallo.org#raw',
        PROPERTY_NAME_BOOLEAN = 'http://visallo.org/dev#boolean',
        PROPERTY_NAME_DOUBLE = 'http://visallo.org/dev#duration',
        PROPERTY_NAME_DATE = 'http://visallo.org/dev#dateOnly',
        PROPERTY_NAME_DATETIME = 'http://visallo.org/dev#dateAndTime',
        PROPERTY_NAME_HEADING = 'http://visallo.org/testing#heading1',
        PROPERTY_NAME_INTEGER = 'http://visallo.org/testing#integer1',
        PROPERTY_NAME_NUMBER = 'http://visallo.org/testing#number1',
        PROPERTY_NAME_CURRENCY = 'http://visallo.org/dev#netIncome',
        PROPERTY_NAME_GENDER = 'http://visallo.org/dev#gender',
        PROPERTY_NAME_GEO = 'http://visallo.org/dev#geolocation',
        PROPERTY_NAME_CONCEPT = 'http://visallo.org#conceptType',
        PROPERTY_NAME_GEO_AND_DATE = 'http://visallo.org/dev#geoLocationAndDate',
        PROPERTY_NAME_GEO_AND_DATE_GEO = 'http://visallo.org/dev#geoLocationAndDate/geolocation',
        PROPERTY_NAME_GEO_AND_DATE_DATE = 'http://visallo.org/dev#geoLocationAndDate/date',
        PROPERTY_NAME_MP4_VIDEO = 'http://visallo.org#video-mp4',
        PROPERTY_NAME_WEBM_VIDEO = 'http://visallo.org#video-webm',
        PROPERTY_NAME_AAC_AUDIO = 'http://visallo.org#audio-aac',
        PROPERTY_NAME_OGG_AUDIO = 'http://visallo.org#audio-ogg',
        PROPERTY_NAME_MIMETYPE = 'http://visallo.org#mimeType',
        COMPOUND_PROPERTY_NAME = 'http://visallo.org/dev#name',
        COMPOUND_TEST_PROPERTY_NAME = 'http://visallo.org/testing#compound1',

        keyIdent = 0,
        vertexIdent = 0,
        addMetadata = function(property, key, value) {
            var newProp = _.extend({}, property);
            if (!newProp.metadata) {
                newProp.metadata = {};
            }
            newProp.metadata[key] = value;
            return newProp;
        },
        created = function(property, date) {
            return addMetadata(property, 'http://visallo.org#createDate', date.getTime());
        },
        confidence = function(property, confidence) {
            return addMetadata(property, 'http://visallo.org#confidence', confidence);
        },
        propertyFactory = function(name, optionalKey, value) {
            if (arguments.length === 2) {
                value = optionalKey;
                optionalKey = null;
            }
            return {
                name: name,
                key: (optionalKey === null || optionalKey === undefined) ? ('pKey' + keyIdent++) : optionalKey,
                value: value
            };
        },
        vertexFactory = function(id, properties) {
            if (_.isObject(id)) {
                properties = id;
                id = null;
            }
            return {
                id: id || ('testVertex' + vertexIdent++),
                properties: properties || []
            }
        }

    describe('vertex formatters', function() {

        describe('sandboxStatus', function() {
            it('should return undefined for published items', function() {
                var p = propertyFactory(PROPERTY_NAME_BOOLEAN, 'k1', true);

                delete p.sandboxStatus
                expect(V.sandboxStatus(p)).to.be.undefined
                p.sandboxStatus = 'PUBLIC'
                expect(V.sandboxStatus(p)).to.be.undefined
            })
            it('should return unpublished if sandboxed', function() {
                var p = propertyFactory(PROPERTY_NAME_BOOLEAN, 'k1', true);
                p.sandboxStatus = 'PRIVATE'
                expect(V.sandboxStatus(p)).to.equal('vertex.status.unpublished')
                p.sandboxStatus = 'PUBLIC_CHANGED'
                expect(V.sandboxStatus(p)).to.equal('vertex.status.unpublished')
            })
            it('should return unpublished if sandboxed', function() {
                var p = propertyFactory(PROPERTY_NAME_BOOLEAN, 'k1', true);
                p.sandboxStatus = 'PRIVATE'
                expect(V.sandboxStatus(p)).to.equal('vertex.status.unpublished')
                p.sandboxStatus = 'PUBLIC_CHANGED'
                expect(V.sandboxStatus(p)).to.equal('vertex.status.unpublished')
            })
            it('should check sandboxStatus of compound property', function() {
                var vertex = vertexFactory([
                        propertyFactory(PROPERTY_NAME_FIRST, 'k1', 'j'),
                        propertyFactory(PROPERTY_NAME_LAST, 'k1', 'h')
                    ]);

                vertex.sandboxStatus = 'PUBLIC'
                vertex.properties[0].sandboxStatus = 'PRIVATE'
                vertex.properties[1].sandboxStatus = 'PRIVATE'

                expect(V.sandboxStatus(vertex, COMPOUND_PROPERTY_NAME, 'k1')).to.equal('vertex.status.unpublished')
            })
            it('should check sandboxStatus of compound property with no matching properties', function() {
                var vertex = vertexFactory([]);

                vertex.sandboxStatus = 'PRIVATE'
                expect(V.sandboxStatus(vertex, COMPOUND_PROPERTY_NAME, 'k1')).to.be.undefined
            })
            it('should check sandboxStatus of compound property with different sandboxStatus', function() {
                var vertex = vertexFactory([
                    propertyFactory(PROPERTY_NAME_FIRST, 'k1', 'j'),
                    propertyFactory(PROPERTY_NAME_LAST, 'k1', 'h')
                ]);

                vertex.sandboxStatus = 'PUBLIC'
                vertex.properties[0].sandboxStatus = 'PRIVATE'
                vertex.properties[1].sandboxStatus = 'PUBLIC'
                expect(V.sandboxStatus(vertex, COMPOUND_PROPERTY_NAME, 'k1')).to.be.undefined
            })
        })

        describe('propDisplay', function() {
            it('should have propDisplay function', function() {
                V.should.have.property('propDisplay').that.is.a.function
            })

            it('should accept name and value and format', function() {
                V.propDisplay(PROPERTY_NAME_TITLE, 'test').should.equal('test')
                V.propDisplay(PROPERTY_NAME_BOOLEAN, true).should.equal('boolean.true')
                V.propDisplay(PROPERTY_NAME_NUMBER, 0).should.equal('0')
            })

            it('should accept options for process string values', function() {
                V.propDisplay(PROPERTY_NAME_TITLE, 'test', { uppercase: true }).should.equal('TEST')
                V.propDisplay(PROPERTY_NAME_TITLE, 'TEST', { lowercase: true }).should.equal('test')
                V.propDisplay(PROPERTY_NAME_TITLE, 'TeSt', { lowercase: false }).should.equal('TeSt')
                V.propDisplay(PROPERTY_NAME_TITLE, 'TeSt', { missingFormatter: true }).should.equal('TeSt')

                V.propDisplay(PROPERTY_NAME_TITLE, 'test string',
                    { palantirPrettyPrint: true }).should.equal('Test String')
            })
        })

        describe('prop', function() {
            it('should have prop function', function() {
                expect(V).to.have.property('prop').that.is.a.function
            })
            it('should pass options through', function() {
                var vertex = vertexFactory([
                        propertyFactory(PROPERTY_NAME_TITLE, 'k1', 'aAaA')
                    ]);

                V.prop(vertex, PROPERTY_NAME_TITLE, 'k1', { uppercase: true }).should.equal('AAAA')
            })
            it('should get display values for boolean', function() {
                var vertex = vertexFactory([
                        propertyFactory(PROPERTY_NAME_BOOLEAN, 'k1', true),
                        propertyFactory(PROPERTY_NAME_BOOLEAN, 'k2', false)
                    ]),
                    prop = _.partial(V.prop, vertex, PROPERTY_NAME_BOOLEAN);

                expect(prop()).to.equal('boolean.false')
                expect(prop('k1')).to.equal('boolean.true')
                expect(prop('k2')).to.equal('boolean.false')
            })
            it('should get display values for numbers', function() {
                var vertex = vertexFactory([
                        propertyFactory(PROPERTY_NAME_DOUBLE, 'k1', 22 / 7),
                        propertyFactory(PROPERTY_NAME_DOUBLE, 'k2', 1000000000000),
                        propertyFactory(PROPERTY_NAME_NUMBER, 'k3', 2),
                        propertyFactory(PROPERTY_NAME_INTEGER, 'k4', 3),
                        propertyFactory(PROPERTY_NAME_CURRENCY, 'k5', 4)
                    ]),
                    prop = _.partial(V.prop, vertex);

                expect(prop(PROPERTY_NAME_DOUBLE)).to.equal('1,000,000,000,000')
                expect(prop(PROPERTY_NAME_DOUBLE, 'k1')).to.equal('3.14')
                expect(prop(PROPERTY_NAME_DOUBLE, 'k2')).to.equal('1,000,000,000,000')
                expect(prop(PROPERTY_NAME_NUMBER, 'k3')).to.equal('2')
                expect(prop(PROPERTY_NAME_INTEGER, 'k4')).to.equal('3')
                expect(prop(PROPERTY_NAME_CURRENCY, 'k5')).to.equal('4')
            })
            it('should get display values for dates no time', function() {
                var vertex = vertexFactory([
                        propertyFactory(PROPERTY_NAME_DATE, new Date(2015, 1, 9).getTime())
                    ]),
                    prop = _.partial(V.prop, vertex, PROPERTY_NAME_DATE);

                expect(prop()).to.equal('2015-02-09')
            })
            it('should get display values for dates with time', function() {
                var vertex = vertexFactory([
                        propertyFactory(PROPERTY_NAME_DATETIME, new Date(2015, 1, 9, 8, 42).getTime())
                    ]),
                    prop = _.partial(V.prop, vertex, PROPERTY_NAME_DATETIME);

                expect(prop()).to.include('2015-02-09 08:42')
            })
            it('should get display values for heading', function() {
                var vertex = vertexFactory([
                        propertyFactory(PROPERTY_NAME_HEADING, 123)
                    ]),
                    prop = _.partial(V.prop, vertex, PROPERTY_NAME_HEADING);

                expect(prop()).to.equal('field.heading.southeast 123°')
            })
            it('should get display values for gender (possibleValues)', function() {
                var vertex = vertexFactory([
                        propertyFactory(PROPERTY_NAME_GENDER, 'k1', 'M'),
                        propertyFactory(PROPERTY_NAME_GENDER, 'k2', 'F')
                    ]),
                    prop = _.partial(V.prop, vertex, PROPERTY_NAME_GENDER);

                expect(prop()).to.equal('Female')
                expect(prop('k1')).to.equal('Male')
                expect(prop('k2')).to.equal('Female')
            })
            it('should get display values for geolocation', function() {
                var vertex = vertexFactory([
                        propertyFactory(PROPERTY_NAME_GEO, 'point[80,-40]'),
                        propertyFactory(PROPERTY_NAME_GEO, 'k1', {
                            latitude: 82.3413,
                            longitude: -43.2326
                        })
                    ]),
                    prop = _.partial(V.prop, vertex, PROPERTY_NAME_GEO);

                expect(prop()).to.equal('80.000, -40.000')
                expect(prop('k1')).to.equal('82.341, -43.233')
            })
            it('should get display values for geolocation and date', function() {
              var vertex = vertexFactory([
                  propertyFactory(PROPERTY_NAME_GEO_AND_DATE_DATE, 'k1', new Date(2015, 3, 1)),
                  propertyFactory(PROPERTY_NAME_GEO_AND_DATE_GEO, 'k1', {
                    latitude: 82.3413,
                    longitude: -43.2326
                  })
                ]),
                prop = _.partial(V.prop, vertex, PROPERTY_NAME_GEO_AND_DATE);

              expect(prop('k1')).to.equal('2015-04-01 82.341, -43.233')
            })
            it('should get display compound properties', function() {
                var vertex = vertexFactory([
                        propertyFactory(PROPERTY_NAME_FIRST, 'jason'),
                        propertyFactory(PROPERTY_NAME_FIRST, 'k1', 'jason'),
                        propertyFactory(PROPERTY_NAME_LAST, 'k1', 'harwig'),
                        propertyFactory(PROPERTY_NAME_LAST, 'k2', 'harwig')
                    ]),
                    prop = _.partial(V.prop, vertex, COMPOUND_PROPERTY_NAME);

                expect(prop()).to.equal('undefined, jason')
                expect(prop('k1')).to.equal('harwig, jason')
                expect(prop('k2')).to.equal('harwig, undefined')
            })
            it('should get display for empty string keys', function() {
                var vertex = vertexFactory([
                        propertyFactory(PROPERTY_NAME_TITLE, 'k2', 'harwig'),
                        propertyFactory(PROPERTY_NAME_TITLE, '', 'jason')
                    ]),
                    prop = _.partial(V.prop, vertex, PROPERTY_NAME_TITLE);

                expect(prop()).to.equal('harwig')
                expect(prop('')).to.equal('jason')
                expect(prop('k2')).to.equal('harwig')
            })
        })

        describe('props', function() {
            it('should throw error if invalid property name', function() {
                expect(V.props.bind(null, vertexFactory())).to.throw(PROPERTY_NAME_ERROR)
            });
            it('should throw error if invalid vertex', function() {
                expect(V.props.bind(null, {})).to.throw(VERTEX_ERROR)
                expect(V.props.bind(null, {id: 1})).to.throw(VERTEX_ERROR)
                expect(V.props.bind(null, {properties: []})).to.throw(VERTEX_ERROR)
                expect(V.props.bind(null, {id: 1, properties: 1})).to.throw(VERTEX_ERROR)
            });
            it('should return all props for vertex', function() {
                var vertex = vertexFactory([
                        propertyFactory(PROPERTY_NAME_FIRST, 'k1', 'jason'),
                        propertyFactory(PROPERTY_NAME_LAST, 'k1', 'harwig'),
                        propertyFactory(PROPERTY_NAME_FIRST, 'k2', 'jason2')
                    ]),
                    value = V.props(vertex, PROPERTY_NAME_FIRST);

                expect(value).to.be.an('array').and.have.property('length').that.equals(2);
                expect(value[0].value).to.equal('jason')
                expect(value[1].value).to.equal('jason2')
            })
            it('should return single property for vertex when key provided', function() {
                var vertex = vertexFactory([
                        propertyFactory(PROPERTY_NAME_FIRST, 'k1', 'jason'),
                        propertyFactory(PROPERTY_NAME_LAST, 'k1', 'harwig'),
                        propertyFactory(PROPERTY_NAME_FIRST, 'k2', 'jason2')
                    ]),
                    property = V.props(vertex, PROPERTY_NAME_FIRST, 'k2');

                expect(property[0].value).to.equal('jason2')
            })
            it('should return undefined for vertex when key provided and no match', function() {
                var vertex = vertexFactory([
                        propertyFactory(PROPERTY_NAME_FIRST, 'k1', 'jason'),
                        propertyFactory(PROPERTY_NAME_LAST, 'k1', 'harwig'),
                        propertyFactory(PROPERTY_NAME_FIRST, 'k2', 'jason2')
                    ]),
                    property = V.props(vertex, PROPERTY_NAME_FIRST, 'k3');

                expect(property).to.be.an('array').that.has.property('length').that.equals(0)
            })
            it('should throw error if key is passed but is undefined', function() {
                expect(function() {
                    V.props(vertexFactory(), PROPERTY_NAME_FIRST, undefined);
                }).to.throw('Undefined key')
            })
            it('should not throw error if key is passed is empty string', function() {
                var vertex = vertexFactory([
                        propertyFactory(PROPERTY_NAME_TITLE, '', 'jason'),
                        propertyFactory(PROPERTY_NAME_TITLE, 'k1', 'harwig')
                    ]),
                    properties = V.props(vertex, PROPERTY_NAME_TITLE, 'k1'),
                    emptyKeyProperties = V.props(vertex, PROPERTY_NAME_TITLE, '');

                expect(properties).to.be.an('array').that.has.property('length').that.equals(1)
                expect(properties[0].value).to.equal('harwig')

                expect(emptyKeyProperties).to.be.an('array').that.has.property('length').that.equals(1)
                expect(emptyKeyProperties[0].value).to.equal('jason')
            })
            it('should return all props for a compound property', function() {
                var vertex = vertexFactory([
                        propertyFactory(PROPERTY_NAME_FIRST, 'k1', '1'),
                        propertyFactory(PROPERTY_NAME_LAST, 'k1', '2')
                    ]),
                    props = V.props(vertex, COMPOUND_PROPERTY_NAME, 'k1');

                expect(props).to.be.an('array').that.has.property('length').that.equals(2)
                expect(props[0].value).to.equal('1')
                expect(props[1].value).to.equal('2')
            })
        })

        describe('longestProp', function() {
            it('should return only userVisible properties', function() {
                var vertex = vertexFactory([
                        propertyFactory(PROPERTY_NAME_CONCEPT, 'http://visallo.org/dev#person')
                    ]);

                expect(V.longestProp(vertex)).to.be.undefined
            })
            it('should return longest userVisible property value if no params', function() {
                var vertex = vertexFactory([
                        propertyFactory(PROPERTY_NAME_FIRST, 'k1', 'a'),
                        propertyFactory(PROPERTY_NAME_LAST, 'k1', 'aa')
                    ]);

                expect(V.longestProp(vertex)).to.equal('aa')
            })
            it('should return longest userVisible property value restricted to name', function() {
                var vertex = vertexFactory([
                        propertyFactory(PROPERTY_NAME_TITLE, 'a'),
                        propertyFactory(PROPERTY_NAME_TITLE, 'aa'),
                        propertyFactory(PROPERTY_NAME_LAST, 'last longer'),
                        propertyFactory(PROPERTY_NAME_TITLE, 'aaa'),
                        propertyFactory(PROPERTY_NAME_TITLE, 'bbb')
                    ]);

                expect(V.longestProp(vertex, PROPERTY_NAME_TITLE)).to.equal('aaa')
            })
            it('should return undefined if no properties', function() {
                var vertex = vertexFactory([]);
                expect(V.longestProp(vertex)).to.be.undefined
            })
            it('should return undefined if no properties matching name', function() {
                var vertex = vertexFactory([
                    propertyFactory(PROPERTY_NAME_FIRST, 'a')
                ]);
                expect(V.longestProp(vertex, PROPERTY_NAME_LAST)).to.be.undefined
            })
            it('should use compound property instead of dependents', function() {
                var vertex = vertexFactory([
                    propertyFactory(PROPERTY_NAME_TITLE, 'lastname1'),
                    propertyFactory(PROPERTY_NAME_FIRST, 'k1', 'first'),
                    propertyFactory(PROPERTY_NAME_LAST, 'k1', 'lastname'),
                    propertyFactory(PROPERTY_NAME_CONCEPT, 'http://visallo.org/dev#person')
                ]);
                V.longestProp(vertex).should.equal('lastname, first')
            })
            it('should use compound property instead of dependents even if compound is parent concept', function() {
                var vertex = vertexFactory([
                    propertyFactory(PROPERTY_NAME_TITLE, 'lastname1'),
                    propertyFactory(PROPERTY_NAME_FIRST, 'k1', 'first'),
                    propertyFactory(PROPERTY_NAME_LAST, 'k1', 'lastname'),
                    propertyFactory(PROPERTY_NAME_CONCEPT, 'http://visallo.org/dev#personSub')
                ]);
                V.longestProp(vertex).should.equal('lastname, first')
            })
            it('should not use compound properties that arent in concept', function() {
                var vertex = vertexFactory([
                    propertyFactory(PROPERTY_NAME_TITLE, 'lastname1'),
                    propertyFactory(PROPERTY_NAME_FIRST, 'k1', 'first'),
                    propertyFactory(PROPERTY_NAME_LAST, 'k1', 'lastname'),
                    propertyFactory(PROPERTY_NAME_CONCEPT, 'http://visallo.org/dev#location')
                ]);
                V.longestProp(vertex).should.equal('lastname1')
            })

        })

        describe('externalImage', function() {
            it('should transform urls to external resource', function() {
                var vertex = vertexFactory('v1', []),
                    url = V.externalImage(vertex, null, 'http://www.google/icon');

                url.should.include('workspaceId=w1')
                url.should.include('url=http%3A%2F%2Fwww.google%2Ficon')
                url.should.include('maxWidth=400')
                url.should.include('maxHeight=400')
            })
        })

        describe('title', function() {
            it('should get a title even if it refers to compound property', function() {
                var vertex = vertexFactory([
                        propertyFactory(PROPERTY_NAME_FIRST, 'k1', 'jason'),
                        propertyFactory(PROPERTY_NAME_LAST, 'k1', 'harwig'),
                        propertyFactory(PROPERTY_NAME_CONCEPT, 'http://visallo.org/dev#person')
                    ]);

                expect(V.title(vertex)).to.equal('harwig, jason')
            })
        })

        describe('propValid', function() {
            it('should validate property with existing values', function() {
                var vertex = vertexFactory([
                        propertyFactory(PROPERTY_NAME_FIRST, 'k1', 'jason'),
                        propertyFactory(PROPERTY_NAME_LAST, 'k1', 'harwig'),
                        propertyFactory(PROPERTY_NAME_LAST, 'k2', 'harwig'),
                        propertyFactory(PROPERTY_NAME_CONCEPT, 'http://visallo.org/dev#person')
                    ]);

                expect(V.propValid(vertex, [], COMPOUND_PROPERTY_NAME, 'k1')).to.be.true
                expect(V.propValid(vertex, [], COMPOUND_PROPERTY_NAME, 'k2')).to.be.false
            })

            it('should validate property with overriding values', function() {
                var vertex = vertexFactory([
                        propertyFactory(PROPERTY_NAME_FIRST, 'k1', 'jason'),
                        propertyFactory(PROPERTY_NAME_LAST, 'k1', 'harwig'),
                        propertyFactory(PROPERTY_NAME_LAST, 'k2', 'harwig'),
                        propertyFactory(PROPERTY_NAME_CONCEPT, 'http://visallo.org/dev#person')
                    ]);

                expect(V.propValid(vertex, ['override last name', undefined], COMPOUND_PROPERTY_NAME, 'k1')).to.be.false
                expect(V.propValid(vertex, ['last', 'first'], COMPOUND_PROPERTY_NAME, 'k2')).to.be.true
            })

            it('should accept compound values with nested arrays', function() {
                var vertex = vertexFactory([
                        propertyFactory(PROPERTY_NAME_CONCEPT, 'http://visallo.org/dev#person')
                    ]);

                expect(V.propValid(vertex, [['override last name'], [undefined]], COMPOUND_PROPERTY_NAME)).to.be.false
                expect(V.propValid(vertex, [['override last name'], ['first']], COMPOUND_PROPERTY_NAME)).to.be.true
            })

            it('should not modify vertex', function() {
                var vertex = vertexFactory([
                        propertyFactory(PROPERTY_NAME_FIRST, 'k1', 'jason'),
                        propertyFactory(PROPERTY_NAME_CONCEPT, 'http://visallo.org/dev#person')
                    ]);

                expect(V.propValid(vertex, ['harwig', 'j2'], COMPOUND_PROPERTY_NAME, 'k1')).to.be.true
                expect(vertex.properties.length).to.equal(2)
                expect(vertex.properties[0].value).to.equal('jason')
            })

            it('should validate property without key', function() {
                var vertex = vertexFactory([
                        propertyFactory(PROPERTY_NAME_FIRST, 'k1', 'jason'),
                        propertyFactory(PROPERTY_NAME_CONCEPT, 'http://visallo.org/dev#person')
                    ]);

                expect(V.propValid(vertex, [], COMPOUND_PROPERTY_NAME)).to.be.false
                vertex.properties.push(propertyFactory(PROPERTY_NAME_LAST, 'k1', 'harwig'));
                expect(V.propValid(vertex, [], COMPOUND_PROPERTY_NAME)).to.be.true
                expect(V.propValid(vertex, ['override last name', undefined], COMPOUND_PROPERTY_NAME)).to.be.false
                expect(V.propValid(vertex, ['l', 'f'], COMPOUND_PROPERTY_NAME)).to.be.true
            })
        })

        describe('propFromAudit', function() {
            it('should format values for audit')
        })

        describe('propRaw', function() {
            it('should have propRaw function', function() {
                expect(V).to.have.property('propRaw').that.is.a.function
            })

            it('should expand property name', function() {
                var vertex = vertexFactory([
                        propertyFactory(PROPERTY_NAME_FIRST, 'jason'),
                        propertyFactory(PROPERTY_NAME_LAST, 'harwig'),
                        propertyFactory(PROPERTY_NAME_CONCEPT, 'http://visallo.org/dev#person')
                    ]);

                expect(V.propRaw(vertex, 'conceptType')).to.equal('http://visallo.org/dev#person')
            })

            it('should get prop values', function() {
                var value = V.propRaw(
                    vertexFactory([
                        propertyFactory(PROPERTY_NAME_FIRST, 'jason'),
                        propertyFactory(PROPERTY_NAME_FIRST, 'jason1'),
                        propertyFactory(PROPERTY_NAME_FIRST, 'jason2')
                    ]),
                    PROPERTY_NAME_FIRST
                )
                expect(value).to.equal('jason')
            })

            it('should return undefined if no default value', function() {
                var vertex = vertexFactory(),
                    value = V.propRaw(vertex, PROPERTY_NAME_FIRST);

                expect(value).to.equal(undefined)
            })

            it('should return default if passed defaultValue', function() {
                var vertex = vertexFactory(),
                    defaultValue = 'defaultValueTest',
                    value = V.propRaw(vertex, PROPERTY_NAME_FIRST, null, {
                        defaultValue: defaultValue
                    });

                expect(value).to.equal(defaultValue)
            })

            it('should accept no key but with options', function() {
                var vertex = vertexFactory(),
                    defaultValue = 'defaultValueTest',
                    value = V.propRaw(vertex, PROPERTY_NAME_FIRST, {});

                expect(value).to.equal(undefined)
            })

            it('should throw error if vertex is invalid', function() {
                expect(_.partial(V.propRaw, null, 'title')).to.throw(VERTEX_ERROR)
                expect(_.partial(V.propRaw, {}, 'title')).to.throw(VERTEX_ERROR)
                expect(_.partial(V.propRaw, { properties: null }, 'title')).to.throw(VERTEX_ERROR)
                expect(_.partial(V.propRaw, { id: 'testing' }, 'title')).to.throw(VERTEX_ERROR)
            })

            it('should throw error if name is invalid', function() {
                var vertex = vertexFactory();
                expect(_.partial(V.propRaw, vertex)).to.throw(PROPERTY_NAME_ERROR)
                expect(_.partial(V.propRaw, vertex, {})).to.throw(PROPERTY_NAME_ERROR)
                expect(_.partial(V.propRaw, vertex, '')).to.throw(PROPERTY_NAME_ERROR)
            })

            it('should get property with key', function() {
                var vertex = vertexFactory([
                    propertyFactory(PROPERTY_NAME_TITLE, 'k1', 'first key'),
                    propertyFactory(PROPERTY_NAME_TITLE, '', 'no key'),
                    propertyFactory(PROPERTY_NAME_TITLE, 'k2', 'last')
                ]);

                expect(V.propRaw(vertex, PROPERTY_NAME_TITLE, '')).to.equal('no key')
                expect(V.propRaw(vertex, PROPERTY_NAME_TITLE, 'k1')).to.equal('first key')
                expect(V.propRaw(vertex, PROPERTY_NAME_TITLE, 'k2')).to.equal('last')
                expect(V.propRaw(vertex, PROPERTY_NAME_TITLE, undefined)).to.equal('first key')
            })

            it('should get property with most confidence', function() {
                var vertex = vertexFactory([
                    confidence(propertyFactory(PROPERTY_NAME_FIRST, 'first'), 0.5),
                    confidence(propertyFactory(PROPERTY_NAME_FIRST, 'most confident'), 0.6),
                    confidence(propertyFactory(PROPERTY_NAME_FIRST, 'middle'), 0.55)
                ]);

                expect(V.propRaw(vertex, PROPERTY_NAME_FIRST)).to.equal('most confident')
            })
            it('should get property with most confidence above those with no confidence', function() {
                var vertex = vertexFactory([
                    propertyFactory(PROPERTY_NAME_FIRST, 'first'),
                    confidence(propertyFactory(PROPERTY_NAME_FIRST, 'most confident'), 0.6),
                    confidence(propertyFactory(PROPERTY_NAME_FIRST, 'middle'), 0.55)
                ]);
                expect(V.propRaw(vertex, PROPERTY_NAME_FIRST)).to.equal('most confident')
            })
            it('should get property with most confidence even if created earliest', function() {
                var vertex = vertexFactory([
                    created(propertyFactory(PROPERTY_NAME_FIRST, 'recent'), new Date(2015, 3, 1)),
                    created(
                        confidence(propertyFactory(PROPERTY_NAME_FIRST, 'most confident'), 0.6),
                        new Date(2015, 1, 1)
                    ),
                    created(
                        confidence(propertyFactory(PROPERTY_NAME_FIRST, 'middle'), 0.55),
                        new Date(2015, 2, 1)
                    )
                ]);
                expect(V.propRaw(vertex, PROPERTY_NAME_FIRST)).to.equal('most confident')
            })
            it('should get property most recently created when confidence same', function() {
                var vertex = vertexFactory([
                    created(propertyFactory(PROPERTY_NAME_FIRST, 'recent'), new Date(2015, 3, 1)),
                    created(
                        confidence(propertyFactory(PROPERTY_NAME_FIRST, 'b confident'), 0.6),
                        new Date(2015, 1, 1)
                    ),
                    created(confidence(propertyFactory(PROPERTY_NAME_FIRST, 'a'), 0.6), new Date(2015, 2, 1))
                ]);
                expect(V.propRaw(vertex, PROPERTY_NAME_FIRST)).to.equal('a')
            })

            describe('Compound properties', function() {

                it('should handle compound properties and return array of values', function() {
                    var sharedKey = 'A',
                        vertex = vertexFactory([
                            // lastname defined first in ontology for /dev#name
                            propertyFactory(PROPERTY_NAME_LAST, sharedKey, 'smith'),
                            propertyFactory(PROPERTY_NAME_FIRST, sharedKey, 'john')
                        ]),
                        values = V.propRaw(vertex, COMPOUND_PROPERTY_NAME);

                    expect(values).to.be.an('array').that.has.property('length').that.equals(2)
                    expect(values[0]).to.equal(vertex.properties[0].value)
                    expect(values[1]).to.equal(vertex.properties[1].value)
                })

                it('should handle pairing compound properties', function() {
                    var vertex = vertexFactory([
                            propertyFactory(PROPERTY_NAME_FIRST, 'k1', 'jason'),
                            propertyFactory(PROPERTY_NAME_LAST, 'k2', 'smith'),
                            propertyFactory(PROPERTY_NAME_FIRST, 'k2', 'john')
                        ]),
                        values = V.propRaw(vertex, COMPOUND_PROPERTY_NAME);

                    expect(values).to.be.an('array').that.has.property('length').that.equals(2)
                    expect(values[0]).to.be.undefined
                    expect(values[1]).to.equal('jason')

                    values = V.propRaw(vertex, COMPOUND_PROPERTY_NAME, 'k2');
                    expect(values[0]).to.equal('smith')
                    expect(values[1]).to.equal('john')
                })

                it('should handle getting highest confidence compound property', function() {
                    var vertex = vertexFactory([
                            propertyFactory(PROPERTY_NAME_FIRST, 'k1', 'jason'),
                            confidence(propertyFactory(PROPERTY_NAME_LAST, 'k2', 'smith'), 0.5),
                            confidence(propertyFactory(PROPERTY_NAME_FIRST, 'k2', 'john'), 0.5)
                        ]),
                        values = V.propRaw(vertex, COMPOUND_PROPERTY_NAME);

                    expect(values).to.be.an('array').that.has.property('length').that.equals(2)
                    expect(values[0]).to.equal('smith')
                    expect(values[1]).to.equal('john')

                    values = V.propRaw(vertex, COMPOUND_PROPERTY_NAME, 'k1');
                    expect(values[0]).to.be.undefined
                    expect(values[1]).to.equal('jason')
                })

                it('should throw errors for compound that depends on compound', function() {
                    var vertex = vertexFactory([
                            propertyFactory(PROPERTY_NAME_TITLE, 'jason'),
                            propertyFactory(PROPERTY_NAME_FIRST, 'k1', 'jason'),
                            propertyFactory(PROPERTY_NAME_LAST, 'k1', 'smith')
                        ]);

                    expect(function() {
                        V.propRaw(vertex, COMPOUND_TEST_PROPERTY_NAME);
                    }).to.throw('compound properties')
                })
            })
        })

        describe('isArtifact', function() {
            it('should return false if the vertex has no raw propery', function() {
                var vertex = vertexFactory('v1', []);
                expect(V.isArtifact(vertex)).to.be.false;
            })
            it('should return true if the vertex has a raw propery', function() {
                var vertex = vertexFactory('v1', [
                    propertyFactory(PROPERTY_NAME_RAW, 'some document content')
                ]);
                expect(V.isArtifact(vertex)).to.be.true;
            })
        })

        describe('displayType', function() {
            it('should return \'entity\' if the passed in entity is a non-artifact entity', function() {
                var vertex = vertexFactory('v1', [
                    propertyFactory(PROPERTY_NAME_CONCEPT, 'http://visallo.org/dev#person')
                ]);
                expect(V.displayType(vertex)).to.equal('entity');
            })
            it('should return \'edge\' if the passed in entity is an edge', function() {
                var vertex = vertexFactory('v1', [
                    propertyFactory(PROPERTY_NAME_CONCEPT, 'relationship')
                ]);
                expect(V.displayType(vertex)).to.equal('edge');
            })
            it('should return \'video\' if the vertex has transcoded mp4 video', function() {
                var vertex = vertexFactory('v1', [
                    propertyFactory(PROPERTY_NAME_CONCEPT, 'anything'),
                    propertyFactory(PROPERTY_NAME_RAW, 'some content'),
                    propertyFactory(PROPERTY_NAME_MP4_VIDEO, 'some content')
                ]);
                expect(V.displayType(vertex)).to.equal('video');
            })
            it('should return \'video\' if the vertex has transcoded webm video', function() {
                var vertex = vertexFactory('v1', [
                    propertyFactory(PROPERTY_NAME_CONCEPT, 'anything'),
                    propertyFactory(PROPERTY_NAME_RAW, 'some content'),
                    propertyFactory(PROPERTY_NAME_WEBM_VIDEO, 'some content')
                ]);
                expect(V.displayType(vertex)).to.equal('video');
            })
            it('should return \'audio\' if the vertex has transcoded aac audio', function() {
                var vertex = vertexFactory('v1', [
                    propertyFactory(PROPERTY_NAME_CONCEPT, 'anything'),
                    propertyFactory(PROPERTY_NAME_RAW, 'some content'),
                    propertyFactory(PROPERTY_NAME_AAC_AUDIO, 'some content')
                ]);
                expect(V.displayType(vertex)).to.equal('audio');
            })
            it('should return \'audio\' if the vertex has transcoded ogg audio', function() {
                var vertex = vertexFactory('v1', [
                    propertyFactory(PROPERTY_NAME_CONCEPT, 'anything'),
                    propertyFactory(PROPERTY_NAME_RAW, 'some content'),
                    propertyFactory(PROPERTY_NAME_OGG_AUDIO, 'some content')
                ]);
                expect(V.displayType(vertex)).to.equal('audio');
            })
            it('should return \'image\' if the vertex raw property has an image mime-type', function() {
                var vertex = vertexFactory('v1', [
                    propertyFactory(PROPERTY_NAME_CONCEPT, 'anything'),
                    addMetadata(propertyFactory(PROPERTY_NAME_RAW, 'some content'), PROPERTY_NAME_MIMETYPE, 'image/png')
                ]);
                expect(V.displayType(vertex)).to.equal('image');
            })
            it('should return \'document\' if the vertex is an artifact but not a video, audio, or image', function() {
                var vertex = vertexFactory('v1', [
                    propertyFactory(PROPERTY_NAME_CONCEPT, 'anything'),
                    addMetadata(propertyFactory(PROPERTY_NAME_RAW, 'some content'), PROPERTY_NAME_MIMETYPE, 'application/pdf')
                ]);
                expect(V.displayType(vertex)).to.equal('document');
            })
            it('should return \'document\' if the vertex has raw but is missing mime type', function() {
                var vertex = vertexFactory('v1', [
                    propertyFactory(PROPERTY_NAME_CONCEPT, 'anything'),
                    propertyFactory(PROPERTY_NAME_RAW, 'some content')
                ]);
                expect(V.displayType(vertex)).to.equal('document');
            })
        })
    });
});
