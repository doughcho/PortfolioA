/**
 * 
 */

'use strict'

$(document).ready(function() {     		// Performs just after creating DOM tree
//	alert('Came in index.ready()');
	setCookie('authStat', '0');			// to prevent unauthorized use
	setCookie('whatFunc', '0');			// to prevent external use of 'Login.html'
	
	var t1=document.getElementById('Btn0');
	t1.addEventListener('click', function() {
//		alert('Login button clicked');
		setCookie('authStat', '0');		// initialize authorization status
		setCookie('whatFunc', '1'); 	// after login, come back to main menu
		var pOption = 'width=500, height=250, left=710, top=200, toolbar=no, menubar=no, scrollbars=no, location=no, status=no, resizable=no, fullscreen=no';
		window.open('Login.html', 'LoginPopup', pOption);
	});

	var t2=document.getElementById('Btn1');
	t2.addEventListener('click', function() {
//		alert('Order Mgmt. button clicked');
		var authStat = getCookie('authStat');
		if (authStat == '3') {
//			alert('User Authenticated...');
			setCookie('whatFunc', '9');	// from main page (will be checked by the function)
			var pOption = 'width=1580, height=800, left=170, top=100, toolbar=no, menubar=no, scrollbars=no, location=no, status=no, resizable=no, fullscreen=no';
			window.open('OrderMgmt.html', 'OrdMgmtPopup', pOption);		
		} else {
			alert('You must login first');	
			setCookie('whatFunc', '2');	// after login, go to the function
			var pOption = 'width=500, height=250, left=710, top=200, toolbar=no, menubar=no, scrollbars=no, location=no, status=no, resizable=no, fullscreen=no';
			window.open('Login.html', 'LoginPopup', pOption);
		}
		return false;
	});
});