define('data/web-worker/services/org-visallo-web-adminRdfImport', [
  'data/web-worker/util/ajax'
], function(ajax) {
  return {
    uploadRdf: function(file) {
      var formData = new FormData();
      formData.append('file', file);
      return ajax('POST->HTML', '/admin/import-rdf', formData);
    }
  }
});
