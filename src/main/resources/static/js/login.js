const form = document.getElementById("loginForm");
const message = document.getElementById("message");

form.addEventListener("submit", async function (event) {
    event.preventDefault();

    const email = document.getElementById("email").value;
    const password = document.getElementById("password").value;

    try {
        const response = await fetch("/test/login", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                email: email,
                password: password
            })
        });

        if (!response.ok) {
            throw new Error("Request failed: HTTP " + response.status);;
        }

        const valid = await response.json();

        if (valid) {
            message.textContent = "Your inputs passed validation.";
        } else {
            message.textContent =
                "Enter a Gmail address and a nonblank password.";
        }
    }  catch (error) {
    console.error(error);
    message.textContent = "Unable to connect. Please try again.";
}
});