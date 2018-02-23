((definition) => {
    if (typeof define === "function" && define.amd) {
        define([
            "vocabulary",
            "angular",
            "./execution-list-repository",
            "app/components/executions/log-tail/execution-log-tail-dialog",
            "./../execution-api",
            "./../../pipelines/pipeline-api",
        ], definition);
    }
})((vocabulary, angular, _repository, _logTailDialog, executionApi, pipelineApi) => {

    function factory($http, $mdDialog, $statusService, $mdMedia, repository) {

        let $scope;

        function initialize(scope) {
            $scope = scope;
            $scope.filter = {
                "labelSearch": "",
            };
            $scope.repository = repository.create($scope.filter);
        }

        function execute(execution) {
            const iri = execution["pipeline"]["iri"];
            executePipeline(iri);
        }

        function executePipeline(iri) {
            pipelineApi.execute($http, iri)
            .then(updateRepository)
            .catch(handleExecutionStartFailure);
        }

        function updateRepository() {
            $scope.repository.update();
        }

        function handleExecutionStartFailure(response) {
            $statusService.httpPostFailed({
                "title": "Can't start the execution.",
                "response": response
            });
        }

        function cancelExecution(execution) {
            executionApi.cancel($http, execution.id, "User request")
            .then(() => {
                execution.cancelling = true;
            }).catch(handleExecutionCancelFailure);
        }

        function handleExecutionCancelFailure(response) {
            $statusService.httpPostFailed({
                'title': "Can't cancel the execution.",
                'response': response
            });
        }

        function openLogTail(execution) {
            const useFullScreen = ($mdMedia('sm') || $mdMedia('xs'));
            $mdDialog.show({
                'controller': 'execution.log-tail.dialog',
                'templateUrl': 'app/components/executions/log-tail/execution-log-tail-dialog.html',
                'clickOutsideToClose': false,
                'fullscreen': useFullScreen,
                'locals': {
                    'execution': execution
                }
            });
        }

        // TODO Consider adding confirmation dialog.
        function deleteExecution(execution, event) {
            repository.delete(execution, $scope.repository);
        }

        function onSearchStringChange() {
            repository.onFilterChanged($scope.repository, "label");
        }

        function loadExecutions() {
            repository.load($scope.repository);
        }

        function increaseVisibleItemLimit() {
            repository.increaseVisibleItemLimit($scope.repository);
        }

        return {
            "initialize": initialize,
            "execute": execute,
            "cancel": cancelExecution,
            "openLogTail": openLogTail,
            "delete": deleteExecution,
            "onSearchStringChange": onSearchStringChange,
            "increaseVisibleItemLimit": increaseVisibleItemLimit,
            "load": loadExecutions
        };
    }

    factory.$inject = [
        "$http",
        "$mdDialog",
        "services.status",
        "$mdMedia",
        "execution.list.repository"];

    let initialized = false;
    return function init(app) {
        if (initialized) {
            return;
        }
        initialized = true;
        _repository(app);
        _logTailDialog(app);
        app.factory("execution.list.service", factory);
    }
});





