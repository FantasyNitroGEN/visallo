
define(['configuration/plugins/registry'], function(registry) {

    describe('Registry', function() {

        before(function() {
            registry.clear()
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
        })
    })
})
