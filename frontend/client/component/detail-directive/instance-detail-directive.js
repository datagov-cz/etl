define([
  "jquery",
  "../../template/detail/general-tab/template-general-tab",
  "../embed-directive/template-embed-directive",
  "../../template/template-service",
  "../dialog-service",
  "../../template/detail/instance-hierarchy-tab/instance-hierarchy-tab",
  // User-Interface
  "../ui/iri-list/iri-list",
  "../ui/localized-text-input/localized-text-input",
  "../ui/sparql-editor/yasqe-editor",
  "./dialog-rdf-service"
], function (
  jQuery, generalTab, embedDirective, templateService, templateDialogService,
  hierarchyTab, _iriList, _localizedTextInput, _yasqeEditore, _rdfService) {
  "use strict";

  function directive($rootScope, templateService, templateDialogService) {

    function controller($scope) {

      // Instance used to communicate with the dialog.
      $scope.dialogService = templateDialogService.new();

      // Scope used by the dialog.
      $scope.dialogScope = $rootScope.$new(true);
      $scope.dialogScope.application = {
        "changeToHierarchyTab": changeToHierarchyTab
      };

      // Services to bind to the dialog.
      $scope.dialogLocals = {
        "$service": $scope.dialogService
      };

      /**
       * Set all working variables.
       */
      function load() {

        // const template = $scope.api.store.template;
        const instance = $scope.api.store.instance;
        const parent = $scope.api.store.parent;
        const configuration = $scope.api.store.configuration;

        $scope.dialogService.api.setIri(instance.id);
        $scope.instance = instance;
        $scope.parent = parent;
        $scope.dialogService.api.setInstanceConfig(configuration);
        // Load parent configuration.
        templateService.fetchEffectiveConfig(parent.id).then((config) => {
          // To be sure, we use new object here.
          $scope.dialogService.api.setTemplateConfig(
            jQuery.extend(true, [], config));
          // Load dialogs.
          $scope.dialogs = templateService.getDialogs(
            parent.id, false);
        });
      }

      /**
       * Make sure that the content in the shared objects is
       * updated.
       */
      $scope.api.save = () => {
        if ($scope.dialogService.onStore !== undefined) {
          $scope.dialogService.onStore();
        }
      };

      $scope.api.load = load;

      // The data might be already ready, as we do not know
      // if the parent, or we get to run first.
      if ($scope.api.store) {
        load();
      }

      function changeToHierarchyTab() {
        $scope.activeTab = $scope.dialogs.length + 1;
      }

    }

    controller.$inject = ["$scope"];

    return {
      "restrict": "E",
      "template": require("./instance-detail-directive.html"),
      "scope": {
        // Shared API object.
        // Use store property to transfer data.
        "api": "="
      },
      "controller": controller
    };
  }

  let _initialized = false;
  return function init(app) {
    if (_initialized) {
      return;
    }
    _initialized = true;
    //
    generalTab(app);
    embedDirective(app);
    templateService(app);
    templateDialogService(app);
    hierarchyTab(app);
    _iriList(app);
    _localizedTextInput(app);
    _yasqeEditore(app);
    _rdfService(app);
    //
    app.directive("lpInstanceDetail", ["$rootScope",
      "template.service", "template.dialog.service", directive]);
  };

});
