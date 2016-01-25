/* globals chai:false */
require(['configuration/plugins/registry'], function(registry) {
    var gen = {},
        rootLayout = 'org.visallo.layout.root',
        layoutTypePoint = 'org.visallo.layout.type',
        layoutComponentPoint = 'org.visallo.layout.component',
        collectionItemExample;

    describeComponent('detail/item/item', function() {

        beforeEach(function() {
            collectionItemExample = [
                {
                    identifier: rootLayout,
                    children: [
                        { ref: 'list', className: 'my-list', model: function(model) { return model.properties } }
                    ]
                },
                {
                    identifier: 'list',
                    className: 'my-list-2',
                    collectionItem: { ref: 'item', className: 'my-list-item-ref' }
                },
                {
                    identifier: 'item',
                    className: 'my-list-item'
                }
            ]
        })

        describe('No root children', function() {
            beforeEach(function() {
                return setupItemComponent.call(this, {
                    identifier: rootLayout,
                    className: 'testing',
                    children: []
                }, [vertexGen()])
            })
            it('should have no children', function() {
                this.component.node.children.length.should.equal(0)
            })
            it('should set class name', function() {
                this.component.node.classList.contains('testing').should.be.true
            })
        })

        describe('Change root template', function() {

            beforeEach(function() {
                return setupItemComponent.call(this, [
                    {
                        identifier: rootLayout,
                        children: [
                            { ref: 'a' },
                            { ref: 'b' }
                        ]
                    },
                    { identifier: 'a', render: function(el, m) { el.textContent = 'a' } },
                    { identifier: 'b', render: function(el, m) { el.textContent = 'b' } },
                    { identifier: rootLayout, applyTo: { conceptIri: 'c' }, children: [ { ref: 'c'} ] },
                    { identifier: 'c', render: function(el, m) { el.textContent = 'c' } }
                ], [vertexGen()])
            })

            it('should change children based on applyTo', function() {
                var $node = this.component.$node,
                    node = this.component.node;

                node.childElementCount.should.equal(2)

                $node.on('modelUpdated', function(event, data) {
                        node.childElementCount.should.equal(1)
                        node.children[0].className.should.equal('c')
                    })
                    .trigger('updateModel', {
                        model: vertexGen([propGen('http://visallo.org#conceptType', 'c')])
                    })
            })

        })

        describe('Root component', function() {
            beforeEach(function() {
                return setupItemComponent.call(this, [
                    {
                        identifier: rootLayout,
                        componentPath: '../test/unit/spec/detail/itemTestCollectionItemComponent',
                        children: [{ ref: 'x' }]
                    },
                    {
                        identifier: 'x',
                        render: function(el, model) {
                            el.textContent = 'Model' + model.id
                        }
                    }
                ], vertexGen())
            })

            it('should prevent updateModel loop', function(done) {
                var $node = this.component.$node,
                    times = 0;

                $node.on('modelUpdated', function(event, data) {
                        ++times;
                        if (times > 1) {
                            times.should.equal(1)
                            $node.off('modelUpdated')
                            done()
                        }
                        _.delay(function() {
                            times.should.equal(1)
                            done()
                        }, 500)
                    })
                    .trigger('updateModel', {
                        model: vertexGen([propGen('a1')])
                    })
            })

        })

        describe('CollectionItem with render', function() {
            beforeEach(function() {
                collectionItemExample[2].render = function(el, model) {
                    el.textContent = model.name;
                }
                return setupItemComponent.call(this, collectionItemExample, vertexGen([propGen(), propGen()]))
            })
            it('should have one child', function() {
                this.component.node.children.length.should.equal(1)
            })
            it('should set class name from both configs', function() {
                this.component.$node.find('.my-list').length.should.equal(1)
                this.component.$node.find('.my-list-2').length.should.equal(1)
            })
            it('should set class even when rendering', function() {
                this.component.$node.find('.item').each(function() {
                    this.classList.contains('my-list-item').should.be.true
                    this.classList.contains('my-list-item-ref').should.be.true
                })
            })
        })

        describe('CollectionItem errors', function() {

            it('should throw if collectionItem is passed non-array', function() {
                collectionItemExample[0].children = [
                        { ref: 'list', className: 'my-list' }
                ];
                collectionItemExample[2].render = function(el, model) {
                    el.textContent = model.name;
                }
                var originalError = console.error;
                console.error = chai.spy()
                return setupItemComponent.call(this, collectionItemExample, vertexGen([propGen(), propGen()]))
                    .catch()
                    .then(function() {
                        console.error.should.have.been.called.at.least(1)
                        console.error = originalError
                    })
            })
        })

        describe('CollectionItem with children and components', function() {
            define('collectionitem', ['flight/lib/component'], function(defineComponent) {
                return defineComponent(CollectionItemDataSet);
                function CollectionItemDataSet() {
                    this.attributes({
                        model: null,
                        ignoreUpdateModelNotImplemented: true
                    })
                    this.after('initialize', function() {
                        this.node.dataset.componentSet = 'true'
                    })
                }
            })
            beforeEach(function() {
                collectionItemExample[2].componentPath = 'collectionitem'
                collectionItemExample[2].children = [
                    { componentPath: '../test/unit/spec/detail/itemTestCollectionItemComponent', attributes: function(model) { return {prefix: 'My Comp:', model: model.name } } }
                ];
                return setupItemComponent.call(this, collectionItemExample, vertexGen([propGen(), propGen(), propGen()]))
            })

            it('should have create 3 components', function() {
                var $items = this.component.$node.find('.my-list .item')
                $items.length.should.equal(3)
                $items.each(function() {
                    $(this).eq(0).lookupAllComponents().length.should.equal(1)
                    this.dataset.componentSet.should.equal('true')
                    $(this).children().eq(0).lookupAllComponents().length.should.equal(1)
                })
            })
            it('should set text of each component', function() {
                this.component.$node.find('.my-list .item').each(function(i) {
                    this.children[0].textContent.should.match(/^My Comp:propName[\d]+$/)
                })
            })
        })

        describe('CollectionItem updating with render call', function() {
            beforeEach(function() {
                collectionItemExample[2].render = function(el, model) {
                    el.textContent = model.name
                }
                collectionItemExample.push({
                    identifier: 'list',
                    applyTo: function(model) {
                        return _.any(model, function(p) {
                            return p.name === 'layout'
                        })
                    },
                    className: 'my-list-layout',
                    collectionItem: { ref: 'item', className: 'my-list-item-ref' }
                })
                return setupItemComponent.call(this, collectionItemExample, vertexGen([propGen('a'), propGen('b'), propGen('c')]))
            })

            it('should have render', function() {
                var $items = this.component.$node.find('.item')
                
                $items.length.should.equal(3)
                $items.eq(0).text().should.equal('a')
                $items.eq(1).text().should.equal('b')
                $items.eq(2).text().should.equal('c')
            })

            it('should remove row, and update', function(done) {
                var $node = this.component.$node,
                    $list = $node.find('.my-list');

                $node.on('modelUpdated', function(event) {
                        if ($(event.target).is($node)) {
                            var $items = $list.children()
                            $items.length.should.equal(2)
                            $items.eq(0).text().should.equal('a1')
                            $items.eq(1).text().should.equal('b1')
                            done()
                        }
                    })
                    .trigger('updateModel', {
                        model: vertexGen([propGen('a1'), propGen('b1')])
                    })
            })

            it('should add row, and update', function(done) {
                var $node = this.component.$node,
                    $list = $node.find('.my-list');

                $node.on('modelUpdated', function(event) {
                        if ($(event.target).is($node)) {
                            var $items = $list.children()
                            $items.length.should.equal(4)
                            $items.eq(0).text().should.equal('a1')
                            $items.eq(1).text().should.equal('b1')
                            $items.eq(2).text().should.equal('c1')
                            $items.eq(3).text().should.equal('d1')
                            done()
                        }
                    })
                    .trigger('updateModel', {
                        model: vertexGen([propGen('a1'), propGen('b1'), propGen('c1'), propGen('d1')])
                    })
            })

            it('should update/replace classname', function(done) {
                var $node = this.component.$node,
                    $list = $node.find('.my-list'),
                    list = $list[0];

                list.classList.contains('my-list-2').should.be.true
                $node.on('modelUpdated', function(event) {
                        list.classList.contains('my-list-2').should.be.false
                        list.classList.contains('my-list-layout').should.be.true
                        done()
                    })
                    .trigger('updateModel', {
                        model: vertexGen([propGen('layout'), propGen('b1'), propGen('c1'), propGen('d1')])
                    })
            })
        })

        describe('Components should register updateModel event', function() {
            var warn = console.warn,
                warnSpy;
            beforeEach(function() {
                warnSpy = chai.spy(console, 'warn')
                console.warn = chai.spy(function() { })
            })
            afterEach(function() {
                console.warn = warn
            })
            it('should show warning if no updateModel event', function() {
                collectionItemExample[2].children = [{ componentPath: '../test/unit/spec/detail/itemTestCollectionItemComponentNoEvent' }]
                return setupItemComponent.call(this, collectionItemExample, vertexGen([propGen('a')]))
                    .then(function() {
                        console.warn.should.have.been.called.once
                    })
            })

            it('should suppress warning on attribute', function() {
                collectionItemExample[2].children = [{ componentPath: '../test/unit/spec/detail/itemTestCollectionItemComponentNoEventSuppress' }]
                return setupItemComponent.call(this, collectionItemExample, vertexGen([propGen('a')]))
                    .then(function() {
                        console.warn.should.not.have.been.called();
                    })
            })
        })

        describe('CollectionItem updating with component', function() {
            beforeEach(function() {
                collectionItemExample[2].children = [{ componentPath: '../test/unit/spec/detail/itemTestCollectionItemComponent', model: function(m) { return m.name; } }]
                collectionItemExample.push({
                    identifier: 'list',
                    applyTo: function(model) {
                        return _.any(model, function(p) {
                            return p.name === 'layout'
                        })
                    },
                    className: 'my-list-layout',
                    layout: { type: 'flex', options: { direction: 'row'} },
                    collectionItem: { ref: 'item', className: 'my-list-item-ref' }
                })
                return setupItemComponent.call(this, collectionItemExample, vertexGen([propGen('a'), propGen('b'), propGen('c')]))
            })

            it('should have rendered', function() {
                var $items = this.component.$node.find('.item div');
                
                $items.length.should.equal(3)
                $items.each(function() {
                    var attached = $(this).lookupAllComponents()
                    attached.length.should.equal(1)
                    attached[0].toString().should.equal('ItemTestCollectionItem')
                })
            })

            it('should update once', function(done) {
                var $node = this.component.$node,
                    $list = $node.find('.my-list'),
                    spy = chai.spy(),
                    comps = $list.children().eq(0).find('div').lookupAllComponents(),
                    div = $list.children().eq(0).find('div').on('updateModel', spy)
                    
                comps.length.should.equal(1)
                comps.toString().should.equal('ItemTestCollectionItem')

                $node.on('modelUpdated', function(event) {
                        _.delay(function() {
                            spy.should.be.called.once
                            done()
                        }, 1000)
                    })
                    .trigger('updateModel', {
                        model: vertexGen([propGen('a1'), propGen('b1'), propGen('c1'), propGen('d1')])
                    })
            })

            it('should update and add rows', function(done) {
                var $node = this.component.$node,
                    $list = $node.find('.my-list')

                $node.on('modelUpdated', function(event) {
                        var $items = $list.children()
                        $items.length.should.equal(4)
                        $items.eq(0).text().should.equal('a1')
                        $items.eq(1).text().should.equal('b1')
                        $items.eq(2).text().should.equal('c1')
                        $items.eq(3).text().should.equal('d1')
                        done()
                    })
                    .trigger('updateModel', {
                        model: vertexGen([propGen('a1'), propGen('b1'), propGen('c1'), propGen('d1')])
                    })
            })

            it('should update and remove rows', function(done) {
                var $node = this.component.$node,
                    $list = $node.find('.my-list')

                $node.on('modelUpdated', function(event) {
                        var $items = $list.children()
                        $items.length.should.equal(2)
                        $items.eq(0).text().should.equal('a1')
                        $items.eq(1).text().should.equal('b1')
                        done()
                    })
                    .trigger('updateModel', {
                        model: vertexGen([propGen('a1'), propGen('b1')])
                    })
            })

            it('should update while using a layout for collectionItem', function(done) {
                var $node = this.component.$node,
                    $list = $node.find('.my-list'),
                    comps = $list.lookupAllComponents();

                comps.length.should.equal(0)

                $node.on('modelUpdated', function(event) {
                        comps = $list.lookupAllComponents();
                        comps.length.should.equal(1)
                        comps[0].toString().should.equal('FlexLayout')
                        done()
                    })
                    .trigger('updateModel', {
                        model: vertexGen([propGen('layout'), propGen('b1')])
                    })
            })

        })

        describe('org.visallo.layout.text component tests', function() {
            it('should render text as strings', function() {
                var self = this
                return setup(this, { ref: 'org.visallo.layout.text', model: 'testing' }).then(function() {
                    self.component.$node.find('.org-visallo-layout-text').text().should.equal('testing')
                })
            })

            it('should render objects as strings', function() {
                var self = this
                return setup(this, { ref: 'org.visallo.layout.text' }).then(function() {
                    self.component.$node.find('.org-visallo-layout-text').text().should.equal('[object Object]')
                })
            })

            it('should render with style', function() {
                var self = this,
                    styles = ['title', 'subtitle', 'heading1', 'heading2', 'heading3', 'body', 'footnote'];

                return _.reduce(styles, function(promise, s) {
                    return promise
                        .then(function() {
                            return setup(self, { ref: 'org.visallo.layout.text', style: s })
                        })
                        .then(function() {
                            var $text = self.component.$node.find('.org-visallo-layout-text');
                            $text.text().should.equal('[object Object]')
                            $text.hasClass(s).should.be.true
                            self.component.teardown()
                        })
                }, Promise.resolve())
            })

            function setup(ctx, config) {
                return Promise.require('detail/item/layoutComponents/generic')
                    .then(function(generic) {
                        var text = _.findWhere(generic, { identifier: 'org.visallo.layout.text' })
                        return setupItemComponent.call(ctx, [
                            text,
                            {
                                identifier: rootLayout,
                                children: [config]
                            }
                        ], vertexGen())
                    })
            }
        })

        describe('Flex layouts', function() {
            beforeEach(function() {
                return setupItemComponent.call(this, [
                    {
                        identifier: rootLayout,
                        layout: { type: 'flex', options: { direction: 'column' } },
                        children: [
                            { ref: 'x', model: 'x value' },
                            { ref: 'y', model: function(m) { return m.id } }
                        ]
                    },
                    {
                        identifier: 'x',
                        render: function(el, model) {
                            el.textContent = model
                        }
                    },
                    {
                        identifier: 'y',
                        layout: { type: 'flex', options: { direction: 'row' } },
                        children: [
                            { componentPath: '../test/unit/spec/detail/itemTestCollectionItemComponent' },
                            { componentPath: '../test/unit/spec/detail/itemTestCollectionItemComponentNoEventSuppress' }
                        ]
                    },
                    {
                        identifier: 'y',
                        applyTo: function(model) {
                            return (/^1_OVERRIDE/).test(model)
                        },
                        layout: { type: 'flex', options: { direction: 'row' } },
                        children: [
                            { componentPath: '../test/unit/spec/detail/itemTestCollectionItemComponentNoEventSuppress' },
                            { componentPath: '../test/unit/spec/detail/itemTestCollectionItemComponent' },
                            { componentPath: '../test/unit/spec/detail/itemTestCollectionItemComponent' },
                            { ref: 'x' }
                        ]
                    },
                    {
                        identifier: 'y',
                        applyTo: function(model) {
                            return (/^2_OVERRIDE/).test(model)
                        },
                        layout: { type: 'flex', options: { direction: 'row' } },
                        children: [
                            { componentPath: '../test/unit/spec/detail/itemTestCollectionItemComponent' },
                            { ref: 'x' }
                        ]
                    },

                    // No layout, test switching from layout->none and ensure
                    // layout teardown
                    {
                        identifier: 'y',
                        applyTo: function(model) {
                            return (/^3_OVERRIDE/).test(model)
                        },
                        children: [{ ref: 'x' }]
                    },

                    // Switch layout type
                    {
                        identifier: 'y',
                        applyTo: function(model) {
                            return (/^4_OVERRIDE/).test(model)
                        },
                        layout: { type: 'test_layout', options: { } },
                        children: [{ ref: 'x' }]
                    }
                ], vertexGen('layoutV', []), {
                    type: 'test_layout',
                    componentPath: '../test/unit/spec/detail/itemTestLayout'
                })
            })

            it('should have rendered with styles', function() {
                var $node = this.component.$node,
                    root = $node.get(0);

                root.style.flexDirection.should.equal('column')
                $node.find('.y').get(0).style.flexDirection.should.equal('row')
            })

            it('should handle updating', function(done) {
                var $node = this.component.$node,
                    $y = $node.find('.y')

                $y.children().length.should.equal(2)
                $node.on('modelUpdated', function(event) {
                        var $c = $y.children()
                        $c.length.should.equal(4)
                        // Component order is changed
                        $c.eq(0).text().should.equal('1_OVERRIDE_1')
                        $c.eq(0).lookupAllComponents()[0].toString()
                            .should.equal('ItemTestCollectionItemNoEventSuppress')
                        $c.eq(1).text().should.equal('1_OVERRIDE_1')
                        $c.eq(1).lookupAllComponents()[0].toString()
                            .should.equal('ItemTestCollectionItem')
                        $c.eq(2).text().should.equal('1_OVERRIDE_1')
                        $c.eq(2).lookupAllComponents()[0].toString()
                            .should.equal('ItemTestCollectionItem')
                        $c.eq(3).text().should.equal('1_OVERRIDE_1')
                        done()
                    })
                    .trigger('updateModel', {
                        model: vertexGen('1_OVERRIDE_1', [])
                    })
            })

            it('should reuse elements and send updateModel events if available', function(done) {
                var $node = this.component.$node,
                    $y = $node.find('.y'),
                    $c = $y.children()

                $c.length.should.equal(2)
                $c[0].dataset.flag = 1

                $node.on('modelUpdated', function(event) {
                        var $c = $y.children()
                        $c.length.should.equal(2)
                        // Check if it's the same element
                        $c[0].dataset.flag.should.equal('1')
                        $c.eq(0).text().should.equal('2_OVERRIDE')
                        $c.eq(0).lookupAllComponents()[0].toString().should
                            .equal('ItemTestCollectionItem')
                        $c.eq(1).text().should.equal('2_OVERRIDE')
                        // Make sure that we teardown old components
                        $c.eq(1).lookupAllComponents().should.be.empty
                        done()
                    })
                    .trigger('updateModel', {
                        model: vertexGen('2_OVERRIDE', [])
                    })
            })
            
            it('should switch from layout to no layout and teardown component', function(done) {
                var $node = this.component.$node,
                    $y = $node.find('.y'),
                    $c = $y.children()

                $c.length.should.equal(2)

                $node.on('modelUpdated', function(event) {
                        $y.children().length.should.equal(1)
                        $y.lookupAllComponents().should.be.empty
                        done()
                    })
                    .trigger('updateModel', {
                        model: vertexGen('3_OVERRIDE', [])
                    })
            })

            it('should switch between 2 different layouts and teardown/initialize', function(done) {
                var $node = this.component.$node,
                    $y = $node.find('.y'),
                    $c = $y.children(),
                    comps = $y.lookupAllComponents()

                $c.length.should.equal(2)
                comps.length.should.equal(1)
                comps[0].toString().should.equal('FlexLayout')

                $node.on('modelUpdated', function(event) {
                        comps = $y.lookupAllComponents()
                        comps.length.should.equal(1)
                        comps[0].toString().should.equal('ItemTestLayout')
                        done()
                    })
                    .trigger('updateModel', {
                        model: vertexGen('4_OVERRIDE', [])
                    })

            })

            it('should update layout options on component switch')

            it('should test applyTo sorting')

            it('should reset layout styles on layout change')
            it('should reset layout styles on layout removal')
        })

    })

    function genName(prefix) {
        var i = 0;
        if (prefix in gen) {
            i = gen[prefix]++;
        } else {
            gen[prefix] = i + 1;
        }
        return prefix + i
    }

    function propGen(name, value) {
        return {
            name: name || genName('propName'),
            value: value || genName('propValue')
        }
    }

    function vertexGen(id, properties) {
        return {
            id: arguments.length === 2 ? id : genName('vId'),
            type: 'vertex',
            properties: (arguments.length === 1 ? id : properties) || []
        }
    }

    function setupItemComponent(root, model, optionalLayout) {
        doc()
        if (optionalLayout) {
            registry.registerExtension(layoutTypePoint, optionalLayout)
        }
        registry.unregisterAllExtensions(layoutComponentPoint)
        var self = this,
            extensionsToRegister = _.isArray(root) ? root : [root]
            extensionsToRegister.forEach(function(e) {
                registry.registerExtension('org.visallo.layout.component', e)
            })

        doc()
        if (this.component && this.component.node) {
            this.component.node.textContent = '';
            this.component.node.className = '';
            this.component = null;
        }
        setupComponent(this, { model: model });
        return new Promise(function(f) {
            self.component.$node.on('finishedLoadingTypeContent', function() {
                f();
            })
        })

        function doc() {
            registry.documentExtensionPoint(layoutComponentPoint, 'desc', function() { return true; })
        }
    }
})
