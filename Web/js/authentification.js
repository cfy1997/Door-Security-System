  /**
   * This javascript file will be called when user input its username and password for login.
   * If auth.() is successful, then the user will be redirect to the home page
   * Else it prompt the user for re-enter the information
   */      

      document.getElementById('form').addEventListener('submit', function(evt){ // listen to the form submit event
        evt.preventDefault();
        const name = document.getElementById("user").value; // get the username
        const pass = document.getElementById("pass").value; // get the password
        const auth = firebase.auth(); // initialize the Firebase authtification
        var info = document.getElementById("info"); 
        info.innerHTML = "connecting..."; // change the text
        auth.signInWithEmailAndPassword(name, pass).then(function(user){
             info.innerHTML = "successful!"; // change the text 
             firebase.auth().onAuthStateChanged(user => {
               if(user) {
                 window.location = 'html/home.html'; //After successful login, user will be redirected to home.html
                }
              });

            }).catch(function(error)
            {
              //Failed to login
              console.log('there was an error');
              var errorCode = error.code;
              var errorMessage = error.message;
              console.log(errorCode + ' - ' + errorMessage);
              info.innerHTML = "Login in failed, please contact admin for permission";
              swal("Oops!", "You may entered incorrect username or password, try again", "error");
            });
          });
