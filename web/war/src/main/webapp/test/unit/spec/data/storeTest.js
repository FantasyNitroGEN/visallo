define([
    'data/store',
    'configuration/plugins/registry'
], function(store, registry) {

    describe.only('Store', function() {
        beforeEach(function() {
            store._clear();
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
            store.should.have.property('update').that.is.a('function')
            store.should.have.property('replace').that.is.a('function')
        })

        it('should be able to register new store listeners', done => {
            store.execNowAndOnUpdate('test', switchOnCallCount({
                0: store => {
                    Object.keys(store).length.should.equal(2)
                    store.should.have.property('registerForStoreUpdater')
                    store.should.have.property('unregisterForStoreUpdater')
                    store.registerForStoreUpdater('id0')
                },
                1: store => {
                    Object.keys(store).length.should.equal(3)
                    store.should.have.property('id0').that.equals('id0Value')
                },
                default: store => {
                    store.should.have.property('id0').that.equals('id0Value2')
                    done();
                }
            }))
        })

        it('should do concatenate registration', done => {
            store.execNowAndOnUpdate('test', switchOnCallCount({
                0: store => {
                    store.registerForStoreUpdater('id0')
                },
                1: store => {
                    store.registerForStoreUpdater('id1')
                },
                default: store => {
                    var keys = Object.keys(store)
                    keys.indexOf('id0').should.be.at.least(0)
                    keys.indexOf('id1').should.be.at.least(0)
                    done()
                }
            }))
        })

        it('should allow unregistration', done => {
            store.execNowAndOnUpdate('test', switchOnCallCount({
                0: store => {
                    store.registerForStoreUpdater('id0')
                },
                1: store => {
                    store.registerForStoreUpdater('id0')
                    store.unregisterForStoreUpdater('id0')
                },
                default: store => {
                    Object.keys(store).indexOf('id0').should.be.at.least(0)
                    done()
                }
            }))
        })

        it('should only update when key values change', done => {
            var key = 'update-but-no-change'
            store.execNowAndOnUpdate('test', switchOnCallCount({
                0: store => {
                    store.registerForStoreUpdater(key)
                },
                1: store => {
                    store[key].should.equal(key + '-value')
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
            var spy = chai.spy()
            store.execNowAndOnUpdate('test1', switchOnCallCount({
                0: store => {
                    store.registerForStoreUpdater('id2')
                },
                1: store => {
                    store.id2.should.equal('id2Value')
                    spy()
                },
                default: store => {
                    throw new Error('Nothing registered, should not update')
                }
            }))
            store.execNowAndOnUpdate('test2', switchOnCallCount({
                0: store => {
                    store.registerForStoreUpdater('id0')
                    store.registerForStoreUpdater('id1')
                },
                1: store => {
                    spy()
                },
                default: store => {
                    store.id0.should.equal('id0Value2')
                    spy.should.have.been.called.twice
                    done()
                }
            }))
        })

    })

    function log(store, ...rest) {
        var log = ['Keys:', Object.keys(store)].concat(rest)
        console.log.apply(console, log);
    }

    function switchOnCallCount(map) {
        var callNumber = 0;
        return function() {
            var args = _.toArray(arguments);
            if (callNumber in map) {
                map[callNumber].apply(null, args);
            } else if ('default' in map) {
                map['default'].apply(null, args);
            }
            callNumber++;
        }
    }
})
