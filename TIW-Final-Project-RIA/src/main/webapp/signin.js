{
	const signinButton = document.getElementById("signinButton");
	signinButton.addEventListener("click", (e) => {
		var form = e.target.closest("form");

		if (form.checkValidity()) {

			makeCall("POST", 'SignIn', form, function(req) {
				if (req.readyState === XMLHttpRequest.DONE) {
					var message = req.responseText;
					if (req.status === 200) {
						window.location.href = "LoginPage.html";
					}
					else {
						document.getElementById("errorMsg").textContent = message;
					}
				}
			});
		}
		else {
			form.reportValidity();
		}

	});
};