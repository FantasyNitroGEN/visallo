define([
  'flight/lib/component',
  'configuration/admin/utils/withFormHelpers',
  'configuration/admin/utils/fileUpload',
  'hbs!org/visallo/web/plugin/adminImportRdf/templates/admin-import-rdf',
  'util/formatters',
  'util/withDataRequest'
], function(
  defineComponent,
  withFormHelpers,
  FileUpload,
  template,
  F,
  withDataRequest
) {
  'use strict';
  return defineComponent(AdminImportRdf, withDataRequest, withFormHelpers);

  function AdminImportRdf() {

    this.defaultAttrs({
      uploadSelector: '.btn-primary'
    });

    this.after('initialize', function() {
      var self = this;

      this.on('fileChanged', this.onFileChanged);
      this.on('click', {
        uploadSelector: this.onUpload
      });

      this.$node.html(template({}));

      FileUpload.attachTo(this.$node.find('.upload'));
    });

    this.onUpload = function() {
      var self = this,
        importButton = this.select('uploadSelector'),
        request = this.dataRequest('org-visallo-web-adminRdfImport', 'uploadRdf', this.rdfFile);

      this.handleSubmitButton(importButton, request);

      request.then(function() {
          self.showSuccess('Upload successful');
          self.trigger(importButton, 'reset');
        })
        .catch(this.showError.bind(this, 'Upload failed'));
    };

    this.onFileChanged = function(event, data) {
      this.rdfFile = data.file;
      this.checkValid();
    };

    this.checkValid = function() {
      if (this.rdfFile) {
        this.select('uploadSelector').removeAttr('disabled');
      } else {
        this.select('uploadSelector').attr('disabled', true);
      }
    };
  }
});
