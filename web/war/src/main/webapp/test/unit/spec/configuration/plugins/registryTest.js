/* globals chai:false */
define(['configuration/plugins/registry'], function(registry) {

    describe('Registry', function() {
        var originalWarn = console.warn;

        beforeEach(function() {
            console.warn = sinon.spy()
            registry.clear()
        })

        afterEach(function() {
            console.warn = originalWarn
        })

        it('should be able to register and unregister extensions', function() {
            var uuid = registry.registerExtension('a', {})

            registry.documentExtensionPoint('a', 'desc', function() {
                return true
            })
            registry.extensionsForPoint('a').length.should.equal(1)
            registry.unregisterExtension(uuid)
            registry.extensionsForPoint('a').length.should.equal(0)
        })

        it('should throw error when extension point is not provided', function() {
            expect(registry.registerExtension).to.throw('extension')
            expect(function() {
                registry.registerExtension('a');
            }).to.throw('extension')
        })

        it('should use validator when asking for extensions', function() {
            registry.registerExtension('a', 'My extension')

            registry.documentExtensionPoint('a', 'desc', function() {
                return false
            })
            registry.extensionsForPoint('a').should.be.empty

            registry.documentExtensionPoint('a', 'desc', function() {
                return true
            })
            registry.extensionsForPoint('a').length.should.equal(1)
            console.warn.should.be.called.once
        })

        it('should be able to document with string url', function() {
            var url = 'http://www.example.com'
            registry.documentExtensionPoint('a', 'desc', function() {
                return false
            }, url)
            registry.extensionsForPoint('a').should.be.empty
            registry.extensionPointDocumentation().a.externalDocumentationUrl.should.equal(url)
        })

        it('should be able to document with options url', function() {
            var url = 'http://www.example.com'
            registry.documentExtensionPoint('a', 'desc', function() { return false }, { url: url })
            registry.extensionsForPoint('a').should.be.empty
            registry.extensionPointDocumentation().a.externalDocumentationUrl.should.equal(url)
        })

        it('should be able to document with options legacy', function() {
            var legacyUuid = registry.registerExtension('a-1', { beforeDocs: true })
            registry.documentExtensionPoint('a-2', 'desc', function() { return true }, { legacyName: 'a-1' })
            registry.extensionsForPoint('a-2').length.should.equal(1)
            registry.extensionsForPoint('a-1').length.should.equal(1)

            registry.registerExtension('a-1', { afterDocs: true })
            registry.registerExtension('a-2', { afterDocs: true })
            var newE = registry.extensionsForPoint('a-2')
            var oldE = registry.extensionsForPoint('a-1')
            _.each([newE, oldE], function(e, i) {
                e.length.should.equal(3)
                e[0].beforeDocs.should.equal(true)
                e[1].afterDocs.should.equal(true)
                e[2].afterDocs.should.equal(true)
            })

            registry.unregisterExtension(legacyUuid);
            newE = registry.extensionsForPoint('a-2')
            oldE = registry.extensionsForPoint('a-1')
            _.each([newE, oldE], function(e, i) {
                e.length.should.equal(2)
                e[0].afterDocs.should.equal(true)
                e[1].afterDocs.should.equal(true)
            })

            console.warn.should.be.called.once
            console.warn.should.be.calledWithMatch('renamed from a-1 to a-2')
        })
    })
})
