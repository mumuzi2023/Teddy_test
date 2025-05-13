'use strict';

/**
 * Login controller.
 */
angular.module('docs').controller('Login', function(Restangular, $scope, $rootScope, $state, $stateParams, $dialog, User, $translate, $uibModal, $http) {
  $scope.codeRequired = false;

  // Get the app configuration
  Restangular.one('app').get().then(function(data) {
    $rootScope.app = data;
  });

  // Login as guest
  $scope.loginAsGuest = function() {
    $scope.user = {
      username: 'guest',
      password: ''
    };
    $scope.login();
  };
  
  // Login
  $scope.login = function() {
    User.login($scope.user).then(function() {
      User.userInfo(true).then(function(data) {
        $rootScope.userInfo = data;
      });

      if($stateParams.redirectState !== undefined && $stateParams.redirectParams !== undefined) {
        $state.go($stateParams.redirectState, JSON.parse($stateParams.redirectParams))
          .catch(function() {
            $state.go('document.default');
          });
      } else {
        $state.go('document.default');
      }
    }, function(data) {
      if (data.data.type === 'ValidationCodeRequired') {
        // A TOTP validation code is required to login
        $scope.codeRequired = true;
      } else {
        // Login truly failed
        var title = $translate.instant('login.login_failed_title');
        var msg = $translate.instant('login.login_failed_message');
        var btns = [{result: 'ok', label: $translate.instant('ok'), cssClass: 'btn-primary'}];
        $dialog.messageBox(title, msg, btns);
      }
    });
  };
  $scope.registerRequest = {};

  $scope.openRegisterRequestModal = function () {
    document.getElementById("registerRequestModal").style.display = "block";
  };

  $scope.closeRegisterRequestModal = function () {
    document.getElementById("registerRequestModal").style.display = "none";
  };

  $scope.submitRegisterRequest = function() {
    $scope.submitted = true; // 标记为已提交，用于触发表单验证提示
    if ($scope.manualRegisterForm && $scope.manualRegisterForm.$invalid) {
      alert($translate.instant('login.register_request_modal.validation_error'));
      return; // 如果表单无效，则停止提交
    }
    var postData = {
      username: $scope.registerRequest.username,
      email: $scope.registerRequest.email
    };
    $http.post('../api/register', postData)
      .then(function(response) {
        alert("Request submitted"); // 或者更详细的成功消息
        $scope.closeRegisterRequestModal(); // 关闭模态框
        $scope.registerRequest = {}; // 清空表单模型
        $scope.submitted = false; // 重置提交状态
      })
      .catch(function(errorResponse) {
        var errorMsg = "Fail to request, please submit later."; // 默认错误消息
        if (errorResponse.data && errorResponse.data.message) {
             errorMsg = errorResponse.data.message;
        }
        alert(errorMsg); // 显示错误
        console.error("Registration request failed:", errorResponse); // 仍然在控制台记录详细错误
      });
  }; // 结束 submitRegisterRequest 函数
  // Password lost
  $scope.openPasswordLost = function () {
    $uibModal.open({
      templateUrl: 'partial/docs/passwordlost.html',
      controller: 'ModalPasswordLost'
    }).result.then(function (username) {
      if (username === null) {
        return;
      }

      // Send a password lost email
      Restangular.one('user').post('password_lost', {
        username: username
      }).then(function () {
        var title = $translate.instant('login.password_lost_sent_title');
        var msg = $translate.instant('login.password_lost_sent_message', { username: username });
        var btns = [{result: 'ok', label: $translate.instant('ok'), cssClass: 'btn-primary'}];
        $dialog.messageBox(title, msg, btns);
      }, function () {
        var title = $translate.instant('login.password_lost_error_title');
        var msg = $translate.instant('login.password_lost_error_message');
        var btns = [{result: 'ok', label: $translate.instant('ok'), cssClass: 'btn-primary'}];
        $dialog.messageBox(title, msg, btns);
      });
    });
  };
});