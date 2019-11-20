'use strict';

var myac = angular.module('myAppControllers', ['ui.bootstrap','ngSanitize']);

myac.controller('PluginListController', ['$scope', '$modal', '$http','$sce', function($scope, $modal, $http, $sce) {

    // Define the model properties. The view will loop
    // through the services array and genreate a li
    // element for every one of its items.
    $scope.plugins = [];
    $scope.blocks = [];
    //var pluginPath = "http://localhost:4040/Images/processing/json/?op=list&type=plugin"
    var pluginPath = "test/plugins.json";
    //var blockPath= "http://localhost:4040/Images/processing/json/?op=list&type=block"
    var blockPath = "test/blocks.json";

    $http.get(pluginPath).success(function(data) {
        console.log("loading plugins:" + data);
        for (var i = 0; i < data.length; i++) {
            console.log("loading plugins:" + i + "=>" + data[i].name);
            data[i]["active"] = false;
            $scope.plugins.push(data[i]);
        }
    }).
    error(function(data, status, headers, config) {
        console.log("Plugin Json Request failed! " + data + " s:" + status);
    });

    $http.get(blockPath).success(function(data) {
        console.log("loading blocks:" + data);
        for (var i = 0; i < data.length; i++) {
            console.log("loading block:" + i + "=>" + data[i].name);
            data[i]["active"] = false;
            $scope.blocks.push(data[i]);
        }
    }).
    error(function(data, status, headers, config) {
        console.log("Block Json Request failed! " + data + " s:" + status);
    });
    
    $scope.graphScript = "graph TD; "
    
    $scope.addToRun = function(s) {
        $scope.graphScript+=" Img-->"+s.name+"; ";
        setTimeout(function(){mermaid.init();},1000)
    };
    
    $scope.getGraph = function() {
        
        return $sce.trustAsHtml($scope.graphScript);
    }

    $scope.getClass = function(s) {
        if (s.active) {
            return "active";
        } else {
            return "";
        }
    }

    // Helper method for calculating the total price

    $scope.total = function() {

        var total = 0;

        // Use the angular forEach helper method to
        // loop through the services array:

        angular.forEach($scope.plugins, function(s) {
            if (s.active) {
                total += 1;
            }
        });

        return total;
    };


    $scope.openSettings = function(pluginName) {

        var modalInstance = $modal.open({
            templateUrl: 'partials/arguments.html',
            controller: 'PluginSettings',
            size: 'lg',
            resolve: {
                pluginName: function() {
                    return pluginName;
                }
            }
        });

        modalInstance.result.then(function(selectedItem) {
            $scope.selected = selectedItem;
        }, function() {
            console.log('Modal dismissed at: ' + new Date());
        });
    };



}]);


myac.controller('PluginSettings', ['$scope', '$routeParams', 'GetSettings', function($scope, $routeParams, GetSettings) {

        // Define the model properties. The view will loop
        // through the services array and generate a li
        // element for every one of its items.
        $scope.arguments = GetSettings.query({
                pluginName: $routeParams.pluginName
            },
            function(setList) {
                console.log("Settings loaded:" + setList);
            });
        $scope.layers = function() {
            var layers = {};
            angular.forEach($scope.arguments, function(s) {
                layers[s.layer] = 1
            });
            return Object.keys(layers);
        };

        $scope.getLayout = function(clz) {
            var cntltype = {
                class: "form-control",
                type: "text",
                pattern: ""
            }
            switch (clz.class) {
                case "Boolean":
                    cntltype.type = "checkbox";
                    break;
                case "Double":
                    cntltype.class += " active";
                    cntltype.pattern = "/^\d{0,9}(\.\d{1,9})?$/"
                    break;
            }
            return cntltype;
        }

        $scope.getPattern = function(clz) {
            return $scope.getLayout(clz).pattern
        }

        $scope.getClass = function(clz) {
            return $scope.getLayout(clz).class
        }
        $scope.getType = function(clz) {
            return $scope.getLayout(clz).type
        }
    }

]);

/* Services */

var myAppServices = angular.module('myAppServices', ['ngResource']);

myAppServices.factory('GetSettings', ['$resource',
    function($resource) {
        // http://localhost:4040/Images/processing/json/?op=getarg&type=block&value=pluginName
        return $resource('test/arguments.json', {}, {});
    }
]);