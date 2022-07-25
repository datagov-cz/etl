define(["angular"], function (angular) {

  const htmlPrefix = "log-tail-dialog-";

  function controller($scope, $mdDialog, $interval, $http, $refresh, execution) {

    function scrollToBottom() {
      const div = document.getElementById(htmlPrefix + "content-end");
      div.scrollIntoView(false);
    }

    function isExecutionFinished(execution) {
      return execution["status-monitor"] ===
        "http://etl.linkedpipes.com/resources/status/finished";
    }

    const refreshData = () => {
      const element = document.getElementById(htmlPrefix + "content");
      if (element === null || element === undefined) {
        // Dialog was closed.
        $refresh.remove("log-tail");
        return;
      }
      const url = "./api/v1/executions-logs-tail?n=100&iri="
        + encodeURIComponent(execution.iri);
      $http.get(url).then((response) => {
          const lastSize = element.textContent.length;
          element.textContent = response.data;
          if (response.data.length !== lastSize) {
            scrollToBottom();
          }
          if (isExecutionFinished(execution)) {
            $refresh.remove("log-tail");
          }
        }, (response) => {
        element.textContent = "There are no log data available.";
          console.warn("Request failed.", response);
        }
      );
    };

    $scope.onClose = function () {
      $refresh.remove("log-tail");
      $mdDialog.hide();
    };

    angular.element(document).ready(refreshData);

    $scope.label = execution.label;
    $refresh.add("log-tail", refreshData);
  }

  controller.$inject = [
    "$scope",
    "$mdDialog",
    "$interval",
    "$http",
    "refresh",
    "execution"
  ];

  let _initialized = false;
  return function init(app) {
    if (_initialized) {
      return;
    }
    _initialized = true;
    app.controller("execution.log-tail.dialog", controller);
  }

});
