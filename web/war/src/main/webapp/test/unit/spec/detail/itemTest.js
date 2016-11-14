require(['configuration/plugins/registry'], function(registry) {
    var gen = {},
        rootLayout = 'org.visallo.layout.root',
        layoutTypePoint = 'org.visallo.layout.type',
        layoutComponentPoint = 'org.visallo.layout.component',
        PATH_PREFIX = '../test/unit/spec/detail',
        collectionItemExample;

    describe('Item Layout Tests', function() {

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
                        }, 250)
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
                var originalError = console.error,
                    catchCheck = chai.spy();
                console.error = chai.spy()
                return setupItemComponent.call(this, collectionItemExample, vertexGen([propGen(), propGen()]))
                    .catch(catchCheck)
                    .then(function() {
                        catchCheck.should.have.been.called.once
                        console.error.should.have.been.called.at.least(1)
                        console.error = originalError
                    })
            })
        })

        describe('CollectionItem with children and components', function() {
            beforeEach(function() {
                collectionItemExample[2].componentPath = PATH_PREFIX + '/itemTestCollectionItemDataset'
                collectionItemExample[2].children = [
                    {
                        componentPath: PATH_PREFIX + '/itemTestCollectionItemComponent',
                        attributes: function(model) {
                            return { prefix: 'My Comp:', model: model.name }
                        }
                    }
                ];
                return setupItemComponent.call(this, collectionItemExample, vertexGen([propGen(), propGen(), propGen()]))
            })

            it('should have created 3 components', function() {
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
            var warn = console.warn;
            beforeEach(function() {
                console.warn = chai.spy('console.warn')
            })
            afterEach(function() {
                console.warn = warn
            })
            it('should show warning if no updateModel event', function() {
                collectionItemExample[2].children = [{ componentPath: PATH_PREFIX + '/itemTestCollectionItemComponentNoEvent' }]
                return setupItemComponent.call(this, collectionItemExample, vertexGen([propGen('a')]))
                    .then(function() {
                        console.warn.should.have.been.called.once
                    })
            })

            it('should suppress warning on attribute', function() {
                collectionItemExample[2].children = [{ componentPath: PATH_PREFIX + '/itemTestCollectionItemComponentNoEventSuppress' }]
                return setupItemComponent.call(this, collectionItemExample, vertexGen([propGen('a')]))
                    .then(function() {
                        console.warn.should.not.have.been.called();
                    })
            })
        })

        describe('CollectionItem updating with component', function() {
            beforeEach(function() {
                collectionItemExample[2].children = [{ componentPath: PATH_PREFIX + '/itemTestCollectionItemComponent', model: function(m) { return m.name; } }]
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
                    _.last(attached[0].mixedIn).name.should.equal('ItemTestCollectionItem')
                })
            })

            it('should update once', function(done) {
                var $node = this.component.$node,
                    $list = $node.find('.my-list'),
                    spy = chai.spy(),
                    comps = $list.children().eq(0).find('div').lookupAllComponents(),
                    div = $list.children().eq(0).find('div').on('updateModel', spy)
                    
                comps.length.should.equal(1)
                _.last(comps[0].mixedIn).name.should.equal('ItemTestCollectionItem')

                $node.on('modelUpdated', function(event) {
                        _.delay(function() {
                            spy.should.be.called.once
                            done()
                        }, 250)
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
                        _.last(comps[0].mixedIn).name.should.equal('FlexLayout')
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

            it('should render empty strings without error', function() {
                var self = this
                return setup(this, { ref: 'org.visallo.layout.text', model: '' }).then(function() {
                    self.component.$node.find('.org-visallo-layout-text').text().should.equal('')
                })
            })

            it('should render functions that return empty strings without error', function() {
                var self = this
                return setup(this, { ref: 'org.visallo.layout.text', model: function() {
                    return '';
                } }).then(function() {
                    self.component.$node.find('.org-visallo-layout-text').text().should.equal('')
                })
            })

            it('should render functions that return promise of empty strings without error', function() {
                var self = this
                return setup(this, { ref: 'org.visallo.layout.text', model: function() {
                    return Promise.resolve('');
                } }).then(function() {
                    self.component.$node.find('.org-visallo-layout-text').text().should.equal('')
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
                        layout: { type: 'flex', options: { direction: 'row', wrap: 'nowrap' } },
                        children: [
                            { componentPath: PATH_PREFIX + '/itemTestCollectionItemComponent' },
                            { componentPath: PATH_PREFIX + '/itemTestCollectionItemComponentNoEventSuppress', style: { flex: 1 } }
                        ]
                    },
                    {
                        identifier: 'y',
                        applyTo: function(model) {
                            return (/^1_OVERRIDE/).test(model)
                        },
                        layout: { type: 'flex', options: { direction: 'row' } },
                        children: [
                            { componentPath: PATH_PREFIX + '/itemTestCollectionItemComponentNoEventSuppress', style: { flex: 1 } },
                            { componentPath: PATH_PREFIX + '/itemTestCollectionItemComponent' },
                            { componentPath: PATH_PREFIX + '/itemTestCollectionItemComponent' },
                            { ref: 'x' }
                        ]
                    },
                    {
                        identifier: 'y',
                        applyTo: function(model) {
                            return (/^2_OVERRIDE/).test(model)
                        },
                        layout: { type: 'flex', options: { direction: 'column' } },
                        children: [
                            { componentPath: PATH_PREFIX + '/itemTestCollectionItemComponent' },
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
                    registerLayout: {
                        type: 'test_layout',
                        componentPath: PATH_PREFIX + '/itemTestLayout'
                    }
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
                    $y = $node.find('.y'),
                    tryNum = 0;

                $y.children().length.should.equal(2)
                $node.on('modelUpdated', function(event) {
                        if (tryNum === 0) {
                            var $c = $y.children()
                            $c.length.should.equal(4)
                            // Component order is changed
                            $c.eq(0).text().should.equal('1_OVERRIDE_1')
                            _.last($c.eq(0).lookupAllComponents()[0].mixedIn).name
                                .should.equal('ItemTestCollectionItemNoEventSuppress')
                            $c.eq(1).text().should.equal('1_OVERRIDE_1')
                            _.last($c.eq(1).lookupAllComponents()[0].mixedIn).name
                                .should.equal('ItemTestCollectionItem')
                            $c.eq(2).text().should.equal('1_OVERRIDE_1')
                            _.last($c.eq(2).lookupAllComponents()[0].mixedIn).name
                                .should.equal('ItemTestCollectionItem')
                            $c.eq(3).text().should.equal('1_OVERRIDE_1')
                            tryNum++;
                            $node.trigger('updateModel', { model: vertexGen('default', []) })
                        } else {
                            $y.children().length.should.equal(2)
                            done()
                        }
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
                        _.last($c.eq(0).lookupAllComponents()[0].mixedIn).name
                            .should.equal('ItemTestCollectionItem')
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

            it('should test applyTo sorting')

            it('should reset layout styles on layout removal', function() {
                var $node = this.component.$node,
                    $y = $node.find('.y')
                return new Promise(function(f) {
                    var tryNum = 0;
                    $node.on('modelUpdated', function(event) {
                            var $c = $y.children()
                            if (tryNum === 0) {
                                $c.length.should.equal(4)
                                $c[0].style.flex.should.contain('1')
                                tryNum++;
                                $node.trigger('updateModel', { model: vertexGen('3_OVERRIDE_1', []) })
                            } else {
                                $c.length.should.equal(1)
                                expect($c[0].style.flex || false).to.be.false
                                f();
                            }
                        })
                        .trigger('updateModel', {
                            model: vertexGen('1_OVERRIDE_1', [])
                        })
                })
            })

            it('should change layout styles on layout change', function() {
                var $node = this.component.$node,
                    $y = $node.find('.y')

                $node[0].style.flexDirection.should.equal('column')
                $y[0].style.flexDirection.should.equal('row')
                $y[0].style.flexWrap.should.equal('nowrap')
                $y.children().length.should.equal(2)
                $y.children()[1].style.flex.should.contain('1')

                return new Promise(function(f) {
                    var tryNum = 0;
                    $node.on('modelUpdated', function(event) {
                            $y = $node.find('.y')
                            var $c = $y.children()
                            if (tryNum === 0) {
                                $node[0].style.flexDirection.should.equal('column')
                                $y[0].style.flexDirection.should.equal('row')
                                expect($y[0].style.flexWrap || false).to.be.false
                                $c.length.should.equal(4)
                                $c[0].style.flex.should.contain('1')
                                expect($c[1].style.flex || false).to.be.false
                                
                                tryNum++;
                                $node.trigger('updateModel', { model: vertexGen('2_OVERRIDE_1', []) })
                            } else {
                                $c.length.should.equal(2)
                                $node[0].style.flexDirection.should.equal('column')
                                $y[0].style.flexDirection.should.equal('column')
                                expect($c[0].style.flex || false).to.be.false
                                expect($c[1].style.flex || false).to.be.false
                                f();
                            }
                        }).trigger('updateModel', {
                            model: vertexGen('1_OVERRIDE_1', [])
                        })
                })
            })
        })


        describe('Constraints', function() {
            var setup,
                warn = console.warn;
            before(function() {
                setup = _.bind(_setup, this)
            })
            beforeEach(function() {
                console.warn = chai.spy('console.warn')
            })
            afterEach(function() {
                console.warn = warn
            })

            it('should accept contraints and return one that matches both', function() {
                return setup({
                    constraints: ['height', 'width']
                }).then(function(node) {
                    node.textContent.should.equal('width/height')
                })
            })

            it('should accept contraints and return one that matches if one, then update', function() {
                return setup({
                    constraints: ['width']
                }).then(function(node) {
                    node.textContent.should.equal('width')

                    return new Promise(function(resolve) {
                        $(node)
                            .on('constraintsUpdated', function() {
                                node.textContent.should.equal('width/height')
                                resolve(); 
                            })
                            .trigger('updateConstraints', {
                                constraints: ['width', 'height']
                            })
                    })
                })
            })
            
            it('should fall back to unlabeled constrain component', function() {
                return setup({
                    constraints: ['unknown']
                }).then(function(node) {
                    console.warn.should.not.have.been.called.once
                    node.textContent.should.equal('initial')
                })
            })

            it('should fall back to unlabeled when match constrain component', function() {
                return setup({
                    constraints: ['height']
                }).then(function(node) {
                    console.warn.should.not.have.been.called.once
                    node.textContent.should.equal('initial')
                })
            })

            it('should still respect other applyTo params', function() {
                return setup({
                    constraints: ['width', 'height']
                }, edgeGen()).then(function(node) {
                    node.textContent.should.equal('width/height for edge')
                })
            })

            it('should be able to add children with new constraints and propagate the tree', function() {
                return setup({}, vertexGen('childHasConstraintAdded')).then(function(node) {
                    node.textContent.should.equal('y with width constraint')
                })
            })

            it('should be able to add children with new constraints added to parents', function() {
                return setup({ constraints: ['height'] }, vertexGen('childHasConstraintAdded')).then(function(node) {
                    node.textContent.should.equal('x with width/height constraint')
                })
            })

            it('should select default component if no contraints required', function() {
                return setup({ }, vertexGen()).then(function(node) {
                    node.textContent.should.equal('initial')
                })
            })

            function _setup(attrs, model) {
                var self = this;
                return setupItemComponent.call(this, [
                        { identifier: rootLayout, render: r('initial') },
                        { identifier: rootLayout, render: r('width'), applyTo: { constraints: ['width'] } },
                        { identifier: rootLayout, render: r('width/height'), applyTo: { constraints: ['width', 'height'] } },
                        { identifier: rootLayout, render: r('width/height for edge'), 
                            applyTo: {
                                type: 'edge',
                                constraints: ['width', 'height'] 
                            }
                        },
                        {
                            identifier: rootLayout,
                            applyTo: function(model) { return model.id === 'childHasConstraintAdded' },
                            children: [
                                { ref: 'x', constraints: ['width'] }
                            ]
                        },
                        {
                            identifier: 'x',
                            render: r('x')
                        },
                        {
                            identifier: 'x',
                            applyTo: { constraints: ['width'] },
                            children: [
                                { ref: 'y' }
                            ]
                        },
                        {
                            identifier: 'x',
                            applyTo: { constraints: ['height', 'width'] },
                            render: r('x with width/height constraint')
                        },
                        {
                            identifier: 'y',
                            render: r('y')
                        },
                        {
                            identifier: 'y',
                            applyTo: { constraints: ['width'] },
                            render: r('y with width constraint')
                        }
                    ], model || vertexGen(), { attrs: attrs })
                    .then(function() {
                        return self.component.node;
                    })
            }
        })

        describe('Contexts', function() {
            var setup
            before(function() {
                setup = _.bind(_setup, this)
            })

            it('should return initial if no context specified', function() {
                return setup({ }).then(function(node) {
                    node.textContent.should.equal('initial')
                })
            })

            it('should return matching context', function() {
                return setup({ context: 'test2' }).then(function(node) {
                    node.textContent.should.equal('test12')
                })
            })

            it('should update context', function() {
                return setup({
                    context: 'test2'
                }).then(function(node) {
                    node.textContent.should.equal('test12')

                    return new Promise(function(resolve) {
                        $(node)
                            .on('contextUpdated', function() {
                                node.textContent.should.equal('test0')
                                resolve(); 
                            })
                            .trigger('updateContext', {
                                context: 'test0'
                            })
                    })
                })
            })

            it('should select context before constraints', function() {
                return setup({ context: 'test2', constraints: ['width'] }).then(function(node) {
                    node.textContent.should.equal('width & test12')
                })
            })

            function _setup(attrs, model) {
                var self = this;
                return setupItemComponent.call(this, [
                        { identifier: rootLayout, render: r('initial') },
                        { identifier: rootLayout, render: r('test0'), applyTo: { contexts: ['test0'] } },
                        { identifier: rootLayout, render: r('width'), applyTo: { constraints: ['width'] } },
                        { identifier: rootLayout, render: r('test12'), applyTo: { contexts: ['test1', 'test2'] } },
                        { identifier: rootLayout, render: r('width & test12'), applyTo: { constraints: ['width'], contexts: ['test1', 'test2'] } },
                        { identifier: rootLayout, render: r('no context') }
                    ], model || vertexGen(), { attrs: attrs })
                    .then(function() {
                        return self.component.node;
                    })
            }
        })

    })

    })

    function r(text) {
        return function(el) {
            el.textContent = text;
        }
    }

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

    function edgeGen(id, properties) {
        return elementGen('edge', id, properties)
    }

    function vertexGen(id, properties) {
        return elementGen('vertex', id, properties)
    }

    function elementGen(type, id, properties) {
        return {
            id: (id && !_.isArray(id)) ? id : genName('eId'),
            type: type,
            properties: (properties && id ? properties : _.isArray(id) ? id : []) || []
        }
    }

    function setupItemComponent(root, model, options) {
        doc()
        if (options && options.registerLayout) {
            registry.registerExtension(layoutTypePoint, options.registerLayout)
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
        setupComponent(this, _.extend({}, options && options.attrs || {}, { model: model }));
        return new Promise(function(f, r) {
            self.component.$node.on('errorLoadingTypeContent', function() {
                r();
            })
            self.component.$node.on('finishedLoadingTypeContent', function() {
                f();
            })
        })

        function doc() {
            registry.documentExtensionPoint(layoutComponentPoint, 'desc', function() { return true; })
        }
    }
})
