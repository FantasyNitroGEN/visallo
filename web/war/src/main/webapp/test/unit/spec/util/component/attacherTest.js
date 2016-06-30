define([
    'util/component/attacher',
    'react',
    'testutils/called-deferred'
], function(attacher, react, calledDeferred) {

    var Simulate = react.addons.TestUtils.Simulate;

    describe('attacher', function() {

        beforeEach(function() {
            this.node = document.createElement('div');
        })

        describe('General', function() {
            it('should support jQuery nodes', function() {
                var $node = $(this.node);
                attacher().node($node)._node.should.equal(this.node)
            })
            it('should support plain dom nodes', function() {
                attacher().node(this.node)._node.should.equal(this.node)
            })
            it('should accept path', function() {
                var p = 'path';
                attacher().node(this.node).path(p)._path.should.equal(p);
            })
            it('should accept component', function() {
                var c = {};
                attacher().node(this.node).component(c)._component.should.equal(c);
            })
            it('should only allow object for params', function() {
                return attacher()
                    .node(this.node)
                    .path('fakepath')
                    .params(['something'])
                    .attach()
                    .then(function() {
                        throw new Error('No error thrown when passing invalid params')
                    }, function(error) {
                        error.message.indexOf('Params must be an object').should.be.at.least(0)
                    })
            })
            it('should error if no node', function() {
                return attacher()
                    .path('../test/unit/spec/util/component/attacherFlightSimple')
                    .attach()
                    .then(function() {
                        throw new Error('No error for missing node')
                    }, function(error) {
                        error.message.indexOf('Node').should.be.at.least(0);
                    })
            })
            it('should error if no path or component', function() {
                return attacher()
                    .node(this.node)
                    .attach()
                    .then(function() {
                        throw new Error('No error thrown when omitting path')
                    }, function(error) {
                        error.message.indexOf('component or path').should.be.at.least(0)
                    })
            })
            it('should error if params includes visalloApi', function() {
                return attacher()
                    .node(this.node)
                    .path('fakepath')
                    .params({ visalloApi: {} })
                    .attach()
                    .then(function() {
                        throw new Error('No error thrown when passing overridden visalloApi')
                    }, function(error) {
                        error.message.indexOf('avoid collisions').should.be.at.least(0)
                    })
            })
            it('should support behaviors', function() {
                attacher().should.respondTo('behavior')
                attacher().should.respondTo('legacyMapping')
            })
            it('reattach should not keep old behaviors', function() {
                var p = { param: 'test' }
                return attacher()
                    .node(this.node)
                    .params(p)
                    .path('../test/unit/spec/util/component/attacherReactParams')
                    .behavior({
                        old: function() {}
                    })
                    .attach()
                    .then(function(inst) {
                        return inst.behavior({ newBehavior: function() {} }).attach()
                    })
                    .then(function(inst) {
                        p.should.equal(inst._params)
                    });
            })
        })

        describe('flight', function() {
            it('simple render', function() {
                return attacher()
                    .node(this.node)
                    .path('../test/unit/spec/util/component/attacherFlightSimple')
                    .attach()
                    .then(function(a) {
                        a._node.textContent.should.equal('FlightSimple')
                        a._flightComponent.should.exist
                    })
            })
            it('accepts params', function() {
                return attacher()
                    .node(this.node)
                    .path('../test/unit/spec/util/component/attacherFlightParams')
                    .params({
                        param: 'WithParams'
                    })
                    .attach()
                    .then(function(a) {
                        a._node.textContent.should.equal('WithParams')
                    })
            })
            it('uses api', function() {
                return attacher()
                    .node(this.node)
                    .path('../test/unit/spec/util/component/attacherFlightApi')
                    .attach()
                    .then(function(a) {
                        a._node.textContent.should.equal('10 cats');
                    })
            })
            it('accept behaviors', function() {
                var called = calledDeferred(),
                    addEventSpy = chai.spy.on(this.node, 'addEventListener')
                return attacher()
                    .node(this.node)
                    .path('../test/unit/spec/util/component/attacherFlightBehavior')
                    .behavior({
                        customBehavior: function(param) {
                            param.should.equal('param1fromflight')
                            called.resolve(true);
                        }
                    })
                    .attach()
                    .then(function(a) {
                        addEventSpy.should.be.spy;
                        addEventSpy.should.have.been.called.with('customBehavior')
                        addEventSpy.should.have.been.called.with('click')
                        addEventSpy.should.have.been.called.twice
                        $(a._node.querySelector('div')).trigger('click')
                        return called.promise.should.become(true);
                    })
            })
            it('accept behaviors with custom mapping', function() {
                var called = calledDeferred(),
                    addEventSpy = chai.spy.on(this.node, 'addEventListener');
                return attacher()
                    .node(this.node)
                    .path('../test/unit/spec/util/component/attacherFlightBehavior')
                    .behavior({
                        customBehavior: function(param) {
                            param.should.equal('param1fromflightMapped')
                            called.resolve(true)
                        }
                    })
                    .legacyMapping({
                        customBehavior: 'legacy'
                    })
                    .attach()
                    .then(function(a) {
                        addEventSpy.should.not.have.been.called.with('customBehavior')
                        addEventSpy.should.have.been.called.with('legacy')
                        addEventSpy.should.have.been.called.with('click')
                        addEventSpy.should.have.been.called.twice
                        $(a._node.querySelector('span')).trigger('click')
                        return called.promise.should.become(true)
                    })
            })
            it('should remove listeners on component teardown', function() {
                var called = calledDeferred(),
                    removeEventSpy = chai.spy.on(this.node, 'removeEventListener');
                return attacher()
                    .node(this.node)
                    .path('../test/unit/spec/util/component/attacherFlightBehavior')
                    .behavior({
                        customBehavior: function(param) {
                            param.should.equal('param1fromflight')
                            called.resolve(true)
                        },
                        needsMapping: function() {}
                    })
                    .legacyMapping({
                        needsMapping: 'mapped'
                    })
                    .attach()
                    .then(function(a) {
                        var $node = $(a._node);
                        $node.trigger('click')
                        return called.promise.should.become(true).then(function() {
                            var comps = $node.lookupAllComponents()
                            comps.length.should.equal(1);
                            removeEventSpy.reset()
                            comps[0].teardown()
                            removeEventSpy.should.have.not.have.been.called.with('needsMapping')
                            removeEventSpy.should.have.been.called.with('customBehavior')
                            removeEventSpy.should.have.been.called.with('mapped')
                            removeEventSpy.should.have.been.called.with('click')
                            removeEventSpy.should.have.been.called.exactly(3)
                            $node.lookupAllComponents().length.should.equal(0);
                        })
                    })
            })
        })

        describe('react', function() {
            it('simple render', function() {
                return attacher()
                    .node(this.node)
                    .path('../test/unit/spec/util/component/attacherReactSimple')
                    .attach()
                    .then(function(a) {
                        a._node.textContent.should.equal('ReactSimple')
                        a._reactElement.should.exist
                    })
            })
            it('accepts params', function() {
                return attacher()
                    .node(this.node)
                    .path('../test/unit/spec/util/component/attacherReactParams')
                    .params({
                        param: 'WithParams'
                    })
                    .attach()
                    .then(function(a) {
                        a._node.textContent.should.equal('WithParams')
                    })
            })
            it('uses api', function() {
                return attacher()
                    .node(this.node)
                    .path('../test/unit/spec/util/component/attacherReactApi')
                    .attach()
                    .then(function(a) {
                        a._node.textContent.should.equal('1 cat');
                    })
            })
            it('accept behaviors', function() {
                var called = calledDeferred();
                return attacher()
                    .node(this.node)
                    .path('../test/unit/spec/util/component/attacherReactBehavior')
                    .behavior({
                        customBehavior: function(param) {
                            param.should.equal('param1')
                            called.resolve(true)
                        }
                    })
                    .attach()
                    .then(function(a) {
                        Simulate.click(a._node.querySelector('div'));
                        return called.promise.should.become(true)
                    })
            })
            it('reattach should update with same attacher and new params', function() {
                var called = calledDeferred(),
                    a = attacher();

                return a
                    .node(this.node)
                    .params({ param: 'first' })
                    .path('../test/unit/spec/util/component/attacherReactReattach')
                    .behavior({
                        changeParam: function(newParam) {
                            a.params({ param: newParam }).attach().then(function(a) {
                                a._node.textContent.should.equal(newParam)
                                called.resolve(true);
                            })
                        }
                    })
                    .attach()
                    .then(function(a) {
                        a._node.textContent.should.equal('first');
                        Simulate.click(a._node.querySelector('div'));
                        return called.promise.should.become(true)
                    })
            })
        })
    })
})
