define([
  "jquery",
  "../general-tab/template-general-tab",
  "../template-hierarchy-tab/template-hierarchy-tab",
  "@client/component/embed-directive/template-embed-directive",
  "../usage-tab/template-usage-tab",
  "../../template-service",
  "@client/component/dialog-service"
], function (jQuery, generalTab, hierarchyTab, embedDirective,
             usageTab, templateService, templateDialogService) {
  "use strict";

  function directive($rootScope, $location, $status,
                     templateService, templateDialogService) {

    function controller($scope) {

      $scope.isUsed = true;

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
        const template = $scope.api.store.template;
        const templateToEdit = $scope.api.store.templateToEdit;
        const configuration = $scope.api.store.configuration;

        $scope.template = template;
        $scope.dialogService.api.setIri(template.id);
        $scope.templateToEdit = templateToEdit;
        $scope.dialogService.api.setInstanceConfig(configuration);
        //
        const parent = template.template;
        if (parent === undefined) {
          $scope.dialogService.api.setTemplateConfig(undefined);
          // Load dialogs.
          $scope.dialogs = templateService.getDialogs(
            template.id, true);
          return;
        }
        // Load parent configuration.
        templateService.fetchEffectiveConfig(parent).then((config) => {
          // For sure we use new object here.
          $scope.dialogService.api.setTemplateConfig(
            jQuery.extend(true, [], config))
          // Load dialogs.
          $scope.dialogs = templateService.getDialogs(
            template.id, true);
        });
        // Load usage.
        templateService.getUsage(template.id).then((usage) => {
          $scope.usage = usage;
          $scope.isUsed = Object.keys(usage).length !== 0;
        });
      }

      $scope.onDelete = () => {
        templateService.delete($scope.template.id).then(() => {
          $status.success("Template deleted.");
          $location.path("/templates").search({});
        }, (response) => {
          $status.httpError("Can't delete the template.", response);
        });
      };

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

      function changeToHierarchyTab() {
        $scope.activeTab = 1;
      }

      // The data might be already ready, as we do not know
      // if the parent, or we get to run first.
      if ($scope.api.store) {
        load();
      }
    }

    controller.$inject = ["$scope"];

    return {
      "restrict": "E",
      "template": require("./template-detail-directive.html"),
      "scope": {
        // Shared API object.
        // Use store property to transfer data.
        "api": "=",
        "showTemplateManagement": "=",
        "enableTemplateLink": "="
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
    hierarchyTab(app);
    embedDirective(app);
    usageTab(app);
    templateService(app);
    templateDialogService(app);
    //
    app.directive("lpTemplateDetail", ["$rootScope", "$location",
      "status", "template.service",
      "template.dialog.service", directive]);
  };

});
