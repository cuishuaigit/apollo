'use strict';

angular.module('apollo')
  .controller('signupCtrl', ['apolloApiService', '$scope',
                                    '$timeout' , '$state', 'growl', 'usSpinnerService',
            function (apolloApiService, $scope, $timeout, $state, growl, usSpinnerService) {

            $scope.signup = function() {
                apolloApiService.signup($scope.signup_email, $scope.first_name, $scope.last_name, $scope.password).then(
                function(response){

                    growl.success("Successfully created new user!", {ttl:7000});
                    $scope.buttonDisabled = true;
                },
                function(error){
                    growl.error("Got from apollo API: " + error.status + " (" + error.statusText + ")", {ttl: 7000});
                })
            }
}]);