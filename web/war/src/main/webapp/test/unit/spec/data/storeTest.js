define([
    'data/store',
    'configuration/plugins/registry'
], function(store, registry) {

    describe('Store', function() {
        beforeEach(function() {
            var w = console.warn;
            console.warn = function() { };
            store._clear();
            console.warn = w;
            this.uuids = [
                registry.registerExtension('org.visallo.store.updater', {
                    identifier: 'id0',
                    activate: function(update) {
                        update('id0Value')
                        _.delay(function() {
                            update('id0Value2');
                        }, 100)
                    }
                }),
                registry.registerExtension('org.visallo.store.updater', {
                    identifier: 'id1',
                    activate: function(update) {
                        update('id1Value')
                    }
                }),
                registry.registerExtension('org.visallo.store.updater', {
                    identifier: 'id2',
                    activate: function(update) {
                        update('id2Value')
                    }
                }),
                registry.registerExtension('org.visallo.store.updater', {
                    identifier: 'update-but-no-change',
                    activate: function(update) {
                        update('update-but-no-change-value')
                        _.delay(function() {
                            update('update-but-no-change-value')
                        }, 100)
                    }
                }),
                registry.registerExtension('org.visallo.store.updater', {
                    identifier: 'configuration',
                    activate: function(update, configuration) {
                        if (configuration.configValue1 === 'x') {
                            update('x')
                        } else if (configuration.configValue1 === 'y') {
                            update('y')
                        }
                    }
                })
            ];
        })
        afterEach(function() {
            this.uuids.forEach(function(uuid) {
                registry.unregisterExtension(uuid);
            });
        })

        it('should have stable api', function() {
            store.should.have.property('execNowAndOnUpdate').that.is.a('function')
            store.should.have.property('removeCallback').that.is.a('function')
        })

        it('should be able to register new store listeners', done => {
            var getId0;
            store.execNowAndOnUpdate('test', switchOnCallCount({
                0: store => {
                    Object.keys(store).length.should.equal(2)
                    store.should.have.property('registerForStoreUpdater')
                    store.should.have.property('unregisterForStoreUpdater')
                    getId0 = store.registerForStoreUpdater('id0')
                },
                1: store => {
                    Object.keys(store).length.should.equal(3)
                    getId0.should.be.a('function')
                    getId0(store).should.equal('id0Value')
                },
                default: store => {
                    getId0(store).should.equal('id0Value2')
                    done();
                }
            }))
        })

        it('should validate identifier', () => {
            var fn = store.execNowAndOnUpdate,
                invalids = ['', {}, [], undefined, null, 0];

            invalids.forEach(function(invalid) {
                expect(() => { fn(invalid) }).to.throw('Identifier')
            })
        })

        it('should validate callback', () => {
            var fn = store.execNowAndOnUpdate,
                invalids = ['', {}, [], undefined, null, 0];

            invalids.forEach(function(invalid) {
                expect(() => { fn('validId', invalid) }).to.throw('Callback')
            })
        })

        it('should not reuse identifiers', () => {
            store.execNowAndOnUpdate('test', function() { })

            expect(function() {
                store.execNowAndOnUpdate('test', function() { })
            }, function() { }).to.throw('unique')
        })

        it('should remove callbacks', (done) => {
            store.execNowAndOnUpdate('test', function() { })
            store.removeCallback('test')
            store.execNowAndOnUpdate('test', () => { done() })
        })

        it('should throw when requesting non-existing updater', done => {
            store.execNowAndOnUpdate('test', switchOnCallCount({
                0: store => {
                    expect(function() {
                        store.registerForStoreUpdater('non-existent')
                    }).to.throw('No store updater found')
                    done()
                }
            }))
        })

        it('should do concatenate registration', done => {
            var getId0, getId1;
            store.execNowAndOnUpdate('test', switchOnCallCount({
                0: store => {
                    getId0 = store.registerForStoreUpdater('id0')
                },
                1: store => {
                    getId1 = store.registerForStoreUpdater('id1')
                },
                default: store => {
                    var keys = Object.keys(store)
                    getId0(store).should.equal('id0Value2')
                    getId1(store).should.equal('id1Value')
                    keys.length.should.equal(4)
                    done()
                }
            }))
        })

        it('should allow unregistration with getter function', done => {
            var getId1, getId2;
            store.execNowAndOnUpdate('test', switchOnCallCount({
                0: store => {
                    getId1 = store.registerForStoreUpdater('id1')
                },
                1: store => {
                    Object.keys(store).length.should.equal(3)
                    getId1(store).should.equal('id1Value')
                    store.unregisterForStoreUpdater(getId1)
                    getId2 = store.registerForStoreUpdater('id2')
                },
                default: store => {
                    Object.keys(store).length.should.equal(3)
                    getId2(store).should.equal('id2Value')
                    done()
                }
            }))
        })
        
        it('should allow unregistration with id', done => {
            var getId1, getId2;
            store.execNowAndOnUpdate('test', switchOnCallCount({
                0: store => {
                    getId1 = store.registerForStoreUpdater('id1')
                },
                1: store => {
                    Object.keys(store).length.should.equal(3)
                    getId1(store).should.equal('id1Value')
                    store.unregisterForStoreUpdater('id1')
                    getId2 = store.registerForStoreUpdater('id2')
                },
                default: store => {
                    Object.keys(store).length.should.equal(3)
                    getId2(store).should.equal('id2Value')
                    done()
                }
            }))
        })

        it('should count unregister calls for identifier', done => {
            var getId0, getId0_2;
            store.execNowAndOnUpdate('test', switchOnCallCount({
                0: store => {
                    getId0 = store.registerForStoreUpdater('id0')
                },
                1: store => {
                    getId0_2 = store.registerForStoreUpdater('id0')
                    store.unregisterForStoreUpdater('id0')
                },
                default: store => {
                    Object.keys(store).length.should.equal(3)
                    done()
                }
            }))
        })

        it('should only update when key values change', done => {
            var getter, key = 'update-but-no-change'
            store.execNowAndOnUpdate('test', switchOnCallCount({
                0: store => {
                    getter = store.registerForStoreUpdater(key)
                },
                1: store => {
                    getter(store).should.equal(key + '-value')
                    _.delay(function() {
                        done()
                    }, 100)
                },
                default: store => {
                    throw new Error('Should not call when no change')
                }
            }))
        })

        it('should only update registered listeners', done => {
            var getId0, getId1, getId2, spy = chai.spy()
            store.execNowAndOnUpdate('test1', switchOnCallCount({
                0: store => {
                    getId2 = store.registerForStoreUpdater('id2')
                },
                1: store => {
                    getId2(store).should.equal('id2Value')
                    spy()
                },
                default: store => {
                    throw new Error('Nothing registered, should not update')
                }
            }))
            store.execNowAndOnUpdate('test2', switchOnCallCount({
                0: store => {
                    getId0 = store.registerForStoreUpdater('id0')
                    getId1 = store.registerForStoreUpdater('id1')
                },
                1: store => {
                    spy()
                },
                default: store => {
                    getId0(store).should.equal('id0Value2')
                    spy.should.have.been.called.twice
                    done()
                }
            }))
        })

        it('should require configuration for listener', done => {
            var getTest1, getTest2, d = deferred();
            store.execNowAndOnUpdate('test1', switchOnCallCount({
                0: store => {
                    getTest1 = store.registerForStoreUpdater('configuration', { configValue1: 'x' })
                    getTest1.should.be.a('function')
                    expect(getTest1).to.throw('Store')
                },
                1: store => {
                    expect(store.configuration).to.not.exist
                    getTest1(store).should.equal('x');
                    d.done();
                }
            }))
            store.execNowAndOnUpdate('test2', switchOnCallCount({
                0: store => {
                    getTest2 = store.registerForStoreUpdater('configuration', { configValue1: 'y' })
                },
                1: store => {
                    getTest2(store).should.equal('y')
                    d.promise.then(done);
                }
            }))
        })

    })

    function log(store, ...rest) {
        var log = ['Keys:', Object.keys(store)].concat(rest)
        console.log.apply(console, log);
    }

    function deferred() {
        var f, promise = new Promise(function(_f) {
            f = _f;
        })
        return {
            done: f,
            promise: promise
        };
    }

    function switchOnCallCount(map) {
        var callNumber = 0;
        return function() {
            var callNumberStr = String(callNumber++),
                args = _.toArray(arguments);
            if (callNumberStr in map) {
                map[callNumberStr].apply(null, args);
            } else if ('default' in map) {
                map.default.apply(null, args);
            } else {
                throw new Error('No function defined for callnumber', callNumberStr)
            }
        }
    }
})
