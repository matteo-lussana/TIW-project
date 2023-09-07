
{
	const loginButton = document.getElementById("login-button");
	loginButton.addEventListener("click", (e) => {

		var form = e.target.closest("form");

		if (form.checkValidity()) {

			makeCall("POST", 'CheckLogin', e.target.closest("form"), function(req) {
				if (req.readyState == XMLHttpRequest.DONE) {
					var message = req.responseText;
					if (req.status === 200) {
						sessionStorage.setItem('username', message);
						window.location.href = "HomePage.html";
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


