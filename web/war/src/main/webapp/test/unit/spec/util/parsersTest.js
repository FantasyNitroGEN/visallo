define(['util/parsers'], function(P) {

    describe('parsers', function() {

        describe('for numbers', function() {

            var VALID_INTEGERS = [['0', 0], ['42', 42], ['42.3', 42], ['+123', 123], ['-654.2', -654]],
                VALID_FLOATS = [['67.89', 67.89], ['-543.21', -543.21], ['99.', 99], ['88', 88]],
                VALID_NUMBERS = VALID_INTEGERS.concat(VALID_FLOATS),
                VALID_INTEGERS_WITH_UNITS = [['24', 24], ['42s', 42], ['42.3xyz', 42], ['+123abc', 123], ['-654.9i', -654]],
                VALID_FLOATS_WITH_UNITS = [['9.99', 9.99], ['10.80s', 10.8], ['+77.7xyz', 77.7], ['-654.9i', -654.9]],
                VALID_NUMBERS_WITH_UNITS = VALID_INTEGERS_WITH_UNITS.concat(VALID_FLOATS_WITH_UNITS),

                INVALID_NUMBERS_WITH_UNITS = ['.99', '-.88', 's42', '12 ms', '$678.29', '', '++4', '--3', '', ' ', 42, 12.34, NaN],
                INVALID_NUMBERS = INVALID_NUMBERS_WITH_UNITS.concat('42s', '-24ms');

            it('should detect valid numbers', function() {
                _.each(VALID_NUMBERS, function(v) {
                    expect(P.number.isValid(v[0])).to.equal(true);
                })
            });

            it('should detect invalid numbers', function() {
                _.each(INVALID_NUMBERS, function(v) {
                    expect(P.number.isValid(v)).to.equal(false);
                })
            });

            it('should detect valid numbers with units', function() {
                _.each(VALID_NUMBERS_WITH_UNITS, function(v) {
                    expect(P.number.isValidWithUnits(v[0])).to.equal(true);
                })
            });

            it('should detect invalid numbers with units', function() {
                _.each(INVALID_NUMBERS_WITH_UNITS, function(v) {
                    expect(P.number.isValidWithUnits(v)).to.equal(false);
                })
            });

            it('should parse valid integers', function() {
                _.each(VALID_INTEGERS, function(v) {
                    expect(P.number.parseInt(v[0])).to.equal(v[1]);
                })
            });

            it('should not parse invalid integers', function() {
                _.each(INVALID_NUMBERS, function(v) {
                    expect(isNaN(P.number.parseInt(v))).to.equal(true);
                })
            });

            it('should parse valid integers with units', function() {
                _.each(VALID_INTEGERS_WITH_UNITS, function(v) {
                    expect(P.number.parseIntWithUnits(v[0])).to.equal(v[1]);
                })
            });

            it('should not parse invalid integers with units', function() {
                _.each(INVALID_NUMBERS_WITH_UNITS, function(v) {
                    expect(isNaN(P.number.parseInt(v))).to.equal(true);
                })
            });

            it('should parse valid floats', function() {
                _.each(VALID_FLOATS, function(v) {
                    expect(P.number.parseFloat(v[0])).to.equal(v[1]);
                })
            });

            it('should not parse invalid floats', function() {
                _.each(INVALID_NUMBERS, function(v) {
                    expect(isNaN(P.number.parseFloat(v))).to.equal(true);
                })
            });

            it('should parse valid floats with units', function() {
                _.each(VALID_FLOATS_WITH_UNITS, function(v) {
                    expect(P.number.parseFloatWithUnits(v[0])).to.equal(v[1]);
                })
            });

            it('should not parse invalid floats with units', function() {
                _.each(INVALID_NUMBERS_WITH_UNITS, function(v) {
                    expect(isNaN(P.number.parseFloat(v))).to.equal(true);
                })
            });
        });
    });
});
