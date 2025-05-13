'use strict';

/**
 * Controller for managing registration requests.
 */
angular.module('docs')
  .controller('SettingsRegisterRequestController', function ($scope, $http, $translate, $window) { // Added $window for confirm
    $scope.registrations = [];
    $scope.loading = true;
    $scope.loadError = false;
    $scope.loadErrorMessage = '';

    function fetchRegistrations() {
      $scope.loading = true;
      $scope.loadError = false;
      $http.get('../api/register') // Ensure this API path is correct
        .then(function(response) {
          if (response.data && angular.isArray(response.data.registrations)) {
            $scope.registrations = response.data.registrations.map(function(reg) {
              reg.isProcessing = false; // For general processing state on the row
              reg.isProcessingConfirm = false;
              reg.isProcessingReject = false;
              return reg;
            });
          } else {
            $scope.registrations = [];
            console.warn('Registrations data not in expected format:', response.data);
          }
          $scope.loading = false;
        })
        .catch(function(error) {
          $scope.loading = false;
          $scope.loadError = true;
          $scope.loadErrorMessage = error.data ? (error.data.message || 'Server error') : 'Could not connect to server';
          console.error('Error fetching registrations:', error);
        });
    }

    $scope.confirmRegistration = function(registration) {
      if (registration.confirmed || registration.isProcessing) {
        return;
      }
      registration.isProcessing = true;
      registration.isProcessingConfirm = true;
      var postData = {
        username: registration.username,
        email: registration.email
      };
      $http.put('../api/register/' + registration.id + '/'+encodeURIComponent(registration.username)+'/'+encodeURIComponent(registration.email)+'/confirm')
        .then(function(response) {
          // Update the specific registration in the list with data from server
          var index = $scope.registrations.findIndex(r => r.id === registration.id);
          if (index !== -1 && response.data) {
             angular.extend($scope.registrations[index], response.data);
             $scope.registrations[index].isProcessing = false;
             $scope.registrations[index].isProcessingConfirm = false;
          } else {
            fetchRegistrations(); // Fallback to reload all if update is tricky
          }
        })
        .catch(function(error) {
          console.error('Error confirming registration for ' + registration.id + ':', error);
          // Use $translate for user-facing messages
          $window.alert($translate.instant('settings.registrations.error_confirm_message') + (error.data && error.data.message ? ': ' + error.data.message : ''));
          registration.isProcessing = false;
          registration.isProcessingConfirm = false;
        });
    };

    $scope.rejectRegistration = function(registration) {
      if (registration.isProcessing) {
        return;
      }
      // Use $translate for the confirmation message
      var confirmMessage = $translate.instant('settings.registrations.confirm_reject_prompt', { username: registration.username });
      if (!$window.confirm(confirmMessage)) {
        return;
      }

      registration.isProcessing = true;
      registration.isProcessingReject = true;

      $http.delete('../api/register/' + registration.id)
        .then(function() {
          // Remove the registration from the list on success
          $scope.registrations = $scope.registrations.filter(function(r) {
            return r.id !== registration.id;
          });
          // No need to reset processing flags for a deleted item
        })
        .catch(function(error) {
          console.error('Error rejecting registration for ' + registration.id + ':', error);
          $window.alert($translate.instant('settings.registrations.error_reject_message') + (error.data && error.data.message ? ': ' + error.data.message : ''));
          registration.isProcessing = false;
          registration.isProcessingReject = false;
        });
    };

    // Initial load of registrations
    fetchRegistrations();
  });