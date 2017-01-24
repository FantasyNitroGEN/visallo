define(['util/promise'], function() {
    'use strict';

    const ajax = sinon.stub().returns(Promise.resolve(null));

    return ajax;
});
