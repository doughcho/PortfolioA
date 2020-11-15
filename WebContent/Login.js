/**
 * 
 */

'use strict'


$(document).ready(function() {         	// Performs just after creating DOM tree
//	alert('came in Login.ready()');
	var whatFunc = getCookie('whatFunc');		
	if (whatFunc == null || whatFunc == '0' || whatFunc == '9') {
	    // not came from home page (0: initial state; 9: other function is running)
		alert('Please use main page to login');
		window.close();
		return;
	}
	document.getElementById('userID').focus();
	$('#Login').click(function() {     // Resumes when 'Login'(id) button clicked
//		alert('Login button clicked');                      // for debugging
		var serializedValue = $('#LoginForm').serialize(); // Collect input data from 'LoginForm'(id) form
		$.ajax({                       // Performs all the async comm. part (Client <-> Server)
			type: 'post',
			url: './LoginServer',             // Url (servlet)
			data: serializedValue,			  // Sending data
//			contentType: 'application/xml',   // Data format to Server
//			dataType: 'text/json',            // Data format from Server
			timeout: 10000,                   // Setup maximum waiting time (if over; error: is processed)
			cache: false,                     // Works only when type is 'get'
			beforeSend: function() {          // Handles XMLHttpRequest object
//			  	alert('now sending...');
			},
        	success: function() {
//        		alert('success function is running');
        		var authStat = getCookie('authStat');
        		var userName = getCookie('userName');     		
//        		alert('authStat = ' + authStat + ', userName = ' + userName + ', whatFunc = ' + whatFunc);
     			switch (authStat) {
     				case '3':
						window.close();
						if (whatFunc == '2') {
							var pOption = 'width=1580, height=800, left=170, top=100, toolbar=no, menubar=no, scrollbars=no, location=no, status=no, resizable=no, fullscreen=no';
							window.open('OrderMgmt.html', 'OrdMgmtPopup', pOption);
						}
						setCookie('whatFunc', '9'); // to prevent external use of 'Login.html'
						break;
     				case '2':
     					alert('User ID inactive');
     					break;		
     				case '1':
     				case '0':
     					alert('User ID and/or password not match');
     					break;
     				default:
     					alert('Some abnormal situation happened');
     			}
			},
			error: function(xhr, status, error) {    // including timeout
				alert(error);
			},
			complete: function() {    // unconditionally process even met return()
			},			
		});            // end ajax
		return false;  // prevents the default action for that event
	});                // end Event listener
});		               // end (document).ready