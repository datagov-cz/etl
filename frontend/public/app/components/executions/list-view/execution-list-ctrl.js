((definition) => {
    if (typeof define === "function" && define.amd) {
        define([
            "angular",
            "./execution-list-service"
        ], definition);
    }
})((angular, _executionListService) => {
    "use strict";

    function controller($scope, $lpScrollWatch, service) {

        service.initialize($scope);

        $scope.onExecute = service.execute;

        $scope.onCancel = service.cancel;

        $scope.onOpenLogTail = service.openLogTail;

        $scope.onDelete = service.delete;

        $scope.$watch("filter.labelSearch", service.onSearchStringChange);

        $scope.noAction = () => {
            // This is do nothing action, we need it else the menu is open
            // on click to item. This cause menu to open which together
            // with navigation break the application.
        };

        let callbackReference = null;

        callbackReference = $lpScrollWatch.registerCallback((byButton) => {
            service.increaseVisibleItemLimit();
            if (!byButton) {
                // This event come outside of Angular scope.
                $scope.$apply();
            }
        });

        $scope.$on("$destroy", () => {
            $lpScrollWatch.unRegisterCallback(callbackReference);
        });

        function initialize() {
            $lpScrollWatch.updateReference();
            service.load();
        }

        angular.element(initialize);
    }

    controller.$inject = [
        "$scope",
        "$lpScrollWatch",
        "execution.list.service"
    ];

    let initialized = false;
    return function init(app) {
        if (initialized) {
            return;
        }
        initialized = true;
        _executionListService(app);
        app.controller("components.executions.list", controller);
    }

});
